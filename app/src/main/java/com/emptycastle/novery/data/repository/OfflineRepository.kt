package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.NovelDownloadData
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Download progress callback
 */
data class DownloadProgress(
    val current: Int,
    val total: Int,
    val currentChapterName: String,
    val status: DownloadStatus
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * Repository for offline storage operations.
 * Handles saving chapters for offline reading.
 */
class OfflineRepository(
    private val offlineDao: OfflineDao
) {

    // ================================================================
    // OBSERVE OFFLINE DATA
    // ================================================================

    /**
     * Observe downloaded chapter URLs for a novel
     */
    fun observeDownloadedChapters(novelUrl: String): Flow<Set<String>> {
        return offlineDao.getDownloadedChapterUrlsFlow(novelUrl).map { it.toSet() }
    }

    // ================================================================
    // GET OFFLINE DATA
    // ================================================================

    /**
     * Get all downloaded chapter URLs for a novel
     */
    suspend fun getDownloadedChapterUrls(novelUrl: String): Set<String> =
        withContext(Dispatchers.IO) {
            offlineDao.getDownloadedChapterUrls(novelUrl).toSet()
        }

    /**
     * Get downloaded chapter content
     */
    suspend fun getChapterContent(chapterUrl: String): String? =
        withContext(Dispatchers.IO) {
            offlineDao.getChapter(chapterUrl)?.content
        }

    /**
     * Check if chapter is downloaded
     */
    suspend fun isChapterDownloaded(chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            offlineDao.getChapter(chapterUrl) != null
        }

    /**
     * Get download count for a novel
     */
    suspend fun getDownloadedCount(novelUrl: String): Int =
        withContext(Dispatchers.IO) {
            offlineDao.getDownloadedCount(novelUrl)
        }

    /**
     * Get all download counts
     */
    suspend fun getAllDownloadCounts(): Map<String, Int> =
        withContext(Dispatchers.IO) {
            offlineDao.getAllNovelCounts().associate { it.novelUrl to it.count }
        }

    /**
     * Get all download data including timestamps for sorting
     */
    suspend fun getAllDownloadData(): List<NovelDownloadData> =
        withContext(Dispatchers.IO) {
            offlineDao.getAllNovelDownloadData()
        }

    /**
     * Check if novel has any downloads
     */
    suspend fun isNovelDownloaded(novelUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            offlineDao.isNovelDownloaded(novelUrl)
        }

    /**
     * Get cached novel details
     */
    suspend fun getNovelDetails(novelUrl: String): NovelDetails? =
        withContext(Dispatchers.IO) {
            offlineDao.getNovelDetails(novelUrl)?.toNovelDetails()
        }

    // ================================================================
    // SAVE OFFLINE DATA
    // ================================================================

    /**
     * Save a single chapter for offline reading
     */
    suspend fun saveChapter(
        chapterUrl: String,
        novelUrl: String,
        title: String,
        content: String
    ) = withContext(Dispatchers.IO) {
        val entity = OfflineChapterEntity(
            url = chapterUrl,
            novelUrl = novelUrl,
            title = title,
            content = content,
            downloadedAt = System.currentTimeMillis()
        )
        offlineDao.saveChapter(entity)
    }

    /**
     * Save novel metadata
     */
    suspend fun saveNovelMetadata(novel: Novel) = withContext(Dispatchers.IO) {
        val entity = OfflineNovelEntity(
            url = novel.url,
            name = novel.name,
            coverUrl = novel.posterUrl
        )
        offlineDao.saveNovel(entity)
    }

    /**
     * Save novel details for offline access
     */
    suspend fun saveNovelDetails(details: NovelDetails) = withContext(Dispatchers.IO) {
        val entity = NovelDetailsEntity.fromNovelDetails(details)
        offlineDao.saveNovelDetails(entity)
    }

    // ================================================================
    // BATCH DOWNLOAD
    // ================================================================

    /**
     * Download multiple chapters with progress callback
     *
     * @param provider The provider to fetch from
     * @param novel The novel being downloaded
     * @param chapters List of chapters to download
     * @param onProgress Progress callback
     * @param shouldContinue Lambda to check if download should continue (for cancellation)
     * @return Number of successfully downloaded chapters
     */
    suspend fun downloadChapters(
        provider: MainProvider,
        novel: Novel,
        chapters: List<Chapter>,
        onProgress: (DownloadProgress) -> Unit,
        shouldContinue: () -> Boolean = { true }
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        val total = chapters.size
        val existingDownloads = getDownloadedChapterUrls(novel.url)

        // Save novel metadata first
        saveNovelMetadata(novel)

        chapters.forEachIndexed { index, chapter ->
            // Check for cancellation
            if (!shouldContinue()) {
                onProgress(
                    DownloadProgress(
                        current = index,
                        total = total,
                        currentChapterName = "Paused",
                        status = DownloadStatus.PAUSED
                    )
                )
                return@withContext successCount
            }

            // Skip if already downloaded
            if (existingDownloads.contains(chapter.url)) {
                onProgress(
                    DownloadProgress(
                        current = index + 1,
                        total = total,
                        currentChapterName = "Skipping: ${chapter.name}",
                        status = DownloadStatus.DOWNLOADING
                    )
                )
                successCount++
                return@forEachIndexed
            }

            onProgress(
                DownloadProgress(
                    current = index + 1,
                    total = total,
                    currentChapterName = chapter.name,
                    status = DownloadStatus.DOWNLOADING
                )
            )

            try {
                val content = provider.loadChapterContent(chapter.url)

                if (content != null) {
                    saveChapter(
                        chapterUrl = chapter.url,
                        novelUrl = novel.url,
                        title = chapter.name,
                        content = content
                    )
                    successCount++
                }

                // Small delay to avoid hammering the server
                kotlinx.coroutines.delay(200)

            } catch (e: Exception) {
                // Log error but continue with next chapter
                e.printStackTrace()
            }
        }

        onProgress(
            DownloadProgress(
                current = total,
                total = total,
                currentChapterName = "Complete!",
                status = DownloadStatus.COMPLETED
            )
        )

        successCount
    }

    // ================================================================
    // DELETE OFFLINE DATA
    // ================================================================

    /**
     * Delete all downloaded chapters for a novel
     */
    suspend fun deleteNovelDownloads(novelUrl: String) = withContext(Dispatchers.IO) {
        offlineDao.deleteChaptersForNovel(novelUrl)
        offlineDao.deleteNovel(novelUrl)
        offlineDao.deleteNovelDetails(novelUrl)
    }

    /**
     * Delete a list of downloaded chapters.
     * If all chapters for a novel are deleted, the novel's offline entry is also removed.
     */
    suspend fun deleteChapters(novelUrl: String, chapterUrls: List<String>) = withContext(Dispatchers.IO) {
        if (chapterUrls.isEmpty()) return@withContext

        offlineDao.deleteChapters(chapterUrls)

        val remainingCount = offlineDao.getDownloadedCount(novelUrl)
        if (remainingCount == 0) {
            offlineDao.deleteNovel(novelUrl)
            offlineDao.deleteNovelDetails(novelUrl)
        }
    }

    /**
     * Delete a single downloaded chapter.
     */
    suspend fun deleteChapter(chapterUrl: String) = withContext(Dispatchers.IO) {
        val chapter = offlineDao.getChapter(chapterUrl) ?: return@withContext
        deleteChapters(chapter.novelUrl, listOf(chapterUrl))
    }
}