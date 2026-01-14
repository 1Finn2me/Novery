package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.remote.NetworkException
import com.emptycastle.novery.domain.model.*
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for novel-related operations.
 * Coordinates between network providers and local cache.
 */
class NovelRepository(
    private val offlineDao: OfflineDao
) {

    // ================================================================
    // PROVIDER ACCESS
    // ================================================================

    /**
     * Get all registered providers
     */
    fun getProviders(): List<MainProvider> = MainProvider.getProviders()

    /**
     * Get provider by name
     */
    fun getProvider(name: String): MainProvider? = MainProvider.getProvider(name)

    /**
     * Get default provider (first registered)
     */
    fun getDefaultProvider(): MainProvider? = MainProvider.getProviders().firstOrNull()

    // ================================================================
    // BROWSE / CATALOG
    // ================================================================

    /**
     * Load main catalog page from a provider
     */
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

    /**
     * Search for novels using a specific provider
     */
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

    /**
     * Search across all providers
     */
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

    // ================================================================
    // NOVEL DETAILS
    // ================================================================

    /**
     * Load novel details from provider, with offline fallback
     */
    suspend fun loadNovelDetails(
        provider: MainProvider,
        url: String,
        forceRefresh: Boolean = false
    ): Result<NovelDetails> = withContext(Dispatchers.IO) {
        try {
            // Try to load from network
            val details = provider.load(url)

            if (details != null) {
                // Cache the details for offline access
                cacheNovelDetails(details)
                Result.success(details)
            } else {
                // Try offline fallback
                val cached = getOfflineNovelDetails(url)
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(NetworkException("Failed to load novel details"))
                }
            }
        } catch (e: Exception) {
            // Network failed, try offline
            val cached = getOfflineNovelDetails(url)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
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
    // CHAPTER CONTENT
    // ================================================================

    /**
     * Load chapter content from provider, with offline fallback
     */
    suspend fun loadChapterContent(
        provider: MainProvider,
        chapterUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check offline first (if available, it's faster)
            val offlineChapter = offlineDao.getChapter(chapterUrl)
            if (offlineChapter != null) {
                return@withContext Result.success(offlineChapter.content)
            }

            // Load from network
            val content = provider.loadChapterContent(chapterUrl)

            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(NetworkException("Empty chapter content"))
            }
        } catch (e: Exception) {
            // Try offline fallback
            val offlineChapter = offlineDao.getChapter(chapterUrl)
            if (offlineChapter != null) {
                Result.success(offlineChapter.content)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Check if chapter is available offline
     */
    suspend fun isChapterOffline(chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            offlineDao.getChapter(chapterUrl) != null
        }
}