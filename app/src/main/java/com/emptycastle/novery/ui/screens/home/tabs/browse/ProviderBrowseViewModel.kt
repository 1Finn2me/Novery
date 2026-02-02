package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetManager
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetSource
import com.emptycastle.novery.ui.screens.home.shared.LibraryStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.emptycastle.novery.provider.MainProvider

class ProviderBrowseViewModel(
    private val providerName: String
) : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()

    private val _uiState = MutableStateFlow(ProviderBrowseUiState())
    val uiState: StateFlow<ProviderBrowseUiState> = _uiState.asStateFlow()

    // Each ViewModel has its own ActionSheetManager instance
    private val actionSheetManager = ActionSheetManager()
    val actionSheetState = actionSheetManager.state

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        private const val MIN_SEARCH_LENGTH = 2
    }

    init {
        // Live initialize and react to preference/provider registry changes
        var hadProvider = false

        viewModelScope.launch {
            // React to app settings changes
            launch {
                RepositoryProvider.getPreferencesManager().appSettings.collect {
                    val providers = novelRepository.getProviders()
                    val provider = providers.find { it.name == providerName }

                    if (provider != null) {
                        _uiState.update {
                            it.copy(
                                provider = provider,
                                selectedTag = provider.tags.firstOrNull()?.value,
                                selectedSort = provider.orderBys.firstOrNull()?.value,
                                isLoading = false,
                                error = null
                            )
                        }

                        if (!hadProvider) {
                            hadProvider = true
                            loadPage()
                        }
                    } else {
                        hadProvider = false
                        _uiState.update {
                            it.copy(
                                provider = null,
                                error = "Provider '$providerName' not found",
                                isLoading = false
                            )
                        }
                    }
                }
            }

            // React to provider registry changes
            launch {
                MainProvider.providersState().collect {
                    val providers = novelRepository.getProviders()
                    val provider = providers.find { it.name == providerName }

                    if (provider != null) {
                        _uiState.update {
                            it.copy(
                                provider = provider,
                                selectedTag = provider.tags.firstOrNull()?.value,
                                selectedSort = provider.orderBys.firstOrNull()?.value,
                                isLoading = false,
                                error = null
                            )
                        }

                        if (!hadProvider) {
                            hadProvider = true
                            loadPage()
                        }
                    } else {
                        hadProvider = false
                        _uiState.update {
                            it.copy(
                                provider = null,
                                error = "Provider '$providerName' not found",
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================================================================
    // Refresh (Pull-to-Refresh)
    // ============================================================================

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }

        if (_uiState.value.isSearchMode) {
            refreshSearch()
        } else {
            refreshBrowse()
        }
    }

    private fun refreshSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < MIN_SEARCH_LENGTH) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(query, isRefresh = true)
        }
    }

    private fun refreshBrowse() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            executeLoadPage(isRefresh = true)
        }
    }

    // ============================================================================
    // Search Functions
    // ============================================================================

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            exitSearchMode()
            return
        }

        if (query.length < MIN_SEARCH_LENGTH) {
            return
        }

        // Debounced auto-search
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            executeSearch(query)
        }
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < MIN_SEARCH_LENGTH) {
            _uiState.update {
                it.copy(
                    searchError = "Please enter at least $MIN_SEARCH_LENGTH characters"
                )
            }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(query)
        }
    }

    private suspend fun executeSearch(query: String, isRefresh: Boolean = false) {
        val trimmedQuery = query.trim()
        val provider = _uiState.value.provider ?: return

        _uiState.update {
            it.copy(
                isSearchMode = true,
                isSearching = !isRefresh,
                isRefreshing = isRefresh,
                searchError = null
            )
        }

        try {
            val result = novelRepository.searchInProvider(
                provider = provider,
                query = trimmedQuery
            )

            result.fold(
                onSuccess = { novels ->
                    _uiState.update {
                        it.copy(
                            searchResults = novels,
                            isSearching = false,
                            isRefreshing = false,
                            searchError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            searchError = error.message ?: "Search failed",
                            isSearching = false,
                            isRefreshing = false
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    searchError = e.message ?: "Search failed",
                    isSearching = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearchMode = false,
                isSearching = false,
                isRefreshing = false,
                searchError = null
            )
        }
    }

    private fun exitSearchMode() {
        _uiState.update {
            it.copy(
                isSearchMode = false,
                searchResults = emptyList(),
                searchError = null,
                isSearching = false
            )
        }
    }

    // ============================================================================
    // Browse Functions
    // ============================================================================

    fun loadPage() {
        if (_uiState.value.isSearchMode) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            executeLoadPage(isRefresh = false)
        }
    }

    private suspend fun executeLoadPage(isRefresh: Boolean) {
        val provider = _uiState.value.provider ?: return

        _uiState.update {
            it.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null,
                isCloudflareError = false
            )
        }

        try {
            val result = novelRepository.loadMainPage(
                provider = provider,
                page = _uiState.value.currentPage,
                orderBy = _uiState.value.selectedSort,
                tag = _uiState.value.selectedTag
            )

            result.fold(
                onSuccess = { pageResult ->
                    _uiState.update {
                        it.copy(
                            novels = pageResult.novels,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            isCloudflareError = false
                        )
                    }
                },
                onFailure = { error ->
                    handleLoadError(error)
                }
            )
        } catch (e: Exception) {
            handleLoadError(e)
        }
    }

    private fun handleLoadError(error: Throwable) {
        val errorMessage = error.message ?: "Failed to load novels"
        val isCloudflare = isCloudflareError(errorMessage)

        _uiState.update {
            it.copy(
                error = if (isCloudflare) {
                    "This source requires verification. Please open in WebView to verify."
                } else {
                    errorMessage
                },
                isLoading = false,
                isRefreshing = false,
                isCloudflareError = isCloudflare
            )
        }
    }

    private fun isCloudflareError(message: String): Boolean {
        val lowerMessage = message.lowercase()
        val cloudflareIndicators = listOf(
            "cloudflare", "cf_clearance", "challenge", "403", "503",
            "blocked", "just a moment", "verify you are human", "checking your browser"
        )
        return cloudflareIndicators.any { lowerMessage.contains(it) }
    }

    fun setSelectedTag(tag: String?) {
        if (_uiState.value.isSearchMode) exitSearchMode()

        val newTag = if (_uiState.value.selectedTag == tag) {
            _uiState.value.provider?.tags?.firstOrNull()?.value
        } else {
            tag
        }

        _uiState.update {
            it.copy(
                selectedTag = newTag,
                currentPage = 1,
                novels = emptyList(),
                error = null
            )
        }
        loadPage()
    }

    fun setSelectedSort(sort: String?) {
        if (_uiState.value.isSearchMode) exitSearchMode()

        val newSort = if (_uiState.value.selectedSort == sort) {
            _uiState.value.provider?.orderBys?.firstOrNull()?.value
        } else {
            sort
        }

        _uiState.update {
            it.copy(
                selectedSort = newSort,
                currentPage = 1,
                novels = emptyList(),
                error = null
            )
        }
        loadPage()
    }

    fun nextPage() {
        if (_uiState.value.isSearchMode || _uiState.value.isLoading || _uiState.value.isRefreshing) return

        _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        loadPage()
    }

    fun previousPage() {
        if (_uiState.value.isSearchMode || _uiState.value.isLoading || _uiState.value.isRefreshing) return

        if (_uiState.value.currentPage > 1) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
            loadPage()
        }
    }

    fun clearFilters() {
        val provider = _uiState.value.provider ?: return
        if (_uiState.value.isSearchMode) exitSearchMode()

        _uiState.update {
            it.copy(
                selectedSort = provider.orderBys.firstOrNull()?.value,
                selectedTag = provider.tags.firstOrNull()?.value,
                currentPage = 1,
                novels = emptyList()
            )
        }
        loadPage()
    }

    // ============================================================================
    // Action Sheet - Uses instance, not singleton
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

    suspend fun getHistoryChapter(novelUrl: String) = actionSheetManager.getHistoryChapter(novelUrl)

    suspend fun getContinueReadingChapter(novelUrl: String) = actionSheetManager.getContinueReadingChapter(novelUrl)

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        loadJob?.cancel()
    }

    class Factory(private val providerName: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProviderBrowseViewModel(providerName) as T
        }
    }
}