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

    // Library
    val defaultLibrarySort: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val defaultLibraryFilter: LibraryFilter = LibraryFilter.DOWNLOADED,

    // Search
    val searchResultsPerProvider: Int = 6,

    // Reader
    val keepScreenOn: Boolean = true,
    val infiniteScroll: Boolean = false
)

/**
 * Library sort options
 */
enum class LibrarySortOrder {
    LAST_READ,
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED,
    UNREAD_COUNT;

    fun displayName(): String = when (this) {
        LAST_READ -> "Last Read"
        TITLE_ASC -> "Title (A-Z)"
        TITLE_DESC -> "Title (Z-A)"
        DATE_ADDED -> "Date Added"
        UNREAD_COUNT -> "Unread Count"
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