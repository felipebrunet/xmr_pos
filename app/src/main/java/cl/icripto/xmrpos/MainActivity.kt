package cl.icripto.xmrpos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.data.SettingsRepository
import cl.icripto.xmrpos.ui.theme.XmrPosTheme
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XmrPosTheme {
                val settingsRepository = SettingsRepository(applicationContext)
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(settingsRepository)
                )
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "pos") {
                    composable("pos") { PosScreen(navController, settingsViewModel) }
                    composable(
                        "payment/{amount}",
                        arguments = listOf(navArgument("amount") { type = NavType.StringType })
                    ) { backStackEntry ->
                        PaymentScreen(
                            navController = navController,
                            amount = backStackEntry.arguments?.getString("amount") ?: "0",
                            settingsViewModel = settingsViewModel
                        )
                    }
                    composable("settings") { SettingsScreen(navController, settingsViewModel) }
                }
            }
        }
    }
}

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

@Composable
fun PaymentScreen(navController: NavController, amount: String, settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settingsFlow.collectAsState(initial = AppSettings("",false,"","","","","",""))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val settings by viewModel.settingsFlow.collectAsState(initial = AppSettings("",false,"","","","","",""))
    val scope = rememberCoroutineScope()

    var currency by remember(settings.currency) { mutableStateOf(settings.currency) }
    var tipsEnabled by remember(settings.tipsEnabled) { mutableStateOf(settings.tipsEnabled) }
    var moneroServerUrl by remember(settings.moneroServerUrl) { mutableStateOf(settings.moneroServerUrl) }
    var moneroAddress by remember(settings.moneroAddress) { mutableStateOf(settings.moneroAddress) }
    var secretViewKey by remember(settings.secretViewKey) { mutableStateOf(settings.secretViewKey) }
    var majorIndex by remember(settings.majorIndex) { mutableStateOf(settings.majorIndex) }
    var maxMinorIndex by remember(settings.maxMinorIndex) { mutableStateOf(settings.maxMinorIndex) }
    var restaurantName by remember(settings.restaurantName) { mutableStateOf(settings.restaurantName) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF8E1)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.settings_screen_title), fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            SettingRow(stringResource(R.string.settings_currency_label)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(value = currency, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("USD") }, onClick = { currency = "USD"; expanded = false })
                        DropdownMenuItem(text = { Text("EUR") }, onClick = { currency = "EUR"; expanded = false })
                        DropdownMenuItem(text = { Text("XMR") }, onClick = { currency = "XMR"; expanded = false })
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_tips_label), fontSize = 16.sp)
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = tipsEnabled, onCheckedChange = { tipsEnabled = it })
                }
                Button(
                    onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935), // Material Red 600
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_pin_button))
                }
            }

            SettingTextField(label = stringResource(R.string.settings_server_url_label), value = moneroServerUrl, onValueChange = { moneroServerUrl = it })
            SettingTextField(label = stringResource(R.string.settings_base_address_label), value = moneroAddress, onValueChange = { moneroAddress = it })
            SettingTextField(label = stringResource(R.string.settings_view_key_label), value = secretViewKey, onValueChange = { secretViewKey = it })
            SettingTextField(label = stringResource(R.string.settings_major_index_label), value = majorIndex, onValueChange = { majorIndex = it }, keyboardType = KeyboardType.Number)
            SettingTextField(label = stringResource(R.string.settings_minor_index_label), value = maxMinorIndex, onValueChange = { maxMinorIndex = it }, keyboardType = KeyboardType.Number)
            SettingTextField(label = stringResource(R.string.settings_restaurant_name_label), value = restaurantName, onValueChange = { restaurantName = it })

            Spacer(modifier = Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)), modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text(stringResource(R.string.go_back_button)) }
                Button(onClick = { 
                    scope.launch {
                        viewModel.saveSettings(AppSettings(currency, tipsEnabled, moneroServerUrl, moneroAddress, secretViewKey, majorIndex, maxMinorIndex, restaurantName))
                        navController.popBackStack()
                    }
                 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text(stringResource(R.string.save_button)) }
            }
        }
    }
}

@Composable
fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp)
        content()
    }
}

@Composable
fun SettingTextField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced padding
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settingsFlow = repository.settingsFlow

    suspend fun saveSettings(settings: AppSettings) {
        repository.saveSettings(settings)
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Preview(showBackground = true)
@Composable
fun PosScreenPreview() {
    XmrPosTheme {
        val context = LocalContext.current
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(SettingsRepository(context)))
        PosScreen(rememberNavController(), settingsViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentScreenPreview() {
    XmrPosTheme {
        val context = LocalContext.current
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(SettingsRepository(context)))
        PaymentScreen(rememberNavController(), "123.45", settingsViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    XmrPosTheme {
        val context = LocalContext.current
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(SettingsRepository(context)))
        SettingsScreen(rememberNavController(), settingsViewModel)
    }
}
