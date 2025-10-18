package cl.icripto.xmrpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.data.SettingsRepository

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
