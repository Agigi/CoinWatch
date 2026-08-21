package dev.shorthouse.coinwatch.ui.screen.details.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.theme.AppTheme

@Composable
fun ChartPeriodSelector(
    selectedChartPeriod: ChartPeriod,
    onChartPeriodSelected: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        ChartPeriod.entries.forEach { chartPeriod ->
            val selected = chartPeriod == selectedChartPeriod

            InputChip(
                selected = selected,
                onClick = { onChartPeriodSelected(chartPeriod) },
                label = {
                    Text(
                        text = chartPeriod.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.Transparent
                ),
                border = null
            )
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
