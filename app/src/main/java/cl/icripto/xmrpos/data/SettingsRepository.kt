package cl.icripto.xmrpos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val currency: String,
    val tipsEnabled: Boolean,
    val moneroServerUrl: String,
    val moneroAddress: String,
    val secretViewKey: String,
    val majorIndex: String,
    val maxMinorIndex: String,
    val restaurantName: String
)

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val TIPS_ENABLED = booleanPreferencesKey("tips_enabled")
        val MONERO_SERVER_URL = stringPreferencesKey("monero_server_url")
        val MONERO_ADDRESS = stringPreferencesKey("monero_address")
        val SECRET_VIEW_KEY = stringPreferencesKey("secret_view_key")
        val MAJOR_INDEX = stringPreferencesKey("major_index")
        val MAX_MINOR_INDEX = stringPreferencesKey("max_minor_index")
        val RESTAURANT_NAME = stringPreferencesKey("restaurant_name")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            currency = preferences[PreferencesKeys.CURRENCY] ?: "USD",
            tipsEnabled = preferences[PreferencesKeys.TIPS_ENABLED] ?: false,
            moneroServerUrl = preferences[PreferencesKeys.MONERO_SERVER_URL] ?: "",
            moneroAddress = preferences[PreferencesKeys.MONERO_ADDRESS] ?: "",
            secretViewKey = preferences[PreferencesKeys.SECRET_VIEW_KEY] ?: "",
            majorIndex = preferences[PreferencesKeys.MAJOR_INDEX] ?: "1",
            maxMinorIndex = preferences[PreferencesKeys.MAX_MINOR_INDEX] ?: "0",
            restaurantName = preferences[PreferencesKeys.RESTAURANT_NAME] ?: ""
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = settings.currency
            preferences[PreferencesKeys.TIPS_ENABLED] = settings.tipsEnabled
            preferences[PreferencesKeys.MONERO_SERVER_URL] = settings.moneroServerUrl
            preferences[PreferencesKeys.MONERO_ADDRESS] = settings.moneroAddress
            preferences[PreferencesKeys.SECRET_VIEW_KEY] = settings.secretViewKey
            preferences[PreferencesKeys.MAJOR_INDEX] = settings.majorIndex
            preferences[PreferencesKeys.MAX_MINOR_INDEX] = settings.maxMinorIndex
            preferences[PreferencesKeys.RESTAURANT_NAME] = settings.restaurantName
        }
    }
}