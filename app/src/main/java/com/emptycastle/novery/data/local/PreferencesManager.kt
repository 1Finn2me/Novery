package com.emptycastle.novery.data.local

import android.content.Context
import android.content.SharedPreferences
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.CustomThemeColors
import com.emptycastle.novery.domain.model.DisplayMode
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.FontWeight
import com.emptycastle.novery.domain.model.GridColumns
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.PageAnimation
import com.emptycastle.novery.domain.model.ProgressStyle
import com.emptycastle.novery.domain.model.RatingFormat
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.ReadingDirection
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.ScrollMode
import com.emptycastle.novery.domain.model.TapAction
import com.emptycastle.novery.domain.model.TapZoneConfig
import com.emptycastle.novery.domain.model.TextAlign
import com.emptycastle.novery.domain.model.ThemeMode
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.domain.model.VolumeKeyDirection
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.screens.details.ChapterDisplayMode
import com.emptycastle.novery.ui.screens.details.ChaptersPerPage
import com.emptycastle.novery.ui.screens.reader.logic.AuthorNoteDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages user preferences using SharedPreferences.
 * Provides reactive state flows for settings and handles persistence.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val scrollPrefs: SharedPreferences = context.getSharedPreferences(
        SCROLL_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // =========================================================================
    // STATE FLOWS
    // =========================================================================

    private val _readerSettings = MutableStateFlow(loadReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _appSettings = MutableStateFlow(loadAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(loadSearchHistory())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()

    private val _favoriteProviders = MutableStateFlow<Set<String>>(loadFavoriteProviders())
    val favoriteProviders: StateFlow<Set<String>> = _favoriteProviders.asStateFlow()

    // =========================================================================
    // AUTHOR NOTE SETTINGS
    // =========================================================================

    fun getAuthorNoteDisplayMode(): AuthorNoteDisplayMode {
        val id = prefs.getString(KEY_AUTHOR_NOTE_DISPLAY_MODE, AuthorNoteDisplayMode.COLLAPSED.id)
        return AuthorNoteDisplayMode.fromId(id ?: AuthorNoteDisplayMode.COLLAPSED.id)
    }

    fun setAuthorNoteDisplayMode(mode: AuthorNoteDisplayMode) {
        prefs.edit().putString(KEY_AUTHOR_NOTE_DISPLAY_MODE, mode.id).apply()
    }

    // =========================================================================
    // SEARCH HISTORY
    // =========================================================================

    @Serializable
    data class SearchHistoryItem(
        val query: String,
        val timestamp: Long = System.currentTimeMillis(),
        val providerName: String? = null, // Optional: track which provider was used
        val resultCount: Int = 0 // Optional: track how many results were found
    )

    private fun loadSearchHistory(): List<SearchHistoryItem> {
        val jsonString = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SearchHistoryItem>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSearchHistory(): List<SearchHistoryItem> = _searchHistory.value

    fun saveSearchHistory(history: List<SearchHistoryItem>) {
        val limitedHistory = history
            .distinctBy { it.query.lowercase() }
            .sortedByDescending { it.timestamp }
            .take(MAX_SEARCH_HISTORY_SIZE)

        prefs.edit()
            .putString(KEY_SEARCH_HISTORY, json.encodeToString(limitedHistory))
            .apply()
        _searchHistory.value = limitedHistory
    }

    fun addSearchHistoryItem(query: String, providerName: String? = null, resultCount: Int = 0) {
        if (query.isBlank()) return

        val trimmedQuery = query.trim()
        val currentHistory = _searchHistory.value.toMutableList()

        // Remove existing entry with same query (case-insensitive)
        currentHistory.removeAll { it.query.equals(trimmedQuery, ignoreCase = true) }

        // Add new entry at the beginning
        currentHistory.add(0, SearchHistoryItem(
            query = trimmedQuery,
            timestamp = System.currentTimeMillis(),
            providerName = providerName,
            resultCount = resultCount
        ))

        saveSearchHistory(currentHistory)
    }

    fun removeSearchHistoryItem(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.removeAll { it.query.equals(query, ignoreCase = true) }
        saveSearchHistory(currentHistory)
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
        _searchHistory.value = emptyList()
    }

    fun getRecentSearchQueries(limit: Int = 10): List<String> {
        return _searchHistory.value
            .sortedByDescending { it.timestamp }
            .take(limit)
            .map { it.query }
    }

    // =========================================================================
    // FAVORITE PROVIDERS
    // =========================================================================

    private fun loadFavoriteProviders(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_PROVIDERS, emptySet()) ?: emptySet()
    }

    fun getFavoriteProviders(): Set<String> = _favoriteProviders.value

    fun setFavoriteProviders(favorites: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_FAVORITE_PROVIDERS, favorites)
            .apply()
        _favoriteProviders.value = favorites
    }

    fun addFavoriteProvider(providerName: String) {
        val currentFavorites = _favoriteProviders.value.toMutableSet()
        currentFavorites.add(providerName)
        setFavoriteProviders(currentFavorites)
    }

    fun removeFavoriteProvider(providerName: String) {
        val currentFavorites = _favoriteProviders.value.toMutableSet()
        currentFavorites.remove(providerName)
        setFavoriteProviders(currentFavorites)
    }

    fun toggleFavoriteProvider(providerName: String) {
        if (isProviderFavorite(providerName)) {
            removeFavoriteProvider(providerName)
        } else {
            addFavoriteProvider(providerName)
        }
    }

    fun isProviderFavorite(providerName: String): Boolean {
        return _favoriteProviders.value.contains(providerName)
    }

    fun clearFavoriteProviders() {
        prefs.edit().remove(KEY_FAVORITE_PROVIDERS).apply()
        _favoriteProviders.value = emptySet()
    }

    // =========================================================================
    // READING POSITION
    // =========================================================================

    data class SavedReadingPosition(
        val segmentId: String,
        val segmentIndex: Int,
        val progress: Float,
        val offset: Int,
        val chapterIndex: Int,
        val sentenceIndex: Int,
        val timestamp: Long
    )

    fun saveReadingPosition(
        chapterUrl: String,
        segmentId: String,
        segmentIndex: Int,
        progress: Float,
        offset: Int,
        chapterIndex: Int = 0,
        sentenceIndex: Int = 0
    ) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            putString("${key}${KEY_SEGMENT_ID}", segmentId)
            putInt("${key}${KEY_SEGMENT_INDEX}", segmentIndex)
            putFloat("${key}${KEY_PROGRESS}", progress)
            putInt("${key}${KEY_OFFSET}", offset)
            putInt("${key}${KEY_CHAPTER_INDEX}", chapterIndex)
            putInt("${key}${KEY_SENTENCE_INDEX}", sentenceIndex)
            putLong("${key}${KEY_TIMESTAMP}", System.currentTimeMillis())
            apply()
        }
    }

    fun getReadingPosition(chapterUrl: String): SavedReadingPosition? {
        val key = chapterUrl.hashCode().toString()

        val timestamp = scrollPrefs.getLong("${key}${KEY_TIMESTAMP}", 0)
        if (timestamp == 0L) return null

        // Check freshness (configurable, default 30 days)
        val maxAgeMs = getPositionRetentionDays() * 24L * 60 * 60 * 1000
        if (System.currentTimeMillis() - timestamp > maxAgeMs) {
            clearReadingPosition(chapterUrl)
            return null
        }

        val segmentId = scrollPrefs.getString("${key}${KEY_SEGMENT_ID}", null)

        // Migration from old format
        if (segmentId == null) {
            val oldIndex = scrollPrefs.getInt("${key}_index", -1)
            if (oldIndex >= 0) {
                val oldOffset = scrollPrefs.getInt("${key}${KEY_OFFSET}", 0)
                return SavedReadingPosition(
                    segmentId = "seg-$oldIndex",
                    segmentIndex = oldIndex,
                    progress = 0f,
                    offset = oldOffset,
                    chapterIndex = 0,
                    sentenceIndex = 0,
                    timestamp = timestamp
                )
            }
            return null
        }

        return SavedReadingPosition(
            segmentId = segmentId,
            segmentIndex = scrollPrefs.getInt("${key}${KEY_SEGMENT_INDEX}", 0),
            progress = scrollPrefs.getFloat("${key}${KEY_PROGRESS}", 0f),
            offset = scrollPrefs.getInt("${key}${KEY_OFFSET}", 0),
            chapterIndex = scrollPrefs.getInt("${key}${KEY_CHAPTER_INDEX}", 0),
            sentenceIndex = scrollPrefs.getInt("${key}${KEY_SENTENCE_INDEX}", 0),
            timestamp = timestamp
        )
    }

    fun clearReadingPosition(chapterUrl: String) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            remove("${key}${KEY_SEGMENT_ID}")
            remove("${key}${KEY_SEGMENT_INDEX}")
            remove("${key}${KEY_PROGRESS}")
            remove("${key}${KEY_OFFSET}")
            remove("${key}${KEY_CHAPTER_INDEX}")
            remove("${key}${KEY_SENTENCE_INDEX}")
            remove("${key}${KEY_TIMESTAMP}")
            // Remove old format keys
            remove("${key}_index")
            apply()
        }
    }

    fun getPositionRetentionDays(): Int = prefs.getInt(KEY_POSITION_RETENTION_DAYS, 30)

    fun setPositionRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_POSITION_RETENTION_DAYS, days.coerceIn(1, 365)).apply()
    }

    // =========================================================================
    // APP SETTINGS
    // =========================================================================

    private fun loadAppSettings(): AppSettings {
        val statusesString = prefs.getString(KEY_AUTO_DOWNLOAD_STATUSES, null)
        val autoDownloadStatuses = if (statusesString.isNullOrEmpty()) {
            setOf(ReadingStatus.READING)
        } else {
            statusesString.split(",").mapNotNull {
                try { ReadingStatus.valueOf(it) } catch (e: Exception) { null }
            }.toSet()
        }

        val providerOrderString = prefs.getString(KEY_PROVIDER_ORDER, null)
        val providerOrder = if (providerOrderString.isNullOrBlank()) {
            MainProvider.getProviders().map { it.name }
        } else {
            providerOrderString.split(",").map { it }
        }

        val disabledString = prefs.getString(KEY_DISABLED_PROVIDERS, "")
        val disabledSet = if (disabledString.isNullOrBlank()) emptySet() else disabledString.split(",").toSet()

        // Load custom theme colors
        val customThemeColors = CustomThemeColors(
            primaryColor = prefs.getLong(
                KEY_CUSTOM_PRIMARY_COLOR,
                CustomThemeColors.DEFAULT.primaryColor
            ),
            secondaryColor = prefs.getLong(
                KEY_CUSTOM_SECONDARY_COLOR,
                CustomThemeColors.DEFAULT.secondaryColor
            ),
            backgroundColor = prefs.getLong(
                KEY_CUSTOM_BACKGROUND_COLOR,
                CustomThemeColors.DEFAULT.backgroundColor
            ),
            surfaceColor = prefs.getLong(
                KEY_CUSTOM_SURFACE_COLOR,
                CustomThemeColors.DEFAULT.surfaceColor
            )
        )

        return AppSettings(
            themeMode = ThemeMode.valueOf(
                prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ),
            amoledBlack = prefs.getBoolean(KEY_AMOLED_BLACK, false),
            useDynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            useCustomTheme = prefs.getBoolean(KEY_USE_CUSTOM_THEME, false),
            customThemeColors = customThemeColors,
            uiDensity = UiDensity.valueOf(
                prefs.getString(KEY_UI_DENSITY, UiDensity.DEFAULT.name) ?: UiDensity.DEFAULT.name
            ),
            libraryGridColumns = GridColumns.fromInt(prefs.getInt(KEY_LIBRARY_GRID_COLUMNS, 0)),
            browseGridColumns = GridColumns.fromInt(prefs.getInt(KEY_BROWSE_GRID_COLUMNS, 0)),
            searchGridColumns = GridColumns.fromInt(prefs.getInt(KEY_SEARCH_GRID_COLUMNS, 0)),
            searchResultsPerProvider = prefs.getInt(KEY_SEARCH_RESULTS_PER_PROVIDER, 6),
            showBadges = prefs.getBoolean(KEY_SHOW_BADGES, true),
            defaultLibrarySort = LibrarySortOrder.valueOf(
                prefs.getString(KEY_DEFAULT_LIBRARY_SORT, LibrarySortOrder.LAST_READ.name)
                    ?: LibrarySortOrder.LAST_READ.name
            ),
            libraryDisplayMode = DisplayMode.valueOf(
                prefs.getString(KEY_LIBRARY_DISPLAY_MODE, DisplayMode.GRID.name)
                    ?: DisplayMode.GRID.name
            ),
            browseDisplayMode = DisplayMode.valueOf(
                prefs.getString(KEY_BROWSE_DISPLAY_MODE, DisplayMode.GRID.name)
                    ?: DisplayMode.GRID.name
            ),
            searchDisplayMode = DisplayMode.valueOf(
                prefs.getString(KEY_SEARCH_DISPLAY_MODE, DisplayMode.GRID.name)
                    ?: DisplayMode.GRID.name
            ),
            ratingFormat = try {
                RatingFormat.valueOf(
                    prefs.getString(KEY_RATING_FORMAT, RatingFormat.TEN_POINT.name)
                        ?: RatingFormat.TEN_POINT.name
                )
            } catch (e: Exception) {
                RatingFormat.TEN_POINT
            },
            defaultLibraryFilter = LibraryFilter.valueOf(
                prefs.getString(KEY_DEFAULT_LIBRARY_FILTER, LibraryFilter.DOWNLOADED.name)
                    ?: LibraryFilter.DOWNLOADED.name
            ),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            infiniteScroll = prefs.getBoolean(KEY_INFINITE_SCROLL, false),
            autoDownloadEnabled = prefs.getBoolean(KEY_AUTO_DOWNLOAD_ENABLED, false),
            autoDownloadOnWifiOnly = prefs.getBoolean(KEY_AUTO_DOWNLOAD_WIFI_ONLY, true),
            autoDownloadLimit = prefs.getInt(KEY_AUTO_DOWNLOAD_LIMIT, 10),
            autoDownloadForStatuses = autoDownloadStatuses,
            providerOrder = providerOrder,
            disabledProviders = disabledSet
        )
    }

    fun updateAppSettings(settings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putBoolean(KEY_AMOLED_BLACK, settings.amoledBlack)
            putBoolean(KEY_DYNAMIC_COLOR, settings.useDynamicColor)
            putBoolean(KEY_USE_CUSTOM_THEME, settings.useCustomTheme)

            // Custom theme colors
            putLong(KEY_CUSTOM_PRIMARY_COLOR, settings.customThemeColors.primaryColor)
            putLong(KEY_CUSTOM_SECONDARY_COLOR, settings.customThemeColors.secondaryColor)
            putLong(KEY_CUSTOM_BACKGROUND_COLOR, settings.customThemeColors.backgroundColor)
            putLong(KEY_CUSTOM_SURFACE_COLOR, settings.customThemeColors.surfaceColor)

            putString(KEY_UI_DENSITY, settings.uiDensity.name)
            putInt(KEY_LIBRARY_GRID_COLUMNS, GridColumns.toInt(settings.libraryGridColumns))
            putInt(KEY_BROWSE_GRID_COLUMNS, GridColumns.toInt(settings.browseGridColumns))
            putInt(KEY_SEARCH_GRID_COLUMNS, GridColumns.toInt(settings.searchGridColumns))
            putInt(KEY_SEARCH_RESULTS_PER_PROVIDER, settings.searchResultsPerProvider)
            putBoolean(KEY_SHOW_BADGES, settings.showBadges)
            putString(KEY_DEFAULT_LIBRARY_SORT, settings.defaultLibrarySort.name)
            putString(KEY_DEFAULT_LIBRARY_FILTER, settings.defaultLibraryFilter.name)
            putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            putBoolean(KEY_INFINITE_SCROLL, settings.infiniteScroll)
            putBoolean(KEY_AUTO_DOWNLOAD_ENABLED, settings.autoDownloadEnabled)
            putBoolean(KEY_AUTO_DOWNLOAD_WIFI_ONLY, settings.autoDownloadOnWifiOnly)
            putInt(KEY_AUTO_DOWNLOAD_LIMIT, settings.autoDownloadLimit)
            putString(
                KEY_AUTO_DOWNLOAD_STATUSES,
                settings.autoDownloadForStatuses.joinToString(",") { it.name }
            )
            putString(KEY_RATING_FORMAT, settings.ratingFormat.name)
            putString(KEY_PROVIDER_ORDER, settings.providerOrder.joinToString(","))
            putString(KEY_DISABLED_PROVIDERS, settings.disabledProviders.joinToString(","))
            putString(KEY_LIBRARY_DISPLAY_MODE, settings.libraryDisplayMode.name)
            putString(KEY_BROWSE_DISPLAY_MODE, settings.browseDisplayMode.name)
            putString(KEY_SEARCH_DISPLAY_MODE, settings.searchDisplayMode.name)

            apply()
        }
        _appSettings.value = settings
    }

    fun updateRatingFormat(format: RatingFormat) {
        updateAppSettings(_appSettings.value.copy(ratingFormat = format))
    }

    // Convenience methods for app settings
    fun updateDensity(density: UiDensity) {
        updateAppSettings(_appSettings.value.copy(uiDensity = density))
    }

    fun updateLibraryDisplayMode(mode: DisplayMode) {
        updateAppSettings(_appSettings.value.copy(libraryDisplayMode = mode))
    }

    fun updateBrowseDisplayMode(mode: DisplayMode) {
        updateAppSettings(_appSettings.value.copy(browseDisplayMode = mode))
    }

    fun updateSearchDisplayMode(mode: DisplayMode) {
        updateAppSettings(_appSettings.value.copy(searchDisplayMode = mode))
    }

    fun updateThemeMode(mode: ThemeMode) {
        updateAppSettings(_appSettings.value.copy(themeMode = mode))
    }

    fun updateAmoledBlack(enabled: Boolean) {
        updateAppSettings(_appSettings.value.copy(amoledBlack = enabled))
    }

    fun updateLibraryGridColumns(columns: GridColumns) {
        updateAppSettings(_appSettings.value.copy(libraryGridColumns = columns))
    }

    fun updateBrowseGridColumns(columns: GridColumns) {
        updateAppSettings(_appSettings.value.copy(browseGridColumns = columns))
    }

    fun updateSearchGridColumns(columns: GridColumns) {
        updateAppSettings(_appSettings.value.copy(searchGridColumns = columns))
    }

    fun updateAutoDownloadEnabled(enabled: Boolean) {
        updateAppSettings(_appSettings.value.copy(autoDownloadEnabled = enabled))
    }

    fun updateAutoDownloadWifiOnly(wifiOnly: Boolean) {
        updateAppSettings(_appSettings.value.copy(autoDownloadOnWifiOnly = wifiOnly))
    }

    fun updateAutoDownloadLimit(limit: Int) {
        updateAppSettings(_appSettings.value.copy(autoDownloadLimit = limit.coerceIn(0, 100)))
    }

    fun updateAutoDownloadStatuses(statuses: Set<ReadingStatus>) {
        updateAppSettings(_appSettings.value.copy(autoDownloadForStatuses = statuses))
    }

    // =========================================================================
