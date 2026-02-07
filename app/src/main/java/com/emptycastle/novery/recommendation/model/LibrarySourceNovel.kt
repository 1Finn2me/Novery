package com.emptycastle.novery.recommendation.model

import com.emptycastle.novery.domain.model.Novel

/**
 * Represents a library novel that can be used as a source for recommendations.
 */
data class LibrarySourceNovel(
    val novel: Novel,
    val lastReadAt: Long,
    val chaptersRead: Int,
    val totalChapters: Int,
    val readingProgress: Float, // 0.0 - 1.0
    val hasRecommendations: Boolean = true // Whether we have enough data to recommend
) {
    val displayProgress: String
        get() = when {
            readingProgress >= 1f -> "Completed"
            readingProgress > 0f -> "${(readingProgress * 100).toInt()}% read"
            else -> "Not started"
        }

    companion object {
        fun fromLibraryItem(
            item: com.emptycastle.novery.data.repository.LibraryItem,
            chaptersRead: Int = 0
        ): LibrarySourceNovel {
            val total = item.totalChapterCount.coerceAtLeast(1)
            val progress = if (total > 0) chaptersRead.toFloat() / total else 0f

            return LibrarySourceNovel(
                novel = item.novel,
                lastReadAt = item.lastReadPosition?.timestamp ?: item.addedAt,
                chaptersRead = chaptersRead,
                totalChapters = item.totalChapterCount,
                readingProgress = progress.coerceIn(0f, 1f),
                hasRecommendations = true
            )
        }
    }
}