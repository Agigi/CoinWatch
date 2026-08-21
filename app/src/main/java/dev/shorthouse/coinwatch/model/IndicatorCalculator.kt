package dev.shorthouse.coinwatch.model

import kotlin.math.abs

data class EmaPoint(
    val shortEma: Float?,
    val mediumEma: Float?,
    val longEma: Float?
)

enum class EmaAlignmentState {
    BULLISH,    // 多頭排列 (EMA 21 > EMA 55 > EMA 144)
    BEARISH,    // 空頭排列 (EMA 21 < EMA 55 < EMA 144)
    ENTANGLED,  // 糾纏狀態
    INSUFFICIENT_DATA
}

data class CipherBPoint(
    val wt1: Float,
    val wt2: Float,
    val isBullishCross: Boolean = false,
    val isBearishCross: Boolean = false,
    val buySignal: Boolean = false,
    val sellSignal: Boolean = false,
    val goldBuySignal: Boolean = false,
    val goldSellSignal: Boolean = false
)

object IndicatorCalculator {

    fun calculateEma(prices: List<Float>, period: Int): List<Float?> {
        if (prices.size < period) return List(prices.size) { null }
        val result = ArrayList<Float?>(prices.size)
        val k = 2f / (period + 1)
        var prevEma: Float? = null

        for (i in prices.indices) {
            val price = prices[i]
            if (i < period - 1) {
                result.add(null)
            } else if (i == period - 1) {
                val initialSma = prices.subList(0, period).average().toFloat()
                prevEma = initialSma
                result.add(initialSma)
            } else {
                val currentEma = (price * k) + (prevEma!! * (1f - k))
                prevEma = currentEma
                result.add(currentEma)
            }
        }
        return result
    }

    fun calculateEma3(candles: List<CandleEntry>): List<EmaPoint> {
        val closes = candles.map { it.close.toFloat() }
        val ema21 = calculateEma(closes, 21)
        val ema55 = calculateEma(closes, 55)
        val ema144 = calculateEma(closes, 144)

        return candles.indices.map { i ->
            EmaPoint(
                shortEma = ema21[i],
                mediumEma = ema55[i],
                longEma = ema144[i]
            )
        }
    }

    fun getLatestEmaAlignment(emaPoints: List<EmaPoint>): EmaAlignmentState {
        val last = emaPoints.lastOrNull() ?: return EmaAlignmentState.INSUFFICIENT_DATA
        val s = last.shortEma ?: return EmaAlignmentState.INSUFFICIENT_DATA
        val m = last.mediumEma ?: return EmaAlignmentState.INSUFFICIENT_DATA
        val l = last.longEma ?: return EmaAlignmentState.INSUFFICIENT_DATA

        return when {
            s > m && m > l -> EmaAlignmentState.BULLISH
            s < m && m < l -> EmaAlignmentState.BEARISH
            else -> EmaAlignmentState.ENTANGLED
        }
    }

    fun calculateRsi(prices: List<Float>, period: Int = 14): List<Float?> {
        if (prices.size < period + 1) return List(prices.size) { null }
        val result = ArrayList<Float?>(prices.size)
        for (i in prices.indices) result.add(null)

        var avgGain = 0f
        var avgLoss = 0f
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += -change
        }
        avgGain /= period
        avgLoss /= period

