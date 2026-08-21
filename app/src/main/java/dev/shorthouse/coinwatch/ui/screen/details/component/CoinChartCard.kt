package dev.shorthouse.coinwatch.ui.screen.details.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.EmaAlignmentState
import dev.shorthouse.coinwatch.model.IndicatorCalculator
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.component.MpCombinedFinancialChart
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.theme.NegativeRed
import dev.shorthouse.coinwatch.ui.theme.PositiveGreen
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CoinChartCard(
    currentPrice: Price,
    candles: ImmutableList<CandleEntry>,
    chartPeriod: ChartPeriod,
    onClickChartPeriod: (ChartPeriod) -> Unit,
    exchange: String = "",
    modifier: Modifier = Modifier,
) {
    val emaPoints = remember(candles) { IndicatorCalculator.calculateEma3(candles) }
    val emaAlignment = remember(emaPoints) { IndicatorCalculator.getLatestEmaAlignment(emaPoints) }
    val cipherPoints = remember(candles) { IndicatorCalculator.calculateCipherB(candles) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = currentPrice.formattedAmount,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val chartSubtitle = if (exchange.isNotBlank()) {
                        "$exchange • K-Line (${chartPeriod.label})"
                    } else {
                        "K-Line (${chartPeriod.label})"
                    }

                    Text(
                        text = chartSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(8.dp))

                    val (badgeText, badgeBg, badgeFg) = when (emaAlignment) {
                        EmaAlignmentState.BULLISH -> Triple("EMA多頭", PositiveGreen, Color.White)
                        EmaAlignmentState.BEARISH -> Triple("EMA空頭", NegativeRed, Color.White)
                        EmaAlignmentState.ENTANGLED -> Triple("EMA糾纏", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        EmaAlignmentState.INSUFFICIENT_DATA -> Triple("", Color.Transparent, Color.Transparent)
                    }

                    if (badgeText.isNotEmpty()) {
                        Surface(
                            color = badgeBg,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (candles.isNotEmpty()) {
                MpCombinedFinancialChart(
                    candles = candles,
                    emaPoints = emaPoints,
                    cipherPoints = cipherPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .padding(horizontal = 12.dp)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                ) {
                    Text(
                        text = stringResource(R.string.empty_chart_message),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            ChartPeriodSelector(
                selectedChartPeriod = chartPeriod,
                onChartPeriodSelected = onClickChartPeriod,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
