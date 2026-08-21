package dev.shorthouse.coinwatch.ui.component

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
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
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import dev.shorthouse.coinwatch.model.CipherBPoint

class InvertedTriangleShapeRenderer : IShapeRenderer {
    override fun renderShape(
        c: Canvas,
        dataSet: IScatterDataSet,
        viewPortHandler: ViewPortHandler,
        posX: Float,
        posY: Float,
        paint: Paint
    ) {
        val halfSize = dataSet.scatterShapeSize / 2f
        val path = Path().apply {
            moveTo(posX - halfSize, posY - halfSize)
            lineTo(posX + halfSize, posY - halfSize)
            lineTo(posX, posY + halfSize)
            close()
        }
        val prevStyle = paint.style
        paint.style = Paint.Style.FILL
        c.drawPath(path, paint)
        paint.style = prevStyle
    }
}

@Composable
fun MpCipherBChart(
    cipherPoints: List<CipherBPoint>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val wt1Color = AndroidColor.parseColor("#00E5FF")  // Aqua
    val wt2Color = AndroidColor.parseColor("#2979FF")  // Blue
    val greenColor = AndroidColor.parseColor("#00E676")
    val redColor = AndroidColor.parseColor("#FF1744")
    val orangeColor = AndroidColor.parseColor("#FF9100")

    AndroidView(
        modifier = modifier,
        factory = { context ->
            CombinedChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)

                drawOrder = arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                )

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setGridColor(gridColor)
                    setDrawLabels(false) // Labels shown on main chart
                    granularity = 1f
                    isGranularityEnabled = true
                }

                axisLeft.apply {
                    setDrawGridLines(false)
                    setTextColor(textColor)
                    setLabelCount(3, false)
                    axisMinimum = -120f
                    axisMaximum = 120f
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)

                    removeAllLimitLines()
                    addLimitLine(LimitLine(53f, "53").apply {
                        setLineColor(textColor)
                        lineWidth = 0.8f
                        enableDashedLine(8f, 6f, 0f)
                        setTextColor(textColor)
                    })
                    addLimitLine(LimitLine(-53f, "-53").apply {
                        setLineColor(textColor)
                        lineWidth = 0.8f
                        enableDashedLine(8f, 6f, 0f)
                        setTextColor(textColor)
                    })
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            if (cipherPoints.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val combinedData = CombinedData()

            // 1. WT1 & WT2 Line DataSets
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

            combinedData.setData(LineData(wt1DataSet, wt2DataSet))

            // 2. Crossovers and Signal Points
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
                    color = greenColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 8f
                    setDrawValues(false)
                })
            }

            if (bearishCrossEntries.isNotEmpty()) {
                scatterDataSets.add(ScatterDataSet(bearishCrossEntries, "BearishCross").apply {
                    color = redColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 8f
                    setDrawValues(false)
                })
            }

            if (buySignalEntries.isNotEmpty()) {
                scatterDataSets.add(ScatterDataSet(buySignalEntries, "BuySignal").apply {
                    color = greenColor
                    setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                })
            }

            if (sellSignalEntries.isNotEmpty()) {
                scatterDataSets.add(ScatterDataSet(sellSignalEntries, "SellSignal").apply {
                    color = redColor
                    setShapeRenderer(InvertedTriangleShapeRenderer())
                    scatterShapeSize = 12f
                    setDrawValues(false)
                })
            }

            if (goldSignalEntries.isNotEmpty()) {
                scatterDataSets.add(ScatterDataSet(goldSignalEntries, "GoldSignal").apply {
                    color = orangeColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 15f
                    setDrawValues(false)
                })
            }

            if (scatterDataSets.isNotEmpty()) {
                combinedData.setData(ScatterData(scatterDataSets))
            }

            chart.data = combinedData
            chart.notifyDataSetChanged()

            if (cipherPoints.size > 300) {
                chart.setVisibleXRangeMaximum(300f)
                chart.moveViewToX((cipherPoints.size - 1).toFloat())
            } else {
                chart.fitScreen()
            }

            chart.invalidate()
        }
    )
}
