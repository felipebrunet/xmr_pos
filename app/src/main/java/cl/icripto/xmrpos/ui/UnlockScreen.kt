package cl.icripto.xmrpos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cl.icripto.xmrpos.R
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(navController: NavController, viewModel: SettingsViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    val settings by viewModel.settingsFlow.collectAsState(initial = AppSettings("",false,"","","","","", "", ""))
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == settings.pin) {
                navController.navigate("settings") { 
                    popUpTo("unlock") { inclusive = true } 
                }
            } else {
                coroutineScope.launch {
                    isError = true
                    delay(500)
                    enteredPin = ""
                    isError = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isError) Color.Red.copy(alpha = 0.3f) else Color(0xFFFFF8E1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(stringResource(R.string.unlock_screen_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "* ".repeat(enteredPin.length).trim(),
                    fontSize = 48.sp,
                    letterSpacing = 8.sp,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
                PinNumpad(onPinChange = { newPin ->
                    if (newPin.length <= 4) {
                        enteredPin = newPin
                    }
                }, currentPin = enteredPin)
            }

            Button(onClick = { navController.popBackStack() }) {
                Text(stringResource(R.string.back_button))
            }
        }
    }
}
