package dev.shorthouse.coinwatch.ui.screen.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.model.SearchCoin
import dev.shorthouse.coinwatch.ui.component.ErrorState
import dev.shorthouse.coinwatch.ui.component.LoadingIndicator
import dev.shorthouse.coinwatch.ui.insets.AppWindowInsets
import dev.shorthouse.coinwatch.ui.screen.search.component.SearchListItem
import dev.shorthouse.coinwatch.ui.screen.search.component.SearchQueryEmptyState
import dev.shorthouse.coinwatch.ui.screen.search.component.SearchResultsEmptyState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateDetails: (String) -> Unit,
    onNavigateSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    SearchScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onQueryChange = { viewModel.updateSearchQuery(it) },
        onCoinClick = { coin ->
            onNavigateDetails(coin.id)
        }
    )
}

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onCoinClick: (SearchCoin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
            )
        },
        contentWindowInsets = AppWindowInsets.horizontalAndBottomContent,
        modifier = modifier.fillMaxSize()
    ) { scaffoldPadding ->
        when {
            uiState.isSearching -> {
                LoadingIndicator(modifier = Modifier.padding(scaffoldPadding))
            }

            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }

            else -> {
                SearchContent(
                    queryIsEmpty = searchQuery.isEmpty(),
                    searchResults = uiState.searchResults,
                    queryHasNoResults = uiState.queryHasNoResults,
                    onCoinClick = onCoinClick,
                    modifier = Modifier.padding(scaffoldPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {
            keyboardController?.hide()
            focusManager.moveFocus(FocusDirection.Down)
        },
        expanded = true,
        onExpandedChange = {},
        enabled = true,
        placeholder = {
            Text(
                text = stringResource(R.string.search_coins_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(R.string.cd_clear_search)
                    )
                }
            }
        },
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ).inputFieldColors,
        modifier = modifier
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(8.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionDown -> {
                            keyboardController?.hide()
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        }
                        Key.DirectionUp -> {
                            keyboardController?.hide()
                            focusManager.moveFocus(FocusDirection.Up)
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            keyboardController?.hide()
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    )
}

@Composable
fun SearchContent(
    queryIsEmpty: Boolean,
    searchResults: ImmutableList<SearchCoin>,
    queryHasNoResults: Boolean,
    onCoinClick: (SearchCoin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        when {
            queryIsEmpty -> {
                SearchQueryEmptyState()
            }

            queryHasNoResults -> {
                SearchResultsEmptyState()
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = searchResults,
                        key = { coin -> coin.id }
                    ) { item ->
                        SearchListItem(
                            searchCoin = item,
                            onCoinClick = onCoinClick
                        )
                    }
                }
            }
        }
    }
}
