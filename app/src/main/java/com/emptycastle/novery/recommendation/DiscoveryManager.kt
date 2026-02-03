package com.emptycastle.novery.recommendation

import android.util.Log
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.dao.RecommendationDao
import com.emptycastle.novery.data.local.entity.DiscoveredNovelEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.repository.NovelRepository
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DiscoveryManager"

/**
 * Manages discovery of novels for the recommendation pool.
 * Respects network budgets and chain limits.
 */
class DiscoveryManager(
    private val recommendationDao: RecommendationDao,
    private val novelRepository: NovelRepository,
    private val networkBudgetManager: NetworkBudgetManager,
    private val offlineDao: OfflineDao
) {

    data class DiscoveryConfig(
        val minPoolSize: Int = 50,
        val targetPerProvider: Int = 100,
        val seedingPages: Int = 2,
        val enrichTopCount: Int = 10
    )

    private val config = DiscoveryConfig()

    // ================================================================
    // POOL STATUS
    // ================================================================

    suspend fun needsSeeding(): Boolean = withContext(Dispatchers.IO) {
        val count = recommendationDao.getDiscoveredNovelCount()
        count < config.minPoolSize
    }

    suspend fun getPoolSize(): Int = withContext(Dispatchers.IO) {
        recommendationDao.getDiscoveredNovelCount()
    }

    suspend fun getPoolSizeByProvider(): Map<String, Int> = withContext(Dispatchers.IO) {
        val providers = recommendationDao.getDiscoveredProviders()
        providers.associateWith { provider ->
            recommendationDao.getDiscoveredNovelCountByProvider(provider)
        }
    }

    // ================================================================
    // SEEDING WITH BUDGET CONTROL
    // ================================================================

    suspend fun seedDiscoveryPool(
        onProgress: (provider: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): SeedingResult = withContext(Dispatchers.IO) {
        val providers = novelRepository.getProviders()
        var totalDiscovered = 0
        val errors = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val sessionId = networkBudgetManager.createSessionId()

        try {
            providers.forEachIndexed { index, provider ->
                onProgress(provider.name, index + 1, providers.size)

                // Check if we have budget for this provider
                val remaining = networkBudgetManager.getRemainingBudget(
                    provider.name,
                    NetworkBudgetManager.RequestType.DISCOVERY
                )

                if (remaining < 5) {
                    Log.d(TAG, "Skipping ${provider.name} - insufficient budget ($remaining)")
                    skipped.add(provider.name)
                    return@forEachIndexed
                }

                try {
                    val discovered = seedFromProvider(provider, sessionId, remaining)
                    totalDiscovered += discovered
                    Log.d(TAG, "Discovered $discovered novels from ${provider.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error seeding from ${provider.name}", e)
                    errors.add("${provider.name}: ${e.message}")

                    // Record failure for rate limiting
                    if (e.message?.contains("rate limit", ignoreCase = true) == true ||
                        e.message?.contains("429", ignoreCase = true) == true) {
                        networkBudgetManager.recordFailure(provider.name)
                    }
                }
            }
        } finally {
            networkBudgetManager.clearSession(sessionId)
        }

        SeedingResult(
            totalDiscovered = totalDiscovered,
            providersSeeded = providers.size - errors.size - skipped.size,
            providersSkipped = skipped,
            errors = errors
        )
    }

    private suspend fun seedFromProvider(
        provider: MainProvider,
        sessionId: String,
        budgetRemaining: Int
    ): Int {
        var totalDiscovered = 0
        var requestsUsed = 0

        val maxPages = (budgetRemaining / 2).coerceAtMost(config.seedingPages)

        for (page in 1..maxPages) {
            if (!networkBudgetManager.waitForSlot(provider.name)) {
                Log.d(TAG, "No budget slot available for ${provider.name}")
                break
            }

            try {
                val result = provider.loadMainPage(page, orderBy = null, tag = null)
                networkBudgetManager.recordRequest(provider.name)
                requestsUsed++

                // Store basic info for all novels
                val discoveredNovels = result.novels.map { novel ->
                    DiscoveredNovelEntity.fromNovel(novel, source = "seed")
                }
                recommendationDao.insertDiscoveredNovels(discoveredNovels)
                totalDiscovered += discoveredNovels.size

                result.novels.forEach { novel ->
                    networkBudgetManager.recordDiscovery(novel.url, sessionId, depth = 0)
                }

                // IMPROVED: Prioritize enriching novels that need tags
                if (budgetRemaining - requestsUsed > config.enrichTopCount) {
                    val toEnrich = selectNovelsForEnrichment(
                        result.novels,
                        config.enrichTopCount,
                        provider
                    )
                    enrichNovels(provider, toEnrich, sessionId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching page $page from ${provider.name}", e)
                networkBudgetManager.recordFailure(provider.name)
                break
            }
        }

        return totalDiscovered
    }

    /**
     * Select which novels to enrich, prioritizing those without cached details/tags
     */
    private suspend fun selectNovelsForEnrichment(
        novels: List<Novel>,
        count: Int,
        provider: MainProvider
    ): List<Novel> {
        val needsEnrichment = mutableListOf<Novel>()
        val hasDetails = mutableListOf<Novel>()

        for (novel in novels) {
            val existingDetails = offlineDao.getNovelDetails(novel.url)
            val discoveredNovel = recommendationDao.getDiscoveredNovel(novel.url)

            // Prioritize novels without tags
            val hasTags = discoveredNovel?.tagsString?.isNotBlank() == true ||
                    existingDetails?.tags?.isNotEmpty() == true

            if (!hasTags) {
                needsEnrichment.add(novel)
            } else {
                hasDetails.add(novel)
            }
        }

        // Take novels that need enrichment first, then fill with others
        return (needsEnrichment + hasDetails).take(count)
    }

    /**
     * Enrich novels with full details (including tags)
     */
    private suspend fun enrichNovels(
        provider: MainProvider,
        novels: List<Novel>,
        sessionId: String
    ) {
        for (novel in novels) {
            if (!networkBudgetManager.canDiscoverMore(sessionId, currentDepth = 0)) {
                return
            }
            if (!networkBudgetManager.waitForSlot(provider.name)) {
                return
            }

            try {
                val details = provider.load(novel.url)
                networkBudgetManager.recordRequest(provider.name)

                if (details != null) {
                    // Save full details for later use
                    offlineDao.saveNovelDetails(
                        NovelDetailsEntity.fromNovelDetails(details, provider.name)
                    )

                    // Also update discovered novel with tags
                    val enriched = DiscoveredNovelEntity.fromNovelDetails(
                        details = details,
                        apiName = provider.name,
                        source = "seed_enriched"
                    )
                    recommendationDao.insertDiscoveredNovel(enriched)

                    // Cache related novels
                    details.relatedNovels?.let { related ->
                        cacheRelatedNovelsWithLimit(related, provider.name, sessionId, depth = 1)
                    }

                    Log.d(TAG, "Enriched ${novel.name} with ${details.tags?.size ?: 0} tags")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching ${novel.name}", e)
                // Don't record failure for enrichment errors - they're optional
            }
        }
    }

    // ================================================================
    // INCREMENTAL DISCOVERY WITH LIMITS
    // ================================================================

    /**
     * Cache novels from browse results (no network, just storing)
     */
    suspend fun cacheFromBrowse(
        novels: List<Novel>,
        providerName: String
    ) = withContext(Dispatchers.IO) {
        val entities = novels.map { novel ->
            DiscoveredNovelEntity.fromNovel(novel, source = "browse").copy(
                apiName = providerName
            )
        }
        recommendationDao.insertDiscoveredNovels(entities)
        Log.d(TAG, "Cached ${entities.size} novels from browse ($providerName)")
    }

    /**
     * Cache related novels with chain depth limit
     */
    private suspend fun cacheRelatedNovelsWithLimit(
        relatedNovels: List<Novel>,
        sourceProvider: String,
        sessionId: String,
        depth: Int
    ) {
        if (depth > NetworkBudgetManager.ChainLimits.MAX_DEPTH) {
            Log.d(TAG, "Chain depth limit reached, not caching related novels")
            return
        }

        // Limit number of related novels
        val limited = relatedNovels.take(NetworkBudgetManager.ChainLimits.MAX_RELATED_PER_NOVEL)

        val entities = limited.mapNotNull { novel ->
            // Skip if already discovered in this session
            if (networkBudgetManager.isAlreadyDiscovered(novel.url, sessionId)) {
                return@mapNotNull null
            }

            networkBudgetManager.recordDiscovery(novel.url, sessionId, depth)

            DiscoveredNovelEntity.fromNovel(novel, source = "related").copy(
                apiName = novel.apiName.ifBlank { sourceProvider }
            )
        }

        recommendationDao.insertDiscoveredNovels(entities)
        Log.d(TAG, "Cached ${entities.size} related novels at depth $depth")
    }

    /**
     * Cache related novels from details view (with new session)
     */
    suspend fun cacheRelatedNovels(
        relatedNovels: List<Novel>,
        sourceProvider: String
    ) = withContext(Dispatchers.IO) {
        val sessionId = networkBudgetManager.createSessionId()
        try {
            cacheRelatedNovelsWithLimit(relatedNovels, sourceProvider, sessionId, depth = 1)
        } finally {
            networkBudgetManager.clearSession(sessionId)
        }
    }

    /**
     * Cache a novel from details view with full info
     */
    suspend fun cacheFromDetails(
        details: NovelDetails,
        providerName: String
    ) = withContext(Dispatchers.IO) {
        val entity = DiscoveredNovelEntity.fromNovelDetails(
            details = details,
            apiName = providerName,
            source = "details"
        )
        recommendationDao.insertDiscoveredNovel(entity)

        // Cache related novels with chain limit
        details.relatedNovels?.let { related ->
            cacheRelatedNovels(related, providerName)
        }
    }

    /**
     * Cache novels from search results (no network, just storing)
     */
    suspend fun cacheFromSearch(
        novels: List<Novel>,
        providerName: String
    ) = withContext(Dispatchers.IO) {
        val entities = novels.map { novel ->
            DiscoveredNovelEntity.fromNovel(novel, source = "search").copy(
                apiName = providerName
            )
        }
        recommendationDao.insertDiscoveredNovels(entities)
    }

    // ================================================================
    // BACKGROUND REFRESH WITH BUDGET
    // ================================================================

    suspend fun refreshPool(
        onProgress: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        var totalNew = 0
        val providers = novelRepository.getProviders()

        providers.forEach { provider ->
            // Check budget first
            val remaining = networkBudgetManager.getRemainingBudget(
                provider.name,
                NetworkBudgetManager.RequestType.DISCOVERY
            )

            if (remaining < 2) {
                onProgress("${provider.name}: no budget")
                return@forEach
            }

            if (!networkBudgetManager.waitForSlot(provider.name)) {
                onProgress("${provider.name}: rate limited")
                return@forEach
            }

            onProgress("Checking ${provider.name}...")

            try {
                val result = provider.loadMainPage(1, orderBy = null, tag = null)
                networkBudgetManager.recordRequest(provider.name)

                result.novels.forEach { novel ->
                    val existing = recommendationDao.getDiscoveredNovel(novel.url)
                    if (existing == null) {
                        recommendationDao.insertDiscoveredNovel(
                            DiscoveredNovelEntity.fromNovel(novel, source = "refresh")
                        )
                        totalNew++
                    } else {
                        recommendationDao.insertDiscoveredNovel(
                            existing.copy(lastVerifiedAt = System.currentTimeMillis())
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing from ${provider.name}", e)
                networkBudgetManager.recordFailure(provider.name)
            }
        }

        totalNew
    }

    // ================================================================
    // MAINTENANCE
    // ================================================================

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        recommendationDao.deleteOldDiscoveredNovels(threshold)
        networkBudgetManager.cleanup()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        recommendationDao.clearAllDiscoveredNovels()
    }
}

data class SeedingResult(
    val totalDiscovered: Int,
    val providersSeeded: Int,
    val providersSkipped: List<String> = emptyList(),
    val errors: List<String>
)