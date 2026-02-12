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
 * State for individual provider search
 */
sealed class ProviderSearchState {
    data object Loading : ProviderSearchState()
    data class Success(val novels: List<Novel>) : ProviderSearchState()
    data class Error(val message: String) : ProviderSearchState()
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
    val hasSearched: Boolean = false,
    val expandedProvider: String? = null,

    // Real-time search results per provider
    val providerSearchStates: Map<String, ProviderSearchState> = emptyMap(),

    // Search history
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

    /**
     * Get the actual search results map (only successful results)
     */
    val searchResults: Map<String, List<Novel>>
        get() = providerSearchStates.mapNotNull { (name, state) ->
            when (state) {
                is ProviderSearchState.Success -> name to state.novels
                else -> null
            }
        }.toMap()

    /**
     * Check if all providers have no results
     */
    val isSearchEmpty: Boolean
        get() = hasSearched &&
                !isSearching &&
                providerSearchStates.isNotEmpty() &&
                providerSearchStates.values.all { state ->
                    state is ProviderSearchState.Success && state.novels.isEmpty() ||
                            state is ProviderSearchState.Error
                }

    val totalSearchResults: Int
        get() = filteredSearchResults.values.sumOf { it.size }

    val providersWithResults: Int
        get() = filteredSearchResults.count { it.value.isNotEmpty() }

    val totalProviders: Int
        get() = providerSearchStates.size

    val loadingProvidersCount: Int
        get() = providerSearchStates.count { it.value is ProviderSearchState.Loading }

    val completedProvidersCount: Int
        get() = providerSearchStates.count { it.value !is ProviderSearchState.Loading }

    /**
     * Filtered search history based on current query
     */
    val filteredSearchHistory: List<PreferencesManager.SearchHistoryItem>
        get() {
            if (searchQuery.isBlank()) return searchHistory
            val queryLower = searchQuery.lowercase().trim()
            return searchHistory.filter { item ->
                item.query.lowercase().contains(queryLower)
            }
        }

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

    /**
     * Get filtered provider states (respects filter settings)
     */
    val filteredProviderStates: Map<String, ProviderSearchState>
        get() = if (filters.selectedProviders.isEmpty()) {
            providerSearchStates
        } else {
            providerSearchStates.filterKeys { it in filters.selectedProviders }
        }
}