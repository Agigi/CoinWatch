package dev.shorthouse.coinwatch.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry as MpCandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.CipherBPoint
import dev.shorthouse.coinwatch.model.EmaPoint
import dev.shorthouse.coinwatch.ui.theme.NegativeRed
import dev.shorthouse.coinwatch.ui.theme.PositiveGreen
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MpCombinedFinancialChart(
    candles: ImmutableList<CandleEntry>,
    emaPoints: List<EmaPoint> = emptyList(),
    cipherPoints: List<CipherBPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val greenColor = PositiveGreen.toArgb()
    val redColor = NegativeRed.toArgb()

    val shortEmaColor = AndroidColor.parseColor("#FFA726")  // 橘色 EMA 21
    val mediumEmaColor = AndroidColor.parseColor("#2196F3") // 藍色 EMA 55
    val longEmaColor = AndroidColor.parseColor("#AB47BC")   // 紫色 EMA 144

    val wt1Color = AndroidColor.parseColor("#00E5FF")  // Aqua
    val wt2Color = AndroidColor.parseColor("#2979FF")  // Blue
    val scatterGreen = AndroidColor.parseColor("#00E676")
    val scatterRed = AndroidColor.parseColor("#FF1744")
    val scatterOrange = AndroidColor.parseColor("#FF9100")

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val mainChart = EmaCombinedChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
                )
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                isAutoScaleMinMaxEnabled = true
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                drawOrder = arrayOf(
                    CombinedChart.DrawOrder.CANDLE,
                    CombinedChart.DrawOrder.LINE
                )

                minOffset = 0f
                setExtraOffsets(0f, 0f, 0f, 0f)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setTextColor(textColor)
                    setLabelCount(6, false)
                    setAvoidFirstLastClipping(true)
                    setDrawLabels(false) // Labels placed under sub-chart
                    granularity = 1f
                    isGranularityEnabled = true
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setTextColor(textColor)
                    setLabelCount(5, false)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                    minWidth = 55f
                    maxWidth = 55f
                }

                axisRight.isEnabled = false
            }

            val spacingPx = (2 * context.resources.displayMetrics.density).toInt()
            val subChart = CombinedChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    0.28f
                ).apply {
                    topMargin = spacingPx
                }
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                minOffset = 0f
                setExtraOffsets(0f, 0f, 0f, 0f)

                drawOrder = arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                )

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setTextColor(textColor)
                    setLabelCount(6, false)
                    setAvoidFirstLastClipping(true)
                    setDrawLabels(true)
                    granularity = 1f
                    isGranularityEnabled = true
                }

                axisLeft.apply {
                    setDrawGridLines(false)
                    setTextColor(textColor)
                    setLabelCount(3, false)
                    axisMinimum = -110f
                    axisMaximum = 110f
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                    minWidth = 55f
                    maxWidth = 55f

                    removeAllLimitLines()
                    addLimitLine(LimitLine(53f).apply {
                        setLineColor(textColor)
                        lineWidth = 0.8f
                        enableDashedLine(8f, 6f, 0f)
                    })
                    addLimitLine(LimitLine(-53f).apply {
                        setLineColor(textColor)
                        lineWidth = 0.8f
                        enableDashedLine(8f, 6f, 0f)
                    })
                }

                axisRight.isEnabled = false
            }

            // Sync matrix between mainChart and subChart
            var isSyncing = false
            fun sync(src: CombinedChart, dst: CombinedChart) {
                if (!isSyncing) {
                    isSyncing = true
                    dst.viewPortHandler.refresh(src.viewPortHandler.matrixTouch, dst, true)
                    isSyncing = false
                }
            }

            mainChart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, vX: Float, vY: Float) {}

                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                    sync(mainChart, subChart)
                }

                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                    sync(mainChart, subChart)
                }
            }

            subChart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastGesture: ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, vX: Float, vY: Float) {}

                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                    sync(subChart, mainChart)
                }

                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                    sync(subChart, mainChart)
                }
            }

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(mainChart)
                addView(subChart)
            }
        },
        update = { container ->
            val mainChart = container.getChildAt(0) as EmaCombinedChart
            val subChart = container.getChildAt(1) as CombinedChart

            mainChart.emaPoints = emaPoints

            if (candles.isEmpty()) {
                mainChart.clear()
                subChart.clear()
                mainChart.invalidate()
                subChart.invalidate()
                return@AndroidView
            }

            // --- 1. Populate Main Chart (K-Line + EMA3) ---
            val mainCombinedData = CombinedData()

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
            mainCombinedData.setData(CandleData(candleDataSet))

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
                    mainCombinedData.setData(LineData(lineDataSets))
                }
            }

            mainChart.data = mainCombinedData
            mainChart.notifyDataSetChanged()

            // --- 2. Populate Sub Chart (Cipher B) ---
            val subCombinedData = CombinedData()

            if (cipherPoints.size == candles.size) {
                val wt1Entries = cipherPoints.mapIndexed { i, p -> Entry(i.toFloat(), p.wt1) }
                val wt2Entries = cipherPoints.mapIndexed { i, p -> Entry(i.toFloat(), p.wt2) }

                val wt1DataSet = LineDataSet(wt1Entries, "WT1").apply {
                    color = wt1Color
                    lineWidth = 1.2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = wt1Color
                    fillAlpha = 60
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }

                val wt2DataSet = LineDataSet(wt2Entries, "WT2").apply {
                    color = wt2Color
                    lineWidth = 1.2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = wt2Color
                    fillAlpha = 40
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }

                subCombinedData.setData(LineData(wt1DataSet, wt2DataSet))

                val bullishCrossEntries = mutableListOf<Entry>()
                val bearishCrossEntries = mutableListOf<Entry>()
                val buySignalEntries = mutableListOf<Entry>()
                val sellSignalEntries = mutableListOf<Entry>()
                val goldSignalEntries = mutableListOf<Entry>()

                cipherPoints.forEachIndexed { i, p ->
                    val x = i.toFloat()
                    if (p.isBullishCross) bullishCrossEntries.add(Entry(x, p.wt2))
                    if (p.isBearishCross) bearishCrossEntries.add(Entry(x, p.wt2))

                    if (p.buySignal) buySignalEntries.add(Entry(x, -105f))
                    if (p.sellSignal) sellSignalEntries.add(Entry(x, 105f))

                    if (p.goldBuySignal) goldSignalEntries.add(Entry(x, -106f))
                    if (p.goldSellSignal) goldSignalEntries.add(Entry(x, 106f))
                }

                val scatterDataSets = mutableListOf<IScatterDataSet>()

                if (bullishCrossEntries.isNotEmpty()) {
                    scatterDataSets.add(ScatterDataSet(bullishCrossEntries, "BullishCross").apply {
                        color = scatterGreen
                        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                        scatterShapeSize = 8f
                        setDrawValues(false)
                    })
                }

                if (bearishCrossEntries.isNotEmpty()) {
                    scatterDataSets.add(ScatterDataSet(bearishCrossEntries, "BearishCross").apply {
                        color = scatterRed
                        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                        scatterShapeSize = 8f
                        setDrawValues(false)
                    })
                }

                if (buySignalEntries.isNotEmpty()) {
                    scatterDataSets.add(ScatterDataSet(buySignalEntries, "BuySignal").apply {
                        color = scatterGreen
                        setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                        scatterShapeSize = 12f
                        setDrawValues(false)
                    })
                }

                if (sellSignalEntries.isNotEmpty()) {
                    scatterDataSets.add(ScatterDataSet(sellSignalEntries, "SellSignal").apply {
                        color = scatterRed
                        setShapeRenderer(InvertedTriangleShapeRenderer())
                        scatterShapeSize = 12f
                        setDrawValues(false)
                    })
                }

                if (goldSignalEntries.isNotEmpty()) {
                    scatterDataSets.add(ScatterDataSet(goldSignalEntries, "GoldSignal").apply {
                        color = scatterOrange
                        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                        scatterShapeSize = 15f
                        setDrawValues(false)
                    })
                }

                if (scatterDataSets.isNotEmpty()) {
                    subCombinedData.setData(ScatterData(scatterDataSets))
                }
            }

            subChart.data = subCombinedData
            subChart.notifyDataSetChanged()

            // --- 3. Format Date XAxis on Sub Chart ---
            val isLongSpan = if (candles.size >= 2) {
                val spanSec = candles.last().timestamp - candles.first().timestamp
                val spanMillis = if (spanSec < 10_000_000_000L) spanSec * 1000L else spanSec
                spanMillis > 2 * 24 * 3600 * 1000L
            } else false

            val pattern = if (isLongSpan) "yyyy/MM/dd" else "yyyy/MM/dd HH:mm"

            subChart.xAxis.valueFormatter = object : ValueFormatter() {
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

            // --- 4. Viewport Sync & Focus to latest 300 candles ---
            if (candles.size > 300) {
                mainChart.setVisibleXRangeMaximum(300f)
                subChart.setVisibleXRangeMaximum(300f)
                val lastX = (candles.size - 1).toFloat()
                mainChart.moveViewToX(lastX)
                subChart.moveViewToX(lastX)
            } else {
                mainChart.fitScreen()
                subChart.fitScreen()
            }

            mainChart.invalidate()
            subChart.invalidate()
        }
    )
}
