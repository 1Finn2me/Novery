package com.emptycastle.novery.domain.model

/**
 * UI Density presets affecting spacing, icon sizes, and label visibility
 */
enum class UiDensity {
    COMPACT,
    DEFAULT,
    COMFORTABLE;

    fun displayName(): String = when (this) {
        COMPACT -> "Compact"
        DEFAULT -> "Default"
        COMFORTABLE -> "Comfortable"
    }

    fun bottomBarIconSize(): Int = when (this) {
        COMPACT -> 20
        DEFAULT -> 24
        COMFORTABLE -> 28
    }

    fun showBottomBarLabels(): Boolean = when (this) {
        COMPACT -> false
        DEFAULT -> true
        COMFORTABLE -> true
    }

    fun paddingMultiplier(): Float = when (this) {
        COMPACT -> 0.75f
        DEFAULT -> 1.0f
        COMFORTABLE -> 1.25f
    }

    fun cardSpacing(): Int = when (this) {
        COMPACT -> 8
        DEFAULT -> 12
        COMFORTABLE -> 16
    }

    fun gridPadding(): Int = when (this) {
        COMPACT -> 12
        DEFAULT -> 16
        COMFORTABLE -> 20
    }
}

/**
 * Display mode for novel grids/lists
 */
enum class DisplayMode {
    GRID,
    LIST;

    fun displayName(): String = when (this) {
        GRID -> "Grid"
        LIST -> "List"
    }
}

/**
 * Theme mode for the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun displayName(): String = when (this) {
        LIGHT -> "Light"
        DARK -> "Dark"
        SYSTEM -> "System"
    }
}

/**
 * Rating format options for displaying novel ratings
 */
enum class RatingFormat {
    TEN_POINT,
    FIVE_POINT,
    PERCENTAGE,
    ORIGINAL;

    fun displayName(): String = when (this) {
        TEN_POINT -> "10-point scale (8.5/10)"
        FIVE_POINT -> "5-point scale (4.25/5)"
        PERCENTAGE -> "Percentage (85%)"
        ORIGINAL -> "Original (per provider)"
    }

    fun shortDisplayName(): String = when (this) {
        TEN_POINT -> "X/10"
        FIVE_POINT -> "X/5"
        PERCENTAGE -> "X%"
        ORIGINAL -> "Original"
    }
}

/**
 * Grid column configuration
 */
sealed class GridColumns {
    object Auto : GridColumns()
    data class Fixed(val count: Int) : GridColumns()

    fun displayName(): String = when (this) {
        Auto -> "Auto"
        is Fixed -> "$count columns"
    }

    companion object {
        fun fromInt(value: Int): GridColumns = if (value <= 0) Auto else Fixed(value)
        fun toInt(columns: GridColumns): Int = when (columns) {
            Auto -> 0
            is Fixed -> columns.count
        }
    }
}

/**
 * App-wide settings
 */
data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.DARK,
    val amoledBlack: Boolean = false,
    val useDynamicColor: Boolean = false,

    // Layout
    val uiDensity: UiDensity = UiDensity.DEFAULT,
    val libraryGridColumns: GridColumns = GridColumns.Auto,
    val browseGridColumns: GridColumns = GridColumns.Auto,
    val searchGridColumns: GridColumns = GridColumns.Auto,
    val showBadges: Boolean = true,

    // Display mode settings
    val libraryDisplayMode: DisplayMode = DisplayMode.GRID,
    val browseDisplayMode: DisplayMode = DisplayMode.GRID,
    val searchDisplayMode: DisplayMode = DisplayMode.GRID,

    // Rating display
    val ratingFormat: RatingFormat = RatingFormat.TEN_POINT,

    // Library
    val defaultLibrarySort: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val defaultLibraryFilter: LibraryFilter = LibraryFilter.DOWNLOADED,

    // Auto-Download
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadOnWifiOnly: Boolean = true,
    val autoDownloadLimit: Int = 10, // 0 = unlimited, max chapters per novel
    val autoDownloadForStatuses: Set<ReadingStatus> = setOf(ReadingStatus.READING),

    // Search
    val searchResultsPerProvider: Int = 6,

    // Reader
    val keepScreenOn: Boolean = true,
    val infiniteScroll: Boolean = false,

    // Providers
    val providerOrder: List<String> = emptyList(),
    val disabledProviders: Set<String> = emptySet()
)

/**
 * Library sort options
 */
enum class LibrarySortOrder {
    LAST_READ,
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED,
    UNREAD_COUNT,
    NEW_CHAPTERS;

    fun displayName(): String = when (this) {
        LAST_READ -> "Last Read"
        TITLE_ASC -> "Title (A-Z)"
        TITLE_DESC -> "Title (Z-A)"
        DATE_ADDED -> "Date Added"
        UNREAD_COUNT -> "Unread Count"
        NEW_CHAPTERS -> "New Chapters"

    }
}

/**
 * Library filter options
 */
enum class LibraryFilter {
    ALL,
    DOWNLOADED,
    READING,
    COMPLETED,
    ON_HOLD,
    PLAN_TO_READ,
    DROPPED;

    fun displayName(): String = when (this) {
        ALL -> "All"
        DOWNLOADED -> "Downloaded"
        READING -> "Reading"
        COMPLETED -> "Completed"
        ON_HOLD -> "On Hold"
        PLAN_TO_READ -> "Plan to Read"
        DROPPED -> "Dropped"
    }
}