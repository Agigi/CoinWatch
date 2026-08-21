package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.preview.FavouritesPreviewData.favouriteCoins
import dev.shorthouse.coinwatch.ui.screen.favourites.FavouritesUiState
import kotlinx.collections.immutable.persistentListOf

class FavouritesUiStatePreviewProvider : PreviewParameterProvider<FavouritesUiState> {
    override val values = sequenceOf(
        FavouritesUiState(
            favouriteCoins = favouriteCoins,
            isFavouriteCoinsEmpty = false,
            isFavouritesCondensed = false
        ),
        FavouritesUiState(
            favouriteCoins = favouriteCoins,
            isFavouriteCoinsEmpty = false,
            isFavouritesCondensed = true
        ),
        FavouritesUiState(
            favouriteCoins = persistentListOf()
        ),
        FavouritesUiState(
            isLoading = true
        )
    )
}

private object FavouritesPreviewData {
    val favouriteCoins = persistentListOf(
        Coin(
            id = "BINANCE:BTCUSDT",
            symbol = "BTCUSDT",
            name = "BTCUSDT",
            imageUrl = "",
            currentPrice = Price("67420.50"),
            priceChangePercentage24h = Percentage("1.76"),
            isFavourite = true
        ),
        Coin(
            id = "BINANCE:ETHUSDT",
            symbol = "ETHUSDT",
            name = "ETHUSDT",
            imageUrl = "",
            currentPrice = Price("3450.10"),
            priceChangePercentage24h = Percentage("1.84"),
            isFavourite = true
        )
    )
}
