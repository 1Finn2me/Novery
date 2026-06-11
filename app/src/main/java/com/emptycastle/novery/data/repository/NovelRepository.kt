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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
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
 * Repository for novel-related operations.
 * Coordinates between network providers and local cache.
 */
class NovelRepository(
    private val offlineDao: OfflineDao,
    private val contentStrategy: ContentLoadingStrategy
) {

    // ================================================================
    // PROVIDER ACCESS
    // ================================================================

    fun getProviders(): List<MainProvider> {
        val registered = MainProvider.getProviders()
        val prefs = RepositoryProvider.getPreferencesManager().appSettings.value
        val order = prefs.providerOrder.ifEmpty { registered.map { it.name } }
        val disabled = prefs.disabledProviders

        val map = registered.associateBy { it.name }
        val ordered = order.mapNotNull { map[it] }
        val remaining = registered.filter { it.name !in order }
        return (ordered + remaining).filter { it.name !in disabled }
    }

    fun getProvider(name: String): MainProvider? = MainProvider.getProvider(name)

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
        orderBy: String?,
        tag: String?,
        extraFilters: Map<String, String> = emptyMap()
    ): Result<MainPageResult> {
        return try {
            val result = provider.loadMainPage(
                page = page,
                orderBy = orderBy,
                tag = tag,
                extraFilters = extraFilters
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================
    // SEARCH
    // ================================================================

    /**
     * Parallel search using channelFlow.
     * Emits results as each provider completes.
     */
    fun searchAllStreaming(query: String): Flow<Pair<String, Result<List<Novel>>>> = channelFlow {
        getProviders().forEach { provider ->
            launch(Dispatchers.IO) {
                try {
                    val results = provider.search(query)
                    send(provider.name to Result.success(results))
                } catch (e: Exception) {
                    send(provider.name to Result.failure(e))
                }
            }
        }
    }

    suspend fun search(provider: MainProvider, query: String): Result<List<Novel>> = withContext(Dispatchers.IO) {
        try {
            Result.success(provider.search(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchInProvider(provider: MainProvider, query: String): Result<List<Novel>> = search(provider, query)

    // ================================================================
    // NOVEL DETAILS
    // ================================================================

    suspend fun loadNovelDetails(
        provider: MainProvider,
        url: String,
        forceRefresh: Boolean = false
    ): Result<NovelDetails> = withContext(Dispatchers.IO) {
        if (forceRefresh) {
            val res = loadNovelDetailsFromNetwork(provider, url)
            if (res.isSuccess) return@withContext res
        }

        val cached = getOfflineNovelDetails(url)
        if (cached != null) return@withContext Result.success(cached)

        loadNovelDetailsFromNetwork(provider, url)
    }

    private suspend fun loadNovelDetailsFromNetwork(provider: MainProvider, url: String): Result<NovelDetails> {
        return try {
            provider.load(url)?.let { details ->
                cacheNovelDetails(details)
                Result.success(details)
            } ?: Result.failure(NetworkException("Failed to load novel details"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOfflineNovelDetails(url: String): NovelDetails? = withContext(Dispatchers.IO) {
        offlineDao.getNovelDetails(url)?.toNovelDetails()
    }

    suspend fun cacheNovelDetails(details: NovelDetails) = withContext(Dispatchers.IO) {
        offlineDao.saveNovelDetails(NovelDetailsEntity.fromNovelDetails(details))
    }

    // ================================================================
    // REVIEWS
    // ================================================================

    suspend fun loadReviews(
        provider: MainProvider,
        novelUrl: String,
        page: Int,
        showSpoilers: Boolean = false
    ): Result<List<UserReview>> = withContext(Dispatchers.IO) {
        try {
            if (!provider.hasReviews) return@withContext Result.success(emptyList())
            Result.success(provider.loadReviews(novelUrl, page, showSpoilers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================
    // CHAPTER CONTENT
    // ================================================================

    suspend fun loadChapterContent(
        provider: MainProvider,
        chapterUrl: String,
        novelUrl: String = "",
        chapterTitle: String = "",
        mode: LoadingMode = LoadingMode.OFFLINE_FIRST,
        sourceLanguage: String = "auto",
        targetLanguage: String = "es",
        translationEnabled: Boolean = false,
        useOnline: Boolean = false
    ): Result<String> {
        val content = contentStrategy.loadChapter(
            url = chapterUrl,
            novelUrl = novelUrl,
            chapterTitle = chapterTitle,
            provider = provider,
            mode = mode,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            translationEnabled = translationEnabled,
            useOnline = useOnline
        )
        return if (content != null) Result.success(content) else Result.failure(Exception("Failed to load chapter"))
    }

    /**
     * Parallel background preloading and translation.
     */
    suspend fun preloadChapters(
        urls: List<String>,
        novelUrl: String,
        chapterTitles: Map<String, String>,
        provider: MainProvider,
        sourceLanguage: String = "auto",
        targetLanguage: String,
        translationEnabled: Boolean,
        useOnline: Boolean = false,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        var completedCount = 0
        // Process in chunks of 5 for safety
        urls.chunked(5).forEach { chunk ->
            chunk.map { url ->
                async {
                    val res = contentStrategy.loadChapter(
                        url = url,
                        novelUrl = novelUrl,
                        chapterTitle = chapterTitles[url] ?: "",
                        provider = provider,
                        mode = LoadingMode.OFFLINE_FIRST,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        translationEnabled = translationEnabled,
                        useOnline = useOnline
                    )
                    synchronized(this@NovelRepository) {
                        completedCount++
                        onProgress(completedCount, urls.size)
                    }
                    res
                }
            }.awaitAll()
        }
    }

    /**
     * Standard method to get a translated chapter from cache/data layer.
     */
    suspend fun getChapter(
        url: String,
        novelUrl: String,
        chapterTitle: String,
        provider: MainProvider,
        mode: LoadingMode,
        sourceLanguage: String,
        targetLanguage: String,
        translationEnabled: Boolean,
        useOnline: Boolean = false
    ): String? {
        return contentStrategy.loadChapter(url, novelUrl, chapterTitle, provider, mode, sourceLanguage, targetLanguage, translationEnabled, useOnline)
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    suspend fun isChapterOffline(chapterUrl: String): Boolean = withContext(Dispatchers.IO) {
        offlineDao.getChapter(chapterUrl) != null
    }

    fun clearMemoryCache() {
        contentStrategy.clearCache()
    }
}
