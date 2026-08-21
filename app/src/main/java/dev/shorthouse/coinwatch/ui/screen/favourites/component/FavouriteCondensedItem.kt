package dev.shorthouse.coinwatch.ui.screen.favourites.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.ui.component.PercentageChange

@Composable
fun FavouriteCondensedItem(
    favouriteCoin: Coin,
    onCoinClick: (Coin) -> Unit,
    cardShape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCoinClick(favouriteCoin) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favouriteCoin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = favouriteCoin.symbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(4.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    favouriteCoin.currentPrice.formattedAmount,
                    style = MaterialTheme.typography.bodyMedium
                )

                PercentageChange(percentage = favouriteCoin.priceChangePercentage24h)
            }
        }
    }
}
