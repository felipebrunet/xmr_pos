package cl.icripto.xmrpos.ui

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
import cl.icripto.xmrpos.network.fetchFirstTransactionDetails
import cl.icripto.xmrpos.network.fetchMempoolHashes
// import cl.icripto.xmrpos.network.fetchMempoolHashes
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
            maxMinorIndex = "0",
            restaurantName = "",
            pin = ""
        )
    )
    val context = LocalContext.current

    var derivedSubaddress by remember { mutableStateOf("") }
    var xmrAmount by remember { mutableStateOf<String?>(null) }
    var publicSpendKey by remember { mutableStateOf<String?>(null) }

    // Derive the subaddress when the screen is shown
    LaunchedEffect(settings) {
        if (settings.moneroAddress.isNotEmpty() && settings.secretViewKey.isNotEmpty()) {
            try {
                val subaddress = MoneroSubaddress().getAddressFinal(
                    baseAddress = settings.moneroAddress,
                    secretVk = settings.secretViewKey,
                    major = 0, // Hardcoded for testing
                    minor = 1  // Hardcoded for testing
                )
                derivedSubaddress = subaddress
                Toast.makeText(context, "Subaddress: $subaddress", Toast.LENGTH_LONG).show()

                publicSpendKey = getPublicSpendKeyHex(subaddress)
                Log.d("PaymentScreen", "Public Spend Key: $publicSpendKey")

            } catch (e: Exception) {
                Toast.makeText(context, "Error deriving subaddress: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit, amount, settings.currency) {
        Log.d("PaymentScreen", "PaymentScreen LaunchedEffect(Unit) is running.")
        val price = fetchXmrPrice(settings.currency)
        if (price != null) {
            val amountAsDouble = amount.toDoubleOrNull()
            if (amountAsDouble != null) {
                val calculatedXmrAmount = amountAsDouble / price
//                xmrAmount = 0.06881

                xmrAmount = BigDecimal(calculatedXmrAmount).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(3000)
        val mempoolHashesResponse = fetchMempoolHashes(settings.moneroServerUrl)
        if (mempoolHashesResponse != null) {
            Log.d("PaymentScreen", "Mempool hashes: $mempoolHashesResponse")

            // Hardcoded transaction hash for testing
            val testTxHash = "e7d34036f0fc005b3295e4388b1fff3a6b74354dc2c8883ac5f35680468a3721"
            val testTxHashes = listOf(testTxHash)

            delay(500)
            val txDetails = fetchFirstTransactionDetails(settings.moneroServerUrl, testTxHashes)
//            val txDetails = fetchFirstTransactionDetails(settings.moneroServerUrl, mempoolHashesResponse.txHashes)
            if (txDetails != null) {
                val txPublicKey = getTxPublicKeyFromExtra(txDetails.extra)
                val keys = txDetails.vout.map { it.target.taggedKey.key }
                val amounts = txDetails.rctSignatures.ecdhInfo.map { it.amount }
                Log.d("PaymentScreen", "Extra: ${txDetails.extra}")
                Log.d("PaymentScreen", "Amounts: $amounts")
                Log.d("PaymentScreen", "Keys: $keys")
                Log.d("PaymentScreen", "Tx Public Key: $txPublicKey")

                if (txPublicKey != null && publicSpendKey != null) {
                    val (match, index) = isTxContainingPayment(
                        privateViewKeyHex = settings.secretViewKey,
                        publicSpendKeyHex = publicSpendKey!!,
                        txPublicKeyHex = txPublicKey,
                        outputPubkeysHex = keys
                    )
                    Log.d("PaymentScreen", "Payment found in transaction: $match, index: $index")

                    verifyAmount(settings.secretViewKey, publicSpendKey!!, amounts[index], index, 0.06881.toDouble())
//                    verifyAmount(settings.secretViewKey, publicSpendKey!!, amounts[index], index, xmrAmount!!.toDouble())
                } else {
                    Log.w("PaymentScreen", "Cannot check payment, missing txPublicKey or publicSpendKey")
                }
            } else {
                Log.w("PaymentScreen", "Could not retrieve transaction details for test hash")
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
            Text(stringResource(R.string.payment_screen_amount_to_pay, amount, settings.currency), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            xmrAmount?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("XMR: $it", fontSize = 16.sp, fontWeight = FontWeight.Normal)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = stringResource(R.string.payment_screen_qr_code_description), modifier = Modifier.size(250.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.back_button)) }
        }
    }
}
