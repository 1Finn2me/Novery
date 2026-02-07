// ui/screens/home/tabs/more/MoreViewModel.kt

package com.emptycastle.novery.ui.screens.home.tabs.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MoreUiState(
    val isLoading: Boolean = false,

    // Reader info
    val readerLevel: Int = 1,
    val readerLevelName: String = "Novice",

    // Streak
    val currentStreak: Int = 0,
    val isStreakActive: Boolean = false,

    // Stats summary
    val totalChaptersRead: Int = 0,
    val totalHours: Long = 0,

    // Downloads
    val totalDownloads: Int = 0,
    val activeDownloads: Int = 0
)

class MoreViewModel : ViewModel() {

    private val statsRepository = RepositoryProvider.getStatsRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()

    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    private data class ReaderLevel(
        val level: Int,
        val title: String,
        val minHours: Int
    )

    private val readerLevels = listOf(
        ReaderLevel(1, "Novice", 0),
        ReaderLevel(2, "Apprentice", 5),
        ReaderLevel(3, "Bookworm", 15),
        ReaderLevel(4, "Scholar", 30),
        ReaderLevel(5, "Sage", 60),
        ReaderLevel(6, "Master", 100),
        ReaderLevel(7, "Grand Master", 200),
        ReaderLevel(8, "Legendary", 500)
    )

    init {
        loadData()
        observeStreak()
    }

    private fun observeStreak() {
        viewModelScope.launch {
            statsRepository.observeStreak().collect { streak ->
                if (streak != null) {
                    val today = LocalDate.now().toEpochDay()
                    val isActive = streak.lastReadDate >= today - 1
                    val totalHours = streak.totalReadingTimeSeconds / 3600

                    // Calculate level
                    val level = readerLevels.lastOrNull { totalHours >= it.minHours }
                        ?: readerLevels.first()

                    _uiState.update {
                        it.copy(
                            currentStreak = streak.currentStreak,
                            isStreakActive = isActive,
                            totalHours = totalHours,
                            readerLevel = level.level,
                            readerLevelName = level.title
                        )
                    }
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load total chapters
                val allStats = statsRepository.getDailyStats(0, Long.MAX_VALUE)
                val totalChapters = allStats.sumOf { it.chaptersRead }

                // Load download counts
                val downloadCounts = offlineRepository.getAllDownloadCounts()
                val totalDownloads = downloadCounts.values.sum()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalChaptersRead = totalChapters,
                        totalDownloads = totalDownloads
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}