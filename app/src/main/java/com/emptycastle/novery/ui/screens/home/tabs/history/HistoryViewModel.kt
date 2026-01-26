package com.emptycastle.novery.ui.screens.home.tabs.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
) : ViewModel() {

    private val historyRepository = RepositoryProvider.getHistoryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()


    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.observeHistory().collect { items ->
                _uiState.update {
                    it.copy(items = items, isLoading = false)
                }
            }
        }
    }

    fun removeFromHistory(novelUrl: String) {
        viewModelScope.launch {
            historyRepository.removeFromHistory(novelUrl)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}