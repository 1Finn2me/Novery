// com/emptycastle/novery/data/local/entity/LibraryEntity.kt
// REPLACE your existing file with this enhanced version

package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus

/**
 * Database entity for saved library novels with chapter tracking.
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
    val lastScrollOffset: Int = 0,

    // ============ NEW: Chapter tracking for badges ============

    /** Total chapter count from last refresh */
    val totalChapterCount: Int = 0,

    /** Chapter count when user last viewed/acknowledged */
    val acknowledgedChapterCount: Int = 0,

    /** Last time we checked for new chapters */
    val lastCheckedAt: Long = 0,

    /** Last time chapters were updated on the source */
    val lastUpdatedAt: Long = 0,

    /** Index of the last read chapter (for unread calculation) */
    val lastReadChapterIndex: Int = -1,

    /** Cached count of unread chapters (chapters after lastReadChapterIndex) */
    val unreadChapterCount: Int = 0
) {
    /**
     * Number of new chapters since last acknowledgment
     */
    val newChapterCount: Int
        get() = (totalChapterCount - acknowledgedChapterCount).coerceAtLeast(0)

    /**
     * Whether there are new chapters to show badge
     */
    val hasNewChapters: Boolean
        get() = newChapterCount > 0

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
        fun fromNovel(
            novel: Novel,
            status: ReadingStatus = ReadingStatus.READING,
            chapterCount: Int = 0
        ): LibraryEntity {
            return LibraryEntity(
                url = novel.url,
                name = novel.name,
                posterUrl = novel.posterUrl,
                apiName = novel.apiName,
                latestChapter = novel.latestChapter,
                readingStatus = status.name,
                totalChapterCount = chapterCount,
                acknowledgedChapterCount = chapterCount // No new chapters on add
            )
        }
    }
}