package com.emptycastle.novery.ui.screens.notification

import com.emptycastle.novery.data.repository.LibraryItem

data class NotificationUiState(
    val novelsWithUpdates: List<LibraryItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalNewChapters: Int = 0,
    val novelsCount: Int = 0,

    // Download state
    val isDownloadingAll: Boolean = false,
    val downloadingNovelUrls: Set<String> = emptySet(), // Track which novels are downloading

    // Mark as seen state
    val isMarkingAllSeen: Boolean = false
)