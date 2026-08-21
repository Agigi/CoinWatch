package dev.shorthouse.coinwatch

import androidx.lifecycle.ViewModel
import dev.shorthouse.coinwatch.navigation.NavigationBarScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainActivityUiState(startScreen = NavigationBarScreen.Market, isLoading = false))
    val uiState = _uiState.asStateFlow()
}
