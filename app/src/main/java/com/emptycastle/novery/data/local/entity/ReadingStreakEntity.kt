// com/emptycastle/novery/data/local/entity/ReadingStreakEntity.kt

package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks reading streaks (single row table)
 */
@Entity(tableName = "reading_streak")
data class ReadingStreakEntity(
    @PrimaryKey
    val id: Int = 1,  // Single row

    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastReadDate: Long = 0,  // Epoch day

    val totalDaysRead: Int = 0,
    val totalReadingTimeSeconds: Long = 0,

    val updatedAt: Long = System.currentTimeMillis()
)