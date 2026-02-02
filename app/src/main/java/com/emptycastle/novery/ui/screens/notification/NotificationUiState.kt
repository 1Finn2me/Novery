package com.emptycastle.novery.ui.screens.notification

import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.NotificationEntry

/**
 * Combined notification item with library data and notification state
 */
data class NotificationDisplayItem(
    val libraryItem: LibraryItem,
    val notificationEntry: NotificationEntry,
    val isNew: Boolean
)

data class NotificationUiState(
    val displayItems: List<NotificationDisplayItem> = emptyList(),
    val isLoading: Boolean = true,

    val totalNewChapters: Int = 0,
    val totalNovelsCount: Int = 0,
    val unacknowledgedCount: Int = 0,

    val isDownloadingAll: Boolean = false,
    val downloadingNovelUrls: Set<String> = emptySet(),

    val isMarkingAllSeen: Boolean = false,

    val showClearConfirmation: Boolean = false
)