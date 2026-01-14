package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.NovelDetails

/**
 * Cached novel details for offline access.
 */
@Entity(tableName = "novel_details")
data class NovelDetailsEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val author: String? = null,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val tags: String? = null,           // Stored as comma-separated
    val rating: Int? = null,
    val peopleVoted: Int? = null,
    val status: String? = null,
    val chaptersJson: String,           // Stored as JSON
    val savedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromNovelDetails(details: NovelDetails): NovelDetailsEntity {
            // Simple JSON serialization for chapters
            val chaptersJson = details.chapters.joinToString("|||") { "${it.name}::${it.url}" }

            return NovelDetailsEntity(
                url = details.url,
                name = details.name,
                author = details.author,
                posterUrl = details.posterUrl,
                synopsis = details.synopsis,
                tags = details.tags?.joinToString(","),
                rating = details.rating,
                peopleVoted = details.peopleVoted,
                status = details.status,
                chaptersJson = chaptersJson
            )
        }
    }

    fun toNovelDetails(): NovelDetails {
        val chapters = if (chaptersJson.isBlank()) {
            emptyList()
        } else {
            chaptersJson.split("|||").mapNotNull { entry ->
                val parts = entry.split("::")
                if (parts.size >= 2) {
                    Chapter(name = parts[0], url = parts[1])
                } else null
            }
        }

        return NovelDetails(
            url = url,
            name = name,
            chapters = chapters,
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags?.split(",")?.filter { it.isNotBlank() },
            rating = rating,
            peopleVoted = peopleVoted,
            status = status
        )
    }
}