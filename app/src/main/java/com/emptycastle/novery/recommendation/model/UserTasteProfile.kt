package com.emptycastle.novery.recommendation.model

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory

/**
 * Represents the user's overall taste profile.
 * Used to score how well a novel matches user preferences.
 */
data class UserTasteProfile(
    /** Tags the user strongly prefers */
    val preferredTags: List<TagAffinity>,

    /** Tags the user tends to avoid/drop */
    val avoidedTags: List<TagAffinity>,

    /** Overall diversity - does user stick to one genre or explore? */
    val diversityScore: Float,

    /** Average rating of novels user finishes */
    val preferredRatingMin: Int?,

    /** Total novels in profile calculation */
    val sampleSize: Int,

    /** When this profile was last updated */
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * How mature is this profile? More data = more reliable recommendations
     */
    val maturity: ProfileMaturity
        get() = when {
            sampleSize < 3 -> ProfileMaturity.NEW
            sampleSize < 10 -> ProfileMaturity.DEVELOPING
            sampleSize < 25 -> ProfileMaturity.ESTABLISHED
            else -> ProfileMaturity.MATURE
        }

    /**
     * Score how well a set of tags matches this user's taste
     * Returns 0.0 - 1.0
     */
    fun scoreTagMatch(novelTags: Set<TagCategory>): Float {
        if (novelTags.isEmpty() || preferredTags.isEmpty()) return 0.5f

        var matchScore = 0f
        var totalWeight = 0f

        // Positive scoring for preferred tags
        for (affinity in preferredTags) {
            val weight = affinity.score * affinity.confidence
            totalWeight += weight

            if (affinity.tag in novelTags) {
                matchScore += weight
            }
        }

        // Negative scoring for avoided tags
        for (affinity in avoidedTags) {
            if (affinity.tag in novelTags) {
                matchScore -= affinity.score * affinity.confidence * 0.5f
            }
        }

        return if (totalWeight > 0) {
            (matchScore / totalWeight).coerceIn(0f, 1f)
        } else 0.5f
    }

    /**
     * Get the user's top N preferred tag categories
     */
    fun getTopPreferences(n: Int = 5): List<TagCategory> {
        return preferredTags
            .filter { it.isStrong }
            .sortedByDescending { it.score * it.confidence }
            .take(n)
            .map { it.tag }
    }

    companion object {
        val EMPTY = UserTasteProfile(
            preferredTags = emptyList(),
            avoidedTags = emptyList(),
            diversityScore = 0.5f,
            preferredRatingMin = null,
            sampleSize = 0
        )
    }
}

enum class ProfileMaturity {
    NEW,            // < 3 novels
    DEVELOPING,     // 3-10 novels
    ESTABLISHED,    // 10-25 novels
    MATURE          // 25+ novels
}