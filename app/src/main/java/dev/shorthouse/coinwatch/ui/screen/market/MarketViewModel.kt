package dev.shorthouse.coinwatch.ui.screen.market

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shorthouse.coinwatch.data.source.local.datastore.common.CoinSort
import dev.shorthouse.coinwatch.data.source.local.preferences.FavouritesSharedPreferences
import dev.shorthouse.coinwatch.data.source.remote.DefaultSymbols
import dev.shorthouse.coinwatch.data.source.remote.websocket.TradingViewWssManager
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.model.TimeOfDay
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalTime

class MarketViewModel(
    private val wssManager: TradingViewWssManager,
    private val favouritesPreferences: FavouritesSharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState = _uiState.asStateFlow()

    init {
        initialiseUiState()
    }

    fun initialiseUiState() {
        wssManager.subscribeSymbols(DefaultSymbols.SYMBOLS)

        combine(
            wssManager.coinsData,
            favouritesPreferences.favouriteSymbols
        ) { realtimeDataMap, favSet ->
            DefaultSymbols.SYMBOLS.map { symbol ->
                val rt = realtimeDataMap[symbol]
                val isFav = favSet.contains(symbol)
                val coinSymbol = symbol.substringAfter(":")

                Coin(
                    id = symbol,
                    name = coinSymbol,
                    symbol = rt?.description?.ifEmpty { coinSymbol } ?: coinSymbol,
                    imageUrl = rt?.imageUrl ?: "",
                    currentPrice = rt?.currentPrice ?: Price("0"),
                    priceChangePercentage24h = rt?.priceChangePercentage24h ?: Percentage("0"),
                    isFavourite = isFav
                )
            }
        }.onEach { coins ->
            val hour = LocalTime.now().hour
            val timeOfDay = when (hour) {
                in 4..11 -> TimeOfDay.Morning
                in 12..17 -> TimeOfDay.Afternoon
                else -> TimeOfDay.Evening
            }
            _uiState.update {
                it.copy(
                    coins = coins.toImmutableList(),
                    timeOfDay = timeOfDay,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun updateCoinSort(coinSort: CoinSort) {
        _uiState.update { it.copy(coinSort = coinSort) }
    }

    fun pullRefreshCoins() {
        wssManager.subscribeSymbols(DefaultSymbols.SYMBOLS)
    }

    fun dismissErrorMessage(@StringRes dismissedErrorMessageId: Int) {
        _uiState.update {
            val errorMessageIds = it.errorMessageIds.filter { id -> id != dismissedErrorMessageId }
            it.copy(errorMessageIds = errorMessageIds)
        }
    }
}
