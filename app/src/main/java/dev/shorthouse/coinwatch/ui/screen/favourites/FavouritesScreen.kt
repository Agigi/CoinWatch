package dev.shorthouse.coinwatch.ui.screen.favourites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewStream
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.data.source.local.datastore.common.CoinSort
import dev.shorthouse.coinwatch.model.Coin
import dev.shorthouse.coinwatch.ui.component.CoinSortChip
import dev.shorthouse.coinwatch.ui.component.LoadingIndicator
import dev.shorthouse.coinwatch.ui.component.ScrollToTopFab
import dev.shorthouse.coinwatch.ui.insets.AppWindowInsets
import dev.shorthouse.coinwatch.ui.screen.favourites.component.FavouriteCondensedItem
import dev.shorthouse.coinwatch.ui.screen.favourites.component.FavouriteItem
import dev.shorthouse.coinwatch.ui.screen.favourites.component.FavouritesEmptyState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    onNavigateDetails: (String) -> Unit,
    onNavigateSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavouriteScreen(
        uiState = uiState,
        onCoinClick = { coin ->
            onNavigateDetails(coin.id)
        },
        onUpdateIsFavouritesCondensed = { isCondensed ->
            viewModel.updateIsFavouritesCondensed(isCondensed)
        },
        onUpdateCoinSort = { coinSort ->
            viewModel.updateCoinSort(coinSort)
        },
        onRefresh = {
            viewModel.pullRefreshFavouriteCoins()
        },
        onDismissError = { errorMessageId ->
            viewModel.dismissErrorMessage(errorMessageId)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteScreen(
    uiState: FavouritesUiState,
    onCoinClick: (Coin) -> Unit,
    onUpdateIsFavouritesCondensed: (Boolean) -> Unit,
    onUpdateCoinSort: (CoinSort) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val showScrollToTopFab by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 2
        }
    }

    uiState.errorMessageIds.firstOrNull()?.let { errorMessageId ->
        val errorMessage = stringResource(errorMessageId)
        LaunchedEffect(errorMessageId) {
            snackbarHostState.showSnackbar(message = errorMessage)
            onDismissError(errorMessageId)
        }
    }

    Scaffold(
        topBar = {
            FavouritesTopBar(
                isFavouritesCondensed = uiState.isFavouritesCondensed,
                onUpdateIsFavouritesCondensed = onUpdateIsFavouritesCondensed,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopFab,
                enter = scaleIn(animationSpec = tween(durationMillis = 200)),
                exit = scaleOut(animationSpec = tween(durationMillis = 200))
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
        contentWindowInsets = AppWindowInsets.horizontalAndBottomContent,
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }

                else -> {
                    FavouritesContent(
                        favouriteCoins = uiState.favouriteCoins,
                        onCoinClick = onCoinClick,
                        isFavouritesCondensed = uiState.isFavouritesCondensed,
                        coinSort = uiState.coinSort,
                        onUpdateCoinSort = onUpdateCoinSort,
                        gridState = gridState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesTopBar(
    isFavouritesCondensed: Boolean,
    onUpdateIsFavouritesCondensed: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.favourites_screen),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        actions = {
            IconButton(
                onClick = { onUpdateIsFavouritesCondensed(!isFavouritesCondensed) }
            ) {
                Icon(
                    imageVector = if (isFavouritesCondensed) {
                        Icons.Rounded.GridView
                    } else {
                        Icons.Rounded.ViewStream
                    },
                    contentDescription = stringResource(R.string.cd_expand_favourites)
                )
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
fun FavouritesContent(
    favouriteCoins: ImmutableList<Coin>,
    onCoinClick: (Coin) -> Unit,
    isFavouritesCondensed: Boolean,
    coinSort: CoinSort,
    onUpdateCoinSort: (CoinSort) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    when {
        favouriteCoins.isEmpty() -> {
            FavouritesEmptyState()
        }

        else -> {
            FavouritesGrid(
                favouriteCoins = favouriteCoins,
                onCoinClick = onCoinClick,
                isFavouritesCondensed = isFavouritesCondensed,
                coinSort = coinSort,
                onUpdateCoinSort = onUpdateCoinSort,
                gridState = gridState,
                modifier = modifier
            )
        }
    }
}

@Composable
fun FavouritesGrid(
    favouriteCoins: ImmutableList<Coin>,
    onCoinClick: (Coin) -> Unit,
    isFavouritesCondensed: Boolean,
    coinSort: CoinSort,
    onUpdateCoinSort: (CoinSort) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = if (isFavouritesCondensed) {
            GridCells.Fixed(1)
        } else {
            GridCells.Adaptive(minSize = 140.dp)
        },
        horizontalArrangement = Arrangement.spacedBy(
            if (isFavouritesCondensed) 0.dp else 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(
            if (isFavouritesCondensed) 0.dp else 16.dp
        ),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp),
        modifier = modifier,
    ) {
        items(
            count = favouriteCoins.size,
            key = { favouriteCoins[it].id },
            itemContent = { index ->
                val favouriteCoinItem = favouriteCoins[index]

                if (isFavouritesCondensed) {
                    val cardShape = when {
                        favouriteCoins.size == 1 -> MaterialTheme.shapes.medium

                        index == 0 -> MaterialTheme.shapes.medium.copy(
                            bottomStart = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        )

                        index == favouriteCoins.size - 1 -> MaterialTheme.shapes.medium.copy(
                            topStart = CornerSize(0.dp),
                            topEnd = CornerSize(0.dp)
                        )

                        else -> RoundedCornerShape(0.dp)
                    }

                    FavouriteCondensedItem(
                        favouriteCoin = favouriteCoinItem,
                        onCoinClick = { onCoinClick(favouriteCoinItem) },
                        cardShape = cardShape,
                        modifier = Modifier.animateItem(
                            placementSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                    )
                } else {
                    FavouriteItem(
                        favouriteCoin = favouriteCoinItem,
                        onCoinClick = { onCoinClick(favouriteCoinItem) },
                        modifier = Modifier.animateItem(
                            placementSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                    )
                }
            }
        )
    }
}
