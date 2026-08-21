package dev.shorthouse.coinwatch.ui.model

enum class ChartPeriod(
    val label: String,
    val stringName: String
) {
    OneMinute("1m", "1m"),
    ThreeMinute("3m", "3m"),
    FiveMinute("5m", "5m"),
    FifteenMinute("15m", "15m"),
    OneHour("1h", "1h"),
    FourHour("4h", "4h"),
    OneDay("1d", "1d"),
    OneWeek("1w", "1w"),
    OneMonth("1Mon", "1Mon")
}
