package com.emptycastle.novery.ui.screens.home.tabs.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class HistoryDateGroup(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    EARLIER("Earlier")
}

data class HistoryUiState(
    val groupedItems: Map<HistoryDateGroup, List<HistoryItem>> = emptyMap(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val showClearConfirmation: Boolean = false
)

class HistoryViewModel : ViewModel() {

    private val historyRepository = RepositoryProvider.getHistoryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.observeHistory().collect { items ->
                val grouped = groupItemsByDate(items)
                _uiState.update {
                    it.copy(
                        groupedItems = grouped,
                        totalCount = items.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun groupItemsByDate(items: List<HistoryItem>): Map<HistoryDateGroup, List<HistoryItem>> {
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        return items
            .sortedByDescending { it.timestamp }
            .groupBy { item ->
                val itemDate = Instant.ofEpochMilli(item.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()

                val daysBetween = ChronoUnit.DAYS.between(itemDate, now)

                when {
                    daysBetween == 0L -> HistoryDateGroup.TODAY
                    daysBetween == 1L -> HistoryDateGroup.YESTERDAY
                    daysBetween <= 7L -> HistoryDateGroup.THIS_WEEK
                    daysBetween <= 30L -> HistoryDateGroup.THIS_MONTH
                    else -> HistoryDateGroup.EARLIER
                }
            }
    }

    fun removeFromHistory(novelUrl: String) {
        viewModelScope.launch {
            historyRepository.removeFromHistory(novelUrl)
        }
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.update { it.copy(showClearConfirmation = false) }
        }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }
}