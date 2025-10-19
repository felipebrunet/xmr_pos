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

    // Derive the subaddress when the screen is shown
    LaunchedEffect(settings) {
        if (settings.moneroAddress.isNotEmpty() && settings.secretViewKey.isNotEmpty()) {
            try {
                val subaddress = MoneroSubaddress().getAddressFinal(
                    baseAddress = settings.moneroAddress,
                    secretVk = settings.secretViewKey,
                    major = settings.majorIndex.toIntOrNull() ?: 1,
                    minor = 2 // Hardcoded for now
                )
                Toast.makeText(context, "Subaddress: $subaddress", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val moneroAddress = if (settings.moneroAddress.isNotEmpty()) settings.moneroAddress else "44AFFq5kSiGBoZ4NMDwYtN18obc8AemS33DBLWs3H7otXft3XjrpDtQGv7SqSsaBYBb98uNbr2VBBEt7f2wfn3RVGQBEP3A"
    val moneroUri = "monero:$moneroAddress?tx_amount=$amount"
    val qrCodeBitmap = QRCode.from(moneroUri).withSize(1024, 1024).bitmap()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF8E1)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.payment_screen_amount_to_pay, amount, settings.currency), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Image(bitmap = qrCodeBitmap.asImageBitmap(), contentDescription = stringResource(R.string.payment_screen_qr_code_description), modifier = Modifier.size(250.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.back_button)) }
        }
    }
}