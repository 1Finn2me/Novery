// BrowseViewModel.kt
package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetManager
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetSource
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetState
import com.emptycastle.novery.ui.screens.home.shared.LibraryStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowseViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val actionSheetManager = ActionSheetManager()
    val actionSheetState: StateFlow<ActionSheetState> = actionSheetManager.state

    private val defaultTrendingSearches = listOf(
        "Martial God Asura",
        "Solo Leveling",
        "The Beginning After The End",
        "Omniscient Reader",
        "Shadow Slave"
    )

    init {
        refreshProviders()
        loadSearchHistory()
        loadFavoriteProviders()
        loadTrendingSearches()

        viewModelScope.launch {
            launch {
                preferencesManager.appSettings.collect { refreshProviders() }
            }
            launch {
                MainProvider.providersState().collect { refreshProviders() }
            }
            // Observe search history changes
            launch {
                preferencesManager.searchHistory.collect { history ->
                    _uiState.update { it.copy(searchHistory = history) }
                }
            }
            // Observe favorite providers changes
            launch {
                preferencesManager.favoriteProviders.collect { favorites ->
                    _uiState.update { it.copy(favoriteProviders = favorites) }
                    refreshProviders() // Re-sort providers
                }
            }
        }
    }

    // ============================================================================
    // Provider Grid Functions
    // ============================================================================

    private fun refreshProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProviders = true, providerError = null) }

            try {
                val providers = novelRepository.getProviders()
                val favorites = _uiState.value.favoriteProviders
                val sortedProviders = providers.sortedWith(
                    compareByDescending<MainProvider> { it.name in favorites }
                        .thenBy { it.name }
                )
                _uiState.update {
                    it.copy(
                        providers = sortedProviders,
                        isLoadingProviders = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        providerError = e.message ?: "Failed to load providers",
                        isLoadingProviders = false
                    )
                }
            }
        }
    }

    fun retryLoadProviders() = refreshProviders()

    fun toggleFavoriteProvider(providerName: String) {
        preferencesManager.toggleFavoriteProvider(providerName)
    }

    private fun loadFavoriteProviders() {
        _uiState.update { it.copy(favoriteProviders = preferencesManager.getFavoriteProviders()) }
    }

    // ============================================================================
    // Search Functions
    // ============================================================================

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                showSearchHistory = query.isEmpty() && it.searchHistory.isNotEmpty()
            )
        }
    }

    fun onSearchBarFocused() {
        if (_uiState.value.searchQuery.isEmpty()) {
            _uiState.update { it.copy(showSearchHistory = true) }
        }
    }

    fun onSearchBarUnfocused() {
        _uiState.update { it.copy(showSearchHistory = false) }
    }

    fun search(query: String = _uiState.value.searchQuery.trim()) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    isSearching = true,
                    searchResults = emptyMap(),
                    expandedProvider = null,
                    hasSearched = true,
                    showSearchHistory = false
                )
            }

            val results = novelRepository.searchAll(query)

            val successfulResults = results.mapValues { (_, result) ->
                result.getOrNull() ?: emptyList()
            }.filterValues { it.isNotEmpty() }

            _uiState.update {
                it.copy(
                    searchResults = successfulResults,
                    isSearching = false
                )
            }

            // Add to search history with result count
            val totalResults = successfulResults.values.sumOf { it.size }
            preferencesManager.addSearchHistoryItem(
                query = query,
                providerName = null, // Cross-provider search
                resultCount = totalResults
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyMap(),
                hasSearched = false,
                expandedProvider = null,
                showFilters = false
            )
        }
    }

    fun expandProvider(providerName: String?) {
        _uiState.update { it.copy(expandedProvider = providerName) }
    }

    // ============================================================================
    // Search History Functions
    // ============================================================================

    private fun loadSearchHistory() {
        _uiState.update { it.copy(searchHistory = preferencesManager.getSearchHistory()) }
    }

    private fun loadTrendingSearches() {
        _uiState.update { it.copy(trendingSearches = defaultTrendingSearches) }
    }

    fun removeFromSearchHistory(query: String) {
        preferencesManager.removeSearchHistoryItem(query)
    }

    fun clearSearchHistory() {
        preferencesManager.clearSearchHistory()
    }

    // ============================================================================
    // Filter Functions
    // ============================================================================

    fun toggleFiltersPanel() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun updateFilters(filters: SearchFilters) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun toggleProviderFilter(providerName: String) {
        val currentFilters = _uiState.value.filters
        val selectedProviders = currentFilters.selectedProviders.toMutableSet()

        if (providerName in selectedProviders) {
            selectedProviders.remove(providerName)
        } else {
            selectedProviders.add(providerName)
        }

        _uiState.update {
            it.copy(filters = currentFilters.copy(selectedProviders = selectedProviders))
        }
    }

    fun setSortOrder(order: SearchSortOrder) {
        _uiState.update {
            it.copy(filters = it.filters.copy(sortOrder = order))
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = SearchFilters()) }
    }

    // ============================================================================
    // Action Sheet Functions
    // ============================================================================

    fun showActionSheet(novel: Novel) {
        val libraryItem = LibraryStateHolder.getLibraryItem(novel.url)
        actionSheetManager.show(
            novel = novel,
            source = ActionSheetSource.BROWSE,
            libraryItem = libraryItem
        )
    }

    fun hideActionSheet() = actionSheetManager.hide()

    fun updateReadingStatus(status: ReadingStatus) {
        actionSheetManager.updateReadingStatus(status)
    }

    fun addToLibrary(novel: Novel) {
        viewModelScope.launch { actionSheetManager.addToLibrary(novel) }
    }

    fun removeFromLibrary(novelUrl: String) {
        viewModelScope.launch { actionSheetManager.removeFromLibrary(novelUrl) }
    }

    fun getReadingPosition(novelUrl: String) = actionSheetManager.getReadingPosition(novelUrl)

    suspend fun getContinueReadingChapter(novelUrl: String) =
        actionSheetManager.getContinueReadingChapter(novelUrl)
}