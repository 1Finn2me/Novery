// com/emptycastle/novery/data/local/dao/StatsDao.kt

package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.ReadingStatsEntity
import com.emptycastle.novery.data.local.entity.ReadingStreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    // ============ READING STATS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: ReadingStatsEntity)

    @Query("SELECT * FROM reading_stats WHERE novelUrl = :novelUrl AND date = :date")
    suspend fun getStatsForDay(novelUrl: String, date: Long): ReadingStatsEntity?

    @Query("SELECT * FROM reading_stats WHERE date = :date")
    suspend fun getAllStatsForDay(date: Long): List<ReadingStatsEntity>

    @Query("SELECT * FROM reading_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getStatsInRange(startDate: Long, endDate: Long): List<ReadingStatsEntity>

    @Query("SELECT * FROM reading_stats WHERE novelUrl = :novelUrl ORDER BY date DESC")
    fun observeNovelStats(novelUrl: String): Flow<List<ReadingStatsEntity>>

    @Query("""
        SELECT SUM(readingTimeSeconds) as totalTime, 
               SUM(chaptersRead) as totalChapters,
               SUM(wordsRead) as totalWords,
               COUNT(DISTINCT date) as daysRead
        FROM reading_stats 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAggregatedStats(startDate: Long, endDate: Long): AggregatedStats?

    @Query("SELECT DISTINCT novelUrl, novelName FROM reading_stats ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyReadNovels(limit: Int = 10): List<RecentNovelInfo>

    @Query("""
        SELECT novelUrl, novelName, SUM(readingTimeSeconds) as totalTime
        FROM reading_stats
        GROUP BY novelUrl
        ORDER BY totalTime DESC
        LIMIT :limit
    """)
    suspend fun getMostReadNovels(limit: Int = 10): List<NovelReadingTime>

    // ============ STREAK ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStreak(streak: ReadingStreakEntity)

    @Query("SELECT * FROM reading_streak WHERE id = 1")
    suspend fun getStreak(): ReadingStreakEntity?

    @Query("SELECT * FROM reading_streak WHERE id = 1")
    fun observeStreak(): Flow<ReadingStreakEntity?>
}

data class AggregatedStats(
    val totalTime: Long?,
    val totalChapters: Int?,
    val totalWords: Long?,
    val daysRead: Int?
)

data class RecentNovelInfo(
    val novelUrl: String,
    val novelName: String
)

data class NovelReadingTime(
    val novelUrl: String,
    val novelName: String,
    val totalTime: Long
)