package dev.shorthouse.coinwatch.ui.screen.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shorthouse.coinwatch.data.source.local.preferences.FavouritesSharedPreferences
import dev.shorthouse.coinwatch.data.source.remote.websocket.TradingViewWssManager
import dev.shorthouse.coinwatch.model.CoinDetails
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class DetailsViewModel(
    private val coinId: String,
    private val wssManager: TradingViewWssManager,
    private val favouritesPreferences: FavouritesSharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val chartPeriodFlow = MutableStateFlow(ChartPeriod.OneDay)

    init {
        initialiseUiState()
    }

    fun initialiseUiState() {
        if (coinId.isBlank()) {
            _uiState.update { DetailsUiState.Error("Invalid coin ID") }
            return
        }

        wssManager.subscribeSymbols(listOf(coinId))
        wssManager.requestChartData(coinId, "1D")

        combine(
            wssManager.coinsData,
            wssManager.chartCandles,
            favouritesPreferences.favouriteSymbols,
            chartPeriodFlow
        ) { realtimeMap, candlesList, favSet, chartPeriod ->
            val rt = realtimeMap[coinId]
            val symbolClean = coinId.substringAfter(":")
            val exchangeName = if (coinId.contains(":")) coinId.substringBefore(":") else ""
            val isFav = favSet.contains(coinId)
            val desc = rt?.description?.ifEmpty { null } ?: symbolClean
            val logoUrl = rt?.imageUrl ?: ""

            val details = CoinDetails(
                id = coinId,
                name = symbolClean,
                symbol = desc,
                description = "TradingView Pair: $coinId",
                exchange = exchangeName,
                tags = persistentListOf("Crypto"),
                links = persistentListOf(),
                imageUrl = logoUrl,
                currentPrice = rt?.currentPrice ?: Price("0"),
                marketCap = Price("0"),
                fullyDilutedMarketCap = Price("0"),
                marketCapRank = "N/A",
                volume24h = Price(rt?.volume ?: "0"),
                numberOfExchanges = "N/A",
                numberOfMarkets = "N/A",
                circulatingSupply = "N/A",
                totalSupply = "N/A",
                maxSupply = "N/A",
                allTimeHigh = rt?.highPrice ?: Price("0"),
                allTimeHighDate = "N/A",
                listedDate = "N/A"
            )

            DetailsUiState.Success(
                coinDetails = details,
                candles = candlesList.toImmutableList(),
                chartPeriod = chartPeriod,
                isCoinFavourite = isFav
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateChartPeriod(chartPeriod: ChartPeriod) {
        chartPeriodFlow.value = chartPeriod
        val tf = when (chartPeriod) {
            ChartPeriod.OneMinute -> "1m"
            ChartPeriod.ThreeMinute -> "3m"
            ChartPeriod.FiveMinute -> "5m"
            ChartPeriod.FifteenMinute -> "15m"
            ChartPeriod.OneHour -> "1h"
            ChartPeriod.FourHour -> "4h"
            ChartPeriod.OneDay -> "1D"
            ChartPeriod.OneWeek -> "1W"
            ChartPeriod.OneMonth -> "1M"
        }
        wssManager.requestChartData(coinId, tf)
    }

    fun toggleIsCoinFavourite() {
        favouritesPreferences.toggleFavourite(coinId)
    }

    override fun onCleared() {
        super.onCleared()
        wssManager.clearChartData()
    }
}
