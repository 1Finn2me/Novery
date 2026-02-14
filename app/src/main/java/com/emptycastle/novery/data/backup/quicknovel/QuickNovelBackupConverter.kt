package com.emptycastle.novery.data.backup.quicknovel

import com.emptycastle.novery.data.backup.AppSettingsBackup
import com.emptycastle.novery.data.backup.BackupData
import com.emptycastle.novery.data.backup.HistoryBackup
import com.emptycastle.novery.data.backup.LibraryBackup
import com.emptycastle.novery.data.backup.ReadChapterBackup
import com.emptycastle.novery.data.backup.ReaderSettingsBackup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class QuickNovelBackupConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Check if the JSON string is a QuickNovel backup format
     */
    fun isQuickNovelBackup(jsonString: String): Boolean {
        return try {
            val obj = json.parseToJsonElement(jsonString).jsonObject
            obj.containsKey("datastore") &&
                    obj["datastore"]?.jsonObject?.containsKey("_String") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convert QuickNovel backup to Novery BackupData format
     */
    fun convert(jsonString: String): BackupData {
        val quickNovel = json.decodeFromString<QuickNovelBackup>(jsonString)
        val strings = quickNovel.datastore._String

        // Parse all bookmarked novels
        val bookmarkedNovels = mutableMapOf<Long, QuickNovelResult>()
        val bookmarkStates = mutableMapOf<Long, Int>()
        val historyNovels = mutableMapOf<Long, QuickNovelResult>()
        val downloadData = mutableMapOf<Long, QuickNovelDownloadData>()

        // First pass: collect all data
        strings.forEach { (key, value) ->
            try {
                when {
                    // Bookmarked novels (library)
                    key.startsWith("result_bookmarked/") && !key.contains("state") -> {
                        val id = key.removePrefix("result_bookmarked/").toLongOrNull()
                        if (id != null) {
                            bookmarkedNovels[id] = json.decodeFromString<QuickNovelResult>(value)
                        }
                    }

                    // Bookmark states (reading status)
                    key.startsWith("result_bookmarked_state/") -> {
                        val id = key.removePrefix("result_bookmarked_state/").toLongOrNull()
                        if (id != null) {
                            bookmarkStates[id] = value.toIntOrNull() ?: 0
                        }
                    }

                    // History entries
                    key.startsWith("result_history/") -> {
                        val id = key.removePrefix("result_history/").toLongOrNull()
                        if (id != null) {
                            historyNovels[id] = json.decodeFromString<QuickNovelResult>(value)
                        }
                    }

                    // Download data
                    key.startsWith("downloads_data/") -> {
                        val id = key.removePrefix("downloads_data/").toLongOrNull()
                        if (id != null) {
                            downloadData[id] = json.decodeFromString<QuickNovelDownloadData>(value)
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip invalid entries
                e.printStackTrace()
            }
        }

        // Build library entries from bookmarked novels
        val library = bookmarkedNovels.map { (id, novel) ->
            buildLibraryBackup(
                id = id,
                novel = novel,
                state = bookmarkStates[id] ?: 0,
                strings = strings
            )
        }

        // Build history entries
        val history = historyNovels.map { (id, novel) ->
            buildHistoryBackup(novel, strings)
        }

        // Build read chapters list
        val readChapters = buildReadChapters(strings, bookmarkedNovels, historyNovels)

        // Parse settings
        val appSettings = parseAppSettings(quickNovel.settings)
        val readerSettings = parseReaderSettings(strings, quickNovel.settings)

        return BackupData(
            version = BackupData.CURRENT_VERSION,
            createdAt = System.currentTimeMillis(),
            appVersion = "QuickNovel Import",
            deviceInfo = "Imported from QuickNovel",
            library = library,
            bookmarks = emptyList(), // QuickNovel bookmarks are different (chapter bookmarks vs in-text)
            history = history,
            readChapters = readChapters,
            readingStats = emptyList(), // QuickNovel doesn't track detailed stats
            readingStreak = null,
            appSettings = appSettings,
            readerSettings = readerSettings
        )
    }

    private fun buildLibraryBackup(
        id: Long,
        novel: QuickNovelResult,
        state: Int,
        strings: Map<String, String>
    ): LibraryBackup {
        val readingStatus = mapReadingStatus(state)

        // Get reading position by novel name
        val positionKey = "reader_epub_position/${novel.name}"
        val chapterIndex = strings[positionKey]?.toIntOrNull() ?: 0

        // Get chapter name
        val chapterNameKey = "reader_epub_position_chapter/${novel.name}"
        val chapterName = strings[chapterNameKey]
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() }

        // Get scroll position
        val scrollCharKey = "reader_epub_position_scroll_char/${novel.name}"
        val scrollPosition = strings[scrollCharKey]?.toIntOrNull() ?: 0

        // Find last read timestamp
        val lastReadAt = findLastReadTimestamp(novel.name, chapterIndex, strings)

        // Get download last access
        val downloadAccessKey = "downloads_epub_last_access/$id"
        val lastAccessAt = strings[downloadAccessKey]?.toLongOrNull()

        return LibraryBackup(
            url = novel.source,
            name = novel.name,
            posterUrl = novel.poster ?: novel.image?.url,
            apiName = mapApiName(novel.apiName),
            latestChapter = null,
            addedAt = novel.cachedTime ?: System.currentTimeMillis(),
            readingStatus = readingStatus,
            lastChapterUrl = null, // Not directly stored in QuickNovel
            lastChapterName = chapterName,
            lastReadAt = lastReadAt ?: lastAccessAt ?: novel.cachedTime,
            lastScrollIndex = 0, // QuickNovel uses character position, not paragraph index
            lastScrollOffset = scrollPosition,
            totalChapterCount = novel.totalChapters ?: 0,
            acknowledgedChapterCount = novel.totalChapters ?: 0,
            lastCheckedAt = novel.cachedTime ?: 0,
            lastUpdatedAt = novel.cachedTime ?: 0,
            lastReadChapterIndex = chapterIndex,
            unreadChapterCount = maxOf(0, (novel.totalChapters ?: 0) - chapterIndex - 1)
        )
    }

    private fun buildHistoryBackup(
        novel: QuickNovelResult,
        strings: Map<String, String>
    ): HistoryBackup {
        // Get reading position
        val positionKey = "reader_epub_position/${novel.name}"
        val chapterIndex = strings[positionKey]?.toIntOrNull() ?: 0

        // Get chapter name
        val chapterNameKey = "reader_epub_position_chapter/${novel.name}"
        val chapterName = strings[chapterNameKey]
            ?.removeSurrounding("\"")
            ?: "Chapter ${chapterIndex + 1}"

        // Find last read timestamp
        val lastReadAt = findLastReadTimestamp(novel.name, chapterIndex, strings)

        return HistoryBackup(
            novelUrl = novel.source,
            novelName = novel.name,
            posterUrl = novel.poster ?: novel.image?.url,
            chapterName = chapterName,
            chapterUrl = "${novel.source}#chapter-$chapterIndex", // Constructed URL
            apiName = mapApiName(novel.apiName),
            timestamp = lastReadAt ?: novel.cachedTime ?: System.currentTimeMillis()
        )
    }

    private fun buildReadChapters(
        strings: Map<String, String>,
        bookmarkedNovels: Map<Long, QuickNovelResult>,
        historyNovels: Map<Long, QuickNovelResult>
    ): List<ReadChapterBackup> {
        val readChapters = mutableListOf<ReadChapterBackup>()
        val allNovels = (bookmarkedNovels.values + historyNovels.values).associateBy { it.name }

        strings.forEach { (key, value) ->
            // Pattern: reader_epub_position_read/{novel_name}/{chapter_index}
            if (key.startsWith("reader_epub_position_read/")) {
                val pathParts = key.removePrefix("reader_epub_position_read/")
                val lastSlash = pathParts.lastIndexOf('/')

                if (lastSlash > 0) {
                    val novelName = pathParts.substring(0, lastSlash)
                    val chapterIndex = pathParts.substring(lastSlash + 1).toIntOrNull()
                    val readAt = value.toLongOrNull()

                    if (chapterIndex != null && readAt != null) {
                        val novel = allNovels[novelName]
                        if (novel != null) {
                            readChapters.add(
                                ReadChapterBackup(
                                    chapterUrl = "${novel.source}#chapter-$chapterIndex",
                                    novelUrl = novel.source,
                                    readAt = readAt
                                )
                            )
                        }
                    }
                }
            }
        }

        return readChapters
    }

    private fun findLastReadTimestamp(
        novelName: String,
        currentChapter: Int,
        strings: Map<String, String>
    ): Long? {
        // Try current chapter first
        val currentKey = "reader_epub_position_read/$novelName/$currentChapter"
        strings[currentKey]?.toLongOrNull()?.let { return it }

        // Find any read timestamp for this novel
        var latestTimestamp: Long? = null
        strings.forEach { (key, value) ->
            if (key.startsWith("reader_epub_position_read/$novelName/")) {
                val timestamp = value.toLongOrNull()
                if (timestamp != null && (latestTimestamp == null || timestamp > latestTimestamp!!)) {
                    latestTimestamp = timestamp
                }
            }
        }

        return latestTimestamp
    }

    /**
     * Map QuickNovel reading status to Novery ReadingStatus
     * QuickNovel states observed:
     * 0 = None/Default
     * 1 = Plan to Read (assumed)
     * 2 = Reading
     * 3 = Completed
     * 4 = On Hold (assumed)
     * 5 = Following/Dropped (observed on ongoing novels)
     */
    private fun mapReadingStatus(state: Int): String {
        return when (state) {
            0 -> "READING"       // Default - if it's bookmarked, assume reading
            1 -> "PLAN_TO_READ"
            2 -> "READING"
            3 -> "COMPLETED"
            4 -> "ON_HOLD"
            5 -> "READING"       // "Following" - treat as reading for ongoing novels
            else -> "READING"
        }
    }

    /**
     * Map QuickNovel API names to Novery provider names
     */
    private fun mapApiName(apiName: String): String {
        return when (apiName.lowercase()) {
            "novelbin" -> "NovelBin"
            "libread" -> "LibRead"
            "webnovel" -> "WebNovel"
            "royal road", "royalroad" -> "RoyalRoad"
            "novelsonline" -> "NovelsOnline"
            "freewebnovel" -> "FreeWebNovel"
            "mtlnovel" -> "MtlNovel"
            "scribblehub" -> "Scribbleub"
            "wuxiaworld" -> "WuxiaWorld"
            else -> apiName
        }
    }

    private fun parseAppSettings(settings: QuickNovelSettings?): AppSettingsBackup? {
        if (settings == null) return null

        val themeKey = settings._String["theme_key"] ?: ""
        val themeMode = when {
            themeKey.contains("Light", ignoreCase = true) -> "LIGHT"
            else -> "DARK"
        }
        val amoledBlack = themeKey.contains("Amoled", ignoreCase = true)

        val displayMode = when (settings._String["download_format"]) {
            "grid" -> "GRID"
            "list" -> "LIST"
            else -> "GRID"
        }

        // Provider settings
        val enabledProviders = settings._StringSet["search_providers_list"] ?: emptyList()

        return AppSettingsBackup(
            themeMode = themeMode,
            amoledBlack = amoledBlack,
            libraryDisplayMode = displayMode,
            browseDisplayMode = displayMode,
            searchDisplayMode = displayMode,
            providerOrder = enabledProviders,
            disabledProviders = emptyList(),
            keepScreenOn = settings._Bool["external_reader"] != true // Inverse assumption
        )
    }

    private fun parseReaderSettings(
        strings: Map<String, String>,
        settings: QuickNovelSettings?
    ): ReaderSettingsBackup {
        // Extract TTS settings (closest thing to reader settings in QuickNovel)
        val ttsSpeed = strings["reader_epub_tts_speed"]?.toFloatOrNull() ?: 1.0f
        val ttsPitch = strings["reader_epub_tts_pitch"]?.toFloatOrNull() ?: 1.0f

        // Determine theme from settings
        val themeKey = settings?._String?.get("theme_key") ?: ""
        val readerTheme = when {
            themeKey.contains("Amoled", ignoreCase = true) -> "amoled"
            themeKey.contains("Dark", ignoreCase = true) -> "dark"
            themeKey.contains("Light", ignoreCase = true) -> "light"
            else -> "dark"
        }

        return ReaderSettingsBackup(
            theme = readerTheme,
            // TTS speed can hint at reading speed preference
            // If high TTS speed, maybe user prefers faster reading
            autoScrollSpeed = ttsSpeed.coerceIn(0.5f, 3.0f)
        )
    }
}