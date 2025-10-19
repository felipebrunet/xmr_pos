package cl.icripto.xmrpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.icripto.xmrpos.data.AppSettings
import cl.icripto.xmrpos.data.MoneroPriceDataSource
import cl.icripto.xmrpos.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository, private val moneroPriceDataSource: MoneroPriceDataSource) : ViewModel() {
    val settingsFlow = repository.settingsFlow

    private val _xmrPrice = MutableStateFlow<Double?>(null)
    val xmrPrice: StateFlow<Double?> = _xmrPrice

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            val currentSettings = repository.settingsFlow.first()
            repository.saveSettings(currentSettings.copy(pin = pin))
        }
    }

    fun fetchXmrPrice(fiatCurrency: String) {
        viewModelScope.launch {
            _xmrPrice.value = moneroPriceDataSource.getMoneroPrice(fiatCurrency)
        }
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository, private val moneroPriceDataSource: MoneroPriceDataSource) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, moneroPriceDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
