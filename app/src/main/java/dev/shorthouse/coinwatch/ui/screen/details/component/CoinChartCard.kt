package dev.shorthouse.coinwatch.ui.screen.details.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.CipherBPoint
import dev.shorthouse.coinwatch.model.EmaAlignmentState
import dev.shorthouse.coinwatch.model.EmaPoint
import dev.shorthouse.coinwatch.model.IndicatorCalculator
import dev.shorthouse.coinwatch.model.Price
import dev.shorthouse.coinwatch.ui.component.MpCombinedFinancialChart
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.theme.NegativeRed
import dev.shorthouse.coinwatch.ui.theme.PositiveGreen
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val candleTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault())

private fun formatCandleTime(timestamp: Long): String {
    val millis = if (timestamp < 10_000_000_000L) timestamp * 1000L else timestamp
    return candleTimeFormatter.format(Instant.ofEpochMilli(millis))
}

private fun formatIndicatorNumber(value: BigDecimal): String {
    val absVal = value.abs()
    val scale = when {
        absVal >= BigDecimal("1000") -> 2
        absVal >= BigDecimal("1") -> 4
        absVal >= BigDecimal("0.01") -> 6
        else -> 8
    }
    return value.setScale(scale, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

private fun formatIndicatorNumber(value: Float): String {
    return if (value.isNaN() || value.isInfinite()) {
        "--"
    } else {
        formatIndicatorNumber(BigDecimal.valueOf(value.toDouble()))
    }
}

private fun formatCompactVolume(volume: BigDecimal): String {
    val v = volume.toDouble()
    return when {
        v >= 1_000_000_000.0 -> String.format(Locale.US, "%.2fB", v / 1_000_000_000.0)
        v >= 1_000_000.0 -> String.format(Locale.US, "%.2fM", v / 1_000_000.0)
        v >= 1_000.0 -> String.format(Locale.US, "%.2fK", v / 1_000.0)
        v >= 10.0 -> String.format(Locale.US, "%.1f", v)
        v > 0.0 -> String.format(Locale.US, "%.2f", v)
        else -> "0"
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    val chartFocusRequester = remember { FocusRequester() }
    var isChartFocused by remember { mutableStateOf(false) }

    var isManualSelection by remember(chartPeriod) { mutableStateOf(false) }
    var manualSelectedIndex by remember(chartPeriod) { mutableIntStateOf(0) }

    val effectiveSelectedIndex = if (candles.isNotEmpty()) {
        if (isManualSelection) {
            manualSelectedIndex.coerceIn(0, candles.lastIndex)
        } else {
            candles.lastIndex
        }
    } else {
        null
    }

    fun selectCandleByOffset(delta: Int) {
        if (candles.isEmpty()) return
        val baseIdx = effectiveSelectedIndex ?: candles.lastIndex
        val nextIdx = (baseIdx + delta).coerceIn(0, candles.lastIndex)
        manualSelectedIndex = nextIdx
        isManualSelection = nextIdx < candles.lastIndex
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            val chartSubtitle = if (exchange.isNotBlank()) {
                "$exchange • K-Line (${chartPeriod.label})"
            } else {
                "K-Line (${chartPeriod.label})"
            }

            val (badgeText, badgeBg, badgeFg) = when (emaAlignment) {
                EmaAlignmentState.BULLISH -> Triple("EMA多頭", PositiveGreen, Color.White)
                EmaAlignmentState.BEARISH -> Triple("EMA空頭", NegativeRed, Color.White)
                EmaAlignmentState.ENTANGLED -> Triple("EMA糾纏", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                EmaAlignmentState.INSUFFICIENT_DATA -> Triple("", Color.Transparent, Color.Transparent)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = currentPrice.formattedAmount,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    text = chartSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .alignByBaseline()
                )

                if (badgeText.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    val badgeBaselineOffsetPx = with(LocalDensity.current) { 4.dp.roundToPx() }
                    Surface(
                        color = badgeBg,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.alignBy { it.measuredHeight - badgeBaselineOffsetPx }
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

            Spacer(Modifier.height(5.dp))

            if (candles.isNotEmpty() && effectiveSelectedIndex != null) {
                CandleInfoBar(
                    candle = candles[effectiveSelectedIndex],
                    emaPoint = emaPoints.getOrNull(effectiveSelectedIndex),
                    cipherPoint = cipherPoints.getOrNull(effectiveSelectedIndex),
                    isManualSelection = isManualSelection,
                    onResetToLatest = { isManualSelection = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .border(
                            width = 2.dp,
                            color = if (isChartFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(chartFocusRequester)
                        .onFocusChanged { isChartFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    selectCandleByOffset(-1)
                                    true
                                }
                                Key.DirectionRight -> {
                                    selectCandleByOffset(1)
                                    true
                                }
                                Key.MediaRewind, Key.PageUp, Key.ChannelDown -> {
                                    selectCandleByOffset(-15)
                                    true
                                }
                                Key.MediaFastForward, Key.PageDown, Key.ChannelUp -> {
                                    selectCandleByOffset(15)
                                    true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    isManualSelection = false
                                    true
                                }
                                else -> false
                            }
                        }
                        .padding(2.dp)
                ) {
                    MpCombinedFinancialChart(
                        candles = candles,
                        emaPoints = emaPoints,
                        cipherPoints = cipherPoints,
                        selectedCandleIndex = effectiveSelectedIndex,
                        onCandleSelected = { index ->
                            if (index == null) {
                                isManualSelection = false
                            } else {
                                val safeIdx = index.coerceIn(0, candles.lastIndex)
                                manualSelectedIndex = safeIdx
                                isManualSelection = safeIdx < candles.lastIndex
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CandleInfoBar(
    candle: CandleEntry,
    emaPoint: EmaPoint?,
    cipherPoint: CipherBPoint?,
    isManualSelection: Boolean,
    onResetToLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBullish = candle.close >= candle.open
    val priceColor = if (isBullish) PositiveGreen else NegativeRed
    val metaTextStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "時間: ${formatCandleTime(candle.timestamp)}",
                        style = metaTextStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "開: ${formatIndicatorNumber(candle.open)}",
                        style = metaTextStyle,
                        color = priceColor
                    )
                    Text(
                        text = "高: ${formatIndicatorNumber(candle.high)}",
                        style = metaTextStyle,
                        color = priceColor
                    )
                    Text(
                        text = "低: ${formatIndicatorNumber(candle.low)}",
                        style = metaTextStyle,
                        color = priceColor
                    )
                    Text(
                        text = "收: ${formatIndicatorNumber(candle.close)}",
                        style = metaTextStyle.copy(fontWeight = FontWeight.Bold),
                        color = priceColor
                    )
                    Text(
                        text = "量: ${formatCompactVolume(candle.volume)}",
                        style = metaTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isManualSelection) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable(onClick = onResetToLatest)
                    ) {
                        Text(
                            text = "回最新",
                            style = metaTextStyle.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                emaPoint?.shortEma?.let {
                    LegendValueBadge(
                        dotColor = Color(0xFFFFA726),
                        text = "EMA21: ${formatIndicatorNumber(it)}"
                    )
                }
                emaPoint?.mediumEma?.let {
                    LegendValueBadge(
                        dotColor = Color(0xFF2196F3),
                        text = "EMA55: ${formatIndicatorNumber(it)}"
                    )
                }
                emaPoint?.longEma?.let {
                    LegendValueBadge(
                        dotColor = Color(0xFFAB47BC),
                        text = "EMA144: ${formatIndicatorNumber(it)}"
                    )
                }
                cipherPoint?.let {
                    LegendValueBadge(
                        dotColor = Color(0xFF00E5FF),
                        text = "WT1: ${String.format(Locale.US, "%.1f", it.wt1)}"
                    )
                    LegendValueBadge(
                        dotColor = Color(0xFF2979FF),
                        text = "WT2: ${String.format(Locale.US, "%.1f", it.wt2)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendValueBadge(
    dotColor: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
