package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks network request usage per provider per day.
 * Used to enforce daily budgets and prevent rate limiting.
 */
@Entity(
    tableName = "network_budget",
    indices = [Index(value = ["providerName", "date"], unique = true)]
)
data class NetworkBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val providerName: String,

    /** Date as epoch day (days since epoch) */
    val date: Long,

    /** Number of requests made today */
    val requestCount: Int = 0,

    /** Number of requests that were rate-limited/failed */
    val failedCount: Int = 0,

    /** Whether we're currently in cooldown */
    val inCooldown: Boolean = false,

    /** When cooldown ends (if in cooldown) */
    val cooldownUntil: Long = 0,

    /** Last request timestamp */
    val lastRequestAt: Long = 0,

    /** Consecutive failures (for exponential backoff) */
    val consecutiveFailures: Int = 0
)

/**
 * Tracks discovery chain depth to prevent infinite loops
 */
@Entity(
    tableName = "discovery_chains",
    indices = [Index(value = ["sessionId"])]
)
data class DiscoveryChainEntity(
    @PrimaryKey
    val novelUrl: String,

    /** Session ID to track chains within a discovery session */
    val sessionId: String,

    /** How deep in the chain this novel is (0 = direct browse, 1 = related, etc.) */
    val depth: Int,

    /** When this was discovered */
    val discoveredAt: Long = System.currentTimeMillis()
)