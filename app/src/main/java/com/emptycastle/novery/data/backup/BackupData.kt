// com/emptycastle/novery/data/backup/BackupData.kt

package com.emptycastle.novery.data.backup

import kotlinx.serialization.Serializable

/**
 * Complete backup data structure
 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val deviceInfo: String = "",

    // Core data
    val library: List<LibraryBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val history: List<HistoryBackup> = emptyList(),
    val readChapters: List<ReadChapterBackup> = emptyList(),

    // Statistics
    val readingStats: List<ReadingStatsBackup> = emptyList(),
    val readingStreak: ReadingStreakBackup? = null,

    // Settings
    val appSettings: AppSettingsBackup? = null,
    val readerSettings: ReaderSettingsBackup? = null
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_EXTENSION = "novery"
        const val MIME_TYPE = "application/json"
    }
}

@Serializable
data class LibraryBackup(
    val url: String,
    val name: String,
    val posterUrl: String? = null,
    val apiName: String,
    val latestChapter: String? = null,
    val addedAt: Long,
    val readingStatus: String,
    val lastChapterUrl: String? = null,
    val lastChapterName: String? = null,
    val lastReadAt: Long? = null,
    val lastScrollIndex: Int = 0,
    val lastScrollOffset: Int = 0,
    val totalChapterCount: Int = 0,
    val acknowledgedChapterCount: Int = 0,
    val lastCheckedAt: Long = 0,
    val lastUpdatedAt: Long = 0,
    val lastReadChapterIndex: Int = -1,
    val unreadChapterCount: Int = 0
)

@Serializable
data class BookmarkBackup(
    val novelUrl: String,
    val novelName: String,
    val chapterUrl: String,
    val chapterName: String,
    val segmentId: String? = null,
    val segmentIndex: Int = 0,
    val textSnippet: String? = null,
    val note: String? = null,
    val category: String = "default",
    val color: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class HistoryBackup(
    val novelUrl: String,
    val novelName: String,
    val posterUrl: String? = null,
    val chapterName: String,
    val chapterUrl: String,
    val apiName: String,
    val timestamp: Long
)

@Serializable
data class ReadChapterBackup(
    val chapterUrl: String,
    val novelUrl: String,
    val readAt: Long
)

@Serializable
data class ReadingStatsBackup(
    val novelUrl: String,
    val novelName: String,
    val date: Long,
    val readingTimeSeconds: Long,
    val chaptersRead: Int,
    val wordsRead: Long,
    val sessionsCount: Int,
    val longestSessionSeconds: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ReadingStreakBackup(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadDate: Long,
    val totalDaysRead: Int,
    val totalReadingTimeSeconds: Long,
    val updatedAt: Long
)

@Serializable
data class AppSettingsBackup(
    val themeMode: String = "DARK",
    val amoledBlack: Boolean = false,
    val useDynamicColor: Boolean = false,
    val uiDensity: String = "DEFAULT",
    val libraryGridColumns: Int = 0,
    val browseGridColumns: Int = 0,
    val searchGridColumns: Int = 0,
    val showBadges: Boolean = true,
    val libraryDisplayMode: String = "GRID",
    val browseDisplayMode: String = "GRID",
    val searchDisplayMode: String = "GRID",
    val ratingFormat: String = "TEN_POINT",
    val defaultLibrarySort: String = "LAST_READ",
    val defaultLibraryFilter: String = "DOWNLOADED",
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadOnWifiOnly: Boolean = true,
    val autoDownloadLimit: Int = 10,
    val autoDownloadForStatuses: List<String> = listOf("READING"),
    val searchResultsPerProvider: Int = 6,
    val keepScreenOn: Boolean = true,
    val infiniteScroll: Boolean = false,
    val providerOrder: List<String> = emptyList(),
    val disabledProviders: List<String> = emptyList()
)

@Serializable
data class ReaderSettingsBackup(
    // Typography
    val fontSize: Int = 18,
    val lineHeight: Float = 1.6f,
    val fontFamily: String = "system_serif",
    val fontWeight: Int = 400,
    val textAlign: String = "left",
    val letterSpacing: Float = 0f,
    val wordSpacing: Float = 1.0f,
    val hyphenation: Boolean = true,

    // Layout
    val maxWidth: String = "large",
    val marginHorizontal: Int = 20,
    val marginVertical: Int = 16,
    val paragraphSpacing: Float = 1.2f,
    val paragraphIndent: Float = 0f,

    // Appearance
    val theme: String = "dark",
    val brightness: Float = -1f,
    val warmthFilter: Float = 0f,
    val showProgress: Boolean = true,
    val progressStyle: String = "bar",
    val showReadingTime: Boolean = true,
    val showChapterTitle: Boolean = true,

    // Behavior
    val keepScreenOn: Boolean = true,
    val volumeKeyNavigation: Boolean = false,
    val volumeKeyDirection: String = "natural",
    val readingDirection: String = "ltr",
    val longPressSelection: Boolean = true,
    val autoHideControlsDelay: Long = 10000L,

    // Scroll & Navigation
    val scrollMode: String = "continuous",
    val pageAnimation: String = "slide",
    val smoothScroll: Boolean = true,
    val scrollSensitivity: Float = 1.0f,
    val edgeGestures: Boolean = true,

    // Auto-scroll
    val autoScrollEnabled: Boolean = false,
    val autoScrollSpeed: Float = 1.0f,

    // Accessibility
    val forceHighContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val largerTouchTargets: Boolean = false,

    // Tap zones
    val tapHorizontalZoneRatio: Float = 0.25f,
    val tapVerticalZoneRatio: Float = 0.2f,
    val tapLeftAction: String = "previous",
    val tapRightAction: String = "next",
    val tapTopAction: String = "controls",
    val tapBottomAction: String = "controls",
    val tapCenterAction: String = "controls",
    val tapDoubleTapAction: String = "fullscreen"
)

/**
 * Restore options
 */
data class RestoreOptions(
    val restoreLibrary: Boolean = true,
    val restoreBookmarks: Boolean = true,
    val restoreHistory: Boolean = true,
    val restoreStatistics: Boolean = true,
    val restoreSettings: Boolean = true,
    val mergeWithExisting: Boolean = true // If false, clears existing data first
)

/**
 * Restore result
 */
data class RestoreResult(
    val success: Boolean,
    val error: String? = null,
    val libraryRestored: Int = 0,
    val bookmarksRestored: Int = 0,
    val historyRestored: Int = 0,
    val readChaptersRestored: Int = 0,
    val statsRestored: Int = 0,
    val settingsRestored: Boolean = false
) {
    val totalItemsRestored: Int
        get() = libraryRestored + bookmarksRestored + historyRestored + readChaptersRestored + statsRestored
}

/**
 * Backup metadata (for showing backup info before restore)
 */
data class BackupMetadata(
    val version: Int,
    val createdAt: Long,
    val appVersion: String,
    val deviceInfo: String,
    val libraryCount: Int,
    val bookmarkCount: Int,
    val historyCount: Int,
    val hasSettings: Boolean,
    val hasStatistics: Boolean
)