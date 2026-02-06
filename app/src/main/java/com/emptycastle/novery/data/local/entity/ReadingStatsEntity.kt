package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks reading statistics per day per novel
 */
@Entity(
    tableName = "reading_stats",
    indices = [
        Index(value = ["date"]),
        Index(value = ["novelUrl"])
    ]
)
data class ReadingStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val novelUrl: String,
    val novelName: String,

    /** Date stored as epoch day (days since epoch) */
    val date: Long,

    /** Reading metrics */
    val readingTimeSeconds: Long = 0,
    val chaptersRead: Int = 0,
    val wordsRead: Long = 0,

    /** Session tracking */
    val sessionsCount: Int = 0,
    val longestSessionSeconds: Long = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)