package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.model.TimeOfDay
import dev.shorthouse.coinwatch.ui.preview.CoinListPreviewData.coins
import dev.shorthouse.coinwatch.ui.screen.market.MarketUiState
import kotlinx.collections.immutable.persistentListOf

class MarketUiStatePreviewProvider : PreviewParameterProvider<MarketUiState> {
    override val values = sequenceOf(
        MarketUiState(
            coins = coins,
            timeOfDay = TimeOfDay.Evening,
            marketCapChangePercentage24h = Percentage("1.3")
        ),
        MarketUiState(
            coins = persistentListOf()
        ),
        MarketUiState(
            isLoading = true
        )
    )
}

private object CoinListPreviewData {
    val coins = persistentListOf(
        Coin(
            id = "BINANCE:BTCUSDT",
            symbol = "BTCUSDT",
            name = "BTCUSDT",
            imageUrl = "",
            currentPrice = Price("67420.50"),
            priceChangePercentage24h = Percentage("0.76"),
        ),
        Coin(
            id = "BINANCE:ETHUSDT",
            symbol = "ETHUSDT",
            name = "ETHUSDT",
            imageUrl = "",
            currentPrice = Price("3450.10"),
            priceChangePercentage24h = Percentage("-1.11"),
        )
    )
}
