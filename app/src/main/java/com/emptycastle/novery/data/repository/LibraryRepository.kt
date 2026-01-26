// com/emptycastle/novery/data/repository/LibraryRepository.kt
// REPLACE with this enhanced version

package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.LibraryDao
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.LibraryEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Reading position data
 */
data class ReadingPosition(
    val chapterUrl: String,
    val chapterName: String,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val timestamp: Long
)

/**
 * Data class combining library entry with additional info
 */
data class LibraryItem(
    val novel: Novel,
    val readingStatus: ReadingStatus,
    val addedAt: Long,
    val downloadedChaptersCount: Int = 0,
    val lastReadPosition: ReadingPosition? = null,

    // New chapter tracking
    val totalChapterCount: Int = 0,
    val newChapterCount: Int = 0,
    val unreadChapterCount: Int = 0,
    val hasNewChapters: Boolean = false,
    val lastCheckedAt: Long = 0
)

/**
 * Result of a library refresh operation
 */
data class LibraryRefreshResult(
    val updatedCount: Int,
    val totalNewChapters: Int,
    val errors: List<String> = emptyList()
)

/**
 * Repository for library operations.
 */
class LibraryRepository(
    private val libraryDao: LibraryDao,
    private val offlineDao: OfflineDao
) {

    // ================================================================
    // OBSERVE LIBRARY
    // ================================================================

    fun observeLibrary(): Flow<List<LibraryItem>> {
        return libraryDao.getAllFlow().map { entities ->
            entities.map { entity -> entity.toLibraryItem() }
        }
    }

    fun observeIsFavorite(url: String): Flow<Boolean> {
        return libraryDao.existsFlow(url)
    }

    /**
     * Observe novels with new chapters
     */
    fun observeNovelsWithNewChapters(): Flow<List<LibraryItem>> {
        return libraryDao.observeNovelsWithNewChapters().map { entities ->
            entities.map { it.toLibraryItem() }
        }
    }

    /**
     * Observe total new chapter count for badge
     */
    fun observeTotalNewChapterCount(): Flow<Int> {
        return libraryDao.observeTotalNewChapterCount()
    }

    // ================================================================
    // GET LIBRARY
    // ================================================================

    suspend fun getLibrary(): List<LibraryItem> = withContext(Dispatchers.IO) {
        val entities = libraryDao.getAll()
        val downloadCounts = getDownloadCounts()

        entities.map { entity ->
            entity.toLibraryItem(downloadCounts[entity.url] ?: 0)
        }
    }
    /**
     * Get a single library item by URL
     * */
    suspend fun getLibraryItem(url: String): LibraryItem? = withContext(Dispatchers.IO) {
        val entity = libraryDao.getByUrl(url) ?: return@withContext null
        val downloadCount = offlineDao.getDownloadedCount(url)
        entity.toLibraryItem(downloadCount)
    }

    suspend fun getLibraryByStatus(status: ReadingStatus): List<LibraryItem> =
        withContext(Dispatchers.IO) {
            getLibrary().filter { it.readingStatus == status }
        }

    suspend fun isFavorite(url: String): Boolean = withContext(Dispatchers.IO) {
        libraryDao.exists(url)
    }

    suspend fun getEntry(url: String): LibraryEntity? = withContext(Dispatchers.IO) {
        libraryDao.getByUrl(url)
    }

    suspend fun getReadingPosition(novelUrl: String): ReadingPosition? = withContext(Dispatchers.IO) {
        val entity = libraryDao.getByUrl(novelUrl)
        if (entity?.lastChapterUrl != null) {
            ReadingPosition(
                chapterUrl = entity.lastChapterUrl,
                chapterName = entity.lastChapterName ?: "",
                scrollIndex = entity.lastScrollIndex,
                scrollOffset = entity.lastScrollOffset,
                timestamp = entity.lastReadAt ?: 0L
            )
        } else null
    }

    /**
     * Get novels that need refresh
     */
    suspend fun getNovelsNeedingRefresh(
        maxAgeMs: Long = 24 * 60 * 60 * 1000, // 24 hours default
        limit: Int = 10
    ): List<LibraryEntity> = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMs
        libraryDao.getNovelsNeedingRefresh(threshold, limit)
    }

    /**
     * Get total new chapter count
     */
    suspend fun getTotalNewChapterCount(): Int = withContext(Dispatchers.IO) {
        libraryDao.getTotalNewChapterCount()
    }

    // ================================================================
    // SEARCH
    // ================================================================

    suspend fun searchLibrary(query: String): List<LibraryItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getLibrary()

        val entities = libraryDao.search(query.trim())
        val downloadCounts = getDownloadCounts()

        entities.map { entity ->
            entity.toLibraryItem(downloadCounts[entity.url] ?: 0)
        }
    }

    // ================================================================
    // MODIFY LIBRARY
    // ================================================================

    suspend fun addToLibrary(
        novel: Novel,
        status: ReadingStatus = ReadingStatus.READING
    ) = withContext(Dispatchers.IO) {
        val entity = LibraryEntity.fromNovel(novel, status)
        libraryDao.insert(entity)
    }

    suspend fun addToLibraryWithDetails(
        novel: Novel,
        details: NovelDetails,
        status: ReadingStatus = ReadingStatus.READING
    ) = withContext(Dispatchers.IO) {
        val chapterCount = details.chapters.size

        val entity = LibraryEntity.fromNovel(novel, status, chapterCount).copy(
            latestChapter = details.chapters.lastOrNull()?.name,
            lastCheckedAt = System.currentTimeMillis()
        )
        libraryDao.insert(entity)

        offlineDao.saveNovelDetails(NovelDetailsEntity.fromNovelDetails(details))
        offlineDao.saveNovel(
            OfflineNovelEntity(
                url = novel.url,
                name = novel.name,
                coverUrl = novel.posterUrl
            )
        )
    }

    suspend fun removeFromLibrary(url: String) = withContext(Dispatchers.IO) {
        libraryDao.delete(url)
    }

    suspend fun toggleFavorite(novel: Novel): Boolean = withContext(Dispatchers.IO) {
        val exists = libraryDao.exists(novel.url)
        if (exists) {
            libraryDao.delete(novel.url)
            false
        } else {
            libraryDao.insert(LibraryEntity.fromNovel(novel))
            true
        }
    }

    suspend fun updateStatus(url: String, status: ReadingStatus) =
        withContext(Dispatchers.IO) {
            libraryDao.updateStatus(url, status.name)
        }

    suspend fun updateReadingPosition(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        scrollIndex: Int = 0,
        scrollOffset: Int = 0
    ) = withContext(Dispatchers.IO) {
        libraryDao.updateReadingPosition(
            novelUrl = novelUrl,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            timestamp = System.currentTimeMillis(),
            scrollIndex = scrollIndex,
            scrollOffset = scrollOffset
        )
    }

    suspend fun updateLastChapter(
        novelUrl: String,
        chapter: Chapter
    ) = withContext(Dispatchers.IO) {
        libraryDao.updateLastChapter(
            novelUrl = novelUrl,
            chapterUrl = chapter.url,
            chapterName = chapter.name,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun clearLibrary() = withContext(Dispatchers.IO) {
        libraryDao.deleteAll()
    }

    // ================================================================
    // CHAPTER TRACKING & BADGES
    // ================================================================

    /**
     * Update chapter count after fetching novel details
     */
    suspend fun updateChapterCount(
        novelUrl: String,
        totalCount: Int,
        latestChapter: String?
    ) = withContext(Dispatchers.IO) {
        libraryDao.updateChapterCount(
            novelUrl = novelUrl,
            totalCount = totalCount,
            latestChapter = latestChapter
        )
    }

    /**
     * Acknowledge new chapters (clears badge for this novel)
     * Call when user opens novel details
     */
    suspend fun acknowledgeNewChapters(novelUrl: String) = withContext(Dispatchers.IO) {
        libraryDao.acknowledgeNewChapters(novelUrl)
    }

    /**
     * Update unread chapter tracking
     */
    suspend fun updateUnreadTracking(
        novelUrl: String,
        lastReadChapterIndex: Int,
        totalChapters: Int
    ) = withContext(Dispatchers.IO) {
        val unreadCount = (totalChapters - lastReadChapterIndex - 1).coerceAtLeast(0)
        libraryDao.updateUnreadTracking(novelUrl, lastReadChapterIndex, unreadCount)
    }

    /**
     * Refresh a single novel's chapter count
     */
    suspend fun refreshNovelChapters(
        novelUrl: String,
        provider: MainProvider
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val details = provider.load(novelUrl)
            if (details != null) {
                val newCount = details.chapters.size
                updateChapterCount(
                    novelUrl = novelUrl,
                    totalCount = newCount,
                    latestChapter = details.chapters.lastOrNull()?.name
                )

                // Also update cached details
                offlineDao.saveNovelDetails(NovelDetailsEntity.fromNovelDetails(details))

                Result.success(newCount)
            } else {
                Result.failure(Exception("Failed to load novel details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh all library novels
     * @param onProgress Callback with (current, total, novelName)
     */
    suspend fun refreshAllNovels(
        getProvider: (String) -> MainProvider?,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): LibraryRefreshResult = withContext(Dispatchers.IO) {
        val novels = libraryDao.getAll()
        var updatedCount = 0
        var totalNewChapters = 0
        val errors = mutableListOf<String>()

        novels.forEachIndexed { index, entity ->
            onProgress(index + 1, novels.size, entity.name)

            val provider = getProvider(entity.apiName)
            if (provider != null) {
                try {
                    val details = provider.load(entity.url)
                    if (details != null) {
                        val oldCount = entity.totalChapterCount
                        val newCount = details.chapters.size

                        if (newCount > oldCount) {
                            totalNewChapters += (newCount - oldCount)
                            updatedCount++
                        }

                        updateChapterCount(
                            novelUrl = entity.url,
                            totalCount = newCount,
                            latestChapter = details.chapters.lastOrNull()?.name
                        )

                        // Update cached details
                        offlineDao.saveNovelDetails(NovelDetailsEntity.fromNovelDetails(details))
                    }
                } catch (e: Exception) {
                    errors.add("${entity.name}: ${e.message}")
                }
            }

            // Small delay to avoid rate limiting
            kotlinx.coroutines.delay(300)
        }

        LibraryRefreshResult(
            updatedCount = updatedCount,
            totalNewChapters = totalNewChapters,
            errors = errors
        )
    }

    // ================================================================
    // DOWNLOAD COUNTS
    // ================================================================

    suspend fun getDownloadCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        offlineDao.getAllNovelCounts().associate { it.novelUrl to it.count }
    }

    suspend fun getDownloadCount(novelUrl: String): Int = withContext(Dispatchers.IO) {
        offlineDao.getDownloadedCount(novelUrl)
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private fun LibraryEntity.toLibraryItem(downloadCount: Int = 0): LibraryItem {
        return LibraryItem(
            novel = toNovel(),
            readingStatus = getStatus(),
            addedAt = addedAt,
            downloadedChaptersCount = downloadCount,
            lastReadPosition = if (lastChapterUrl != null) {
                ReadingPosition(
                    chapterUrl = lastChapterUrl,
                    chapterName = lastChapterName ?: "",
                    scrollIndex = lastScrollIndex,
                    scrollOffset = lastScrollOffset,
                    timestamp = lastReadAt ?: 0L
                )
            } else null,
            totalChapterCount = totalChapterCount,
            newChapterCount = newChapterCount,
            unreadChapterCount = unreadChapterCount,
            hasNewChapters = hasNewChapters,
            lastCheckedAt = lastCheckedAt
        )
    }
}