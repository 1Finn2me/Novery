// Update com/emptycastle/novery/recommendation/NovelScorer.kt

package com.emptycastle.novery.recommendation

import com.emptycastle.novery.recommendation.model.NovelVector
import com.emptycastle.novery.recommendation.model.ScoreBreakdown
import com.emptycastle.novery.recommendation.model.UserTasteProfile

/**
 * Scores how well a novel matches a user's preferences.
 */
class NovelScorer(
    private val userProfile: UserTasteProfile,
    private val authorAffinities: Map<String, Float> = emptyMap(),  // NEW
    private val favoriteAuthors: Set<String> = emptySet()           // NEW
) {

    data class ScoringConfig(
        val tagMatchWeight: Float = 0.30f,
        val preferenceMatchWeight: Float = 0.25f,
        val authorAffinityWeight: Float = 0.15f,
        val ratingQualityWeight: Float = 0.10f,
        val providerBoostWeight: Float = 0.10f,
        val popularityWeight: Float = 0.05f,
        val synopsisWeight: Float = 0.05f,

        val sameProviderMultiplier: Float = 1.15f,
        val favoriteAuthorMultiplier: Float = 1.25f,  // NEW
        val minRatingThreshold: Int = 300
    )

    private val config = ScoringConfig()

    /**
     * Score a novel for the user
     * Returns score between 0.0 and 1.0 with breakdown
     */
    fun scoreNovel(
        novel: NovelVector,
        preferredProvider: String? = null
    ): Pair<Float, ScoreBreakdown> {

        // 1. Tag preference match
        val tagMatch = userProfile.scoreTagMatch(novel.tags)

        // 2. Direct user preference match
        val preferenceMatch = calculatePreferenceMatch(novel)

        // 3. Author affinity - NOW ACTUALLY WORKS!
        val authorMatch = calculateAuthorAffinity(novel.authorNormalized)

        // 4. Rating quality score
        val ratingScore = calculateRatingScore(novel.rating)

        // 5. Provider boost
        val providerBoost = if (preferredProvider != null &&
            novel.providerName == preferredProvider) 1f else 0f

        // 6. Popularity score
        val popularityScore = 0.5f

        val breakdown = ScoreBreakdown(
            tagSimilarity = tagMatch,
            userPreferenceMatch = preferenceMatch,
            authorMatch = authorMatch,
            ratingScore = ratingScore,
            providerBoost = providerBoost,
            synopsisMatch = 0f,
            popularityScore = popularityScore
        )

        // Calculate weighted total
        var total = (
                tagMatch * config.tagMatchWeight +
                        preferenceMatch * config.preferenceMatchWeight +
                        authorMatch * config.authorAffinityWeight +
                        ratingScore * config.ratingQualityWeight +
                        providerBoost * config.providerBoostWeight +
                        popularityScore * config.popularityWeight
                )

        // Apply multipliers
        if (providerBoost > 0) {
            total *= config.sameProviderMultiplier
        }

        // Favorite author gets a big boost
        if (novel.authorNormalized != null && novel.authorNormalized in favoriteAuthors) {
            total *= config.favoriteAuthorMultiplier
        }

        return total.coerceIn(0f, 1f) to breakdown
    }

    /**
     * Calculate how well novel's tags match user's explicit preferences
     */
    private fun calculatePreferenceMatch(novel: NovelVector): Float {
        if (novel.tags.isEmpty() || userProfile.preferredTags.isEmpty()) return 0.5f

        var matchScore = 0f
        var weightSum = 0f

        for (tag in novel.tags) {
            val affinity = userProfile.preferredTags.find { it.tag == tag }
            if (affinity != null) {
                val weight = affinity.confidence
                matchScore += affinity.score * weight
                weightSum += weight
            }
        }

        // Check for avoided tags (negative signal)
        for (avoidedTag in userProfile.avoidedTags) {
            if (avoidedTag.tag in novel.tags) {
                matchScore -= avoidedTag.score * 0.5f
            }
        }

        return if (weightSum > 0) {
            (matchScore / weightSum).coerceIn(0f, 1f)
        } else 0.5f
    }

    /**
     * Calculate affinity for author based on user history
     * NOW USES ACTUAL DATA!
     */
    private fun calculateAuthorAffinity(authorNormalized: String?): Float {
        if (authorNormalized.isNullOrBlank()) return 0f

        // Check if we have affinity data for this author
        val affinity = authorAffinities[authorNormalized]

        return when {
            affinity == null -> 0f  // Unknown author
            affinity >= 0.8f -> 1.0f  // Favorite author
            affinity >= 0.6f -> 0.8f  // Liked author
            affinity >= 0.4f -> 0.5f  // Neutral
            else -> 0.2f  // Disliked author
        }
    }

    /**
     * Score based on rating quality
     */
    private fun calculateRatingScore(rating: Int?): Float {
        if (rating == null) return 0.5f

        val adjustedRating = (rating - config.minRatingThreshold)
            .coerceAtLeast(0)
        val range = 1000 - config.minRatingThreshold

        return (adjustedRating.toFloat() / range).coerceIn(0f, 1f)
    }

    /**
     * Score multiple novels and sort by score
     */
    fun scoreAndRank(
        novels: List<NovelVector>,
        preferredProvider: String? = null,
        limit: Int = 20
    ): List<Pair<NovelVector, ScoreBreakdown>> {
        return novels
            .map { novel ->
                val (_, breakdown) = scoreNovel(novel, preferredProvider)
                novel to breakdown
            }
            .sortedByDescending { (_, breakdown) -> breakdown.total }
            .take(limit)
    }

    /**
     * Filter novels that meet minimum score threshold
     */
    fun filterByMinScore(
        novels: List<NovelVector>,
        minScore: Float = 0.4f,
        preferredProvider: String? = null
    ): List<Pair<NovelVector, ScoreBreakdown>> {
        return novels
            .map { novel ->
                val (_, breakdown) = scoreNovel(novel, preferredProvider)
                novel to breakdown
            }
            .filter { (_, breakdown) -> breakdown.total >= minScore }
            .sortedByDescending { (_, breakdown) -> breakdown.total }
    }

    companion object {
        /**
         * Create a scorer with author data
         */
        suspend fun create(
            userProfile: UserTasteProfile,
            authorPreferenceManager: AuthorPreferenceManager
        ): NovelScorer {
            val likedAuthors = authorPreferenceManager.getLikedAuthors(50)
            val affinities = likedAuthors.associate {
                it.authorNormalized to (it.affinityScore / 1000f)
            }
            val favorites = likedAuthors
                .filter { it.isFavorite }
                .map { it.authorNormalized }
                .toSet()

            return NovelScorer(
                userProfile = userProfile,
                authorAffinities = affinities,
                favoriteAuthors = favorites
            )
        }
    }
}