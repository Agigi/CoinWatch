package dev.shorthouse.coinwatch.data.source.local.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavouritesSharedPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("coin_favourites", Context.MODE_PRIVATE)

    private val defaultFavourites = setOf(
        "BINANCE:BTCUSDT",
        "BINANCE:ETHUSDT",
        "BINANCE:SOLUSDT"
    )

    private val _favouriteSymbols = MutableStateFlow<Set<String>>(getSavedFavourites())
    val favouriteSymbols: StateFlow<Set<String>> = _favouriteSymbols.asStateFlow()

    private fun getSavedFavourites(): Set<String> {
        val saved = prefs.getStringSet("favourite_symbols", null)
        return saved ?: defaultFavourites
    }

    fun isFavourite(symbol: String): Boolean {
        return _favouriteSymbols.value.contains(symbol)
    }

    fun toggleFavourite(symbol: String) {
        val current = _favouriteSymbols.value.toMutableSet()
        if (current.contains(symbol)) {
            current.remove(symbol)
        } else {
            current.add(symbol)
        }
        prefs.edit().putStringSet("favourite_symbols", current).apply()
        _favouriteSymbols.value = current
    }

    fun addFavourite(symbol: String) {
        val current = _favouriteSymbols.value.toMutableSet()
        if (current.add(symbol)) {
            prefs.edit().putStringSet("favourite_symbols", current).apply()
            _favouriteSymbols.value = current
        }
    }

    fun removeFavourite(symbol: String) {
        val current = _favouriteSymbols.value.toMutableSet()
        if (current.remove(symbol)) {
            prefs.edit().putStringSet("favourite_symbols", current).apply()
            _favouriteSymbols.value = current
        }
    }
}
