package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
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
    val tags: List<String>? = null,
    val rating: Int? = null,
    val peopleVoted: Int? = null,
    val status: String? = null,
    val views: Int? = null,
    val relatedNovelsJson: String? = null,

    // Persist chapter list so cached novel details include their chapters
    val chapters: List<ChapterEntity>? = null,

    // ADD THESE NEW FIELDS:
    val apiName: String = "",           // Provider name
    val chapterCount: Int = 0,          // Number of chapters
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toNovelDetails(): NovelDetails {
        val relatedNovels = relatedNovelsJson?.let { json ->
            try {
                // Parse JSON to List<Novel> - implement based on your JSON library
                parseRelatedNovels(json)
            } catch (e: Exception) {
                null
            }
        }

        val chapterList = chapters?.map { it.toChapter() } ?: emptyList()

        return NovelDetails(
            url = url,
            name = name,
            chapters = chapterList,
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status,
            views = views,
            relatedNovels = relatedNovels
        )
    }

    companion object {
        fun fromNovelDetails(
            details: NovelDetails,
            apiName: String = ""  // ADD THIS PARAMETER
        ): NovelDetailsEntity {
            return NovelDetailsEntity(
                url = details.url,
                name = details.name,
                author = details.author,
                posterUrl = details.posterUrl,
                synopsis = details.synopsis,
                tags = details.tags,
                rating = details.rating,
                peopleVoted = details.peopleVoted,
                status = details.status,
                views = details.views,
                relatedNovelsJson = details.relatedNovels?.let { serializeRelatedNovels(it) },
                chapters = details.chapters.map { ChapterEntity.fromChapter(it) },
                apiName = apiName,
                chapterCount = details.chapters.size
            )
        }

        private fun parseRelatedNovels(json: String): List<Novel>? {
            // Implement JSON parsing
            return null
        }

        private fun serializeRelatedNovels(novels: List<Novel>): String {
            // Implement JSON serialization
            return "[]"
        }
    }
}

/**
 * Embedded entity for chapters
 */
data class ChapterEntity(
    val name: String,
    val url: String,
    val dateOfRelease: String? = null
) {
    fun toChapter(): Chapter {
        return Chapter(
            name = name,
            url = url,
            dateOfRelease = dateOfRelease
        )
    }

    companion object {
        fun fromChapter(chapter: Chapter): ChapterEntity {
            return ChapterEntity(
                name = chapter.name,
                url = chapter.url,
                dateOfRelease = chapter.dateOfRelease
            )
        }
    }
}

/**
 * Embedded entity for related novels
 */
data class RelatedNovelEntity(
    val name: String,
    val url: String,
    val posterUrl: String? = null,
    val apiName: String
) {
    fun toNovel(): Novel {
        return Novel(
            name = name,
            url = url,
            posterUrl = posterUrl,
            apiName = apiName
        )
    }

    companion object {
        fun fromNovel(novel: Novel): RelatedNovelEntity {
            return RelatedNovelEntity(
                name = novel.name,
                url = novel.url,
                posterUrl = novel.posterUrl,
                apiName = novel.apiName
            )
        }
    }
}