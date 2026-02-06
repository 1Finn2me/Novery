// com/emptycastle/novery/data/cache/CacheManager.kt

package com.emptycastle.novery.data.cache

import android.content.Context
import coil.imageLoader
import com.emptycastle.novery.data.local.NovelDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Detailed cache information
 */
data class CacheInfo(
    val downloadedChapters: CacheCategory = CacheCategory(),
    val novelDetailsCache: CacheCategory = CacheCategory(),
    val imageCache: CacheCategory = CacheCategory(),
    val otherCache: CacheCategory = CacheCategory()
) {
    val totalSize: Long
        get() = downloadedChapters.sizeBytes + novelDetailsCache.sizeBytes +
                imageCache.sizeBytes + otherCache.sizeBytes

    val totalCount: Int
        get() = downloadedChapters.itemCount + novelDetailsCache.itemCount +
                imageCache.itemCount + otherCache.itemCount

    fun formattedTotalSize(): String = formatSize(totalSize)
}

data class CacheCategory(
    val sizeBytes: Long = 0,
    val itemCount: Int = 0,
    val novelCount: Int = 0 // For downloads, how many novels have downloads
) {
    fun formattedSize(): String = formatSize(sizeBytes)
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Per-novel download info
 */
data class NovelDownloadInfo(
    val novelUrl: String,
    val novelName: String,
    val chapterCount: Int,
    val sizeBytes: Long
) {
    fun formattedSize(): String = formatSize(sizeBytes)
}

/**
 * Result of cache clear operation
 */
data class ClearCacheResult(
    val success: Boolean,
    val clearedBytes: Long = 0,
    val clearedItems: Int = 0,
    val error: String? = null
) {
    fun formattedClearedSize(): String = formatSize(clearedBytes)
}

/**
 * Manages app cache and downloaded content
 */
class CacheManager(
    private val context: Context,
    private val database: NovelDatabase
) {

    // ================================================================
    // CACHE INFO
    // ================================================================

    /**
     * Get complete cache information
     */
    suspend fun getCacheInfo(): CacheInfo = withContext(Dispatchers.IO) {
        CacheInfo(
            downloadedChapters = getDownloadedChaptersInfo(),
            novelDetailsCache = getNovelDetailsCacheInfo(),
            imageCache = getImageCacheInfo(),
            otherCache = getOtherCacheInfo()
        )
    }

    private suspend fun getDownloadedChaptersInfo(): CacheCategory {
        val offlineDao = database.offlineDao()

        val chapters = offlineDao.getAllChapters()
        val novelCounts = offlineDao.getAllNovelCounts()

        // Estimate size: content length * 2 bytes per char (UTF-16)
        val totalSize = chapters.sumOf { (it.content.length + it.title.length) * 2L }

        return CacheCategory(
            sizeBytes = totalSize,
            itemCount = chapters.size,
            novelCount = novelCounts.size
        )
    }

    private suspend fun getNovelDetailsCacheInfo(): CacheCategory {
        val offlineDao = database.offlineDao()

        val details = offlineDao.getAllNovelDetails()

        val totalSize = details.sumOf { detail ->
            var size = 0L
            // Basic string fields
            size += (detail.name.length + (detail.synopsis?.length ?: 0)) * 2L
            size += (detail.author?.length ?: 0) * 2L
            size += (detail.status?.length ?: 0) * 2L
            size += (detail.url.length) * 2L
            size += (detail.posterUrl?.length ?: 0) * 2L
            size += (detail.relatedNovelsJson?.length ?: 0) * 2L
            size += (detail.apiName.length) * 2L

            // Estimate size for chapters list
            detail.chapters?.forEach { chapter ->
                size += (chapter.name.length + chapter.url.length + (chapter.dateOfRelease?.length ?: 0)) * 2L
            }

            // Estimate size for tags list
            detail.tags?.forEach { tag ->
                size += tag.length * 2L
            }

            size
        }

        return CacheCategory(
            sizeBytes = totalSize,
            itemCount = details.size
        )
    }

    private fun getImageCacheInfo(): CacheCategory {
        var totalSize = 0L
        var fileCount = 0

        // Custom notification image cache
        val notificationCacheDir = File(context.cacheDir, "notification_images")
        if (notificationCacheDir.exists()) {
            notificationCacheDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                    fileCount++
                }
            }
        }

        // Coil disk cache
        val coilCacheDir = File(context.cacheDir, "image_cache")
        if (coilCacheDir.exists()) {
            coilCacheDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                    fileCount++
                }
            }
        }

        return CacheCategory(
            sizeBytes = totalSize,
            itemCount = fileCount
        )
    }

    private fun getOtherCacheInfo(): CacheCategory {
        var totalSize = 0L
        var fileCount = 0

        val excludeDirs = setOf("notification_images", "image_cache")

        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name !in excludeDirs) {
                if (file.isDirectory) {
                    file.walkTopDown().forEach { f ->
                        if (f.isFile) {
                            totalSize += f.length()
                            fileCount++
                        }
                    }
                } else {
                    totalSize += file.length()
                    fileCount++
                }
            }
        }

        return CacheCategory(
            sizeBytes = totalSize,
            itemCount = fileCount
        )
    }

    // ================================================================
    // PER-NOVEL DOWNLOAD INFO
    // ================================================================

    /**
     * Get download info for all novels
     */
    suspend fun getNovelDownloads(): List<NovelDownloadInfo> = withContext(Dispatchers.IO) {
        val offlineDao = database.offlineDao()
        val libraryDao = database.libraryDao()

        val novelCounts = offlineDao.getAllNovelCounts()
        val allChapters = offlineDao.getAllChapters()

        // Group chapters by novel URL for size calculation
        val chaptersByNovel = allChapters.groupBy { it.novelUrl }

        novelCounts.map { count ->
            val novel = offlineDao.getNovel(count.novelUrl)
            val libraryEntry = libraryDao.getByUrl(count.novelUrl)

            // Calculate size from chapters
            val novelChapters = chaptersByNovel[count.novelUrl] ?: emptyList()
            val sizeBytes = novelChapters.sumOf { chapter ->
                (chapter.content.length + chapter.title.length) * 2L
            }

            NovelDownloadInfo(
                novelUrl = count.novelUrl,
                novelName = novel?.name ?: libraryEntry?.name ?: "Unknown Novel",
                chapterCount = count.count,
                sizeBytes = sizeBytes
            )
        }.sortedByDescending { it.sizeBytes }
    }

    // ================================================================
    // CLEAR CACHE
    // ================================================================

    /**
     * Clear all downloaded chapters
     */
    suspend fun clearDownloadedChapters(): ClearCacheResult = withContext(Dispatchers.IO) {
        try {
            val offlineDao = database.offlineDao()

            // Get size before clearing
            val info = getDownloadedChaptersInfo()

            offlineDao.deleteAllChapters()
            offlineDao.deleteAllNovels()

            ClearCacheResult(
                success = true,
                clearedBytes = info.sizeBytes,
                clearedItems = info.itemCount
            )
        } catch (e: Exception) {
            ClearCacheResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Clear downloads for a specific novel
     */
    suspend fun clearNovelDownloads(novelUrl: String): ClearCacheResult = withContext(Dispatchers.IO) {
        try {
            val offlineDao = database.offlineDao()

            // Calculate size before clearing
            val allChapters = offlineDao.getAllChapters()
            val novelChapters = allChapters.filter { it.novelUrl == novelUrl }

            val clearedBytes = novelChapters.sumOf { chapter ->
                (chapter.content.length + chapter.title.length) * 2L
            }
            val clearedItems = novelChapters.size

            offlineDao.deleteChaptersForNovel(novelUrl)
            offlineDao.deleteNovel(novelUrl)
            offlineDao.deleteNovelDetails(novelUrl)

            ClearCacheResult(
                success = true,
                clearedBytes = clearedBytes,
                clearedItems = clearedItems
            )
        } catch (e: Exception) {
            ClearCacheResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Clear novel details cache
     */
    suspend fun clearNovelDetailsCache(): ClearCacheResult = withContext(Dispatchers.IO) {
        try {
            val offlineDao = database.offlineDao()

            val info = getNovelDetailsCacheInfo()

            offlineDao.deleteAllNovelDetails()

            ClearCacheResult(
                success = true,
                clearedBytes = info.sizeBytes,
                clearedItems = info.itemCount
            )
        } catch (e: Exception) {
            ClearCacheResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Clear image cache (Coil + notification images)
     */
    suspend fun clearImageCache(): ClearCacheResult = withContext(Dispatchers.IO) {
        try {
            val info = getImageCacheInfo()

            // Clear Coil memory cache
            context.imageLoader.memoryCache?.clear()

            // Clear Coil disk cache
            context.imageLoader.diskCache?.clear()

            // Clear custom notification image cache
            val notificationCacheDir = File(context.cacheDir, "notification_images")
            deleteDirectory(notificationCacheDir)

            // Clear coil cache directory (backup)
            val coilCacheDir = File(context.cacheDir, "image_cache")
            deleteDirectory(coilCacheDir)

            ClearCacheResult(
                success = true,
                clearedBytes = info.sizeBytes,
                clearedItems = info.itemCount
            )
        } catch (e: Exception) {
            ClearCacheResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Clear all caches (keeps library, history, bookmarks)
     */
    suspend fun clearAllCaches(): ClearCacheResult = withContext(Dispatchers.IO) {
        try {
            val totalInfo = getCacheInfo()

            // Clear downloads
            clearDownloadedChapters()

            // Clear novel details
            clearNovelDetailsCache()

            // Clear images
            clearImageCache()

            // Clear other cache files
            val excludeDirs = setOf("notification_images", "image_cache")
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name !in excludeDirs) {
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }

            ClearCacheResult(
                success = true,
                clearedBytes = totalInfo.totalSize,
                clearedItems = totalInfo.totalCount
            )
        } catch (e: Exception) {
            ClearCacheResult(
                success = false,
                error = e.message
            )
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun deleteDirectory(directory: File): Boolean {
        if (!directory.exists()) return true

        directory.walkBottomUp().forEach { file ->
            file.delete()
        }
        return !directory.exists()
    }
}