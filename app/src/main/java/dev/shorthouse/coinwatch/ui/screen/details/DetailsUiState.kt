package dev.shorthouse.coinwatch.ui.screen.details

import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.CoinDetails
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import kotlinx.collections.immutable.ImmutableList

sealed interface DetailsUiState {
    data class Success(
        val coinDetails: CoinDetails,
        val candles: ImmutableList<CandleEntry>,
        val chartPeriod: ChartPeriod,
        val isCoinFavourite: Boolean
    ) : DetailsUiState

    data class Error(val message: String?) : DetailsUiState
    data object Loading : DetailsUiState
}
