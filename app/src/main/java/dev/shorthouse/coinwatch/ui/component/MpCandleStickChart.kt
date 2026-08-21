package dev.shorthouse.coinwatch.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry as MpCandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.EmaPoint
import dev.shorthouse.coinwatch.ui.theme.NegativeRed
import dev.shorthouse.coinwatch.ui.theme.PositiveGreen
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EmaCombinedChart(context: Context) : CombinedChart(context) {
    var emaPoints: List<EmaPoint> = emptyList()

    private val bullishPaint = Paint().apply {
        color = AndroidColor.parseColor("#2617C683") // 15% opacity green
        style = Paint.Style.FILL
    }
    private val bearishPaint = Paint().apply {
        color = AndroidColor.parseColor("#26EA3943") // 15% opacity red
        style = Paint.Style.FILL
    }
    private val entangledPaint = Paint().apply {
        color = AndroidColor.parseColor("#262196F3") // 15% opacity blue
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        if (emaPoints.isNotEmpty() && mData != null) {
            val transformer = getTransformer(YAxis.AxisDependency.LEFT)
            val contentTop = mViewPortHandler.contentTop()
            val contentBottom = mViewPortHandler.contentBottom()
            val contentLeft = mViewPortHandler.contentLeft()
            val contentRight = mViewPortHandler.contentRight()

            val pts = FloatArray(4)

            for (i in emaPoints.indices) {
                val point = emaPoints[i]
                val s = point.shortEma ?: continue
                val m = point.mediumEma ?: continue
                val l = point.longEma ?: continue

                val paint = when {
                    s > m && m > l -> bullishPaint
                    s < m && m < l -> bearishPaint
                    else -> entangledPaint
                }

                pts[0] = i - 0.5f
                pts[1] = 0f
                pts[2] = i + 0.5f
                pts[3] = 0f
                transformer.pointValuesToPixel(pts)

                val leftPx = pts[0].coerceIn(contentLeft, contentRight)
                val rightPx = pts[2].coerceIn(contentLeft, contentRight)

                if (leftPx < rightPx) {
                    canvas.drawRect(leftPx, contentTop, rightPx, contentBottom, paint)
                }
            }
        }
        super.onDraw(canvas)
    }
}

@Composable
fun MpCandleStickChart(
    candles: ImmutableList<CandleEntry>,
    emaPoints: List<EmaPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val greenColor = PositiveGreen.toArgb()
    val redColor = NegativeRed.toArgb()

    val shortEmaColor = AndroidColor.parseColor("#FFA726")  // 橘色 EMA 21
    val mediumEmaColor = AndroidColor.parseColor("#2196F3") // 藍色 EMA 55
    val longEmaColor = AndroidColor.parseColor("#AB47BC")   // 紫色 EMA 144

    AndroidView(
        modifier = modifier,
        factory = { context ->
            EmaCombinedChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                drawOrder = arrayOf(
                    CombinedChart.DrawOrder.CANDLE,
                    CombinedChart.DrawOrder.LINE
                )

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setTextColor(textColor)
                    setLabelCount(6, false)
                    setAvoidFirstLastClipping(true)
                    granularity = 1f
                    isGranularityEnabled = true
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setTextColor(textColor)
                    setLabelCount(5, false)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            chart.emaPoints = emaPoints

            if (candles.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val combinedData = CombinedData()

            // 1. K-line Dataset
            val candleEntries = candles.mapIndexed { index, candle ->
                MpCandleEntry(
                    index.toFloat(),
                    candle.high.toFloat(),
                    candle.low.toFloat(),
                    candle.open.toFloat(),
                    candle.close.toFloat()
                )
            }

            val candleDataSet = CandleDataSet(candleEntries, "KLine").apply {
                color = AndroidColor.GRAY
                setShadowColor(AndroidColor.GRAY)
                shadowWidth = 1f
                setDecreasingColor(redColor)
                setDecreasingPaintStyle(Paint.Style.FILL)
                setIncreasingColor(greenColor)
                setIncreasingPaintStyle(Paint.Style.FILL)
                setNeutralColor(greenColor)
                setDrawValues(false)
                setDrawIcons(false)
                highlightLineWidth = 1f
                highLightColor = textColor
            }
            combinedData.setData(CandleData(candleDataSet))

            // 2. EMA Line Datasets
            if (emaPoints.size == candles.size) {
                val shortEntries = mutableListOf<Entry>()
                val mediumEntries = mutableListOf<Entry>()
                val longEntries = mutableListOf<Entry>()

                emaPoints.forEachIndexed { i, point ->
                    point.shortEma?.let { shortEntries.add(Entry(i.toFloat(), it)) }
                    point.mediumEma?.let { mediumEntries.add(Entry(i.toFloat(), it)) }
                    point.longEma?.let { longEntries.add(Entry(i.toFloat(), it)) }
                }

                val lineDataSets = mutableListOf<ILineDataSet>()

                if (shortEntries.isNotEmpty()) {
                    lineDataSets.add(LineDataSet(shortEntries, "EMA 21").apply {
                        color = shortEmaColor
                        lineWidth = 1.5f
                        setDrawCircles(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    })
                }

                if (mediumEntries.isNotEmpty()) {
                    lineDataSets.add(LineDataSet(mediumEntries, "EMA 55").apply {
                        color = mediumEmaColor
                        lineWidth = 1.5f
                        setDrawCircles(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    })
                }

                if (longEntries.isNotEmpty()) {
                    lineDataSets.add(LineDataSet(longEntries, "EMA 144").apply {
                        color = longEmaColor
                        lineWidth = 1.5f
                        setDrawCircles(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    })
                }

                if (lineDataSets.isNotEmpty()) {
                    combinedData.setData(LineData(lineDataSets))
                }
            }

            val isLongSpan = if (candles.size >= 2) {
                val spanSec = candles.last().timestamp - candles.first().timestamp
                val spanMillis = if (spanSec < 10_000_000_000L) spanSec * 1000L else spanSec
                spanMillis > 2 * 24 * 3600 * 1000L
            } else false

            val pattern = if (isLongSpan) "yyyy/MM/dd" else "yyyy/MM/dd HH:mm"

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                private val formatter = DateTimeFormatter.ofPattern(pattern)
                    .withZone(ZoneId.systemDefault())

                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index in candles.indices) {
                        val ts = candles[index].timestamp
                        val millis = if (ts < 10_000_000_000L) ts * 1000L else ts
                        return formatter.format(Instant.ofEpochMilli(millis))
                    }
                    return ""
                }
            }

            chart.data = combinedData
            chart.notifyDataSetChanged()

            if (candles.size > 300) {
                chart.setVisibleXRangeMaximum(300f)
                chart.moveViewToX((candles.size - 1).toFloat())
            } else {
                chart.fitScreen()
            }

            chart.invalidate()
        }
    )
}
