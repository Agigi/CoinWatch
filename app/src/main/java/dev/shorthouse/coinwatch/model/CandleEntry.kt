package dev.shorthouse.coinwatch.model

import java.math.BigDecimal

data class CandleEntry(
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal = BigDecimal.ZERO,
    val timestamp: Long
)
