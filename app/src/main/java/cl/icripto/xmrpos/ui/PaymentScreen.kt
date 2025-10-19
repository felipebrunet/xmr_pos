package cl.icripto.xmrpos.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import net.glxn.qrgen.android.QRCode
import java.text.DecimalFormat

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
    val xmrPrice by settingsViewModel.xmrPrice.collectAsState(initial = null)
    val context = LocalContext.current

    var derivedSubaddress by remember { mutableStateOf<String?>(null) }
    var xmrAmount by remember { mutableStateOf<Double?>(null) }

    // Derive the subaddress when the screen is shown
    LaunchedEffect(settings) {
        if (settings.moneroAddress.isNotEmpty() && settings.secretViewKey.isNotEmpty()) {
            try {
                val subaddress = MoneroSubaddress().getAddressFinal(
                    baseAddress = settings.moneroAddress,
                    secretVk = settings.secretViewKey,
                    major = settings.majorIndex.toIntOrNull() ?: 1,
                    minor = 1 // Hardcoded for now
                )
                derivedSubaddress = subaddress
                Toast.makeText(context, "Subaddress: $subaddress", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error deriving subaddress: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        settingsViewModel.fetchXmrPrice(settings.currency)
    }

    LaunchedEffect(amount, xmrPrice) {
        val fiatAmount = amount.toDoubleOrNull()
        if (fiatAmount != null && xmrPrice != null) {
            xmrAmount = fiatAmount / xmrPrice!!
        }
    }

    val addressForQr = derivedSubaddress ?: settings.moneroAddress
    val moneroUri = if (xmrAmount != null) {
        val formattedXmrAmount = String.format("%.12f", xmrAmount) // Format to 12 decimal places
        "monero:$addressForQr?tx_amount=$formattedXmrAmount"
    } else {
        "monero:$addressForQr"
    }
    val qrCodeBitmap = QRCode.from(moneroUri).withSize(1024, 1024).bitmap()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF8E1)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.payment_screen_amount_to_pay, amount, settings.currency), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            xmrAmount?.let { 
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(~${DecimalFormat("0.000000000000").format(it)} XMR)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = stringResource(R.string.payment_screen_qr_code_description), modifier = Modifier.size(250.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.back_button)) }
        }
    }
}
