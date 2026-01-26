package com.emptycastle.novery.ui.screens.home.tabs.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetManager
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetSource
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetState
import com.emptycastle.novery.ui.screens.home.shared.LibraryStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Each ViewModel has its own ActionSheetManager instance
    private val actionSheetManager = ActionSheetManager()
    val actionSheetState: StateFlow<ActionSheetState> = actionSheetManager.state

    init {
        viewModelScope.launch {
            val providers = novelRepository.getProviders().map { it.name }
            _uiState.update { it.copy(providers = providers) }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, results = emptyMap(), expandedProvider = null) }

            val results = novelRepository.searchAll(query)

            val successfulResults = results.mapValues { (_, result) ->
                result.getOrNull() ?: emptyList()
            }.filterValues { it.isNotEmpty() }

            _uiState.update {
                it.copy(
                    results = successfulResults,
                    isSearching = false
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", results = emptyMap(), expandedProvider = null) }
    }

    fun expandProvider(providerName: String?) {
        _uiState.update { it.copy(expandedProvider = providerName) }
    }

    // ============================================================================
    // Action Sheet
    // ============================================================================

    fun showActionSheet(novel: Novel) {
        val libraryItem = LibraryStateHolder.getLibraryItem(novel.url)
        actionSheetManager.show(
            novel = novel,
            source = ActionSheetSource.SEARCH,
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

    suspend fun getContinueReadingChapter(novelUrl: String) = actionSheetManager.getContinueReadingChapter(novelUrl)
}