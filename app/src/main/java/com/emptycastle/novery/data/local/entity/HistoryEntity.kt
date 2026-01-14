package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for reading history.
 * Equivalent to HistoryEntry in React.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val novelUrl: String,
    val novelName: String,
    val posterUrl: String? = null,
    val chapterName: String,
    val chapterUrl: String,
    val apiName: String,
    val timestamp: Long = System.currentTimeMillis()
)