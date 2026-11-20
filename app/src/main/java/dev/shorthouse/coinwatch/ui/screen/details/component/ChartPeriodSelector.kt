package dev.shorthouse.coinwatch.ui.screen.details.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.theme.AppTheme

private val FocusAccentBlue = Color(0xFF448AFF)

private val EvenlySpacedMinGap = object : Arrangement.Horizontal {
    override val spacing: Dp = 4.dp

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return
        val minGapPx = spacing.roundToPx()
        val totalChildrenWidth = sizes.sum()
        val gap = if (sizes.size > 1) {
            ((totalSize - totalChildrenWidth) / (sizes.size - 1)).coerceAtLeast(minGapPx)
        } else {
            0
        }

        if (layoutDirection == LayoutDirection.Ltr) {
            var currentX = 0
            sizes.forEachIndexed { index, size ->
                outPositions[index] = currentX
                currentX += size + gap
            }
        } else {
            var currentX = totalSize
            sizes.forEachIndexed { index, size ->
                currentX -= size
                outPositions[index] = currentX
                currentX -= gap
            }
        }
    }
}

@Composable
fun ChartPeriodSelector(
    selectedChartPeriod: ChartPeriod,
    onChartPeriodSelected: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    focusRequesters: Map<ChartPeriod, FocusRequester> = remember {
        ChartPeriod.entries.associateWith { FocusRequester() }
    },
    onMoveFocusDown: (() -> Unit)? = null,
) {
    val periods = ChartPeriod.entries

    if (requestInitialFocus) {
        LaunchedEffect(Unit) {
            focusRequesters[selectedChartPeriod]?.requestFocus()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val minRowWidth = maxWidth
        Row(
            horizontalArrangement = EvenlySpacedMinGap,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = minRowWidth)
        ) {
            periods.forEachIndexed { index, chartPeriod ->
                val selected = chartPeriod == selectedChartPeriod
                var isFocused by remember { mutableStateOf(false) }
                val requester = focusRequesters.getValue(chartPeriod)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        selected -> MaterialTheme.colorScheme.primary
                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else -> Color.Transparent
                    },
                    border = if (isFocused) {
                        BorderStroke(2.dp, FocusAccentBlue)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .focusRequester(requester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    if (index > 0) {
                                        focusRequesters[periods[index - 1]]?.requestFocus()
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (index < periods.lastIndex) {
                                        focusRequesters[periods[index + 1]]?.requestFocus()
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (onMoveFocusDown != null) {
                                        onMoveFocusDown()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        }
                        .clickable { onChartPeriodSelected(chartPeriod) }
                ) {
                    Text(
                        text = chartPeriod.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun ChartPeriodSelectorPreview() {
    AppTheme {
        var selectedChartPeriod by remember { mutableStateOf(ChartPeriod.OneDay) }

        ChartPeriodSelector(
            selectedChartPeriod = selectedChartPeriod,
            onChartPeriodSelected = { selectedChartPeriod = it }
        )
    }
}
