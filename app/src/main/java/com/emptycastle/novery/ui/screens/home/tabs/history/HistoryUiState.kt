package com.emptycastle.novery.ui.screens.home.tabs.history

import com.emptycastle.novery.data.repository.HistoryItem

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = true
)