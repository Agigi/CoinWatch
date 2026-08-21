package dev.shorthouse.coinwatch.data.source.local.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerPreferences(context: Context) {
    private val _serverUrl = MutableStateFlow("wss://data.tradingview.com/socket.io/websocket")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url.trim()
    }
}
