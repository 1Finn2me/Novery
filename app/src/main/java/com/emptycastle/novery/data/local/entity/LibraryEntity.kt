package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus

/**
 * Database entity for saved library novels.
 */
@Entity(tableName = "library")
data class LibraryEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val posterUrl: String? = null,
    val apiName: String,
    val latestChapter: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val readingStatus: String = ReadingStatus.READING.name,

    // Reading position tracking
    val lastChapterUrl: String? = null,
    val lastChapterName: String? = null,
    val lastReadAt: Long? = null,
    val lastScrollIndex: Int = 0,
    val lastScrollOffset: Int = 0
) {
    fun toNovel(): Novel = Novel(
        name = name,
        url = url,
        posterUrl = posterUrl,
        apiName = apiName,
        latestChapter = latestChapter
    )

    fun getStatus(): ReadingStatus = try {
        ReadingStatus.valueOf(readingStatus)
    } catch (e: Exception) {
        ReadingStatus.READING
    }

    companion object {
        fun fromNovel(novel: Novel, status: ReadingStatus = ReadingStatus.READING): LibraryEntity {
            return LibraryEntity(
                url = novel.url,
                name = novel.name,
                posterUrl = novel.posterUrl,
                apiName = novel.apiName,
                latestChapter = novel.latestChapter,
                readingStatus = status.name
            )
        }
    }
}