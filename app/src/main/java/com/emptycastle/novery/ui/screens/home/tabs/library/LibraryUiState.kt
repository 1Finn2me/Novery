package com.emptycastle.novery.ui.screens.home.tabs.library

import com.emptycastle.novery.data.repository.EpubImportRepository
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.domain.model.ImportedBooksDisplay
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder

data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val filteredItems: List<LibraryItem> = emptyList(),
    val downloadCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val totalNewChapters: Int = 0,
    val showNewChaptersCard: Boolean = false,
    val refreshProgress: RefreshProgress? = null,
    val isAutoDownloading: Boolean = false,
    val autoDownloadProgress: AutoDownloadProgress? = null,

    // EPUB Import state
    val isImporting: Boolean = false,
    val importProgress: EpubImportRepository.ImportProgress? = null,
    val showImportButton: Boolean = true,
    val importedBooksDisplay: ImportedBooksDisplay = ImportedBooksDisplay.MIXED,

    // Section mode: which section is selected
    val selectedSection: LibrarySection = LibrarySection.ONLINE
)

/**
 * Sections for SECTION display mode
 */
enum class LibrarySection {
    ONLINE,
    LOCAL
}

data class RefreshProgress(
    val current: Int,
    val total: Int,
    val currentNovelName: String,
    val novelsWithNewChapters: Int = 0,
    val newChaptersFound: Int = 0
)

data class AutoDownloadProgress(
    val currentNovel: String,
    val currentChapter: Int,
    val totalChapters: Int,
    val novelsCompleted: Int,
    val totalNovels: Int
)