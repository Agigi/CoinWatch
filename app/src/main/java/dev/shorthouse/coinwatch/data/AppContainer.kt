package dev.shorthouse.coinwatch.data

import android.content.Context
import dev.shorthouse.coinwatch.data.source.local.preferences.FavouritesSharedPreferences
import dev.shorthouse.coinwatch.data.source.remote.TradingViewSymbolSearchApi
import dev.shorthouse.coinwatch.data.source.remote.websocket.TradingViewWssManager

class AppContainer(context: Context) {
    val favouritesPreferences = FavouritesSharedPreferences(context)
    val wssManager = TradingViewWssManager()
    val symbolSearchApi = TradingViewSymbolSearchApi()
}
