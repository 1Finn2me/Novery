package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for offline-saved chapters.
 */
@Entity(
    tableName = "offline_chapters",
    indices = [
        Index(value = ["novelUrl"]),
        Index(value = ["downloadedAt"])
    ]
)
data class OfflineChapterEntity(
    @PrimaryKey
    val url: String,
    val novelUrl: String,
    val title: String,
    val content: String,
    val downloadedAt: Long = System.currentTimeMillis()
)