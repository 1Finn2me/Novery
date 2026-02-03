// BrowseUiState.kt
package com.emptycastle.novery.ui.screens.home.tabs.browse

import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider

/**
 * Filter options for search results
 */
data class SearchFilters(
    val selectedProviders: Set<String> = emptySet(),
    val sortOrder: SearchSortOrder = SearchSortOrder.RELEVANCE
)

enum class SearchSortOrder(val label: String) {
    RELEVANCE("Relevance"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    PROVIDER("Provider")
}

/**
 * UI State for the unified Browse screen
 */
data class BrowseUiState(
    // Provider grid state
    val providers: List<MainProvider> = emptyList(),
    val isLoadingProviders: Boolean = true,
    val providerError: String? = null,
    val favoriteProviders: Set<String> = emptySet(),

    // Search state
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: Map<String, List<Novel>> = emptyMap(),
    val hasSearched: Boolean = false,
    val expandedProvider: String? = null,

    // Search history - using PreferencesManager's type
    val searchHistory: List<PreferencesManager.SearchHistoryItem> = emptyList(),
    val showSearchHistory: Boolean = false,
    val trendingSearches: List<String> = emptyList(),

    // Filters
    val filters: SearchFilters = SearchFilters(),
    val showFilters: Boolean = false
) {
    val isInSearchMode: Boolean
        get() = hasSearched && searchQuery.isNotBlank()

    val showProviderGrid: Boolean
        get() = !isInSearchMode && expandedProvider == null

    val isSearchEmpty: Boolean
        get() = hasSearched && filteredSearchResults.isEmpty() && !isSearching

    val totalSearchResults: Int
        get() = filteredSearchResults.values.sumOf { it.size }

    val filteredSearchResults: Map<String, List<Novel>>
        get() {
            var results = if (filters.selectedProviders.isEmpty()) {
                searchResults
            } else {
                searchResults.filterKeys { it in filters.selectedProviders }
            }

            results = when (filters.sortOrder) {
                SearchSortOrder.NAME_ASC -> results.mapValues { (_, novels) ->
                    novels.sortedBy { it.name.lowercase() }
                }
                SearchSortOrder.NAME_DESC -> results.mapValues { (_, novels) ->
                    novels.sortedByDescending { it.name.lowercase() }
                }
                SearchSortOrder.PROVIDER -> results.toSortedMap()
                SearchSortOrder.RELEVANCE -> results
            }

            return results
        }
}