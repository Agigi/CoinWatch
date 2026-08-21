package dev.shorthouse.coinwatch.model

data class Coin(
    val id: String,
    val name: String,
    val symbol: String,
    val imageUrl: String = "",
    val currentPrice: Price = Price("0"),
    val priceChangePercentage24h: Percentage = Percentage("0"),
    val isFavourite: Boolean = false
)
