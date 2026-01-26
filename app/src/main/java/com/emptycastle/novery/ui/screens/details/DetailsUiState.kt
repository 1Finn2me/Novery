// com/emptycastle/novery/ui/screens/details/DetailsUiState.kt
package com.emptycastle.novery.ui.screens.details

import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.UserReview

/**
 * Chapter filter options
 */
enum class ChapterFilter {
    ALL,
    UNREAD,
    DOWNLOADED,
    NOT_DOWNLOADED
}

/**
 * Tab options for details screen
 */
enum class DetailsTab {
    CHAPTERS,
    RELATED,
    REVIEWS
}

/**
 * UI State for Details Screen
 */
data class DetailsUiState(
    val novelDetails: NovelDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Refresh state
    val isRefreshing: Boolean = false,

    // Library status
    val isFavorite: Boolean = false,
    val isInLibrary: Boolean = false,
    val readingStatus: ReadingStatus = ReadingStatus.READING,

    // Reading position
    val hasStartedReading: Boolean = false,
    val lastReadChapterUrl: String? = null,
    val lastReadChapterName: String? = null,
    val lastReadChapterIndex: Int = -1,

    // Chapter data (for backwards compatibility)
    val chapters: List<Chapter> = emptyList(),

    // Chapter status
    val downloadedChapters: Set<String> = emptySet(),
    val readChapters: Set<String> = emptySet(),

    // Legacy naming support
    val downloadedChapterUrls: Set<String> = emptySet(),
    val readChapterUrls: Set<String> = emptySet(),

    // Chapter sorting & filtering
    val isChapterSortDescending: Boolean = false,
    val isChapterListReversed: Boolean = false,
    val chapterFilter: ChapterFilter = ChapterFilter.ALL,
    val chapterSearchQuery: String = "",
    val isSearchActive: Boolean = false,

    // Synopsis expansion
    val isSynopsisExpanded: Boolean = false,

    // Selection mode for batch operations
    val isSelectionMode: Boolean = false,
    val selectedChapters: Set<String> = emptySet(),
    val lastSelectedIndex: Int = -1,

    // Processing state
    val isProcessing: Boolean = false,

    // Download state
    val isDownloading: Boolean = false,

    // Cover image zoom
    val showCoverZoom: Boolean = false,

    // Download menu
    val showDownloadMenu: Boolean = false,

    // Status menu
    val showStatusMenu: Boolean = false,

    // Filtered chapters
    val filteredChapters: List<Chapter> = emptyList(),

    // Filter version
    val filterVersion: Int = 0,

    // ================================================================
    // TAB STATE
    // ================================================================
    val selectedTab: DetailsTab = DetailsTab.CHAPTERS,

    // ================================================================
    // REVIEWS
    // ================================================================
    val reviews: List<UserReview> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val reviewsPage: Int = 1,
    val hasMoreReviews: Boolean = true,
    val showSpoilers: Boolean = false,
    val hasReviewsSupport: Boolean = false,

    // ================================================================
    // RELATED NOVELS
    // ================================================================
    val relatedNovels: List<Novel> = emptyList()
) {
    // ================================================================
    // COMPUTED PROPERTIES
    // ================================================================

    val readProgress: Float
        get() {
            val total = novelDetails?.chapters?.size ?: chapters.size
            if (total == 0) return 0f
            val readCount = readChapters.size.takeIf { it > 0 } ?: readChapterUrls.size
            return readCount.toFloat() / total
        }

    val downloadProgress: Float
        get() {
            val total = novelDetails?.chapters?.size ?: chapters.size
            if (total == 0) return 0f
            val downloadedCount = downloadedChapters.size.takeIf { it > 0 } ?: downloadedChapterUrls.size
            return downloadedCount.toFloat() / total
        }

    val unreadCount: Int
        get() {
            val total = novelDetails?.chapters?.size ?: chapters.size
            val readCount = readChapters.size.takeIf { it > 0 } ?: readChapterUrls.size
            return total - readCount
        }

    val downloadedCount: Int
        get() = downloadedChapters.size.takeIf { it > 0 } ?: downloadedChapterUrls.size

    val notDownloadedCount: Int
        get() {
            val total = novelDetails?.chapters?.size ?: chapters.size
            return total - downloadedCount
        }

    val selectedDownloadedCount: Int
        get() {
            val downloaded = downloadedChapters.takeIf { it.isNotEmpty() } ?: downloadedChapterUrls
            return selectedChapters.count { downloaded.contains(it) }
        }

    val selectedNotDownloadedCount: Int
        get() {
            val downloaded = downloadedChapters.takeIf { it.isNotEmpty() } ?: downloadedChapterUrls
            return selectedChapters.count { !downloaded.contains(it) }
        }

    val selectedReadCount: Int
        get() {
            val read = readChapters.takeIf { it.isNotEmpty() } ?: readChapterUrls
            return selectedChapters.count { read.contains(it) }
        }

    val selectedUnreadCount: Int
        get() {
            val read = readChapters.takeIf { it.isNotEmpty() } ?: readChapterUrls
            return selectedChapters.count { !read.contains(it) }
        }

    val undownloadedCount: Int
        get() {
            val downloaded = downloadedChapters.takeIf { it.isNotEmpty() } ?: downloadedChapterUrls
            val chapterList = novelDetails?.chapters ?: chapters
            return chapterList.count { !downloaded.contains(it.url) }
        }

    val unreadUndownloadedCount: Int
        get() {
            val downloaded = downloadedChapters.takeIf { it.isNotEmpty() } ?: downloadedChapterUrls
            val read = readChapters.takeIf { it.isNotEmpty() } ?: readChapterUrls
            val chapterList = novelDetails?.chapters ?: chapters
            return chapterList.count {
                !read.contains(it.url) && !downloaded.contains(it.url)
            }
        }

    val hasRelatedNovels: Boolean
        get() = relatedNovels.isNotEmpty()

    val displayChapters: List<Chapter>
        get() {
            val chapterList = novelDetails?.chapters ?: chapters
            val shouldReverse = isChapterSortDescending || isChapterListReversed
            return if (shouldReverse) chapterList.reversed() else chapterList
        }

    // Tab badges
    val chaptersTabBadge: String?
        get() = (novelDetails?.chapters?.size ?: chapters.size).takeIf { it > 0 }?.toString()

    val relatedTabBadge: String?
        get() = relatedNovels.size.takeIf { it > 0 }?.toString()

    val reviewsTabBadge: String?
        get() = reviews.size.takeIf { it > 0 }?.toString()
}