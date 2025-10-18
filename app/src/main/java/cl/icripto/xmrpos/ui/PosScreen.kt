package cl.icripto.xmrpos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cl.icripto.xmrpos.R
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import cl.icripto.xmrpos.data.AppSettings

@Composable
fun PosScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    var amount by remember { mutableStateOf("") }
    val settings by settingsViewModel.settingsFlow.collectAsState(initial = AppSettings("",false,"","","","","",""))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFF8E1) // Pale Yellow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.app_name), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.fillMaxWidth().background(Color(0xFFFF9800), shape = RoundedCornerShape(8.dp)).padding(vertical = 12.dp), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = settings.restaurantName.ifEmpty { stringResource(R.string.restaurant_name_placeholder) }, modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)).padding(12.dp), textAlign = TextAlign.Center, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { navController.navigate("settings") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))) { Text(stringResource(R.string.settings_button)) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (amount.isEmpty()) "0" else amount, fontSize = 32.sp, modifier = Modifier.weight(1f).border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)).padding(vertical = 8.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /*TODO*/ }) { Text(settings.currency) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Numpad(onAmountChange = { newAmount -> amount = newAmount }, currentAmount = amount)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { val finalAmount = if (amount.isEmpty()) "0" else amount; navController.navigate("payment/$finalAmount") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)), contentPadding = PaddingValues(vertical = 16.dp)) { Text(stringResource(R.string.pay_button), fontSize = 20.sp) }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun Numpad(onAmountChange: (String) -> Unit, currentAmount: String) {
    val buttons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "DELETE")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .background(Color(0xFF4CAF50), shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(buttons.size) { index ->
            val buttonText = buttons[index]
            TextButton(
                onClick = {
                    val newAmount = when (buttonText) {
                        "DELETE" -> if (currentAmount.isNotEmpty()) currentAmount.dropLast(1) else ""
                        "." -> if (currentAmount.isNotEmpty() && !currentAmount.contains(".")) currentAmount + "." else currentAmount
                        else -> currentAmount + buttonText
                    }
                    onAmountChange(newAmount)
                }
            ) {
                Text(
                    text = if (buttonText == "DELETE") stringResource(R.string.numpad_delete) else buttonText,
                    color = Color.White,
                    fontSize = if (buttonText == "DELETE") 16.sp else 24.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}
