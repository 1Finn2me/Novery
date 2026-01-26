// com/emptycastle/novery/data/repository/ContentLoadingStrategy.kt

package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Defines how content should be loaded based on availability and user preferences
 */


/**
 * Result of a content load operation with metadata
 */
sealed class ContentResult<T> {
    data class Success<T>(
        val data: T,
        val source: ContentSource,
        val isFresh: Boolean = true
    ) : ContentResult<T>()

    data class Error<T>(
        val exception: Throwable,
        val cachedData: T? = null  // May have stale cached data
    ) : ContentResult<T>()
}


/**
 * Handles intelligent content loading with caching strategies
 */
class ContentLoadingStrategy(
    private val offlineDao: OfflineDao
) {
    // In-memory cache for frequently accessed content
    private val memoryCache = object : LinkedHashMap<String, CacheEntry>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > 50
        }
    }

    private data class CacheEntry(
        val content: Any,
        val timestamp: Long = System.currentTimeMillis(),
        val maxAgeMs: Long = 5 * 60 * 1000 // 5 minutes default
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > maxAgeMs
    }

    /**
     * Load chapter content with intelligent caching
     */
    suspend fun loadChapterContent(
        provider: MainProvider,
        chapterUrl: String,
        mode: LoadingMode = LoadingMode.OFFLINE_FIRST
    ): ContentResult<String> = withContext(Dispatchers.IO) {

        when (mode) {
            LoadingMode.OFFLINE_FIRST -> loadOfflineFirst(provider, chapterUrl)
            LoadingMode.NETWORK_FIRST -> loadNetworkFirst(provider, chapterUrl)
            LoadingMode.OFFLINE_ONLY -> loadOfflineOnly(chapterUrl)
            LoadingMode.NETWORK_ONLY -> loadNetworkOnly(provider, chapterUrl)
        }
    }

    private suspend fun loadOfflineFirst(
        provider: MainProvider,
        chapterUrl: String
    ): ContentResult<String> {
        // 1. Check memory cache
        memoryCache[chapterUrl]?.let { entry ->
            if (!entry.isExpired) {
                return ContentResult.Success(
                    data = entry.content as String,
                    source = ContentSource.MEMORY
                )
            }
        }

        // 2. Check database cache
        val cachedChapter = offlineDao.getChapter(chapterUrl)
        if (cachedChapter != null) {
            // Update memory cache
            memoryCache[chapterUrl] = CacheEntry(cachedChapter.content)
            return ContentResult.Success(
                data = cachedChapter.content,
                source = ContentSource.CACHE
            )
        }

        // 3. Fetch from network
        return loadFromNetwork(provider, chapterUrl)
    }

    private suspend fun loadNetworkFirst(
        provider: MainProvider,
        chapterUrl: String
    ): ContentResult<String> {
        val networkResult = loadFromNetwork(provider, chapterUrl)

        return when (networkResult) {
            is ContentResult.Success -> networkResult
            is ContentResult.Error -> {
                // Try cache as fallback
                val cached = offlineDao.getChapter(chapterUrl)
                if (cached != null) {
                    ContentResult.Success(
                        data = cached.content,
                        source = ContentSource.CACHE,
                        isFresh = false
                    )
                } else {
                    networkResult
                }
            }
        }
    }

    private suspend fun loadOfflineOnly(chapterUrl: String): ContentResult<String> {
        val cached = offlineDao.getChapter(chapterUrl)
        return if (cached != null) {
            ContentResult.Success(cached.content, ContentSource.CACHE)
        } else {
            ContentResult.Error(Exception("Content not available offline"))
        }
    }

    private suspend fun loadNetworkOnly(
        provider: MainProvider,
        chapterUrl: String
    ): ContentResult<String> {
        return loadFromNetwork(provider, chapterUrl)
    }

    private suspend fun loadFromNetwork(
        provider: MainProvider,
        chapterUrl: String
    ): ContentResult<String> {
        return try {
            val content = provider.loadChapterContent(chapterUrl)
            if (content != null) {
                memoryCache[chapterUrl] = CacheEntry(content)
                ContentResult.Success(content, ContentSource.NETWORK)
            } else {
                ContentResult.Error(Exception("Empty content from network"))
            }
        } catch (e: Exception) {
            ContentResult.Error(e)
        }
    }

    /**
     * Preload chapters in background for smoother reading
     */
    suspend fun preloadChapters(
        provider: MainProvider,
        chapterUrls: List<String>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        chapterUrls.forEachIndexed { index, url ->
            if (offlineDao.getChapter(url) == null && memoryCache[url] == null) {
                try {
                    val content = provider.loadChapterContent(url)
                    if (content != null) {
                        memoryCache[url] = CacheEntry(content, maxAgeMs = 30 * 60 * 1000) // 30 min for preloaded
                    }
                } catch (e: Exception) {
                    // Silent fail for preloading
                }
            }
            onProgress(index + 1, chapterUrls.size)
        }
    }

    /**
     * Clear memory cache (call on low memory)
     */
    fun clearMemoryCache() {
        memoryCache.clear()
    }

    /**
     * Check if content is available (cached or downloaded)
     */
    suspend fun isContentAvailable(chapterUrl: String): Boolean {
        return memoryCache.containsKey(chapterUrl) ||
                offlineDao.getChapter(chapterUrl) != null
    }
}