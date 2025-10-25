package cl.icripto.xmrpos.ui

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cl.icripto.xmrpos.R
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.monero.MoneroSubaddress
import cl.icripto.xmrpos.monero.getPublicSpendKeyHex
import cl.icripto.xmrpos.monero.isTxContainingPayment
import cl.icripto.xmrpos.monero.verifyAmount
import cl.icripto.xmrpos.network.fetchTransactionDetails
import cl.icripto.xmrpos.network.fetchMempoolHashes
import cl.icripto.xmrpos.network.fetchLastBlockHashes
import cl.icripto.xmrpos.network.getTxPublicKeyFromExtra
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.glxn.qrgen.android.QRCode
import java.math.BigDecimal
import java.math.RoundingMode

private val httpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}

private suspend fun fetchXmrPrice(currency: String): Double? {
    Log.d("PaymentScreen", "Attempting to fetch XMR price...")
    return try {
        val response: JsonObject = httpClient.get("https://api.coingecko.com/api/v3/simple/price?ids=monero&vs_currencies=${currency.lowercase()}").body()
        val price = response["monero"]?.jsonObject?.get(currency.lowercase())?.jsonPrimitive?.double
        Log.d("PaymentScreen", "Successfully fetched XMR price: $price")
        price
    } catch (e: Exception) {
        Log.e("PaymentScreen", "Error fetching XMR price: ${e.message}", e)
        null
    }
}

@Composable
fun PaymentScreen(navController: NavController, amount: String, settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settingsFlow.collectAsState(
        initial = AppSettings(
            currency = "USD",
            tipsEnabled = false,
            moneroServerUrl = "",
            moneroAddress = "",
            secretViewKey = "",
            majorIndex = "1",
            maxMinorIndex = "10",
            restaurantName = "",
            pin = ""
        )
    )
    val context = LocalContext.current

    var derivedSubaddress by remember { mutableStateOf("") }
    var xmrAmount by remember { mutableStateOf<String?>(null) }
    var publicSpendKey by remember { mutableStateOf<String?>(null) }
    var paymentSuccess by remember { mutableStateOf(false) }

    // Derive the subaddress when the screen is shown
    LaunchedEffect(settings) {
        if (settings.moneroAddress.isNotEmpty() && settings.secretViewKey.isNotEmpty()) {
            try {
                val sharedPref = context.getSharedPreferences("payment_prefs", Context.MODE_PRIVATE)
                val lastMinorIndex = sharedPref.getInt("last_minor_index", 0)
                val nextMinorIndex = lastMinorIndex + 1

                val subaddress = MoneroSubaddress().getAddressFinal(
                    baseAddress = settings.moneroAddress,
                    secretVk = settings.secretViewKey,
                    major = settings.majorIndex.toInt(),
                    minor = nextMinorIndex
                )

                with(sharedPref.edit()) {
                    putInt("last_minor_index", nextMinorIndex)
                    apply()
                }
                derivedSubaddress = subaddress
//                Toast.makeText(context, "Subaddress: $subaddress", Toast.LENGTH_LONG).show()

                publicSpendKey = getPublicSpendKeyHex(subaddress)
                Log.d("PaymentScreen", "Public Spend Key: $publicSpendKey")

            } catch (e: Exception) {
                Toast.makeText(context, "Error deriving subaddress: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(amount, settings.currency) {
        if (settings.currency.equals("XMR", ignoreCase = true)) {
            xmrAmount = try {
                BigDecimal(amount).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            } catch (e: NumberFormatException) {
                Log.e("PaymentScreen", "Invalid amount format for XMR: $amount", e)
                null
            }
        } else {
            val price = fetchXmrPrice(settings.currency)
            if (price != null) {
                val amountBigDecimal = amount.toBigDecimalOrNull()
                if (amountBigDecimal != null) {
                    val priceBigDecimal = BigDecimal(price.toString())
                    xmrAmount = amountBigDecimal.divide(priceBigDecimal, 12, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString()
                } else {
                    Log.e("PaymentScreen", "Invalid amount format: $amount")
                }
            } else {
                Log.e("PaymentScreen", "Error fetching price for ${settings.currency}")
            }
        }
    }

    LaunchedEffect(publicSpendKey, xmrAmount) {
        if (publicSpendKey == null || xmrAmount == null) return@LaunchedEffect

        while (!paymentSuccess) {
            val mempoolHashes = fetchMempoolHashes(settings.moneroServerUrl)?.txHashes ?: emptyList()
            delay(100) // Add a small delay to avoid overwhelming the server
            val lastBlockHashes = fetchLastBlockHashes(settings.moneroServerUrl)
            val allHashes = (mempoolHashes + lastBlockHashes).distinct()
            Log.d("PaymentScreen", "All hashes: $allHashes")

            if (allHashes.isNotEmpty()) {
                Log.d("PaymentScreen", "Total hashes to check: ${allHashes.size}")

                val txsDetails = fetchTransactionDetails(settings.moneroServerUrl, allHashes)
                if (txsDetails.isNotEmpty()) {
                    for (txDetails in txsDetails) {
                        val txPublicKey = getTxPublicKeyFromExtra(txDetails.extra)
                        val keys = txDetails.vout.map { it.target.taggedKey.key }
                        val amounts = txDetails.rctSignatures.ecdhInfo.map { it.amount }

                        if (txPublicKey != null) {
                            val (match, index) = isTxContainingPayment(
                                privateViewKeyHex = settings.secretViewKey,
                                publicSpendKeyHex = publicSpendKey!!,
                                txPublicKeyHex = txPublicKey,
                                outputPubkeysHex = keys
                            )

                            if (match) {
                                Log.d("PaymentScreen", "Payment found in transaction: $match, index: $index")
                                paymentSuccess = verifyAmount(
                                    settings.secretViewKey,
                                    txPublicKey,
                                    amounts[index],
                                    index,
                                    xmrAmount!!.toDouble()
                                )
                                if (paymentSuccess) break
                            } else {
                                Log.d("PaymentScreen", "Payment not found in transaction with tx_pubkey: $txPublicKey")
                            }
                        } else {
                            Log.w("PaymentScreen", "Cannot check payment, missing txPublicKey")
                        }
                    }
                } else {
                    Log.w("PaymentScreen", "Could not retrieve transaction details for any hash")
                }
            }
            if (!paymentSuccess) {
                delay(3000)
            }
        }
    }

    val moneroAddressForQr = derivedSubaddress.ifEmpty { "44AFFq5kSiGBoZ4NMDwYtN18obc8AemS33DBLWs3H7otXft3XjrpDtQGv7SqSsaBYBb98uNbr2VBBEt7f2wfn3RVGQBEP3A" }
    val moneroUri = if (xmrAmount != null) {
        "monero:$moneroAddressForQr?tx_amount=$xmrAmount"
    } else {
        "monero:$moneroAddressForQr"
    }
    val qrCodeBitmap = QRCode.from(moneroUri).withSize(1024, 1024).bitmap()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF8E1)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (paymentSuccess) {
                Text(stringResource(R.string.payment_received), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Image(painter = painterResource(id = R.drawable.green_check), contentDescription = stringResource(R.string.payment_successful), modifier = Modifier.size(250.dp))
            } else {
                Text(stringResource(R.string.payment_screen_amount_to_pay, amount, settings.currency), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                xmrAmount?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("XMR: $it", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = stringResource(R.string.payment_screen_qr_code_description), modifier = Modifier.size(250.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Monero URI", moneroUri)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "QR Code data copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.copy_qr_code))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.back_button)) }
        }
    }
}
