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
    val lastReadPosition: ReadingPosition? = null
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
            entities.map { entity ->
                LibraryItem(
                    novel = entity.toNovel(),
                    readingStatus = entity.getStatus(),
                    addedAt = entity.addedAt,
                    downloadedChaptersCount = 0,
                    lastReadPosition = if (entity.lastChapterUrl != null) {
                        ReadingPosition(
                            chapterUrl = entity.lastChapterUrl,
                            chapterName = entity.lastChapterName ?: "",
                            scrollIndex = entity.lastScrollIndex,
                            scrollOffset = entity.lastScrollOffset,
                            timestamp = entity.lastReadAt ?: 0L
                        )
                    } else null
                )
            }
        }
    }

    fun observeIsFavorite(url: String): Flow<Boolean> {
        return libraryDao.existsFlow(url)
    }

    // ================================================================
    // GET LIBRARY
    // ================================================================

    suspend fun getLibrary(): List<LibraryItem> = withContext(Dispatchers.IO) {
        val entities = libraryDao.getAll()
        val downloadCounts = getDownloadCounts()

        entities.map { entity ->
            LibraryItem(
                novel = entity.toNovel(),
                readingStatus = entity.getStatus(),
                addedAt = entity.addedAt,
                downloadedChaptersCount = downloadCounts[entity.url] ?: 0,
                lastReadPosition = if (entity.lastChapterUrl != null) {
                    ReadingPosition(
                        chapterUrl = entity.lastChapterUrl,
                        chapterName = entity.lastChapterName ?: "",
                        scrollIndex = entity.lastScrollIndex,
                        scrollOffset = entity.lastScrollOffset,
                        timestamp = entity.lastReadAt ?: 0L
                    )
                } else null
            )
        }
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

    // ================================================================
    // SEARCH
    // ================================================================

    suspend fun searchLibrary(query: String): List<LibraryItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getLibrary()

        val entities = libraryDao.search(query.trim())
        val downloadCounts = getDownloadCounts()

        entities.map { entity ->
            LibraryItem(
                novel = entity.toNovel(),
                readingStatus = entity.getStatus(),
                addedAt = entity.addedAt,
                downloadedChaptersCount = downloadCounts[entity.url] ?: 0,
                lastReadPosition = if (entity.lastChapterUrl != null) {
                    ReadingPosition(
                        chapterUrl = entity.lastChapterUrl,
                        chapterName = entity.lastChapterName ?: "",
                        scrollIndex = entity.lastScrollIndex,
                        scrollOffset = entity.lastScrollOffset,
                        timestamp = entity.lastReadAt ?: 0L
                    )
                } else null
            )
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
        val entity = LibraryEntity.fromNovel(novel, status)
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
    // DOWNLOAD COUNTS
    // ================================================================

    suspend fun getDownloadCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        offlineDao.getAllNovelCounts().associate { it.novelUrl to it.count }
    }

    suspend fun getDownloadCount(novelUrl: String): Int = withContext(Dispatchers.IO) {
        offlineDao.getDownloadedCount(novelUrl)
    }
}