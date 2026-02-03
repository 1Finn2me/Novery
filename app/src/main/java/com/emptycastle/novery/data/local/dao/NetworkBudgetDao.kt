// com/emptycastle/novery/data/local/dao/NetworkBudgetDao.kt

package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.DiscoveryChainEntity
import com.emptycastle.novery.data.local.entity.NetworkBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkBudgetDao {

    // ============ BUDGET TRACKING ============

    @Query("SELECT * FROM network_budget WHERE providerName = :providerName AND date = :date")
    suspend fun getBudget(providerName: String, date: Long): NetworkBudgetEntity?

    @Query("SELECT * FROM network_budget WHERE date = :date")
    suspend fun getAllBudgetsForDay(date: Long): List<NetworkBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: NetworkBudgetEntity)

    @Query("""
        UPDATE network_budget SET 
            requestCount = requestCount + 1,
            lastRequestAt = :timestamp
        WHERE providerName = :providerName AND date = :date
    """)
    suspend fun incrementRequestCount(providerName: String, date: Long, timestamp: Long)

    @Query("""
        UPDATE network_budget SET 
            failedCount = failedCount + 1,
            consecutiveFailures = consecutiveFailures + 1,
            inCooldown = 1,
            cooldownUntil = :cooldownUntil
        WHERE providerName = :providerName AND date = :date
    """)
    suspend fun recordFailure(providerName: String, date: Long, cooldownUntil: Long)

    @Query("""
        UPDATE network_budget SET 
            consecutiveFailures = 0,
            inCooldown = 0,
            cooldownUntil = 0
        WHERE providerName = :providerName AND date = :date
    """)
    suspend fun clearCooldown(providerName: String, date: Long)

    @Query("SELECT SUM(requestCount) FROM network_budget WHERE date = :date")
    suspend fun getTotalRequestsForDay(date: Long): Int?

    @Query("DELETE FROM network_budget WHERE date < :threshold")
    suspend fun deleteOldBudgets(threshold: Long)

    // ============ DISCOVERY CHAINS ============

    @Query("SELECT * FROM discovery_chains WHERE novelUrl = :novelUrl AND sessionId = :sessionId")
    suspend fun getChainEntry(novelUrl: String, sessionId: String): DiscoveryChainEntity?

    @Query("SELECT MAX(depth) FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun getMaxDepthForSession(sessionId: String): Int?

    @Query("SELECT COUNT(*) FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun getChainSizeForSession(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainEntry(entry: DiscoveryChainEntity)

    @Query("DELETE FROM discovery_chains WHERE sessionId = :sessionId")
    suspend fun clearChainSession(sessionId: String)

    @Query("DELETE FROM discovery_chains WHERE discoveredAt < :threshold")
    suspend fun deleteOldChains(threshold: Long)

    // ============ STATISTICS ============

    @Query("SELECT * FROM network_budget ORDER BY date DESC, providerName ASC")
    fun observeAllBudgets(): Flow<List<NetworkBudgetEntity>>

    @Query("""
        SELECT providerName, SUM(requestCount) as total 
        FROM network_budget 
        WHERE date >= :startDate 
        GROUP BY providerName
    """)
    suspend fun getRequestsPerProviderSince(startDate: Long): List<ProviderRequestCount>
}

data class ProviderRequestCount(
    val providerName: String,
    val total: Int
)