package dev.shorthouse.coinwatch.ui.screen.search.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.shorthouse.coinwatch.model.SearchCoin

@Composable
fun SearchListItem(
    searchCoin: SearchCoin,
    onCoinClick: (SearchCoin) -> Unit,
    cardShape: Shape = MaterialTheme.shapes.medium,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = cardShape,
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
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onCoinClick(searchCoin) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Left: Symbol Logo / Initial Badge
            if (searchCoin.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = searchCoin.imageUrl,
                    contentDescription = searchCoin.name,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = searchCoin.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            // Left Column: Symbol & Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = searchCoin.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = searchCoin.symbol,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            // Right Column: Type Specs & Exchange Name + Logo
            Column(horizontalAlignment = Alignment.End) {
                if (searchCoin.typespecs.isNotBlank()) {
                    Text(
                        text = searchCoin.typespecs,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchCoin.exchange.isNotBlank()) {
                        Text(
                            text = searchCoin.exchange,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (searchCoin.exchangeLogoUrl.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        AsyncImage(
                            model = searchCoin.exchangeLogoUrl,
                            contentDescription = searchCoin.exchange,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }
}
