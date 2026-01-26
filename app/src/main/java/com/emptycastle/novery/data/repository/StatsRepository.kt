// com/emptycastle/novery/data/repository/StatsRepository.kt

package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.AggregatedStats
import com.emptycastle.novery.data.local.dao.NovelReadingTime
import com.emptycastle.novery.data.local.dao.RecentNovelInfo
import com.emptycastle.novery.data.local.dao.StatsDao
import com.emptycastle.novery.data.local.entity.ReadingStatsEntity
import com.emptycastle.novery.data.local.entity.ReadingStreakEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for reading statistics and streaks
 */
class StatsRepository(
    private val statsDao: StatsDao
) {

    // ================================================================
    // READING SESSION TRACKING
    // ================================================================

    /**
     * Record reading time for a novel.
     * Call this periodically while reading (e.g., every minute)
     */
    suspend fun recordReadingTime(
        novelUrl: String,
        novelName: String,
        durationSeconds: Long,
        wordsRead: Long = 0
    ) = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()

        val existing = statsDao.getStatsForDay(novelUrl, today)

        if (existing != null) {
            val updated = existing.copy(
                readingTimeSeconds = existing.readingTimeSeconds + durationSeconds,
                wordsRead = existing.wordsRead + wordsRead,
                updatedAt = System.currentTimeMillis()
            )
            statsDao.insertStats(updated)
        } else {
            val newStats = ReadingStatsEntity(
                novelUrl = novelUrl,
                novelName = novelName,
                date = today,
                readingTimeSeconds = durationSeconds,
                wordsRead = wordsRead,
                sessionsCount = 1
            )
            statsDao.insertStats(newStats)
        }

        // Update streak
        updateStreak(today)
    }

    /**
     * Record chapter completion
     */
    suspend fun recordChapterRead(
        novelUrl: String,
        novelName: String,
        wordsInChapter: Long = 0
    ) = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()

        val existing = statsDao.getStatsForDay(novelUrl, today)

        if (existing != null) {
            val updated = existing.copy(
                chaptersRead = existing.chaptersRead + 1,
                wordsRead = existing.wordsRead + wordsInChapter,
                updatedAt = System.currentTimeMillis()
            )
            statsDao.insertStats(updated)
        } else {
            val newStats = ReadingStatsEntity(
                novelUrl = novelUrl,
                novelName = novelName,
                date = today,
                chaptersRead = 1,
                wordsRead = wordsInChapter,
                sessionsCount = 1
            )
            statsDao.insertStats(newStats)
        }
    }

    // ================================================================
    // STREAK MANAGEMENT
    // ================================================================

    private suspend fun updateStreak(currentDay: Long) {
        val streak = statsDao.getStreak() ?: ReadingStreakEntity()

        val newStreak = when {
            // Same day, no change
            streak.lastReadDate == currentDay -> streak

            // Consecutive day, increment streak
            streak.lastReadDate == currentDay - 1 -> streak.copy(
                currentStreak = streak.currentStreak + 1,
                longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
                lastReadDate = currentDay,
                totalDaysRead = streak.totalDaysRead + 1,
                updatedAt = System.currentTimeMillis()
            )

            // Gap in reading, reset streak
            else -> streak.copy(
                currentStreak = 1,
                lastReadDate = currentDay,
                totalDaysRead = streak.totalDaysRead + 1,
                updatedAt = System.currentTimeMillis()
            )
        }

        statsDao.updateStreak(newStreak)
    }

    fun observeStreak(): Flow<ReadingStreakEntity?> = statsDao.observeStreak()

    suspend fun getStreak(): ReadingStreakEntity? = withContext(Dispatchers.IO) {
        statsDao.getStreak()
    }

    // ================================================================
    // STATISTICS QUERIES
    // ================================================================

    /**
     * Get stats for today
     */
    suspend fun getTodayStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        statsDao.getAggregatedStats(today, today)
    }

    /**
     * Get stats for this week
     */
    suspend fun getWeekStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val weekStart = today - 6  // Last 7 days
        statsDao.getAggregatedStats(weekStart, today)
    }

    /**
     * Get stats for this month
     */
    suspend fun getMonthStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val monthStart = today - 29  // Last 30 days
        statsDao.getAggregatedStats(monthStart, today)
    }

    /**
     * Get daily breakdown for a date range
     */
    suspend fun getDailyStats(startDate: Long, endDate: Long): List<ReadingStatsEntity> =
        withContext(Dispatchers.IO) {
            statsDao.getStatsInRange(startDate, endDate)
        }

    /**
     * Get stats for a specific novel
     */
    fun observeNovelStats(novelUrl: String): Flow<List<ReadingStatsEntity>> {
        return statsDao.observeNovelStats(novelUrl)
    }

    /**
     * Get most read novels
     */
    suspend fun getMostReadNovels(limit: Int = 10): List<NovelReadingTime> =
        withContext(Dispatchers.IO) {
            statsDao.getMostReadNovels(limit)
        }

    /**
     * Get recently read novels
     */
    suspend fun getRecentlyReadNovels(limit: Int = 10): List<RecentNovelInfo> =
        withContext(Dispatchers.IO) {
            statsDao.getRecentlyReadNovels(limit)
        }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun getCurrentEpochDay(): Long {
        return LocalDate.now().toEpochDay()
    }

    /**
     * Format reading time for display
     */
    fun formatReadingTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}