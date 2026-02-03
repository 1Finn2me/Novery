package com.emptycastle.novery.recommendation.model

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory

/**
 * Feature vector representation of a novel for similarity calculations.
 * Extracts and normalizes features from NovelDetails.
 */
data class NovelVector(
    val url: String,
    val name: String,
    val providerName: String,

    /** Normalized tags */
    val tags: Set<TagCategory>,

    /** Original raw tags (for display) */
    val rawTags: List<String>,

    /** Normalized author name (lowercase, trimmed) */
    val authorNormalized: String?,

    /** Rating on 0-1000 scale */
    val rating: Int?,

    /** Chapter count (indicator of length/commitment) */
    val chapterCount: Int,

    /** Completion status */
    val isCompleted: Boolean,

    /** Keywords extracted from synopsis */
    val synopsisKeywords: Set<String>,

    /** Poster URL for display */
    val posterUrl: String?
) {
    companion object {
        // Common words to filter from synopsis
        private val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
            "be", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "shall", "can", "need",
            "he", "she", "it", "they", "we", "you", "i", "his", "her", "its",
            "their", "our", "your", "my", "this", "that", "these", "those",
            "who", "whom", "which", "what", "where", "when", "why", "how",
            "all", "each", "every", "both", "few", "more", "most", "other",
            "some", "such", "no", "nor", "not", "only", "own", "same", "so",
            "than", "too", "very", "just", "also", "now", "here", "there",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "upon",
            "about", "out", "up", "down", "off", "over", "any", "because"
        )

        /**
         * Extract meaningful keywords from synopsis
         */
        fun extractKeywords(synopsis: String?, maxKeywords: Int = 20): Set<String> {
            if (synopsis.isNullOrBlank()) return emptySet()

            return synopsis
                .lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { word ->
                    word.length >= 3 &&
                            word !in stopWords &&
                            !word.all { it.isDigit() }
                }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(maxKeywords)
                .map { it.key }
                .toSet()
        }

        /**
         * Normalize author name for comparison
         */
        fun normalizeAuthor(author: String?): String? {
            if (author.isNullOrBlank()) return null
            return author
                .lowercase()
                .trim()
                .replace(Regex("\\s+"), " ")
        }
    }
}