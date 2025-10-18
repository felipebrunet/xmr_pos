package cl.icripto.xmrpos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cl.icripto.xmrpos.R
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import cl.icripto.xmrpos.data.AppSettings
import kotlinx.coroutines.launch

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
