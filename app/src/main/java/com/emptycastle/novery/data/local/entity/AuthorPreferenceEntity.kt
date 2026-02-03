// com/emptycastle/novery/data/local/entity/AuthorPreferenceEntity.kt
package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks user's reading history and preferences for specific authors.
 * Used to recommend other works by authors the user has enjoyed.
 */
@Entity(
    tableName = "author_preferences",
    indices = [
        Index(value = ["affinityScore"]),
        Index(value = ["displayName"])
    ]
)
data class AuthorPreferenceEntity(
    /** Normalized author name (lowercase, trimmed) for matching */
    @PrimaryKey
    val authorNormalized: String,

    /** Original display name (preserves casing) */
    val displayName: String,

    /**
     * Affinity score: 0-1000 scale
     * - 0-300: Dropped/disliked
     * - 300-500: Read but neutral
     * - 500-700: Enjoyed
     * - 700-900: Really liked
     * - 900-1000: Favorite author
     */
    val affinityScore: Int = 500,

    /** Number of novels by this author the user has read */
    val novelsRead: Int = 0,

    /** Number of novels by this author the user has completed */
    val novelsCompleted: Int = 0,

    /** Number of novels by this author the user has dropped */
    val novelsDropped: Int = 0,

    /** Total chapters read across all novels by this author */
    val totalChaptersRead: Int = 0,

    /** Total reading time (seconds) spent on this author's works */
    val totalReadingTimeSeconds: Long = 0,

    /** List of novel URLs by this author in user's library (comma-separated) */
    val novelUrlsInLibrary: String = "",

    /** When this preference was first created */
    val createdAt: Long = System.currentTimeMillis(),

    /** When this preference was last updated */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Completion rate for this author's novels */
    val completionRate: Float
        get() = if (novelsRead > 0) {
            novelsCompleted.toFloat() / novelsRead
        } else 0f

    /** Drop rate for this author's novels */
    val dropRate: Float
        get() = if (novelsRead > 0) {
            novelsDropped.toFloat() / novelsRead
        } else 0f

    /** Average chapters read per novel */
    val avgChaptersPerNovel: Float
        get() = if (novelsRead > 0) {
            totalChaptersRead.toFloat() / novelsRead
        } else 0f

    /** Whether user seems to like this author (score >= 600 and at least 1 novel) */
    val isLiked: Boolean
        get() = affinityScore >= 600 && novelsRead >= 1

    /** Whether user seems to love this author (score >= 800 and completed at least 1) */
    val isFavorite: Boolean
        get() = affinityScore >= 800 && novelsCompleted >= 1

    /** Get list of novel URLs */
    fun getNovelUrls(): List<String> {
        return if (novelUrlsInLibrary.isBlank()) {
            emptyList()
        } else {
            novelUrlsInLibrary.split(",").filter { it.isNotBlank() }
        }
    }

    /** Add a novel URL to the list */
    fun withNovelUrl(url: String): AuthorPreferenceEntity {
        val currentUrls = getNovelUrls().toMutableSet()
        currentUrls.add(url)
        return copy(novelUrlsInLibrary = currentUrls.joinToString(","))
    }

    /** Remove a novel URL from the list */
    fun withoutNovelUrl(url: String): AuthorPreferenceEntity {
        val currentUrls = getNovelUrls().toMutableSet()
        currentUrls.remove(url)
        return copy(novelUrlsInLibrary = currentUrls.joinToString(","))
    }
}