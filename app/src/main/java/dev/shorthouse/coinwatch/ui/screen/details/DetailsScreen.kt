package dev.shorthouse.coinwatch.ui.screen.details

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.model.CandleEntry
import dev.shorthouse.coinwatch.model.CoinDetails
import dev.shorthouse.coinwatch.ui.component.ErrorState
import dev.shorthouse.coinwatch.ui.component.LoadingIndicator
import dev.shorthouse.coinwatch.ui.insets.AppWindowInsets
import dev.shorthouse.coinwatch.ui.model.ChartPeriod
import dev.shorthouse.coinwatch.ui.screen.details.component.CoinChartCard
import dev.shorthouse.coinwatch.ui.screen.details.component.EmptyTopBar
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onClickFavouriteCoin = { viewModel.toggleIsCoinFavourite() },
        onClickChartPeriod = { chartPeriod ->
            viewModel.updateChartPeriod(chartPeriod)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    uiState: DetailsUiState,
    onNavigateUp: () -> Unit,
    onClickFavouriteCoin: () -> Unit,
    onClickChartPeriod: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            when (uiState) {
                is DetailsUiState.Success -> {
                    DetailsTopBar(
                        coinDetails = uiState.coinDetails,
                        isCoinFavourite = uiState.isCoinFavourite,
                        onNavigateUp = onNavigateUp,
                        onClickFavouriteCoin = onClickFavouriteCoin,
                        scrollBehavior = scrollBehavior
                    )
                }

                else -> {
                    EmptyTopBar(
                        onNavigateUp = onNavigateUp
                    )
                }
            }
        },
        contentWindowInsets = AppWindowInsets.horizontalAndBottomContent,
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { scaffoldPadding ->
        when (uiState) {
            is DetailsUiState.Success -> {
                DetailsContent(
                    coinDetails = uiState.coinDetails,
                    candles = uiState.candles,
                    chartPeriod = uiState.chartPeriod,
                    onClickChartPeriod = onClickChartPeriod,
                    scrollState = scrollState,
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }

            is DetailsUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }

            is DetailsUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.padding(scaffoldPadding))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailsTopBar(
    coinDetails: CoinDetails,
    isCoinFavourite: Boolean,
    onNavigateUp: () -> Unit,
    onClickFavouriteCoin: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeTopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_top_bar_back)
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (coinDetails.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = coinDetails.imageUrl,
                        contentDescription = coinDetails.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = coinDetails.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = coinDetails.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = coinDetails.symbol,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (coinDetails.exchange.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = coinDetails.exchange,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onClickFavouriteCoin) {
                Icon(
                    imageVector = if (isCoinFavourite) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = stringResource(R.string.cd_top_bar_favourite),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
fun DetailsContent(
    coinDetails: CoinDetails,
    candles: ImmutableList<CandleEntry>,
    chartPeriod: ChartPeriod,
    onClickChartPeriod: (ChartPeriod) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        CoinChartCard(
            currentPrice = coinDetails.currentPrice,
            candles = candles,
            chartPeriod = chartPeriod,
            onClickChartPeriod = onClickChartPeriod,
            exchange = coinDetails.exchange
        )
    }
}
