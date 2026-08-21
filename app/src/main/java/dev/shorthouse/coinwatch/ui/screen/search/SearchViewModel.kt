package dev.shorthouse.coinwatch.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shorthouse.coinwatch.data.source.remote.DefaultSymbols
import dev.shorthouse.coinwatch.data.source.remote.TradingViewSymbolResult
import dev.shorthouse.coinwatch.data.source.remote.TradingViewSymbolSearchApi
import dev.shorthouse.coinwatch.model.SearchCoin
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val symbolSearchApi: TradingViewSymbolSearchApi
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    val searchQuery = MutableStateFlow("")

    init {
        initialiseUiState()
    }

    fun initialiseUiState() {
        searchQuery
            .debounce(300L)
            .onEach { query ->
                val q = query.trim()
                if (q.isNotBlank()) {
                    _uiState.update { it.copy(isSearching = true) }

                    var results = symbolSearchApi.searchSymbols(q)

                    // Fallback to local default symbols if API request fails
                    if (results.isEmpty()) {
                        val localMatches = DefaultSymbols.SYMBOLS.filter {
                            it.contains(q, ignoreCase = true)
                        }.map {
                            TradingViewSymbolResult(
                                symbol = it.substringAfter(":"),
                                fullName = it,
                                description = it.substringAfter(":"),
                                exchange = it.substringBefore(":")
                            )
                        }

                        val directMatch = TradingViewSymbolResult(
                            symbol = q.substringAfter(":").uppercase(),
                            fullName = q.uppercase(),
                            description = q.uppercase(),
                            exchange = if (q.contains(":")) q.substringBefore(":").uppercase() else ""
                        )

                        results = (localMatches + listOf(directMatch)).distinctBy { it.fullName }
                    }

                    val searchResults = results.map {
                        SearchCoin(
                            id = it.fullName,
                            name = it.symbol,
                            symbol = it.description,
                            imageUrl = it.logoUrl,
                            exchange = it.exchange,
                            exchangeLogoUrl = it.exchangeLogoUrl,
                            typespecs = it.typespecs
                        )
                    }.toPersistentList()

                    _uiState.update {
                        it.copy(
                            searchResults = searchResults,
                            queryHasNoResults = searchResults.isEmpty(),
                            isSearching = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            searchResults = persistentListOf(),
                            queryHasNoResults = false,
                            isSearching = false,
                            errorMessage = null
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
