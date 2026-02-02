package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.HistoryDao
import com.emptycastle.novery.data.local.entity.HistoryEntity
import com.emptycastle.novery.data.local.entity.ReadChapterEntity
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Data class for history entries with formatted data
 */
data class HistoryItem(
    val novel: Novel,
    val chapterName: String,
    val chapterUrl: String,
    val timestamp: Long

)

/**
 * Repository for reading history operations.
 */
class HistoryRepository(
    private val historyDao: HistoryDao
) {

    // ================================================================
    // OBSERVE HISTORY
    // ================================================================

    fun observeHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAllFlow().map { entities ->
            entities.map { entity ->
                HistoryItem(
                    novel = Novel(
                        name = entity.novelName,
                        url = entity.novelUrl,
                        posterUrl = entity.posterUrl,
                        apiName = entity.apiName
                    ),
                    chapterName = entity.chapterName,
                    chapterUrl = entity.chapterUrl,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    fun observeReadChapters(novelUrl: String): Flow<Set<String>> {
        return historyDao.getReadChapterUrlsFlow(novelUrl).map { it.toSet() }
    }

    // ================================================================
    // GET HISTORY
    // ================================================================

    suspend fun getReadChapterCount(novelUrl: String): Int =
        withContext(Dispatchers.IO) {
            try {
                historyDao.getReadCountForNovel(novelUrl)
            } catch (e: Exception) {
                0
            }
        }
    suspend fun getHistory(): List<HistoryItem> = withContext(Dispatchers.IO) {
        historyDao.getAll().map { entity ->
            HistoryItem(
                novel = Novel(
                    name = entity.novelName,
                    url = entity.novelUrl,
                    posterUrl = entity.posterUrl,
                    apiName = entity.apiName
                ),
                chapterName = entity.chapterName,
                chapterUrl = entity.chapterUrl,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun getLastRead(novelUrl: String): HistoryEntity? =
        withContext(Dispatchers.IO) {
            historyDao.getByNovelUrl(novelUrl)
        }

    suspend fun getReadChapterUrls(novelUrl: String): Set<String> =
        withContext(Dispatchers.IO) {
            historyDao.getReadChapterUrls(novelUrl).toSet()
        }

    suspend fun isChapterRead(novelUrl: String, chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            historyDao.getReadChapterUrls(novelUrl).contains(chapterUrl)
        }

    // ================================================================
    // ADD TO HISTORY
    // ================================================================

    suspend fun addToHistory(
        novel: Novel,
        chapter: Chapter
    ) = withContext(Dispatchers.IO) {
        val historyEntity = HistoryEntity(
            novelUrl = novel.url,
            novelName = novel.name,
            posterUrl = novel.posterUrl,
            chapterName = chapter.name,
            chapterUrl = chapter.url,
            apiName = novel.apiName,
            timestamp = System.currentTimeMillis()
        )
        historyDao.insert(historyEntity)
        markChapterRead(novel.url, chapter.url)
    }

    suspend fun markChapterRead(novelUrl: String, chapterUrl: String) =
        withContext(Dispatchers.IO) {
            val readEntity = ReadChapterEntity(
                chapterUrl = chapterUrl,
                novelUrl = novelUrl
            )
            historyDao.markChapterRead(readEntity)
        }

    suspend fun markChaptersRead(novelUrl: String, chapterUrls: List<String>) =
        withContext(Dispatchers.IO) {
            val entities = chapterUrls.map { chapterUrl ->
                ReadChapterEntity(
                    chapterUrl = chapterUrl,
                    novelUrl = novelUrl
                )
            }
            historyDao.markChaptersRead(entities)
        }

    suspend fun markChapterUnread(novelUrl: String, chapterUrl: String) =
        withContext(Dispatchers.IO) {
            historyDao.markChapterUnread(novelUrl, chapterUrl)
        }

    suspend fun markChaptersUnread(novelUrl: String, chapterUrls: List<String>) =
        withContext(Dispatchers.IO) {
            historyDao.markChaptersUnread(novelUrl, chapterUrls)
        }

    // ================================================================
    // REMOVE FROM HISTORY
    // ================================================================

    suspend fun removeFromHistory(novelUrl: String) = withContext(Dispatchers.IO) {
        historyDao.deleteByNovelUrl(novelUrl)
    }

    // ================================================================
    // CLEAR HISTORY
    // ================================================================

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.deleteAll()
    }

    suspend fun clearReadChapters(novelUrl: String) = withContext(Dispatchers.IO) {
        historyDao.clearReadChapters(novelUrl)
    }
}