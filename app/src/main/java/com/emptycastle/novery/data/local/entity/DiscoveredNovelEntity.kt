package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails

/**
 * Lightweight entity for discovered novels used in recommendations.
 * Stores just enough info to calculate similarity and display recommendations.
 */
@Entity(
    tableName = "discovered_novels",
    indices = [
        Index(value = ["apiName"]),
        Index(value = ["discoveredAt"])
    ]
)
data class DiscoveredNovelEntity(
    @PrimaryKey
    val url: String,

    val name: String,
    val apiName: String,
    val posterUrl: String? = null,
    val rating: Int? = null,

    /** Comma-separated tags for storage efficiency */
    val tagsString: String? = null,

    val author: String? = null,
    val status: String? = null,
    val synopsis: String? = null,

    /** How this novel was discovered */
    val source: String = "browse", // browse, related, search, popular

    /** When this was discovered/cached */
    val discoveredAt: Long = System.currentTimeMillis(),

    /** Last time this was verified to still exist */
    val lastVerifiedAt: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = tagsString?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    fun toNovel(): Novel = Novel(
        name = name,
        url = url,
        posterUrl = posterUrl,
        rating = rating,
        apiName = apiName
    )

    companion object {
        fun fromNovel(novel: Novel, source: String = "browse"): DiscoveredNovelEntity {
            return DiscoveredNovelEntity(
                url = novel.url,
                name = novel.name,
                apiName = novel.apiName,
                posterUrl = novel.posterUrl,
                rating = novel.rating,
                source = source
            )
        }

        fun fromNovelDetails(details: NovelDetails, apiName: String, source: String = "details"): DiscoveredNovelEntity {
            return DiscoveredNovelEntity(
                url = details.url,
                name = details.name,
                apiName = apiName,
                posterUrl = details.posterUrl,
                rating = details.rating,
                tagsString = details.tags?.joinToString(","),
                author = details.author,
                status = details.status,
                synopsis = details.synopsis?.take(500), // Limit synopsis length
                source = source
            )
        }
    }
}