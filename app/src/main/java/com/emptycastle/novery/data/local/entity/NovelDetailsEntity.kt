package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.emptycastle.novery.data.local.Converters
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails

/**
 * Cached novel details for offline access.
 */
@Entity(tableName = "novel_details")
@TypeConverters(Converters::class)
data class NovelDetailsEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val chapters: List<ChapterEntity>,
    val author: String? = null,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val tags: List<String>? = null,
    val rating: Int? = null,
    val peopleVoted: Int? = null,
    val status: String? = null,
    val views: Int? = null,
    val relatedNovels: List<RelatedNovelEntity>? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toNovelDetails(): NovelDetails {
        return NovelDetails(
            url = url,
            name = name,
            chapters = chapters.map { it.toChapter() },
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status,
            views = views,
            relatedNovels = relatedNovels?.map { it.toNovel() }
        )
    }

    companion object {
        fun fromNovelDetails(details: NovelDetails): NovelDetailsEntity {
            return NovelDetailsEntity(
                url = details.url,
                name = details.name,
                chapters = details.chapters.map { ChapterEntity.fromChapter(it) },
                author = details.author,
                posterUrl = details.posterUrl,
                synopsis = details.synopsis,
                tags = details.tags,
                rating = details.rating,
                peopleVoted = details.peopleVoted,
                status = details.status,
                views = details.views,
                relatedNovels = details.relatedNovels?.map { RelatedNovelEntity.fromNovel(it) },
                lastUpdated = System.currentTimeMillis()
            )
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