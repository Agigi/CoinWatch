package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.shorthouse.coinwatch.model.CoinDetails
import dev.shorthouse.coinwatch.model.CoinLink
import dev.shorthouse.coinwatch.model.CoinLinkType
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.screen.details.DetailsUiState
import kotlinx.collections.immutable.persistentListOf

data class DetailsScreenPreviewState(
    val uiState: DetailsUiState,
    val scrollPosition: Int = 0,
)

class DetailsScreenPreviewStateProvider : PreviewParameterProvider<DetailsScreenPreviewState> {
    override val values = sequenceOf(
        DetailsScreenPreviewState(
            uiState = detailsSuccessUiState,
            scrollPosition = 0
        ),
        DetailsScreenPreviewState(
            uiState = detailsSuccessUiState,
            scrollPosition = 1700
        ),
        DetailsScreenPreviewState(
            uiState = detailsSuccessUiState,
            scrollPosition = 3400
        ),
        DetailsScreenPreviewState(
            uiState = DetailsUiState.Error("No internet connection")
        ),
        DetailsScreenPreviewState(
            uiState = DetailsUiState.Loading
        )
    )
}

private val detailsSuccessUiState = DetailsUiState.Success(
    CoinDetails(
        id = "BINANCE:ETHUSDT",
        name = "ETHUSDT",
        symbol = "ETHUSDT",
        description = "Ethereum is a decentralized blockchain with smart contract functionality.",
        tags = persistentListOf("smart-contracts", "staking", "layer-1"),
        links = persistentListOf(
            CoinLink(
                type = CoinLinkType.Website,
                url = "https://ethereum.org",
            ),
            CoinLink(
                type = CoinLinkType.GitHub,
                url = "https://github.com/ethereum",
            )
        ),
        imageUrl = "",
        currentPrice = Price("1879.14"),
        marketCap = Price("0"),
        fullyDilutedMarketCap = Price("0"),
        marketCapRank = "2",
        volume24h = Price("6,627,669,115"),
        numberOfExchanges = "248",
        numberOfMarkets = "1,098",
        circulatingSupply = "120,186,525",
        totalSupply = "120,500,000",
        maxSupply = "210,000,000",
        allTimeHigh = Price("4878.26"),
        allTimeHighDate = "10 Nov 2021",
        listedDate = "7 Aug 2015"
    ),
    candles = persistentListOf(),
    chartPeriod = ChartPeriod.OneWeek,
    isCoinFavourite = true
)
