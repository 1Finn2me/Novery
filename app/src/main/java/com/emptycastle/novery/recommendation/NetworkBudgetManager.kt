// com/emptycastle/novery/recommendation/NetworkBudgetManager.kt

package com.emptycastle.novery.recommendation

import android.util.Log
import com.emptycastle.novery.data.local.dao.NetworkBudgetDao
import com.emptycastle.novery.data.local.entity.DiscoveryChainEntity
import com.emptycastle.novery.data.local.entity.NetworkBudgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private const val TAG = "NetworkBudgetManager"

/**
 * Manages network request budgets to prevent rate limiting and bans.
 * Enforces daily limits per provider and implements exponential backoff.
 */
class NetworkBudgetManager(
    private val budgetDao: NetworkBudgetDao
) {

    /**
     * Provider-specific budget configuration
     */
    data class ProviderBudget(
        /** Maximum requests per day for discovery/recommendation */
        val dailyDiscoveryLimit: Int,

        /** Maximum requests per day for user-initiated actions (browsing, reading) */
        val dailyUserLimit: Int,

        /** Minimum delay between requests in ms */
        val minRequestDelayMs: Long,

        /** Base cooldown after rate limit hit (will be multiplied by failure count) */
        val baseCooldownMs: Long,

        /** Maximum cooldown time */
        val maxCooldownMs: Long
    )

    /**
     * Default budgets per provider - conservative to avoid bans
     */
    private val providerBudgets = mapOf(
        "Royal Road" to ProviderBudget(
            dailyDiscoveryLimit = 50,      // Very strict - almost banned before
            dailyUserLimit = 200,           // More for user actions
            minRequestDelayMs = 2000,       // 2 seconds between requests
            baseCooldownMs = 60_000,        // 1 minute base cooldown
            maxCooldownMs = 3600_000        // 1 hour max cooldown
        ),
        "NovelFire" to ProviderBudget(
            dailyDiscoveryLimit = 150,
            dailyUserLimit = 500,
            minRequestDelayMs = 500,
            baseCooldownMs = 30_000,
            maxCooldownMs = 1800_000
        ),
        "NovelBin" to ProviderBudget(
            dailyDiscoveryLimit = 100,
            dailyUserLimit = 400,
            minRequestDelayMs = 750,
            baseCooldownMs = 30_000,
            maxCooldownMs = 1800_000
        ),
        "Webnovel" to ProviderBudget(
            dailyDiscoveryLimit = 75,
            dailyUserLimit = 300,
            minRequestDelayMs = 1000,
            baseCooldownMs = 60_000,
            maxCooldownMs = 3600_000
        ),
        "LibRead" to ProviderBudget(
            dailyDiscoveryLimit = 100,
            dailyUserLimit = 400,
            minRequestDelayMs = 500,
            baseCooldownMs = 30_000,
            maxCooldownMs = 1800_000
        ),
        "NovelsOnline" to ProviderBudget(
            dailyDiscoveryLimit = 100,
            dailyUserLimit = 400,
            minRequestDelayMs = 500,
            baseCooldownMs = 30_000,
            maxCooldownMs = 1800_000
        )
    )

    /** Default budget for unknown providers */
    private val defaultBudget = ProviderBudget(
        dailyDiscoveryLimit = 75,
        dailyUserLimit = 300,
        minRequestDelayMs = 1000,
        baseCooldownMs = 60_000,
        maxCooldownMs = 3600_000
    )

    /** Discovery chain limits */
    object ChainLimits {
        const val MAX_DEPTH = 2              // Only go 2 levels deep in related novels
        const val MAX_NOVELS_PER_SESSION = 50 // Max novels to discover in one session
        const val MAX_RELATED_PER_NOVEL = 5   // Only cache first 5 related novels
    }

    /** Request types */
    enum class RequestType {
        DISCOVERY,  // Background discovery for recommendations
        USER        // User-initiated (browse, read, search)
    }

    // Mutex for thread-safe budget updates
    private val budgetMutex = Mutex()

    // Track last request time per provider (in-memory for speed)
    private val lastRequestTimes = mutableMapOf<String, Long>()
    private val requestTimeMutex = Mutex()

    // ================================================================
    // BUDGET CHECKING
    // ================================================================

    /**
     * Check if we can make a request to a provider
     * @return Pair of (canRequest, waitTimeMs if can't)
     */
    suspend fun canMakeRequest(
        providerName: String,
        requestType: RequestType = RequestType.USER
    ): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = getCurrentEpochDay()
            val budget = getOrCreateBudget(providerName, today)
            val config = getProviderBudget(providerName)

            // Check cooldown
            if (budget.inCooldown && System.currentTimeMillis() < budget.cooldownUntil) {
                val waitTime = budget.cooldownUntil - System.currentTimeMillis()
                Log.d(TAG, "$providerName in cooldown, wait ${waitTime}ms")
                return@withContext false to waitTime
            }

            // Clear cooldown if expired
            if (budget.inCooldown && System.currentTimeMillis() >= budget.cooldownUntil) {
                budgetDao.clearCooldown(providerName, today)
            }

            // Check daily limit
            val limit = when (requestType) {
                RequestType.DISCOVERY -> config.dailyDiscoveryLimit
                RequestType.USER -> config.dailyUserLimit
            }

            if (budget.requestCount >= limit) {
                Log.d(TAG, "$providerName daily limit reached ($limit)")
                // Calculate time until midnight reset
                val msUntilMidnight = getMsUntilMidnight()
                return@withContext false to msUntilMidnight
            }

            // Check minimum delay between requests
            val timeSinceLastRequest = System.currentTimeMillis() - budget.lastRequestAt
            if (timeSinceLastRequest < config.minRequestDelayMs) {
                val waitTime = config.minRequestDelayMs - timeSinceLastRequest
                return@withContext false to waitTime
            }

            true to 0L
        }
    }

    /**
     * Get remaining budget for a provider
     */
    suspend fun getRemainingBudget(
        providerName: String,
        requestType: RequestType = RequestType.USER
    ): Int = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val budget = budgetDao.getBudget(providerName, today)
        val config = getProviderBudget(providerName)

        val limit = when (requestType) {
            RequestType.DISCOVERY -> config.dailyDiscoveryLimit
            RequestType.USER -> config.dailyUserLimit
        }

        (limit - (budget?.requestCount ?: 0)).coerceAtLeast(0)
    }

    /**
     * Get all remaining budgets (for UI display)
     */
    suspend fun getAllRemainingBudgets(): Map<String, BudgetStatus> = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val budgets = budgetDao.getAllBudgetsForDay(today)

        providerBudgets.keys.associateWith { providerName ->
            val budget = budgets.find { it.providerName == providerName }
            val config = getProviderBudget(providerName)

            BudgetStatus(
                used = budget?.requestCount ?: 0,
                discoveryLimit = config.dailyDiscoveryLimit,
                userLimit = config.dailyUserLimit,
                inCooldown = budget?.inCooldown ?: false,
                cooldownUntil = budget?.cooldownUntil ?: 0
            )
        }
    }

    // ================================================================
    // REQUEST TRACKING
    // ================================================================

    /**
     * Record a successful request
     */
    suspend fun recordRequest(providerName: String) = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = getCurrentEpochDay()
            val now = System.currentTimeMillis()

            val existing = budgetDao.getBudget(providerName, today)
            if (existing != null) {
                budgetDao.incrementRequestCount(providerName, today, now)
            } else {
                budgetDao.insertBudget(
                    NetworkBudgetEntity(
                        providerName = providerName,
                        date = today,
                        requestCount = 1,
                        lastRequestAt = now
                    )
                )
            }

            Log.v(TAG, "Recorded request to $providerName")
        }

        // Update in-memory tracker
        requestTimeMutex.withLock {
            lastRequestTimes[providerName] = System.currentTimeMillis()
        }
    }

    /**
     * Record a rate limit or failure
     */
    suspend fun recordFailure(providerName: String) = withContext(Dispatchers.IO) {
        budgetMutex.withLock {
            val today = getCurrentEpochDay()
            val config = getProviderBudget(providerName)

            val existing = budgetDao.getBudget(providerName, today)
            val failures = (existing?.consecutiveFailures ?: 0) + 1

            // Exponential backoff: base * 2^failures, capped at max
            val cooldownDuration = (config.baseCooldownMs * (1 shl failures.coerceAtMost(5)))
                .coerceAtMost(config.maxCooldownMs)

            val cooldownUntil = System.currentTimeMillis() + cooldownDuration

            if (existing != null) {
                budgetDao.recordFailure(providerName, today, cooldownUntil)
            } else {
                budgetDao.insertBudget(
                    NetworkBudgetEntity(
                        providerName = providerName,
                        date = today,
                        requestCount = 1,
                        failedCount = 1,
                        consecutiveFailures = 1,
                        inCooldown = true,
                        cooldownUntil = cooldownUntil,
                        lastRequestAt = System.currentTimeMillis()
                    )
                )
            }

            Log.w(TAG, "$providerName rate limited, cooldown for ${cooldownDuration}ms (failures: $failures)")
        }
    }

    /**
     * Wait until we can make a request (respecting delays)
     */
    suspend fun waitForSlot(providerName: String): Boolean = withContext(Dispatchers.IO) {
        val config = getProviderBudget(providerName)

        repeat(10) { // Max 10 attempts
            val (canRequest, waitTime) = canMakeRequest(providerName, RequestType.DISCOVERY)

            if (canRequest) return@withContext true

            if (waitTime > 60_000) {
                // Don't wait more than a minute
                Log.d(TAG, "$providerName wait time too long ($waitTime ms), skipping")
                return@withContext false
            }

            if (waitTime > 0) {
                kotlinx.coroutines.delay(waitTime)
            }
        }

        false
    }

    // ================================================================
    // CHAIN TRACKING
    // ================================================================

    /**
     * Start a new discovery session
     */
    fun createSessionId(): String = UUID.randomUUID().toString()

    /**
     * Check if we can discover more in this chain
     */
    suspend fun canDiscoverMore(
        sessionId: String,
        currentDepth: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (currentDepth >= ChainLimits.MAX_DEPTH) {
            Log.d(TAG, "Chain depth limit reached ($currentDepth)")
            return@withContext false
        }

        val chainSize = budgetDao.getChainSizeForSession(sessionId)
        if (chainSize >= ChainLimits.MAX_NOVELS_PER_SESSION) {
            Log.d(TAG, "Chain size limit reached ($chainSize)")
            return@withContext false
        }

        true
    }

    /**
     * Check if a novel has already been discovered in this session
     */
    suspend fun isAlreadyDiscovered(
        novelUrl: String,
        sessionId: String
    ): Boolean = withContext(Dispatchers.IO) {
        budgetDao.getChainEntry(novelUrl, sessionId) != null
    }

    /**
     * Record a discovered novel in the chain
     */
    suspend fun recordDiscovery(
        novelUrl: String,
        sessionId: String,
        depth: Int
    ) = withContext(Dispatchers.IO) {
        budgetDao.insertChainEntry(
            DiscoveryChainEntity(
                novelUrl = novelUrl,
                sessionId = sessionId,
                depth = depth
            )
        )
    }

    /**
     * Clear a discovery session
     */
    suspend fun clearSession(sessionId: String) = withContext(Dispatchers.IO) {
        budgetDao.clearChainSession(sessionId)
    }

    // ================================================================
    // MAINTENANCE
    // ================================================================

    /**
     * Clean up old data
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        val weekAgo = getCurrentEpochDay() - 7
        budgetDao.deleteOldBudgets(weekAgo)

        val hourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        budgetDao.deleteOldChains(hourAgo)
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun getProviderBudget(providerName: String): ProviderBudget {
        return providerBudgets[providerName] ?: defaultBudget
    }

    private suspend fun getOrCreateBudget(providerName: String, date: Long): NetworkBudgetEntity {
        return budgetDao.getBudget(providerName, date) ?: NetworkBudgetEntity(
            providerName = providerName,
            date = date
        ).also { budgetDao.insertBudget(it) }
    }

    private fun getCurrentEpochDay(): Long = LocalDate.now().toEpochDay()

    private fun getMsUntilMidnight(): Long {
        val now = System.currentTimeMillis()
        val midnight = (getCurrentEpochDay() + 1) * 24 * 60 * 60 * 1000
        return midnight - now
    }
}

data class BudgetStatus(
    val used: Int,
    val discoveryLimit: Int,
    val userLimit: Int,
    val inCooldown: Boolean,
    val cooldownUntil: Long
) {
    val discoveryRemaining: Int get() = (discoveryLimit - used).coerceAtLeast(0)
    val userRemaining: Int get() = (userLimit - used).coerceAtLeast(0)
    val discoveryPercentUsed: Float get() = (used.toFloat() / discoveryLimit).coerceIn(0f, 1f)
}