package dev.shorthouse.coinwatch.ui.screen.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CustomSettingsUiState(
    val serverUrl: String = "Official TradingView Direct WSS",
    val isLoading: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CustomSettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun updateServerUrl(newUrl: String) {}
}
