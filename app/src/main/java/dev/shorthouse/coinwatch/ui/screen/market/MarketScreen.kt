package dev.shorthouse.coinwatch.ui.screen.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.data.source.local.datastore.common.CoinSort
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.model.Percentage
import dev.shorthouse.coinwatch.ui.component.CoinSortChip
import dev.shorthouse.coinwatch.ui.component.LoadingIndicator
import dev.shorthouse.coinwatch.ui.component.ScrollToTopFab
import dev.shorthouse.coinwatch.ui.insets.AppWindowInsets
import dev.shorthouse.coinwatch.ui.model.TimeOfDay
import dev.shorthouse.coinwatch.ui.preview.AppPreviewWrapper
import dev.shorthouse.coinwatch.ui.preview.MarketUiStatePreviewProvider
import dev.shorthouse.coinwatch.ui.preview.PreviewWrapper
import dev.shorthouse.coinwatch.ui.screen.favourites.component.FavouriteItem
import dev.shorthouse.coinwatch.ui.screen.market.component.MarketDropdownMenu
import dev.shorthouse.coinwatch.ui.screen.market.component.SearchPrompt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
    onNavigateDetails: (String) -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MarketScreen(
        uiState = uiState,
        onCoinClick = { coin ->
            onNavigateDetails(coin.id)
        },
        onNavigateSettings = onNavigateSettings,
        onUpdateCoinSort = { coinSort ->
            viewModel.updateCoinSort(coinSort)
        },
        onRefresh = {
            viewModel.pullRefreshCoins()
        },
        onDismissError = { errorMessageId ->
            viewModel.dismissErrorMessage(errorMessageId)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    uiState: MarketUiState,
    onCoinClick: (Coin) -> Unit,
    onNavigateSettings: () -> Unit,
    onUpdateCoinSort: (CoinSort) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val showScrollToTopFab by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 2
        }
    }

    Scaffold(
        topBar = {
            MarketTopBar(
                timeOfDay = uiState.timeOfDay,
                marketCapChangePercentage24h = uiState.marketCapChangePercentage24h,
                onNavigateSettings = onNavigateSettings,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopFab,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                ScrollToTopFab(
                    onClick = {
                        scope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    }
                )
            }
        },
        contentWindowInsets = AppWindowInsets.horizontalContent,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }

                else -> {
                    MarketContent(
                        coins = uiState.coins,
                        onCoinClick = onCoinClick,
                        coinSort = uiState.coinSort,
                        onUpdateCoinSort = onUpdateCoinSort,
                        gridState = gridState
                    )
                }
            }
            if (uiState.errorMessageIds.isNotEmpty()) {
                val errorMessage = stringResource(uiState.errorMessageIds.first())

                LaunchedEffect(errorMessage, snackbarHostState) {
                    snackbarHostState.showSnackbar(message = errorMessage)
                    onDismissError(uiState.errorMessageIds.first())
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MarketTopBar(
    timeOfDay: TimeOfDay,
    marketCapChangePercentage24h: Percentage?,
    onNavigateSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Column(modifier = Modifier.animateContentSize()) {
                Text(
                    text = stringResource(R.string.time_of_day_prefix_good) +
                        " " + timeOfDay.name.lowercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                marketCapChangePercentage24h?.let {
                    Text(
                        text = when {
                            it.isPositive -> stringResource(R.string.market_is_up)
                            it.isNegative -> stringResource(R.string.market_is_down)
                            else -> stringResource(R.string.market_is_flat)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
private fun MarketContent(
    coins: ImmutableList<Coin>,
    onCoinClick: (Coin) -> Unit,
    coinSort: CoinSort,
    onUpdateCoinSort: (CoinSort) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    if (coins.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {}
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp),
            modifier = modifier
        ) {
            items(
                count = coins.size,
                key = { coins[it].id }
            ) { index ->
                val coinItem = coins[index]
                FavouriteItem(
                    favouriteCoin = coinItem,
                    onCoinClick = { onCoinClick(coinItem) }
                )
            }

            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SearchPrompt(modifier = Modifier.padding(vertical = 24.dp))
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@PreviewWrapper(wrapper = AppPreviewWrapper::class)
private fun MarketScreenPreview(
    @PreviewParameter(MarketUiStatePreviewProvider::class) uiState: MarketUiState,
) {
    MarketScreen(
        uiState = uiState,
        onCoinClick = {},
        onNavigateSettings = {},
        onUpdateCoinSort = {},
        onRefresh = {},
        onDismissError = {}
    )
}