        val rs0 = if (avgLoss == 0f) 100f else avgGain / avgLoss
        result[period] = 100f - (100f / (1f + rs0))

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0f
            val loss = if (change < 0) -change else 0f

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            val rs = if (avgLoss == 0f) 100f else avgGain / avgLoss
            result[i] = 100f - (100f / (1f + rs))
        }
        return result
    }

    fun calculateCipherB(candles: List<CandleEntry>): List<CipherBPoint> {
        if (candles.isEmpty()) return emptyList()

        val closes = candles.map { it.close.toFloat() }
        val highs = candles.map { it.high.toFloat() }
        val lows = candles.map { it.low.toFloat() }
        val hlc3 = candles.map { (it.high.toFloat() + it.low.toFloat() + it.close.toFloat()) / 3f }

        val channelLength = 9
        val averageLength = 12
        val maLength = 3

        val esa = calculateEma(hlc3, channelLength)
        val diffList = hlc3.indices.map { i ->
            val e = esa[i]
            if (e != null) abs(hlc3[i] - e) else 0f
        }
        val d = calculateEma(diffList, channelLength)

        val ciList = hlc3.indices.map { i ->
            val e = esa[i]
            val dev = d[i]
            if (e != null && dev != null && dev != 0f) {
                (hlc3[i] - e) / (0.015f * dev)
            } else 0f
        }

        val wt1List = calculateEma(ciList, averageLength).map { it ?: 0f }

        val wt2List = ArrayList<Float>(wt1List.size)
        for (i in wt1List.indices) {
            if (i < maLength - 1) {
                wt2List.add(wt1List[i])
            } else {
                val sum = (0 until maLength).sumOf { wt1List[i - it].toDouble() }.toFloat()
                wt2List.add(sum / maLength)
            }
        }

        val rsiList = calculateRsi(closes, 14)

        // Thresholds matching backtest_step1_cipher_b.py
        val wtOsLevel = -53f
        val wtObLevel = 53f
        val wtOsLevel3 = -75f
        val wtObLevel3 = 75f
        val wtDivObLevel = 45f
        val wtDivOsLevel = -65f
        val rsiDivObLevel = 60f
        val rsiDivOsLevel = 30f

        // Arrays to store fractal outputs & historical states
        val fTopInd = arrayOfNulls<Float>(candles.size)
        val fBotInd = arrayOfNulls<Float>(candles.size)
        val pTopInd = arrayOfNulls<Float>(candles.size)
        val pTopPrice = arrayOfNulls<Float>(candles.size)
        val pBotInd = arrayOfNulls<Float>(candles.size)
        val pBotPrice = arrayOfNulls<Float>(candles.size)
        val lastRsiTop = arrayOfNulls<Float>(candles.size)
        val lastRsiBot = arrayOfNulls<Float>(candles.size)

        var runningPTopInd: Float? = null
        var runningPTopPrice: Float? = null
        var runningPBotInd: Float? = null
        var runningPBotPrice: Float? = null
        var runningLastRsiTop: Float? = null
        var runningLastRsiBot: Float? = null

        val pendingTopEvents = mutableListOf<Pair<Int, Triple<Float, Float, Float?>>>()
        val pendingBotEvents = mutableListOf<Pair<Int, Triple<Float, Float, Float?>>>()

        for (i in 0 until candles.size) {
            // Apply pending fractal shifts (shift(2) effect from pandas_ta / ffill)
            val topIter = pendingTopEvents.iterator()
            while (topIter.hasNext()) {
                val ev = topIter.next()
                if (i >= ev.first) {
                    runningPTopInd = ev.second.first
                    runningPTopPrice = ev.second.second
                    runningLastRsiTop = ev.second.third
                    topIter.remove()
                }
            }

            val botIter = pendingBotEvents.iterator()
            while (botIter.hasNext()) {
                val ev = botIter.next()
                if (i >= ev.first) {
                    runningPBotInd = ev.second.first
                    runningPBotPrice = ev.second.second
                    runningLastRsiBot = ev.second.third
                    botIter.remove()
                }
            }

            pTopInd[i] = runningPTopInd
            pTopPrice[i] = runningPTopPrice
            pBotInd[i] = runningPBotInd
            pBotPrice[i] = runningPBotPrice
            lastRsiTop[i] = runningLastRsiTop
            lastRsiBot[i] = runningLastRsiBot

            // 5-bar fractal check at index i-2 confirmed at bar i
            if (i >= 4) {
                val s2 = wt2List[i - 2]
                val h2 = highs[i - 2]
                val l2 = lows[i - 2]
                val rsi2 = rsiList[i - 2]

                val s4 = wt2List[i - 4]
                val s3 = wt2List[i - 3]
                val s1 = wt2List[i - 1]
                val s0 = wt2List[i]

                val isTopFractal = (s4 < s2) && (s3 < s2) && (s2 > s1) && (s2 > s0) && (s2 >= wtDivObLevel)
                val isBotFractal = (s4 > s2) && (s3 > s2) && (s2 < s1) && (s2 < s0) && (s2 <= wtDivOsLevel)

                if (isTopFractal) {
                    fTopInd[i] = s2
                    pendingTopEvents.add(Pair(i + 2, Triple(s2, h2, rsi2)))
                }
                if (isBotFractal) {
                    fBotInd[i] = s2
                    pendingBotEvents.add(Pair(i + 2, Triple(s2, l2, rsi2)))
                }
            }
        }

        return candles.indices.map { i ->
            val wt1 = wt1List[i]
            val wt2 = wt2List[i]
            val prevWt1 = if (i > 0) wt1List[i - 1] else wt1
            val prevWt2 = if (i > 0) wt2List[i - 1] else wt2

            val isBullishCross = prevWt1 < prevWt2 && wt1 > wt2
            val isBearishCross = prevWt1 > prevWt2 && wt1 < wt2

            // Standard Buy / Sell Signals (WT2 cross <= -53 or >= 53)
            val buySignal = isBullishCross && wt2 <= wtOsLevel
            val sellSignal = isBearishCross && wt2 >= wtObLevel

            // Gold Signals matching backtest_step1_cipher_b.py exact formula
            var goldBuySignal = false
            var goldSellSignal = false

            if (fBotInd[i] != null) {
                val lCtr = lows[i - 2]
                val sCtr = wt2List[i - 2]
                val prevPrice = pBotPrice[i]
                val prevInd = pBotInd[i]
                val prevRsi = lastRsiBot[i]

                val isBullDiv = prevPrice != null && prevInd != null && (lCtr < prevPrice) && (sCtr > prevInd)

                if (isBullDiv && prevInd != null) {
                    val cond1 = prevInd <= wtOsLevel3
                    val cond2 = wt2 > wtOsLevel3
                    val cond3 = (prevInd - wt2) <= -5f
                    val cond4 = prevRsi == null || prevRsi < rsiDivOsLevel

                    if (cond1 && cond2 && cond3 && cond4) {
                        goldBuySignal = true
                    }
                }
            }

            if (fTopInd[i] != null) {
                val hCtr = highs[i - 2]
                val sCtr = wt2List[i - 2]
                val prevPrice = pTopPrice[i]
                val prevInd = pTopInd[i]
                val prevRsi = lastRsiTop[i]

                val isBearDiv = prevPrice != null && prevInd != null && (hCtr > prevPrice) && (sCtr < prevInd)

                if (isBearDiv && prevInd != null) {
                    val cond1 = prevInd >= wtObLevel3
                    val cond2 = wt2 < wtObLevel3
                    val cond3 = (prevInd - wt2) >= 5f
                    val cond4 = prevRsi == null || prevRsi > rsiDivObLevel

                    if (cond1 && cond2 && cond3 && cond4) {
                        goldSellSignal = true
                    }
                }
            }

            CipherBPoint(
                wt1 = wt1,
                wt2 = wt2,
                isBullishCross = isBullishCross,
                isBearishCross = isBearishCross,
                buySignal = buySignal,
                sellSignal = sellSignal,
                goldBuySignal = goldBuySignal,
                goldSellSignal = goldSellSignal
            )
        }
    }
}
