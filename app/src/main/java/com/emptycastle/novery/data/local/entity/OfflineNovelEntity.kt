package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for novels that have offline content.
 */
@Entity(tableName = "offline_novels")
data class OfflineNovelEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val coverUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)