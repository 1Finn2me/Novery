// com/emptycastle/novery/data/local/entity/BookmarkEntity.kt

package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User bookmarks with optional notes
 */
@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["novelUrl"]),
        Index(value = ["chapterUrl"]),
        Index(value = ["category"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val novelUrl: String,
    val novelName: String,
    val chapterUrl: String,
    val chapterName: String,

    /** Position within chapter */
    val segmentId: String? = null,
    val segmentIndex: Int = 0,
    val textSnippet: String? = null,

    /** User additions */
    val note: String? = null,
    val category: String = "default",
    val color: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)