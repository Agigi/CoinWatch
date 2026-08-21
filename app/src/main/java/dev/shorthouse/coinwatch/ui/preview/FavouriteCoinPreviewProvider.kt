package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price

class FavouriteCoinPreviewProvider : PreviewParameterProvider<Coin> {
    override val values = sequenceOf(
        Coin(
            id = "BINANCE:BTCUSDT",
            symbol = "BTCUSDT",
            name = "BTCUSDT",
            imageUrl = "",
            currentPrice = Price("67420.50"),
            priceChangePercentage24h = Percentage("1.25"),
            isFavourite = true
        )
    )
}
