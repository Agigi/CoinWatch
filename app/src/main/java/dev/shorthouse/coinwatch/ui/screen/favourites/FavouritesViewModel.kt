package dev.shorthouse.coinwatch.ui.screen.favourites

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shorthouse.coinwatch.data.source.local.datastore.common.CoinSort
import dev.shorthouse.coinwatch.data.source.local.preferences.FavouritesSharedPreferences
import dev.shorthouse.coinwatch.data.source.remote.websocket.TradingViewWssManager
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class FavouritesViewModel(
    private val wssManager: TradingViewWssManager,
    private val favouritesPreferences: FavouritesSharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        initialiseUiState()
    }

    fun initialiseUiState() {
        combine(
            wssManager.coinsData,
            favouritesPreferences.favouriteSymbols
        ) { realtimeDataMap, favSet ->
            val favList = favSet.toList()
            wssManager.subscribeSymbols(favList)

            favList.map { symbol ->
                val rt = realtimeDataMap[symbol]
                val coinSymbol = symbol.substringAfter(":")

                Coin(
                    id = symbol,
                    name = coinSymbol,
                    symbol = rt?.description?.ifEmpty { coinSymbol } ?: coinSymbol,
                    imageUrl = rt?.imageUrl ?: "",
                    currentPrice = rt?.currentPrice ?: Price("0"),
                    priceChangePercentage24h = rt?.priceChangePercentage24h ?: Percentage("0"),
                    isFavourite = true
                )
            }
        }.onEach { favCoins ->
            _uiState.update {
                it.copy(
                    favouriteCoins = favCoins.toImmutableList(),
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun updateIsFavouritesCondensed(isFavouritesCondensed: Boolean) {
        _uiState.update { it.copy(isFavouritesCondensed = isFavouritesCondensed) }
    }

    fun updateCoinSort(coinSort: CoinSort) {
        _uiState.update { it.copy(coinSort = coinSort) }
    }

    fun pullRefreshFavouriteCoins() {
        val favs = favouritesPreferences.favouriteSymbols.value.toList()
        if (favs.isNotEmpty()) {
            wssManager.subscribeSymbols(favs)
        }
    }

    fun dismissErrorMessage(@StringRes dismissedErrorMessageId: Int) {
        _uiState.update {
            val errorMessageIds = it.errorMessageIds.filter { id -> id != dismissedErrorMessageId }
            it.copy(errorMessageIds = errorMessageIds)
        }
    }
}
