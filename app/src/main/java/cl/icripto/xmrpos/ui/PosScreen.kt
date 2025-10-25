package cl.icripto.xmrpos.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import cl.icripto.xmrpos.R
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.network.testServerUrl
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PosScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    var amount by remember { mutableStateOf("") }
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
    var showTipDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showTipDialog) {
        TipDialog(
            onDismiss = { showTipDialog = false },
            onTipSelected = { tipPercentage ->
                val originalAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val tip = originalAmount.multiply(tipPercentage)
                val finalAmount = originalAmount.add(tip).setScale(2, RoundingMode.HALF_UP)
                scope.launch {
                    if (testServerUrl(settings.moneroServerUrl)) {
                        navController.navigate("payment/${finalAmount.toPlainString()}")
                    } else {
                        Toast.makeText(context, "Server is unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
                showTipDialog = false
            }
        )
    }

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
                Button(
                    onClick = {
                        if (settings.pin.isEmpty()) {
                            navController.navigate("settings")
                        } else {
                            navController.navigate("unlock")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                ) { Text(stringResource(R.string.settings_button)) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = amount.ifEmpty { "0" }, fontSize = 32.sp, modifier = Modifier.weight(1f).border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)).padding(vertical = 8.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /*TODO*/ }) { Text(settings.currency) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Numpad(onKeyPress = { buttonText ->
                amount = when (buttonText) {
                    "DELETE" -> if (amount.isNotEmpty()) amount.dropLast(1) else ""
                    "." -> {
                        if (amount.isEmpty()) "0."
                        else if (!amount.contains(".")) "$amount."
                        else amount
                    }
                    else -> {
                        if (amount == "0") buttonText
                        else amount + buttonText
                    }
                }
            })
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amountBigDecimal = amount.toBigDecimalOrNull()
                    if (settings.moneroAddress.isEmpty() || settings.secretViewKey.isEmpty()) {
                        Toast.makeText(context, "Please set Base Address and Secret View Key in settings.", Toast.LENGTH_LONG).show()
                    } else if (amountBigDecimal != null && amountBigDecimal > BigDecimal.ZERO) {
                        scope.launch {
                            if (testServerUrl(settings.moneroServerUrl)) {
                                if (settings.tipsEnabled) {
                                    showTipDialog = true
                                } else {
                                    val finalAmount = amount.ifEmpty { "0" }
                                    navController.navigate("payment/$finalAmount")
                                }
                            } else {
                                Toast.makeText(context, "Server is unavailable", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) { Text(stringResource(R.string.pay_button), fontSize = 20.sp) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("XMR POS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val context = LocalContext.current
            Text(
                text = "Open Source",
                fontSize = 16.sp,
                color = Color.Blue,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val url = "https://github.com/felipebrunet/xmr_pos"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TipDialog(onDismiss: () -> Unit, onTipSelected: (BigDecimal) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8E1)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.tip_dialog_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Button(onClick = { onTipSelected(BigDecimal.ZERO) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(stringResource(R.string.tip_dialog_no_tip), fontSize = 12.sp) }
                    Button(onClick = { onTipSelected(BigDecimal("0.05")) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("5%", fontSize = 12.sp) }
                    Button(onClick = { onTipSelected(BigDecimal("0.10")) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("10%", fontSize = 12.sp) }
                    Button(onClick = { onTipSelected(BigDecimal("0.20")) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("20%", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun Numpad(onKeyPress: (String) -> Unit) {
    val buttons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "DELETE")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .background(Color(0xFF4CAF50), shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(buttons) {
            TextButton(
                onClick = { onKeyPress(it) },
                modifier = Modifier.height(80.dp)
            ) {
                Text(
                    text = if (it == "DELETE") stringResource(R.string.numpad_delete) else it,
                    color = Color.White,
                    fontSize = if (it == "DELETE") 16.sp else 24.sp,
                    softWrap = false
                )
            }
        }
    }
}