// CUSTOM THEME CONVENIENCE METHODS
// =========================================================================

    fun updateUseCustomTheme(enabled: Boolean) {
        updateAppSettings(_appSettings.value.copy(useCustomTheme = enabled))
    }

    fun updateCustomThemeColors(colors: CustomThemeColors) {
        updateAppSettings(_appSettings.value.copy(customThemeColors = colors))
    }

    fun updateCustomPrimaryColor(color: Long) {
        val current = _appSettings.value.customThemeColors
        updateCustomThemeColors(current.copy(primaryColor = color))
    }

    fun updateCustomSecondaryColor(color: Long) {
        val current = _appSettings.value.customThemeColors
        updateCustomThemeColors(current.copy(secondaryColor = color))
    }

    fun updateCustomBackgroundColor(color: Long) {
        val current = _appSettings.value.customThemeColors
        updateCustomThemeColors(current.copy(backgroundColor = color))
    }

    fun updateCustomSurfaceColor(color: Long) {
        val current = _appSettings.value.customThemeColors
        updateCustomThemeColors(current.copy(surfaceColor = color))
    }

    fun applyCustomThemePreset(preset: CustomThemeColors) {
        updateAppSettings(_appSettings.value.copy(
            useCustomTheme = true,
            customThemeColors = preset
        ))
    }

    // Provider settings
    fun updateProviderOrder(order: List<String>) {
        prefs.edit().putString(KEY_PROVIDER_ORDER, order.joinToString(",")).apply()
        updateAppSettings(_appSettings.value.copy(providerOrder = order))
    }

    fun setProviderEnabled(providerName: String, enabled: Boolean) {
        val current = _appSettings.value.disabledProviders.toMutableSet()
        if (enabled) current.remove(providerName) else current.add(providerName)
        prefs.edit().putString(KEY_DISABLED_PROVIDERS, current.joinToString(",")).apply()
        updateAppSettings(_appSettings.value.copy(disabledProviders = current))
    }

    // =========================================================================
    // READER SETTINGS
    // =========================================================================

    private fun loadReaderSettings(): ReaderSettings {
        // Check if we need to migrate from old format
        val needsMigration = prefs.getBoolean(KEY_NEEDS_MIGRATION, true)
        if (needsMigration) {
            migrateOldReaderSettings()
        }

        return ReaderSettings(
            // Typography
            fontSize = prefs.getInt(KEY_FONT_SIZE, ReaderSettings.DEFAULT_FONT_SIZE),
            fontFamily = loadFontFamily(),
            fontWeight = loadFontWeight(),
            lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, ReaderSettings.DEFAULT_LINE_HEIGHT),
            letterSpacing = prefs.getFloat(KEY_LETTER_SPACING, ReaderSettings.DEFAULT_LETTER_SPACING),
            wordSpacing = prefs.getFloat(KEY_WORD_SPACING, ReaderSettings.DEFAULT_WORD_SPACING),
            textAlign = loadTextAlign(),
            hyphenation = prefs.getBoolean(KEY_HYPHENATION, true),

            // Layout
            maxWidth = loadMaxWidth(),
            marginHorizontal = prefs.getInt(KEY_MARGIN_HORIZONTAL, ReaderSettings.DEFAULT_MARGIN_HORIZONTAL),
            marginVertical = prefs.getInt(KEY_MARGIN_VERTICAL, ReaderSettings.DEFAULT_MARGIN_VERTICAL),
            paragraphSpacing = prefs.getFloat(KEY_PARAGRAPH_SPACING, ReaderSettings.DEFAULT_PARAGRAPH_SPACING),
            paragraphIndent = prefs.getFloat(KEY_PARAGRAPH_INDENT, ReaderSettings.DEFAULT_PARAGRAPH_INDENT),

            // Appearance
            theme = loadReaderTheme(),
            brightness = prefs.getFloat(KEY_BRIGHTNESS, ReaderSettings.BRIGHTNESS_SYSTEM),
            warmthFilter = prefs.getFloat(KEY_WARMTH_FILTER, 0f),
            showProgress = prefs.getBoolean(KEY_SHOW_PROGRESS, true),
            progressStyle = loadProgressStyle(),
            showReadingTime = prefs.getBoolean(KEY_SHOW_READING_TIME, true),
            showChapterTitle = prefs.getBoolean(KEY_SHOW_CHAPTER_TITLE, true),

            // Behavior
            // TTS / behavior toggles
            ttsAutoAdvanceChapter = prefs.getBoolean(KEY_READER_TTS_AUTO_ADVANCE, true),
            lockScrollDuringTTS = prefs.getBoolean(KEY_READER_LOCK_SCROLL_DURING_TTS, true),
            keepScreenOn = prefs.getBoolean(KEY_READER_KEEP_SCREEN_ON, true),
            volumeKeyNavigation = prefs.getBoolean(KEY_VOLUME_KEY_NAVIGATION, false),
            volumeKeyDirection = loadVolumeKeyDirection(),
            readingDirection = loadReadingDirection(),
            tapZones = loadTapZoneConfig(),
            longPressSelection = prefs.getBoolean(KEY_LONG_PRESS_SELECTION, true),
            autoHideControlsDelay = prefs.getLong(KEY_AUTO_HIDE_CONTROLS_DELAY, ReaderSettings.DEFAULT_AUTO_HIDE_DELAY),

            // Scroll & Navigation
            scrollMode = loadScrollMode(),
            pageAnimation = loadPageAnimation(),
            smoothScroll = prefs.getBoolean(KEY_SMOOTH_SCROLL, true),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f),
            edgeGestures = prefs.getBoolean(KEY_EDGE_GESTURES, true),

            // Accessibility
            forceHighContrast = prefs.getBoolean(KEY_FORCE_HIGH_CONTRAST, false),
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
            largerTouchTargets = prefs.getBoolean(KEY_LARGER_TOUCH_TARGETS, false)
        )
    }

    private fun migrateOldReaderSettings() {
        prefs.edit().apply {
            // Migrate old font family if exists
            val oldFontFamily = prefs.getString(KEY_FONT_FAMILY, null)
            if (oldFontFamily != null) {
                val newFontFamily = when (oldFontFamily) {
                    "SERIF" -> FontFamily.SYSTEM_SERIF.id
                    "SANS" -> FontFamily.SYSTEM_SANS.id
                    "MONO" -> FontFamily.SYSTEM_MONO.id
                    else -> FontFamily.SYSTEM_SERIF.id
                }
                putString(KEY_FONT_FAMILY, newFontFamily)
            }

            // Migrate old text align if exists
            val oldTextAlign = prefs.getString(KEY_TEXT_ALIGN, null)
            if (oldTextAlign != null) {
                val newTextAlign = when (oldTextAlign) {
                    "LEFT" -> TextAlign.LEFT.id
                    "JUSTIFY" -> TextAlign.JUSTIFY.id
                    else -> TextAlign.LEFT.id
                }
                putString(KEY_TEXT_ALIGN, newTextAlign)
            }

            // Migrate old max width if exists
            val oldMaxWidth = prefs.getString(KEY_MAX_WIDTH, null)
            if (oldMaxWidth != null) {
                val newMaxWidth = when (oldMaxWidth) {
                    "MEDIUM" -> MaxWidth.MEDIUM.id
                    "LARGE" -> MaxWidth.LARGE.id
                    "EXTRA_LARGE" -> MaxWidth.EXTRA_LARGE.id
                    "FULL" -> MaxWidth.FULL.id
                    else -> MaxWidth.LARGE.id
                }
                putString(KEY_MAX_WIDTH, newMaxWidth)
            }

            // Migrate old theme if exists
            val oldTheme = prefs.getString(KEY_READER_THEME, null)
            if (oldTheme != null) {
                val newTheme = when (oldTheme) {
                    "LIGHT" -> ReaderTheme.LIGHT.id
                    "SEPIA" -> ReaderTheme.SEPIA.id
                    "DARK" -> ReaderTheme.DARK.id
                    else -> oldTheme.lowercase()
                }
                putString(KEY_READER_THEME, newTheme)
            }

            putBoolean(KEY_NEEDS_MIGRATION, false)
            apply()
        }
    }

    private fun loadFontFamily(): FontFamily {
        val id = prefs.getString(KEY_FONT_FAMILY, FontFamily.SYSTEM_SERIF.id)
        return FontFamily.fromId(id ?: FontFamily.SYSTEM_SERIF.id) ?: FontFamily.SYSTEM_SERIF
    }

    private fun loadFontWeight(): FontWeight {
        val value = prefs.getInt(KEY_FONT_WEIGHT, FontWeight.REGULAR.value)
        return FontWeight.fromValue(value)
    }

    private fun loadTextAlign(): TextAlign {
        val id = prefs.getString(KEY_TEXT_ALIGN, TextAlign.LEFT.id)
        return TextAlign.fromId(id ?: TextAlign.LEFT.id)
    }

    private fun loadMaxWidth(): MaxWidth {
        val id = prefs.getString(KEY_MAX_WIDTH, MaxWidth.LARGE.id)
        return MaxWidth.fromId(id ?: MaxWidth.LARGE.id)
    }

    private fun loadReaderTheme(): ReaderTheme {
        val id = prefs.getString(KEY_READER_THEME, ReaderTheme.DARK.id)
        return ReaderTheme.fromId(id ?: ReaderTheme.DARK.id)
    }

    private fun loadProgressStyle(): ProgressStyle {
        val id = prefs.getString(KEY_PROGRESS_STYLE, ProgressStyle.BAR.id)
        return ProgressStyle.fromId(id ?: ProgressStyle.BAR.id)
    }

    private fun loadVolumeKeyDirection(): VolumeKeyDirection {
        val id = prefs.getString(KEY_VOLUME_KEY_DIRECTION, VolumeKeyDirection.NATURAL.id)
        return VolumeKeyDirection.fromId(id ?: VolumeKeyDirection.NATURAL.id)
    }

    private fun loadReadingDirection(): ReadingDirection {
        val id = prefs.getString(KEY_READING_DIRECTION, ReadingDirection.LTR.id)
        return ReadingDirection.fromId(id ?: ReadingDirection.LTR.id)
    }

    private fun loadScrollMode(): ScrollMode {
        val id = prefs.getString(KEY_SCROLL_MODE, ScrollMode.CONTINUOUS.id)
        return ScrollMode.fromId(id ?: ScrollMode.CONTINUOUS.id)
    }

    private fun loadPageAnimation(): PageAnimation {
        val id = prefs.getString(KEY_PAGE_ANIMATION, PageAnimation.SLIDE.id)
        return PageAnimation.fromId(id ?: PageAnimation.SLIDE.id)
    }

    private fun loadTapZoneConfig(): TapZoneConfig {
        return TapZoneConfig(
            horizontalZoneRatio = prefs.getFloat(KEY_TAP_HORIZONTAL_RATIO, 0.25f),
            verticalZoneRatio = prefs.getFloat(KEY_TAP_VERTICAL_RATIO, 0.2f),
            leftZoneAction = TapAction.fromId(
                prefs.getString(KEY_TAP_LEFT_ACTION, TapAction.PREVIOUS_PAGE.id) ?: TapAction.PREVIOUS_PAGE.id
            ),
            rightZoneAction = TapAction.fromId(
                prefs.getString(KEY_TAP_RIGHT_ACTION, TapAction.NEXT_PAGE.id) ?: TapAction.NEXT_PAGE.id
            ),
            topZoneAction = TapAction.fromId(
                prefs.getString(KEY_TAP_TOP_ACTION, TapAction.TOGGLE_CONTROLS.id) ?: TapAction.TOGGLE_CONTROLS.id
            ),
            bottomZoneAction = TapAction.fromId(
                prefs.getString(KEY_TAP_BOTTOM_ACTION, TapAction.TOGGLE_CONTROLS.id) ?: TapAction.TOGGLE_CONTROLS.id
            ),
            centerZoneAction = TapAction.fromId(
                prefs.getString(KEY_TAP_CENTER_ACTION, TapAction.TOGGLE_CONTROLS.id) ?: TapAction.TOGGLE_CONTROLS.id
            ),
            doubleTapAction = TapAction.fromId(
                prefs.getString(KEY_TAP_DOUBLE_TAP_ACTION, TapAction.TOGGLE_FULLSCREEN.id) ?: TapAction.TOGGLE_FULLSCREEN.id
            )
        )
    }

    fun updateReaderSettings(settings: ReaderSettings) {
        prefs.edit().apply {
            // Typography
            putInt(KEY_FONT_SIZE, settings.fontSize)
            putString(KEY_FONT_FAMILY, settings.fontFamily.id)
            putInt(KEY_FONT_WEIGHT, settings.fontWeight.value)
            putFloat(KEY_LINE_HEIGHT, settings.lineHeight)
            putFloat(KEY_LETTER_SPACING, settings.letterSpacing)
            putFloat(KEY_WORD_SPACING, settings.wordSpacing)
            putString(KEY_TEXT_ALIGN, settings.textAlign.id)
            putBoolean(KEY_HYPHENATION, settings.hyphenation)

            // Layout
            putString(KEY_MAX_WIDTH, settings.maxWidth.id)
            putInt(KEY_MARGIN_HORIZONTAL, settings.marginHorizontal)
            putInt(KEY_MARGIN_VERTICAL, settings.marginVertical)
            putFloat(KEY_PARAGRAPH_SPACING, settings.paragraphSpacing)
            putFloat(KEY_PARAGRAPH_INDENT, settings.paragraphIndent)

            // Appearance
            putString(KEY_READER_THEME, settings.theme.id)
            putFloat(KEY_BRIGHTNESS, settings.brightness)
            putFloat(KEY_WARMTH_FILTER, settings.warmthFilter)
            putBoolean(KEY_SHOW_PROGRESS, settings.showProgress)
            putString(KEY_PROGRESS_STYLE, settings.progressStyle.id)
            putBoolean(KEY_SHOW_READING_TIME, settings.showReadingTime)
            putBoolean(KEY_SHOW_CHAPTER_TITLE, settings.showChapterTitle)

            // Behavior
            putBoolean(KEY_READER_KEEP_SCREEN_ON, settings.keepScreenOn)
            putBoolean(KEY_READER_TTS_AUTO_ADVANCE, settings.ttsAutoAdvanceChapter)
            putBoolean(KEY_READER_LOCK_SCROLL_DURING_TTS, settings.lockScrollDuringTTS)
            putBoolean(KEY_VOLUME_KEY_NAVIGATION, settings.volumeKeyNavigation)
            putString(KEY_VOLUME_KEY_DIRECTION, settings.volumeKeyDirection.id)
            putString(KEY_READING_DIRECTION, settings.readingDirection.id)
            putBoolean(KEY_LONG_PRESS_SELECTION, settings.longPressSelection)
            putLong(KEY_AUTO_HIDE_CONTROLS_DELAY, settings.autoHideControlsDelay)

            // Tap zones
            putFloat(KEY_TAP_HORIZONTAL_RATIO, settings.tapZones.horizontalZoneRatio)
            putFloat(KEY_TAP_VERTICAL_RATIO, settings.tapZones.verticalZoneRatio)
            putString(KEY_TAP_LEFT_ACTION, settings.tapZones.leftZoneAction.id)
            putString(KEY_TAP_RIGHT_ACTION, settings.tapZones.rightZoneAction.id)
            putString(KEY_TAP_TOP_ACTION, settings.tapZones.topZoneAction.id)
            putString(KEY_TAP_BOTTOM_ACTION, settings.tapZones.bottomZoneAction.id)
            putString(KEY_TAP_CENTER_ACTION, settings.tapZones.centerZoneAction.id)
            putString(KEY_TAP_DOUBLE_TAP_ACTION, settings.tapZones.doubleTapAction.id)

            // Scroll & Navigation
            putString(KEY_SCROLL_MODE, settings.scrollMode.id)
            putString(KEY_PAGE_ANIMATION, settings.pageAnimation.id)
            putBoolean(KEY_SMOOTH_SCROLL, settings.smoothScroll)
            putFloat(KEY_SCROLL_SENSITIVITY, settings.scrollSensitivity)
            putBoolean(KEY_EDGE_GESTURES, settings.edgeGestures)

            // Accessibility
            putBoolean(KEY_FORCE_HIGH_CONTRAST, settings.forceHighContrast)
            putBoolean(KEY_REDUCE_MOTION, settings.reduceMotion)
            putBoolean(KEY_LARGER_TOUCH_TARGETS, settings.largerTouchTargets)

            apply()
        }
        _readerSettings.value = settings
    }

    // =========================================================================
    // READER SETTINGS CONVENIENCE METHODS
    // =========================================================================

    fun updateFontSize(size: Int) {
        val current = _readerSettings.value
        updateReaderSettings(current.withFontSize(size))
    }

    fun updateFontFamily(family: FontFamily) {
        val current = _readerSettings.value
        updateReaderSettings(current.withFontFamily(family))
    }

    fun updateFontWeight(weight: FontWeight) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(fontWeight = weight))
    }

    fun updateLineHeight(height: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.withLineHeight(height))
    }

    fun updateLetterSpacing(spacing: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.withLetterSpacing(spacing))
    }

    fun updateWordSpacing(spacing: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(
            wordSpacing = spacing.coerceIn(ReaderSettings.MIN_WORD_SPACING, ReaderSettings.MAX_WORD_SPACING)
        ))
    }

    fun updateTextAlign(align: TextAlign) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(textAlign = align))
    }

    fun updateHyphenation(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(hyphenation = enabled))
    }

    fun updateMaxWidth(maxWidth: MaxWidth) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(maxWidth = maxWidth))
    }

    fun updateMargins(horizontal: Int? = null, vertical: Int? = null) {
        val current = _readerSettings.value
        updateReaderSettings(current.withMargins(
            horizontal = horizontal ?: current.marginHorizontal,
            vertical = vertical ?: current.marginVertical
        ))
    }

    fun updateParagraphSpacing(spacing: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(
            paragraphSpacing = spacing.coerceIn(
                ReaderSettings.MIN_PARAGRAPH_SPACING,
                ReaderSettings.MAX_PARAGRAPH_SPACING
            )
        ))
    }

    fun updateParagraphIndent(indent: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(
            paragraphIndent = indent.coerceIn(
                ReaderSettings.MIN_PARAGRAPH_INDENT,
                ReaderSettings.MAX_PARAGRAPH_INDENT
            )
        ))
    }

    fun updateReaderTheme(theme: ReaderTheme) {
        val current = _readerSettings.value
        updateReaderSettings(current.withTheme(theme))
    }

    fun updateBrightness(brightness: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.withBrightness(brightness))
    }

    fun resetBrightness() {
        val current = _readerSettings.value
        updateReaderSettings(current.resetBrightness())
    }

    fun updateWarmthFilter(warmth: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(warmthFilter = warmth.coerceIn(0f, 1f)))
    }

    fun updateShowProgress(show: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(showProgress = show))
    }

    fun updateProgressStyle(style: ProgressStyle) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(progressStyle = style))
    }

    fun updateShowReadingTime(show: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(showReadingTime = show))
    }

    fun updateShowChapterTitle(show: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(showChapterTitle = show))
    }

    fun updateReaderKeepScreenOn(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(keepScreenOn = enabled))
    }

    fun updateVolumeKeyNavigation(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(volumeKeyNavigation = enabled))
    }

    fun updateVolumeKeyDirection(direction: VolumeKeyDirection) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(volumeKeyDirection = direction))
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(readingDirection = direction))
    }

    fun updateTapZoneConfig(config: TapZoneConfig) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(tapZones = config))
    }

    fun updateLongPressSelection(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(longPressSelection = enabled))
    }

    fun updateAutoHideControlsDelay(delay: Long) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(autoHideControlsDelay = delay.coerceAtLeast(0)))
    }

    fun updateScrollMode(mode: ScrollMode) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(scrollMode = mode))
    }

    fun updatePageAnimation(animation: PageAnimation) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(pageAnimation = animation))
    }

    fun updateSmoothScroll(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(smoothScroll = enabled))
    }

    fun updateScrollSensitivity(sensitivity: Float) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(scrollSensitivity = sensitivity.coerceIn(0.5f, 2.0f)))
    }

    fun updateEdgeGestures(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(edgeGestures = enabled))
    }

    fun updateForceHighContrast(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(forceHighContrast = enabled))
    }

    fun updateReduceMotion(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(reduceMotion = enabled))
    }

    fun updateLargerTouchTargets(enabled: Boolean) {
        val current = _readerSettings.value
        updateReaderSettings(current.copy(largerTouchTargets = enabled))
    }

    // Apply preset
    fun applyReaderPreset(preset: ReaderSettings) {
        updateReaderSettings(preset)
    }

    // Reset to defaults
    fun resetReaderSettings() {
        updateReaderSettings(ReaderSettings.DEFAULT)
    }

    // =========================================================================
    // CHAPTER LIST SETTINGS
    // =========================================================================

    fun getChapterSortDescending(): Boolean {
        return prefs.getBoolean(KEY_CHAPTER_SORT_DESCENDING, true)
    }

    fun setChapterSortDescending(descending: Boolean) {
        prefs.edit().putBoolean(KEY_CHAPTER_SORT_DESCENDING, descending).apply()
    }

    fun getChapterDisplayMode(): ChapterDisplayMode {
        val value = prefs.getString(KEY_CHAPTER_DISPLAY_MODE, ChapterDisplayMode.SCROLL.name)
        return try {
            ChapterDisplayMode.valueOf(value ?: ChapterDisplayMode.SCROLL.name)
        } catch (e: Exception) {
            ChapterDisplayMode.SCROLL
        }
    }

    fun setChapterDisplayMode(mode: ChapterDisplayMode) {
        prefs.edit().putString(KEY_CHAPTER_DISPLAY_MODE, mode.name).apply()
    }

    fun getChaptersPerPage(): ChaptersPerPage {
        val value = prefs.getInt(KEY_CHAPTERS_PER_PAGE, ChaptersPerPage.FIFTY.value)
        return ChaptersPerPage.fromValue(value)
    }

    fun setChaptersPerPage(chaptersPerPage: ChaptersPerPage) {
        prefs.edit().putInt(KEY_CHAPTERS_PER_PAGE, chaptersPerPage.value).apply()
    }

    // =========================================================================
    // READING GOALS
    // =========================================================================

    fun getDailyReadingGoal(): Int {
        return prefs.getInt(KEY_DAILY_READING_GOAL, 30)
    }

    fun setDailyReadingGoal(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_READING_GOAL, minutes.coerceIn(5, 480)).apply()
    }

    fun getWeeklyReadingGoal(): Int {
        return prefs.getInt(KEY_WEEKLY_READING_GOAL, 180)
    }

    fun setWeeklyReadingGoal(minutes: Int) {
        prefs.edit().putInt(KEY_WEEKLY_READING_GOAL, minutes.coerceIn(30, 2400)).apply()
    }

    fun getMonthlyChapterGoal(): Int {
        return prefs.getInt(KEY_MONTHLY_CHAPTER_GOAL, 50)
    }

    fun setMonthlyChapterGoal(chapters: Int) {
        prefs.edit().putInt(KEY_MONTHLY_CHAPTER_GOAL, chapters.coerceIn(1, 500)).apply()
    }

    // =========================================================================
    // SCROLL POSITION MEMORY (Legacy support)
    // =========================================================================

    fun saveScrollPosition(chapterUrl: String, firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            putInt("${key}_index", firstVisibleItemIndex)
            putInt("${key}${KEY_OFFSET}", firstVisibleItemOffset)
            putLong("${key}${KEY_TIMESTAMP}", System.currentTimeMillis())
            apply()
        }
    }

    fun getScrollPosition(chapterUrl: String): Pair<Int, Int>? {
        val key = chapterUrl.hashCode().toString()
        val index = scrollPrefs.getInt("${key}_index", -1)
        val offset = scrollPrefs.getInt("${key}${KEY_OFFSET}", 0)

        if (index < 0) return null

        val timestamp = scrollPrefs.getLong("${key}${KEY_TIMESTAMP}", 0)
        val maxAgeMs = getPositionRetentionDays() * 24L * 60 * 60 * 1000
        if (System.currentTimeMillis() - timestamp > maxAgeMs) {
            clearScrollPosition(chapterUrl)
            return null
        }

        return Pair(index, offset)
    }

    fun clearScrollPosition(chapterUrl: String) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            remove("${key}_index")
            remove("${key}${KEY_OFFSET}")
            remove("${key}${KEY_TIMESTAMP}")
            apply()
        }
    }

    fun clearAllScrollPositions() {
        scrollPrefs.edit().clear().apply()
    }

    // =========================================================================
    // TTS SETTINGS
    // =========================================================================

    fun getTtsSpeed(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.0f)

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed.coerceIn(0.5f, 2.5f)).apply()
    }

    fun getTtsVoice(): String? = prefs.getString(KEY_TTS_VOICE, null)

    fun setTtsVoice(voiceId: String) {
        prefs.edit().putString(KEY_TTS_VOICE, voiceId).apply()
    }

    fun getTtsPitch(): Float = prefs.getFloat(KEY_TTS_PITCH, 1.0f)

    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch.coerceIn(0.5f, 2.0f)).apply()
    }

    fun getTtsVolume(): Float = prefs.getFloat(KEY_TTS_VOLUME, 1.0f)

    fun getTtsUseSystemVoice(): Boolean {
        return prefs.getBoolean(KEY_TTS_USE_SYSTEM_VOICE, false)
    }

    fun setTtsUseSystemVoice(useSystem: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_USE_SYSTEM_VOICE, useSystem).apply()
    }

    fun setTtsVolume(volume: Float) {
        prefs.edit().putFloat(KEY_TTS_VOLUME, volume.coerceIn(0f, 1f)).apply()
    }

    fun getTtsAutoScroll(): Boolean = prefs.getBoolean(KEY_TTS_AUTO_SCROLL, true)

    fun setTtsAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_AUTO_SCROLL, enabled).apply()
    }

    fun getTtsHighlightSentence(): Boolean = prefs.getBoolean(KEY_TTS_HIGHLIGHT_SENTENCE, true)

    fun setTtsHighlightSentence(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_HIGHLIGHT_SENTENCE, enabled).apply()
    }

    fun getTtsPauseOnCalls(): Boolean = prefs.getBoolean(KEY_TTS_PAUSE_ON_CALLS, true)

    fun setTtsPauseOnCalls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_PAUSE_ON_CALLS, enabled).apply()
    }

    fun getTtsSkipChapterHeaders(): Boolean = prefs.getBoolean(KEY_TTS_SKIP_CHAPTER_HEADERS, false)

    fun setTtsSkipChapterHeaders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_SKIP_CHAPTER_HEADERS, enabled).apply()
    }

    fun getTtsContinueOnChapterEnd(): Boolean = prefs.getBoolean(KEY_TTS_CONTINUE_ON_CHAPTER_END, true)

    fun setTtsContinueOnChapterEnd(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_CONTINUE_ON_CHAPTER_END, enabled).apply()
    }

    fun getTtsSentenceDelay(): Long = prefs.getLong(KEY_TTS_SENTENCE_DELAY, 0)

    fun setTtsSentenceDelay(delay: Long) {
        prefs.edit().putLong(KEY_TTS_SENTENCE_DELAY, delay.coerceIn(0, 2000)).apply()
    }

    fun getTtsParagraphDelay(): Long = prefs.getLong(KEY_TTS_PARAGRAPH_DELAY, 200)

    fun setTtsParagraphDelay(delay: Long) {
        prefs.edit().putLong(KEY_TTS_PARAGRAPH_DELAY, delay.coerceIn(0, 3000)).apply()
    }

    // =========================================================================
    // NOTIFICATION SETTINGS
    // =========================================================================

    fun getUpdateNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)

    fun setUpdateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
    }

    fun getDownloadNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_NOTIFICATIONS, true)

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_NOTIFICATIONS, enabled).apply()
    }

    // =========================================================================
    // BACKUP & SYNC
    // =========================================================================

    fun getLastBackupTime(): Long = prefs.getLong(KEY_LAST_BACKUP_TIME, 0)

    fun setLastBackupTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, time).apply()
    }

    fun getAutoBackupEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getAutoBackupInterval(): Int = prefs.getInt(KEY_AUTO_BACKUP_INTERVAL, 7) // days

    fun setAutoBackupInterval(days: Int) {
        prefs.edit().putInt(KEY_AUTO_BACKUP_INTERVAL, days.coerceIn(1, 30)).apply()
    }

    // =========================================================================
    // CACHE SETTINGS
    // =========================================================================

    fun getImageCacheSize(): Int = prefs.getInt(KEY_IMAGE_CACHE_SIZE, 100) // MB

    fun setImageCacheSize(sizeMb: Int) {
        prefs.edit().putInt(KEY_IMAGE_CACHE_SIZE, sizeMb.coerceIn(50, 500)).apply()
    }

    fun getClearCacheOnExit(): Boolean = prefs.getBoolean(KEY_CLEAR_CACHE_ON_EXIT, false)

    fun setClearCacheOnExit(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLEAR_CACHE_ON_EXIT, enabled).apply()
    }

    // =========================================================================
    // FIRST RUN & ONBOARDING
    // =========================================================================

    fun isFirstRun(): Boolean = prefs.getBoolean(KEY_FIRST_RUN, true)

    fun setFirstRunComplete() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun getAppVersion(): Int = prefs.getInt(KEY_APP_VERSION, 0)

    fun setAppVersion(version: Int) {
        prefs.edit().putInt(KEY_APP_VERSION, version).apply()
    }

    // =========================================================================
    // EXPORT ALL SETTINGS
    // =========================================================================

    fun exportSettings(): Map<String, Any?> {
        return buildMap {
            put("readerSettings", exportReaderSettings())
            put("appSettings", exportAppSettings())
            put("ttsSettings", exportTtsSettings())
            put("chapterListSettings", exportChapterListSettings())
            put("searchHistory", exportSearchHistory())
            put("favoriteProviders", _favoriteProviders.value.toList())
        }
    }

    private fun exportReaderSettings(): Map<String, Any?> {
        val settings = _readerSettings.value
        return buildMap {
            put("fontSize", settings.fontSize)
            put("fontFamily", settings.fontFamily.id)
            put("fontWeight", settings.fontWeight.value)
            put("lineHeight", settings.lineHeight)
            put("letterSpacing", settings.letterSpacing)
            put("wordSpacing", settings.wordSpacing)
            put("textAlign", settings.textAlign.id)
            put("hyphenation", settings.hyphenation)
            put("maxWidth", settings.maxWidth.id)
            put("marginHorizontal", settings.marginHorizontal)
            put("marginVertical", settings.marginVertical)
            put("paragraphSpacing", settings.paragraphSpacing)
            put("paragraphIndent", settings.paragraphIndent)
            put("theme", settings.theme.id)
            put("brightness", settings.brightness)
            put("warmthFilter", settings.warmthFilter)
            put("showProgress", settings.showProgress)
            put("progressStyle", settings.progressStyle.id)
            put("showReadingTime", settings.showReadingTime)
            put("showChapterTitle", settings.showChapterTitle)
            put("keepScreenOn", settings.keepScreenOn)
            put("ttsAutoAdvanceChapter", settings.ttsAutoAdvanceChapter)
            put("lockScrollDuringTTS", settings.lockScrollDuringTTS)
            put("volumeKeyNavigation", settings.volumeKeyNavigation)
            put("volumeKeyDirection", settings.volumeKeyDirection.id)
            put("readingDirection", settings.readingDirection.id)
            put("longPressSelection", settings.longPressSelection)
            put("autoHideControlsDelay", settings.autoHideControlsDelay)
            put("scrollMode", settings.scrollMode.id)
            put("pageAnimation", settings.pageAnimation.id)
            put("smoothScroll", settings.smoothScroll)
            put("scrollSensitivity", settings.scrollSensitivity)
            put("edgeGestures", settings.edgeGestures)
            put("forceHighContrast", settings.forceHighContrast)
            put("reduceMotion", settings.reduceMotion)
            put("largerTouchTargets", settings.largerTouchTargets)
        }
    }

    private fun exportAppSettings(): Map<String, Any?> {
        val settings = _appSettings.value
        return buildMap {
            put("themeMode", settings.themeMode.name)
            put("amoledBlack", settings.amoledBlack)
            put("useDynamicColor", settings.useDynamicColor)
            put("uiDensity", settings.uiDensity.name)
            put("showBadges", settings.showBadges)
            put("defaultLibrarySort", settings.defaultLibrarySort.name)
            put("defaultLibraryFilter", settings.defaultLibraryFilter.name)
            put("keepScreenOn", settings.keepScreenOn)
            put("infiniteScroll", settings.infiniteScroll)
            put("autoDownloadEnabled", settings.autoDownloadEnabled)
            put("autoDownloadOnWifiOnly", settings.autoDownloadOnWifiOnly)
            put("autoDownloadLimit", settings.autoDownloadLimit)
        }
    }

    private fun exportTtsSettings(): Map<String, Any?> {
        return buildMap {
            put("speed", getTtsSpeed())
            put("pitch", getTtsPitch())
            put("volume", getTtsVolume())
            put("voice", getTtsVoice())
            put("autoScroll", getTtsAutoScroll())
            put("highlightSentence", getTtsHighlightSentence())
            put("pauseOnCalls", getTtsPauseOnCalls())
            put("skipChapterHeaders", getTtsSkipChapterHeaders())
            put("continueOnChapterEnd", getTtsContinueOnChapterEnd())
            put("sentenceDelay", getTtsSentenceDelay())
            put("paragraphDelay", getTtsParagraphDelay())
        }
    }

    private fun exportChapterListSettings(): Map<String, Any?> {
        return buildMap {
            put("sortDescending", getChapterSortDescending())
            put("displayMode", getChapterDisplayMode().name)
            put("chaptersPerPage", getChaptersPerPage().value)
        }
    }

    private fun exportSearchHistory(): List<Map<String, Any?>> {
        return _searchHistory.value.map { item ->
            buildMap {
                put("query", item.query)
                put("timestamp", item.timestamp)
                put("providerName", item.providerName)
                put("resultCount", item.resultCount)
            }
        }
    }

    // =========================================================================
    // RESET TO DEFAULTS
    // =========================================================================

    /**
     * Resets all settings to their default values.
     * This includes app settings, reader settings, TTS settings, and chapter list settings.
     */
    fun resetToDefaults() {
        // Reset app settings to defaults
        updateAppSettings(AppSettings())

        // Reset reader settings to defaults
        resetReaderSettings()

        // Reset TTS settings to defaults
        resetTtsSettings()

        // Reset chapter list settings to defaults
        resetChapterListSettings()

        // Reset reading goals to defaults
        resetReadingGoals()

        // Reset cache settings to defaults
        resetCacheSettings()

        // Reset notification settings to defaults
        resetNotificationSettings()

        // Reset search history and favorite providers
        clearSearchHistory()
        clearFavoriteProviders()
    }

    /**
     * Resets only app settings (theme, layout, etc.) to defaults.
     */
    fun resetAppSettings() {
        updateAppSettings(AppSettings())
    }

    /**
     * Resets TTS settings to defaults.
     */
    private fun resetTtsSettings() {
        prefs.edit().apply {
            putFloat(KEY_TTS_SPEED, 1.0f)
            putFloat(KEY_TTS_PITCH, 1.0f)
            putFloat(KEY_TTS_VOLUME, 1.0f)
            remove(KEY_TTS_VOICE)
            putBoolean(KEY_TTS_AUTO_SCROLL, true)
            putBoolean(KEY_TTS_HIGHLIGHT_SENTENCE, true)
            putBoolean(KEY_TTS_PAUSE_ON_CALLS, true)
            putBoolean(KEY_TTS_SKIP_CHAPTER_HEADERS, false)
            putBoolean(KEY_TTS_CONTINUE_ON_CHAPTER_END, true)
            putLong(KEY_TTS_SENTENCE_DELAY, 0)
            putLong(KEY_TTS_PARAGRAPH_DELAY, 200)
            putBoolean(KEY_TTS_USE_SYSTEM_VOICE, false)
            apply()
        }
    }

    /**
     * Resets chapter list settings to defaults.
     */
    private fun resetChapterListSettings() {
        prefs.edit().apply {
            putBoolean(KEY_CHAPTER_SORT_DESCENDING, true)
            putString(KEY_CHAPTER_DISPLAY_MODE, ChapterDisplayMode.SCROLL.name)
            putInt(KEY_CHAPTERS_PER_PAGE, ChaptersPerPage.FIFTY.value)
            apply()
        }
    }

    /**
     * Resets reading goals to defaults.
     */
    private fun resetReadingGoals() {
        prefs.edit().apply {
            putInt(KEY_DAILY_READING_GOAL, 30)
            putInt(KEY_WEEKLY_READING_GOAL, 180)
            putInt(KEY_MONTHLY_CHAPTER_GOAL, 50)
            apply()
        }
    }

    /**
     * Resets cache settings to defaults.
     */
    private fun resetCacheSettings() {
        prefs.edit().apply {
            putInt(KEY_IMAGE_CACHE_SIZE, 100)
            putBoolean(KEY_CLEAR_CACHE_ON_EXIT, false)
            apply()
        }
    }

    /**
     * Resets notification settings to defaults.
     */
    private fun resetNotificationSettings() {
        prefs.edit().apply {
            putBoolean(KEY_UPDATE_NOTIFICATIONS, true)
            putBoolean(KEY_DOWNLOAD_NOTIFICATIONS, true)
            apply()
        }
    }

    /**
     * Resets backup settings to defaults (but keeps backup data).
     */
    fun resetBackupSettings() {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_BACKUP_ENABLED, false)
            putInt(KEY_AUTO_BACKUP_INTERVAL, 7)
            apply()
        }
    }

    // =========================================================================
    // COMPANION OBJECT
    // =========================================================================

    companion object {
        private const val PREFS_NAME = "novery_prefs"
        private const val SCROLL_PREFS_NAME = "novery_scroll_positions"
        private const val MAX_SEARCH_HISTORY_SIZE = 50

        // =====================================================================
        // SEARCH HISTORY & FAVORITES KEYS
        // =====================================================================

        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_FAVORITE_PROVIDERS = "favorite_providers"

        // =====================================================================
        // READER SETTINGS KEYS
        // =====================================================================

        private const val KEY_AUTHOR_NOTE_DISPLAY_MODE = "author_note_display_mode"

        // Typography
        private const val KEY_FONT_SIZE = "reader_font_size"
        private const val KEY_FONT_FAMILY = "reader_font_family"
        private const val KEY_FONT_WEIGHT = "reader_font_weight"
        private const val KEY_LINE_HEIGHT = "reader_line_height"
        private const val KEY_LETTER_SPACING = "reader_letter_spacing"
        private const val KEY_WORD_SPACING = "reader_word_spacing"
        private const val KEY_TEXT_ALIGN = "reader_text_align"
        private const val KEY_HYPHENATION = "reader_hyphenation"

        // Layout
        private const val KEY_MAX_WIDTH = "reader_max_width"
        private const val KEY_MARGIN_HORIZONTAL = "reader_margin_horizontal"
        private const val KEY_MARGIN_VERTICAL = "reader_margin_vertical"
        private const val KEY_PARAGRAPH_SPACING = "reader_paragraph_spacing"
        private const val KEY_PARAGRAPH_INDENT = "reader_paragraph_indent"
        private const val KEY_LIBRARY_DISPLAY_MODE = "library_display_mode"
        private const val KEY_BROWSE_DISPLAY_MODE = "browse_display_mode"
        private const val KEY_SEARCH_DISPLAY_MODE = "search_display_mode"

        // Appearance
        private const val KEY_READER_THEME = "reader_theme"
        private const val KEY_BRIGHTNESS = "reader_brightness"
        private const val KEY_WARMTH_FILTER = "reader_warmth_filter"
        private const val KEY_SHOW_PROGRESS = "reader_show_progress"
        private const val KEY_PROGRESS_STYLE = "reader_progress_style"
        private const val KEY_SHOW_READING_TIME = "reader_show_reading_time"
        private const val KEY_SHOW_CHAPTER_TITLE = "reader_show_chapter_title"

        // Behavior
        private const val KEY_READER_KEEP_SCREEN_ON = "reader_keep_screen_on"
        private const val KEY_READER_TTS_AUTO_ADVANCE = "reader_tts_auto_advance"
        private const val KEY_READER_LOCK_SCROLL_DURING_TTS = "reader_lock_scroll_during_tts"
        private const val KEY_VOLUME_KEY_NAVIGATION = "reader_volume_key_navigation"
        private const val KEY_VOLUME_KEY_DIRECTION = "reader_volume_key_direction"
        private const val KEY_READING_DIRECTION = "reader_reading_direction"
        private const val KEY_LONG_PRESS_SELECTION = "reader_long_press_selection"
        private const val KEY_AUTO_HIDE_CONTROLS_DELAY = "reader_auto_hide_controls_delay"

        // Tap zones
        private const val KEY_TAP_HORIZONTAL_RATIO = "reader_tap_horizontal_ratio"
        private const val KEY_TAP_VERTICAL_RATIO = "reader_tap_vertical_ratio"
        private const val KEY_TAP_LEFT_ACTION = "reader_tap_left_action"
        private const val KEY_TAP_RIGHT_ACTION = "reader_tap_right_action"
        private const val KEY_TAP_TOP_ACTION = "reader_tap_top_action"
        private const val KEY_TAP_BOTTOM_ACTION = "reader_tap_bottom_action"
        private const val KEY_TAP_CENTER_ACTION = "reader_tap_center_action"
        private const val KEY_TAP_DOUBLE_TAP_ACTION = "reader_tap_double_tap_action"

        // Scroll & Navigation
        private const val KEY_SCROLL_MODE = "reader_scroll_mode"
        private const val KEY_PAGE_ANIMATION = "reader_page_animation"
        private const val KEY_SMOOTH_SCROLL = "reader_smooth_scroll"
        private const val KEY_SCROLL_SENSITIVITY = "reader_scroll_sensitivity"
        private const val KEY_EDGE_GESTURES = "reader_edge_gestures"

        // Accessibility
        private const val KEY_FORCE_HIGH_CONTRAST = "reader_force_high_contrast"
        private const val KEY_REDUCE_MOTION = "reader_reduce_motion"
        private const val KEY_LARGER_TOUCH_TARGETS = "reader_larger_touch_targets"

        // Migration
        private const val KEY_NEEDS_MIGRATION = "reader_needs_migration"

        // =====================================================================
        // TTS SETTINGS KEYS
        // =====================================================================

        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_VOLUME = "tts_volume"
        private const val KEY_TTS_AUTO_SCROLL = "tts_auto_scroll"
        private const val KEY_TTS_HIGHLIGHT_SENTENCE = "tts_highlight_sentence"
        private const val KEY_TTS_PAUSE_ON_CALLS = "tts_pause_on_calls"
        private const val KEY_TTS_SKIP_CHAPTER_HEADERS = "tts_skip_chapter_headers"
        private const val KEY_TTS_CONTINUE_ON_CHAPTER_END = "tts_continue_on_chapter_end"
        private const val KEY_TTS_SENTENCE_DELAY = "tts_sentence_delay"
        private const val KEY_TTS_PARAGRAPH_DELAY = "tts_paragraph_delay"
        private const val KEY_TTS_USE_SYSTEM_VOICE = "tts_use_system_voice"

        // =====================================================================
        // SCROLL POSITION KEYS
        // =====================================================================

        private const val KEY_SEGMENT_ID = "_segmentId"
        private const val KEY_SEGMENT_INDEX = "_segmentIndex"
        private const val KEY_PROGRESS = "_progress"
        private const val KEY_OFFSET = "_offset"
        private const val KEY_CHAPTER_INDEX = "_chapterIndex"
        private const val KEY_SENTENCE_INDEX = "_sentenceIndex"
        private const val KEY_TIMESTAMP = "_timestamp"
        private const val KEY_POSITION_RETENTION_DAYS = "position_retention_days"

        // =====================================================================
        // CHAPTER LIST SETTINGS
        // =====================================================================

        private const val KEY_CHAPTER_SORT_DESCENDING = "chapter_sort_descending"
        private const val KEY_CHAPTER_DISPLAY_MODE = "chapter_display_mode"
        private const val KEY_CHAPTERS_PER_PAGE = "chapters_per_page"

        // =====================================================================
        // READING GOALS
        // =====================================================================

        private const val KEY_DAILY_READING_GOAL = "daily_reading_goal"
        private const val KEY_WEEKLY_READING_GOAL = "weekly_reading_goal"
        private const val KEY_MONTHLY_CHAPTER_GOAL = "monthly_chapter_goal"

        // =====================================================================
        // AUTO-DOWNLOAD
        // =====================================================================

        private const val KEY_AUTO_DOWNLOAD_ENABLED = "auto_download_enabled"
        private const val KEY_AUTO_DOWNLOAD_WIFI_ONLY = "auto_download_wifi_only"
        private const val KEY_AUTO_DOWNLOAD_LIMIT = "auto_download_limit"
        private const val KEY_AUTO_DOWNLOAD_STATUSES = "auto_download_statuses"

        // =====================================================================
        // APP SETTINGS KEYS
        // =====================================================================

        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AMOLED_BLACK = "amoled_black"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_UI_DENSITY = "ui_density"
        private const val KEY_LIBRARY_GRID_COLUMNS = "library_grid_columns"
        private const val KEY_BROWSE_GRID_COLUMNS = "browse_grid_columns"
        private const val KEY_SEARCH_GRID_COLUMNS = "search_grid_columns"
        private const val KEY_SHOW_BADGES = "show_badges"
        private const val KEY_DEFAULT_LIBRARY_SORT = "default_library_sort"
        private const val KEY_DEFAULT_LIBRARY_FILTER = "default_library_filter"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_INFINITE_SCROLL = "infinite_scroll"
        private const val KEY_SEARCH_RESULTS_PER_PROVIDER = "search_results_per_provider"
        private const val KEY_PROVIDER_ORDER = "provider_order"
        private const val KEY_DISABLED_PROVIDERS = "disabled_providers"
        private const val KEY_RATING_FORMAT = "rating_format"

        // =====================================================================
        // NOTIFICATIONS
        // =====================================================================

        private const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"
        private const val KEY_DOWNLOAD_NOTIFICATIONS = "download_notifications"

        // =====================================================================
        // BACKUP & SYNC
        // =====================================================================

        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_INTERVAL = "auto_backup_interval"

        // =====================================================================
        // CACHE
        // =====================================================================

        private const val KEY_IMAGE_CACHE_SIZE = "image_cache_size"
        private const val KEY_CLEAR_CACHE_ON_EXIT = "clear_cache_on_exit"

        // =====================================================================
        // FIRST RUN & ONBOARDING
        // =====================================================================

        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_APP_VERSION = "app_version"

        // =====================================================================
        // CUSTOM THEME KEYS
        // =====================================================================

        private const val KEY_USE_CUSTOM_THEME = "use_custom_theme"
        private const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color"
        private const val KEY_CUSTOM_SECONDARY_COLOR = "custom_secondary_color"
        private const val KEY_CUSTOM_BACKGROUND_COLOR = "custom_background_color"
        private const val KEY_CUSTOM_SURFACE_COLOR = "custom_surface_color"

        // =====================================================================
        // SINGLETON
        // =====================================================================

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}