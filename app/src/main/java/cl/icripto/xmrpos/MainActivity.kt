package cl.icripto.xmrpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.icripto.xmrpos.data.SettingsRepository
import cl.icripto.xmrpos.ui.PaymentScreen
import cl.icripto.xmrpos.ui.PinScreen
import cl.icripto.xmrpos.ui.PosScreen
import cl.icripto.xmrpos.ui.SettingsScreen
import cl.icripto.xmrpos.ui.UnlockScreen
import cl.icripto.xmrpos.ui.theme.XmrPosTheme
import cl.icripto.xmrpos.viewmodel.SettingsViewModel
import cl.icripto.xmrpos.viewmodel.SettingsViewModelFactory

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
                    composable("pin") { PinScreen(navController, settingsViewModel) }
                    composable("unlock") { UnlockScreen(navController, settingsViewModel) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    XmrPosTheme {
        val context = LocalContext.current
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(SettingsRepository(context)))
        PosScreen(rememberNavController(), settingsViewModel)
    }
}
