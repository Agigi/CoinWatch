package dev.shorthouse.coinwatch.ui.screen.favourites.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.ui.component.PercentageChange

@Composable
fun FavouriteItem(
    favouriteCoin: Coin,
    onCoinClick: (Coin) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isFocused) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onCoinClick(favouriteCoin) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (favouriteCoin.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = favouriteCoin.imageUrl,
                        contentDescription = favouriteCoin.name,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = favouriteCoin.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Column {
                    Text(
                        text = favouriteCoin.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = favouriteCoin.symbol,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = favouriteCoin.currentPrice.formattedAmount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            PercentageChange(percentage = favouriteCoin.priceChangePercentage24h)
        }
    }
}
