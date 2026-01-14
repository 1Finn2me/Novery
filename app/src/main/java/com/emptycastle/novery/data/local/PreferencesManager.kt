package com.emptycastle.novery.data.local

import android.content.Context
import android.content.SharedPreferences
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.GridColumns
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.TextAlign
import com.emptycastle.novery.domain.model.ThemeMode
import com.emptycastle.novery.domain.model.UiDensity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user preferences using SharedPreferences.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "novery_prefs",
        Context.MODE_PRIVATE
    )

    private val scrollPrefs: SharedPreferences = context.getSharedPreferences(
        "novery_scroll_positions",
        Context.MODE_PRIVATE
    )

    private val _readerSettings = MutableStateFlow(loadReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _appSettings = MutableStateFlow(loadAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    data class SavedReadingPosition(
        val segmentId: String,
        val segmentIndex: Int,
        val progress: Float,
        val offset: Int,
        val timestamp: Long
    )

    fun saveReadingPosition(
        chapterUrl: String,
        segmentId: String,
        segmentIndex: Int,
        progress: Float,
        offset: Int
    ) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            putString("${key}${KEY_SEGMENT_ID}", segmentId)
            putInt("${key}${KEY_SEGMENT_INDEX}", segmentIndex)
            putFloat("${key}${KEY_PROGRESS}", progress)
            putInt("${key}_offset", offset)
            putLong("${key}_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    fun getReadingPosition(chapterUrl: String): SavedReadingPosition? {
        val key = chapterUrl.hashCode().toString()

        val timestamp = scrollPrefs.getLong("${key}_timestamp", 0)
        if (timestamp == 0L) return null

        // Check freshness (30 days)
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() - timestamp > thirtyDaysMs) {
            clearReadingPosition(chapterUrl)
            return null
        }

        val segmentId = scrollPrefs.getString("${key}${KEY_SEGMENT_ID}", null)

        // If no segmentId, try to migrate from old format
        if (segmentId == null) {
            val oldIndex = scrollPrefs.getInt("${key}_index", -1)
            if (oldIndex >= 0) {
                val oldOffset = scrollPrefs.getInt("${key}_offset", 0)
                return SavedReadingPosition(
                    segmentId = "seg-$oldIndex",
                    segmentIndex = oldIndex,
                    progress = 0f,
                    offset = oldOffset,
                    timestamp = timestamp
                )
            }
            return null
        }

        return SavedReadingPosition(
            segmentId = segmentId,
            segmentIndex = scrollPrefs.getInt("${key}${KEY_SEGMENT_INDEX}", 0),
            progress = scrollPrefs.getFloat("${key}${KEY_PROGRESS}", 0f),
            offset = scrollPrefs.getInt("${key}_offset", 0),
            timestamp = timestamp
        )
    }

    fun clearReadingPosition(chapterUrl: String) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            remove("${key}${KEY_SEGMENT_ID}")
            remove("${key}${KEY_SEGMENT_INDEX}")
            remove("${key}${KEY_PROGRESS}")
            remove("${key}_offset")
            remove("${key}_timestamp")
            // Also remove old format keys
            remove("${key}_index")
            apply()
        }
    }

    // ============ APP SETTINGS ============

    private fun loadAppSettings(): AppSettings {
        return AppSettings(
            themeMode = ThemeMode.valueOf(
                prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ),
            amoledBlack = prefs.getBoolean(KEY_AMOLED_BLACK, false),
            useDynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
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
            defaultLibraryFilter = LibraryFilter.valueOf(
                prefs.getString(KEY_DEFAULT_LIBRARY_FILTER, LibraryFilter.DOWNLOADED.name)
                    ?: LibraryFilter.DOWNLOADED.name
            ),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            infiniteScroll = prefs.getBoolean(KEY_INFINITE_SCROLL, false)
        )
    }

    fun updateAppSettings(settings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putBoolean(KEY_AMOLED_BLACK, settings.amoledBlack)
            putBoolean(KEY_DYNAMIC_COLOR, settings.useDynamicColor)
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
            apply()
        }
        _appSettings.value = settings
    }

    fun updateDensity(density: UiDensity) {
        val current = _appSettings.value
        updateAppSettings(current.copy(uiDensity = density))
    }

    fun updateThemeMode(mode: ThemeMode) {
        val current = _appSettings.value
        updateAppSettings(current.copy(themeMode = mode))
    }

    fun updateAmoledBlack(enabled: Boolean) {
        val current = _appSettings.value
        updateAppSettings(current.copy(amoledBlack = enabled))
    }

    fun updateLibraryGridColumns(columns: GridColumns) {
        val current = _appSettings.value
        updateAppSettings(current.copy(libraryGridColumns = columns))
    }

    fun updateBrowseGridColumns(columns: GridColumns) {
        val current = _appSettings.value
        updateAppSettings(current.copy(browseGridColumns = columns))
    }

    fun updateSearchGridColumns(columns: GridColumns) {
        val current = _appSettings.value
        updateAppSettings(current.copy(searchGridColumns = columns))
    }

    // ============ READER SETTINGS ============

    private fun loadReaderSettings(): ReaderSettings {
        return ReaderSettings(
            fontSize = prefs.getInt(KEY_FONT_SIZE, 18),
            fontFamily = FontFamily.valueOf(
                prefs.getString(KEY_FONT_FAMILY, FontFamily.SERIF.name) ?: FontFamily.SERIF.name
            ),
            maxWidth = MaxWidth.valueOf(
                prefs.getString(KEY_MAX_WIDTH, MaxWidth.LARGE.name) ?: MaxWidth.LARGE.name
            ),
            lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, 1.8f),
            textAlign = TextAlign.valueOf(
                prefs.getString(KEY_TEXT_ALIGN, TextAlign.LEFT.name) ?: TextAlign.LEFT.name
            ),
            theme = ReaderTheme.valueOf(
                prefs.getString(KEY_READER_THEME, ReaderTheme.DARK.name) ?: ReaderTheme.DARK.name
            )
        )
    }

    fun updateReaderSettings(settings: ReaderSettings) {
        prefs.edit().apply {
            putInt(KEY_FONT_SIZE, settings.fontSize)
            putString(KEY_FONT_FAMILY, settings.fontFamily.name)
            putString(KEY_MAX_WIDTH, settings.maxWidth.name)
            putFloat(KEY_LINE_HEIGHT, settings.lineHeight)
            putString(KEY_TEXT_ALIGN, settings.textAlign.name)
            putString(KEY_READER_THEME, settings.theme.name)
            apply()
        }
        _readerSettings.value = settings
    }

    // ============ CHAPTER LIST SETTINGS ============

    /**
     * Get the chapter sort order preference
     * @return true if descending (newest first), false if ascending (oldest first)
     */
    fun getChapterSortDescending(): Boolean {
        return prefs.getBoolean(KEY_CHAPTER_SORT_DESCENDING, false)
    }

    /**
     * Set the chapter sort order preference
     * @param descending true for newest first, false for oldest first
     */
    fun setChapterSortDescending(descending: Boolean) {
        prefs.edit().putBoolean(KEY_CHAPTER_SORT_DESCENDING, descending).apply()
    }

    // ============ SCROLL POSITION MEMORY ============

    fun saveScrollPosition(chapterUrl: String, firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            putInt("${key}_index", firstVisibleItemIndex)
            putInt("${key}_offset", firstVisibleItemOffset)
            putLong("${key}_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    fun getScrollPosition(chapterUrl: String): Pair<Int, Int>? {
        val key = chapterUrl.hashCode().toString()
        val index = scrollPrefs.getInt("${key}_index", -1)
        val offset = scrollPrefs.getInt("${key}_offset", 0)

        if (index < 0) return null

        val timestamp = scrollPrefs.getLong("${key}_timestamp", 0)
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() - timestamp > thirtyDaysMs) {
            clearScrollPosition(chapterUrl)
            return null
        }

        return Pair(index, offset)
    }

    fun clearScrollPosition(chapterUrl: String) {
        val key = chapterUrl.hashCode().toString()
        scrollPrefs.edit().apply {
            remove("${key}_index")
            remove("${key}_offset")
            remove("${key}_timestamp")
            apply()
        }
    }

    fun clearAllScrollPositions() {
        scrollPrefs.edit().clear().apply()
    }

    // ============ TTS SETTINGS ============

    fun getTtsSpeed(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.0f)

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
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

    companion object {
        // Reader settings keys
        private const val KEY_FONT_SIZE = "reader_font_size"
        private const val KEY_FONT_FAMILY = "reader_font_family"
        private const val KEY_MAX_WIDTH = "reader_max_width"
        private const val KEY_LINE_HEIGHT = "reader_line_height"
        private const val KEY_TEXT_ALIGN = "reader_text_align"
        private const val KEY_READER_THEME = "reader_theme"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_VOLUME = "tts_volume"
        private const val KEY_TTS_AUTO_SCROLL = "tts_auto_scroll"
        private const val KEY_TTS_HIGHLIGHT_SENTENCE = "tts_highlight_sentence"
        private const val KEY_TTS_PAUSE_ON_CALLS = "tts_pause_on_calls"

        // New Scroll position keys
        private const val KEY_SEGMENT_ID = "_segmentId"
        private const val KEY_SEGMENT_INDEX = "_segmentIndex"
        private const val KEY_PROGRESS = "_progress"

        // Chapter list settings
        private const val KEY_CHAPTER_SORT_DESCENDING = "chapter_sort_descending"

        // App settings keys
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
