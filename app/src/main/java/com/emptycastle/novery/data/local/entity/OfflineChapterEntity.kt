package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for offline-saved chapters.
 */
@Entity(
    tableName = "offline_chapters",
    indices = [Index(value = ["novelUrl"])]
)
data class OfflineChapterEntity(
    @PrimaryKey
    val url: String,
    val novelUrl: String,
    val title: String,
    val content: String,
    val savedAt: Long = System.currentTimeMillis()
)