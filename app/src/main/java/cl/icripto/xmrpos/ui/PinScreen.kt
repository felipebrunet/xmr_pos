package cl.icripto.xmrpos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cl.icripto.xmrpos.viewmodel.SettingsViewModel

@Composable
fun PinScreen(navController: NavController, viewModel: SettingsViewModel) {
    var pin by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFF8E1) // Pale Yellow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text("Enter a 4-digit PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // PIN display
                Text(
                    text = "* ".repeat(pin.length).trim(),
                    fontSize = 48.sp,
                    letterSpacing = 8.sp,
                    modifier = Modifier.padding(vertical = 32.dp)
                )

                // Numpad for PIN
                PinNumpad(onPinChange = { newPin ->
                    if (newPin.length <= 4) {
                        pin = newPin
                    }
                }, currentPin = pin)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(stringResource(R.string.back_button))
                }
                Button(
                    onClick = { 
                        viewModel.savePin(pin)
                        navController.popBackStack() 
                    },
                    enabled = pin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.save_button))
                }
            }
        }
    }
}

@Composable
fun PinNumpad(onPinChange: (String) -> Unit, currentPin: String) {
    val buttons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DELETE")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.width(240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(buttons.size) { index ->
            val buttonText = buttons[index]
            if (buttonText.isNotEmpty()) {
                TextButton(
                    onClick = {
                        val newPin = when (buttonText) {
                            "DELETE" -> if (currentPin.isNotEmpty()) currentPin.dropLast(1) else ""
                            else -> currentPin + buttonText
                        }
                        onPinChange(newPin)
                    },
                    modifier = Modifier.height(80.dp)
                ) {
                    Text(
                        text = if (buttonText == "DELETE") stringResource(R.string.numpad_delete) else buttonText,
                        fontSize = if (buttonText == "DELETE") 12.sp else 28.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}
