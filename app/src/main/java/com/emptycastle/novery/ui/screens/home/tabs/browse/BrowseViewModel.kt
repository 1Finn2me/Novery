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
import kotlinx.coroutines.Job
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

    // Track current search job to cancel if new search starts
    private var currentSearchJob: Job? = null

    // Track if search bar is focused
    private var isSearchBarFocused = false

    private val defaultTrendingSearches = listOf(
        "Shadow Slave",
        "Reverend Insanity",
        "Lord of the Mysteries",
        "Solo Leveling",
        "The Beginning After The End",
        "Omniscient Reader"
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
            launch {
                preferencesManager.searchHistory.collect { history ->
                    _uiState.update { it.copy(searchHistory = history) }
                }
            }
            launch {
                preferencesManager.favoriteProviders.collect { favorites ->
                    _uiState.update { it.copy(favoriteProviders = favorites) }
                    refreshProviders()
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
                showSearchHistory = isSearchBarFocused && !it.hasSearched
            )
        }
    }

    fun onSearchBarFocused() {
        isSearchBarFocused = true
        _uiState.update {
            it.copy(showSearchHistory = !it.hasSearched)
        }
    }

    fun onSearchBarUnfocused() {
        isSearchBarFocused = false
        viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            if (!isSearchBarFocused) {
                _uiState.update { it.copy(showSearchHistory = false) }
            }
        }
    }

    fun search(query: String = _uiState.value.searchQuery.trim()) {
        if (query.isBlank()) return

        // Cancel any existing search
        currentSearchJob?.cancel()

        currentSearchJob = viewModelScope.launch {
            // Get all provider names to initialize loading states
            val allProviders = try {
                novelRepository.getProviders().map { it.name }
            } catch (e: Exception) {
                emptyList()
            }

            // Initialize all providers as loading
            val initialStates = allProviders.associateWith { ProviderSearchState.Loading }

            _uiState.update {
                it.copy(
                    searchQuery = query,
                    isSearching = true,
                    providerSearchStates = initialStates,
                    expandedProvider = null,
                    hasSearched = true,
                    showSearchHistory = false
                )
            }

            // Collect streaming results
            var totalResults = 0

            try {
                novelRepository.searchAllStreaming(query).collect { (providerName, result) ->
                    _uiState.update { state ->
                        val newStates = state.providerSearchStates.toMutableMap()

                        result.fold(
                            onSuccess = { novels ->
                                newStates[providerName] = ProviderSearchState.Success(novels)
                                totalResults += novels.size
                            },
                            onFailure = { error ->
                                newStates[providerName] = ProviderSearchState.Error(
                                    error.message ?: "Search failed"
                                )
                            }
                        )

                        // Check if all providers have completed
                        val allCompleted = newStates.values.none { it is ProviderSearchState.Loading }

                        state.copy(
                            providerSearchStates = newStates,
                            isSearching = !allCompleted
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle overall search failure
                _uiState.update { state ->
                    state.copy(isSearching = false)
                }
            }

            // Add to search history after all results are in
            preferencesManager.addSearchHistoryItem(
                query = query,
                providerName = null,
                resultCount = totalResults
            )
        }
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        isSearchBarFocused = false
        _uiState.update {
            it.copy(
                searchQuery = "",
                providerSearchStates = emptyMap(),
                hasSearched = false,
                expandedProvider = null,
                showFilters = false,
                showSearchHistory = false,
                isSearching = false
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

    fun selectHistoryItem(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                showSearchHistory = true
            )
        }
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