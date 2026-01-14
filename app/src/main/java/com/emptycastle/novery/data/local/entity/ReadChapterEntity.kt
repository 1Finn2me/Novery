package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks all chapters that have been read.
 * Used for "Unread" download feature.
 */
@Entity(
    tableName = "read_chapters",
    indices = [Index(value = ["novelUrl"])]
)
data class ReadChapterEntity(
    @PrimaryKey
    val chapterUrl: String,
    val novelUrl: String,
    val readAt: Long = System.currentTimeMillis()
)