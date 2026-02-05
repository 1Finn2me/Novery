// service/TTSBackgroundLoader.kt
package com.emptycastle.novery.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.util.SentenceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Handles loading chapter content for TTS playback in the background.
 * This operates independently of the UI and can load chapters
 * even when the screen is off.
 */
class TTSBackgroundLoader(private val context: Context) {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()

    private val loadMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cached chapter data
    private var novelUrl: String = ""
    private var novelName: String = ""
    private var providerName: String = ""
    private var coverUrl: String? = null
    private var coverBitmap: Bitmap? = null
    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = -1

    // Pre-cached next chapter content
    private var cachedNextContent: TTSContent? = null
    private var cachedNextIndex: Int = -1

    /**
     * Configure the loader with novel and chapter information.
     * Call this when starting TTS playback.
     */
    suspend fun configure(
        novelUrl: String,
        providerName: String,
        chapters: List<Chapter>,
        currentIndex: Int
    ) = withContext(Dispatchers.IO) {
        this@TTSBackgroundLoader.novelUrl = novelUrl
        this@TTSBackgroundLoader.providerName = providerName
        this@TTSBackgroundLoader.chapters = chapters
        this@TTSBackgroundLoader.currentChapterIndex = currentIndex

        // Load novel details for name and cover
        val details = offlineRepository.getNovelDetails(novelUrl)
        novelName = details?.name ?: "Novel"
        coverUrl = details?.posterUrl

        // Pre-load cover bitmap
        coverBitmap = loadCoverBitmap(coverUrl)

        // Pre-cache next chapter if available
        if (hasNextChapter()) {
            preloadNextChapter()
        }
    }

    /**
     * Update the current chapter index.
     */
    fun setCurrentChapterIndex(index: Int) {
        currentChapterIndex = index
    }

    fun getCurrentChapterIndex(): Int = currentChapterIndex

    fun getTotalChapters(): Int = chapters.size

    fun hasNextChapter(): Boolean = currentChapterIndex < chapters.size - 1

    fun hasPreviousChapter(): Boolean = currentChapterIndex > 0

    fun getCurrentChapterName(): String = chapters.getOrNull(currentChapterIndex)?.name ?: ""

    fun getCurrentChapterUrl(): String = chapters.getOrNull(currentChapterIndex)?.url ?: ""

    fun getNovelName(): String = novelName

    fun getNovelUrl(): String = novelUrl

    fun getCoverBitmap(): Bitmap? = coverBitmap

    /**
     * Load the next chapter content.
     * Uses pre-cached content if available.
     */
    suspend fun loadNextChapter(): TTSContent? = loadMutex.withLock {
        if (!hasNextChapter()) return@withLock null

        val nextIndex = currentChapterIndex + 1

        // Use cached content if available and still valid
        if (cachedNextContent != null && cachedNextIndex == nextIndex) {
            currentChapterIndex = nextIndex
            val content = cachedNextContent
            cachedNextContent = null
            cachedNextIndex = -1

            // Start pre-loading the next one
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }

            return@withLock content
        }

        // Load fresh
        val chapter = chapters.getOrNull(nextIndex) ?: return@withLock null
        val content = loadChapterInternal(nextIndex, chapter)

        if (content != null) {
            currentChapterIndex = nextIndex

            // Pre-load the next chapter
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    /**
     * Load the previous chapter content.
     */
    suspend fun loadPreviousChapter(): TTSContent? = loadMutex.withLock {
        if (!hasPreviousChapter()) return@withLock null

        val prevIndex = currentChapterIndex - 1
        val chapter = chapters.getOrNull(prevIndex) ?: return@withLock null

        val content = loadChapterInternal(prevIndex, chapter)

        if (content != null) {
            currentChapterIndex = prevIndex

            // Invalidate next chapter cache since position changed
            cachedNextContent = null
            cachedNextIndex = -1

            // Pre-load next chapter
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    /**
     * Load a specific chapter by index.
     */
    suspend fun loadChapter(index: Int): TTSContent? = loadMutex.withLock {
        if (index < 0 || index >= chapters.size) return@withLock null

        val chapter = chapters.getOrNull(index) ?: return@withLock null
        val content = loadChapterInternal(index, chapter)

        if (content != null) {
            currentChapterIndex = index

            // Invalidate cache
            cachedNextContent = null
            cachedNextIndex = -1

            // Pre-load next
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    private suspend fun loadChapterInternal(index: Int, chapter: Chapter): TTSContent? {
        return withContext(Dispatchers.IO) {
            try {
                val provider = novelRepository.getProvider(providerName) ?: return@withContext null

                // Try cache first, then network
                val rawContent = offlineRepository.getChapterContent(chapter.url)
                    ?: novelRepository.loadChapterContent(provider, chapter.url).getOrNull()
                    ?: return@withContext null

                // Parse content into segments
                val segments = parseContent(rawContent)

                if (segments.isEmpty()) return@withContext null

                TTSContent(
                    novelName = novelName,
                    novelUrl = novelUrl,
                    chapterName = chapter.name,
                    chapterUrl = chapter.url,
                    segments = segments,
                    coverUrl = coverUrl,
                    chapterIndex = index,
                    totalChapters = chapters.size,
                    hasNextChapter = index < chapters.size - 1,
                    hasPreviousChapter = index > 0
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseContent(rawContent: String): List<TTSSegment> {
        // Clean HTML and parse into sentences
        val cleanText = cleanHtml(rawContent)

        // Use the SentenceParser object to parse
        val parsedParagraph = SentenceParser.parse(cleanText)

        // Convert ParsedSentence to TTSSegment
        return parsedParagraph.sentences.map { sentence ->
            TTSSegment(sentence.text, sentence.pauseAfterMs)
        }
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#\\d+;")) { match ->
                val code = match.value.drop(2).dropLast(1).toIntOrNull()
                code?.toChar()?.toString() ?: ""
            }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private suspend fun preloadNextChapter() {
        if (!hasNextChapter()) return

        val nextIndex = currentChapterIndex + 1
        val chapter = chapters.getOrNull(nextIndex) ?: return

        val content = loadChapterInternal(nextIndex, chapter)
        if (content != null) {
            cachedNextContent = content
            cachedNextIndex = nextIndex
        }
    }

    private fun preloadNextChapterAsync() {
        // Launch preload without blocking using the background scope
        backgroundScope.launch {
            preloadNextChapter()
        }
    }

    private suspend fun loadCoverBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                BitmapFactory.decodeStream(connection.getInputStream())
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clear all cached data.
     */
    fun clear() {
        novelUrl = ""
        novelName = ""
        providerName = ""
        coverUrl = null
        coverBitmap = null
        chapters = emptyList()
        currentChapterIndex = -1
        cachedNextContent = null
        cachedNextIndex = -1
    }
}