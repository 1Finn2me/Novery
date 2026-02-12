package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.remote.NetworkException
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Defines how content should be loaded
 */
enum class LoadingMode {
    OFFLINE_FIRST,      // Check cache first, then network (default for reading)
    NETWORK_FIRST,      // Check network first, cache as fallback (for refresh)
    OFFLINE_ONLY,       // Only use cached content
    NETWORK_ONLY        // Only use network (ignore cache)
}

/**
 * Indicates where content was loaded from
 */
enum class ContentSource {
    NETWORK,
    CACHE,
    MEMORY
}

/**
 * Repository for novel-related operations.
 * Coordinates between network providers and local cache.
 */
class NovelRepository(
    private val offlineDao: OfflineDao
) {

    // In-memory cache for frequently accessed content
    private val memoryCache = object : LinkedHashMap<String, MemoryCacheEntry>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryCacheEntry>?): Boolean {
            return size > 50
        }
    }

    private data class MemoryCacheEntry(
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        val maxAgeMs: Long = 5 * 60 * 1000 // 5 minutes default
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > maxAgeMs
    }

    // ================================================================
    // PROVIDER ACCESS
    // ================================================================

    fun getProviders(): List<MainProvider> {
        val registered = MainProvider.getProviders()
        val prefs = RepositoryProvider.getPreferencesManager().appSettings.value
        val order = if (prefs.providerOrder.isEmpty()) registered.map { it.name } else prefs.providerOrder
        val disabled = prefs.disabledProviders

        val map = registered.associateBy { it.name }
        val ordered = order.mapNotNull { map[it] }
        // Append any providers missing from the saved order
        val remaining = registered.filter { it.name !in order }
        val combined = ordered + remaining
        return combined.filter { it.name !in disabled }
    }

    fun getProvider(name: String): MainProvider? = MainProvider.getProvider(name)

    fun getDefaultProvider(): MainProvider? = getProviders().firstOrNull()

    /**
     * Check if provider supports reviews
     */
    fun providerHasReviews(providerName: String): Boolean {
        return getProvider(providerName)?.hasReviews ?: false
    }

    // ================================================================
    // BROWSE / CATALOG
    // ================================================================

    suspend fun loadMainPage(
        provider: MainProvider,
        page: Int,
        orderBy: String? = null,
        tag: String? = null
    ): Result<MainPageResult> = withContext(Dispatchers.IO) {
        try {
            val result = provider.loadMainPage(page, orderBy, tag)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================
    // SEARCH
    // ================================================================

    suspend fun search(
        provider: MainProvider,
        query: String
    ): Result<List<Novel>> = withContext(Dispatchers.IO) {
        try {
            val results = provider.search(query)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchInProvider(
        provider: MainProvider,
        query: String
    ): Result<List<Novel>> = withContext(Dispatchers.IO) {
        try {
            val novels = provider.search(query)
            Result.success(novels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAll(query: String): Map<String, Result<List<Novel>>> =
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, Result<List<Novel>>>()

            getProviders().forEach { provider ->
                results[provider.name] = try {
                    val novels = provider.search(query)
                    Result.success(novels)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            results
        }

    /**
     * Search all providers concurrently, emitting results as each provider completes.
     * Each emission is a Pair of (providerName, Result<List<Novel>>).
     */
    fun searchAllStreaming(query: String): Flow<Pair<String, Result<List<Novel>>>> = flow {
        val providers = getProviders()

        // Launch all searches concurrently and emit results as they complete
        coroutineScope {
            val deferredResults = providers.map { provider ->
                async {
                    val result = try {
                        Result.success(provider.search(query))
                    } catch (e: Exception) {
                        Result.failure<List<Novel>>(e)
                    }
                    provider.name to result
                }
            }

            // Emit results as they complete
            val remaining = deferredResults.toMutableList()
            while (remaining.isNotEmpty()) {
                val completed = remaining.firstOrNull { it.isCompleted }
                if (completed != null) {
                    remaining.remove(completed)
                    emit(completed.await())
                } else {
                    // Wait briefly for any to complete
                    delay(50)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    // ================================================================
    // NOVEL DETAILS
    // ================================================================

    /**
     * Load novel details with offline-first strategy
     */
    suspend fun loadNovelDetails(
        provider: MainProvider,
        url: String,
        forceRefresh: Boolean = false
    ): Result<NovelDetails> = withContext(Dispatchers.IO) {

        // If forcing refresh, go to network first
        if (forceRefresh) {
            return@withContext loadNovelDetailsNetworkFirst(provider, url)
        }

        // Default: Offline-first strategy
        loadNovelDetailsOfflineFirst(provider, url)
    }

    private suspend fun loadNovelDetailsOfflineFirst(
        provider: MainProvider,
        url: String
    ): Result<NovelDetails> {
        // 1. Try cache first
        val cached = getOfflineNovelDetails(url)
        if (cached != null) {
            // Return cached, but also try to refresh in background if stale
            return Result.success(cached)
        }

        // 2. Try network
        return loadNovelDetailsFromNetwork(provider, url)
    }

    private suspend fun loadNovelDetailsNetworkFirst(
        provider: MainProvider,
        url: String
    ): Result<NovelDetails> {
        // 1. Try network first
        val networkResult = loadNovelDetailsFromNetwork(provider, url)

        if (networkResult.isSuccess) {
            return networkResult
        }

        // 2. Fall back to cache
        val cached = getOfflineNovelDetails(url)
        if (cached != null) {
            return Result.success(cached)
        }

        // 3. Return the network error
        return networkResult
    }

    private suspend fun loadNovelDetailsFromNetwork(
        provider: MainProvider,
        url: String
    ): Result<NovelDetails> {
        return try {
            val details = provider.load(url)
            if (details != null) {
                cacheNovelDetails(details)
                Result.success(details)
            } else {
                Result.failure(NetworkException("Failed to load novel details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cached novel details (offline)
     */
    suspend fun getOfflineNovelDetails(url: String): NovelDetails? =
        withContext(Dispatchers.IO) {
            offlineDao.getNovelDetails(url)?.toNovelDetails()
        }

    /**
     * Cache novel details for offline access
     */
    suspend fun cacheNovelDetails(details: NovelDetails) = withContext(Dispatchers.IO) {
        val entity = NovelDetailsEntity.fromNovelDetails(details)
        offlineDao.saveNovelDetails(entity)
    }

    // ================================================================
    // REVIEWS
    // ================================================================

    /**
     * Load reviews for a novel
     */
    suspend fun loadReviews(
        providerName: String,
        novelUrl: String,
        page: Int,
        showSpoilers: Boolean = false
    ): Result<List<UserReview>> = withContext(Dispatchers.IO) {
        try {
            val provider = getProvider(providerName)
                ?: return@withContext Result.failure(Exception("Provider not found: $providerName"))

            if (!provider.hasReviews) {
                return@withContext Result.success(emptyList())
            }

            val reviews = provider.loadReviews(novelUrl, page, showSpoilers)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load reviews using provider instance
     */
    suspend fun loadReviews(
        provider: MainProvider,
        novelUrl: String,
        page: Int,
        showSpoilers: Boolean = false
    ): Result<List<UserReview>> = withContext(Dispatchers.IO) {
        try {
            if (!provider.hasReviews) {
                return@withContext Result.success(emptyList())
            }

            val reviews = provider.loadReviews(novelUrl, page, showSpoilers)
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================
    // CHAPTER CONTENT
    // ================================================================

    /**
     * Load chapter content with offline-first strategy
     * Prioritizes cached/downloaded content for faster loading
     */
    suspend fun loadChapterContent(
        provider: MainProvider,
        chapterUrl: String,
        mode: LoadingMode = LoadingMode.OFFLINE_FIRST
    ): Result<String> = withContext(Dispatchers.IO) {
        when (mode) {
            LoadingMode.OFFLINE_FIRST -> loadChapterOfflineFirst(provider, chapterUrl)
            LoadingMode.NETWORK_FIRST -> loadChapterNetworkFirst(provider, chapterUrl)
            LoadingMode.OFFLINE_ONLY -> loadChapterOfflineOnly(chapterUrl)
            LoadingMode.NETWORK_ONLY -> loadChapterFromNetwork(provider, chapterUrl)
        }
    }

    private suspend fun loadChapterOfflineFirst(
        provider: MainProvider,
        chapterUrl: String
    ): Result<String> {
        // 1. Check memory cache
        memoryCache[chapterUrl]?.let { entry ->
            if (!entry.isExpired) {
                return Result.success(entry.content)
            }
        }

        // 2. Check database (downloaded chapters)
        val offlineChapter = offlineDao.getChapter(chapterUrl)
        if (offlineChapter != null) {
            // Update memory cache
            memoryCache[chapterUrl] = MemoryCacheEntry(offlineChapter.content)
            return Result.success(offlineChapter.content)
        }

        // 3. Load from network
        return loadChapterFromNetwork(provider, chapterUrl)
    }

    private suspend fun loadChapterNetworkFirst(
        provider: MainProvider,
        chapterUrl: String
    ): Result<String> {
        // 1. Try network
        val networkResult = loadChapterFromNetwork(provider, chapterUrl)
        if (networkResult.isSuccess) {
            return networkResult
        }

        // 2. Fall back to cache
        val offlineChapter = offlineDao.getChapter(chapterUrl)
        if (offlineChapter != null) {
            return Result.success(offlineChapter.content)
        }

        // 3. Return network error
        return networkResult
    }

    private suspend fun loadChapterOfflineOnly(chapterUrl: String): Result<String> {
        // Check memory cache
        memoryCache[chapterUrl]?.let { entry ->
            return Result.success(entry.content)
        }

        // Check database
        val offlineChapter = offlineDao.getChapter(chapterUrl)
        return if (offlineChapter != null) {
            Result.success(offlineChapter.content)
        } else {
            Result.failure(Exception("Chapter not available offline"))
        }
    }

    private suspend fun loadChapterFromNetwork(
        provider: MainProvider,
        chapterUrl: String
    ): Result<String> {
        return try {
            val content = provider.loadChapterContent(chapterUrl)
            if (content != null) {
                // Cache in memory
                memoryCache[chapterUrl] = MemoryCacheEntry(content)
                Result.success(content)
            } else {
                Result.failure(NetworkException("Empty chapter content"))
            }
        } catch (e: Exception) {
            // On network failure, try offline as last resort
            val offlineChapter = offlineDao.getChapter(chapterUrl)
            if (offlineChapter != null) {
                Result.success(offlineChapter.content)
            } else {
                Result.failure(e)
            }
        }
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    /**
     * Check if chapter is available offline
     */
    suspend fun isChapterOffline(chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            offlineDao.getChapter(chapterUrl) != null
        }

    /**
     * Check if chapter is available (any source)
     */
    suspend fun isChapterAvailable(chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            memoryCache.containsKey(chapterUrl) || offlineDao.getChapter(chapterUrl) != null
        }

    /**
     * Clear memory cache (call on low memory)
     */
    fun clearMemoryCache() {
        memoryCache.clear()
    }

    /**
     * Preload chapters into memory cache
     */
    suspend fun preloadChapters(
        provider: MainProvider,
        chapterUrls: List<String>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        chapterUrls.forEachIndexed { index, url ->
            // Skip if already cached
            if (memoryCache.containsKey(url) || offlineDao.getChapter(url) != null) {
                onProgress(index + 1, chapterUrls.size)
                return@forEachIndexed
            }

            try {
                val content = provider.loadChapterContent(url)
                if (content != null) {
                    memoryCache[url] = MemoryCacheEntry(content, maxAgeMs = 30 * 60 * 1000) // 30 min
                }
            } catch (e: Exception) {
                // Silent fail for preloading
            }
            onProgress(index + 1, chapterUrls.size)
        }
    }
}