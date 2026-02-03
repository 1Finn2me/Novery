package com.emptycastle.novery.recommendation.model

import com.emptycastle.novery.domain.model.Novel

/**
 * A recommendation result with scoring and explanation
 */
data class Recommendation(
    /** The recommended novel */
    val novel: Novel,

    /** Overall recommendation score (0.0 - 1.0) */
    val score: Float,

    /** Type of recommendation */
    val type: RecommendationType,

    /** Human-readable reason for recommendation */
    val reason: String,

    /** Breakdown of scoring factors */
    val scoreBreakdown: ScoreBreakdown,

    /** Source novel URL if this is a "similar to" recommendation */
    val sourceNovelUrl: String? = null,

    /** Source novel name for display */
    val sourceNovelName: String? = null,

    /** Whether this is from a different provider than user typically uses */
    val isCrossProvider: Boolean = false
)

/**
 * Detailed breakdown of how the score was calculated
 */
data class ScoreBreakdown(
    val tagSimilarity: Float = 0f,
    val userPreferenceMatch: Float = 0f,
    val authorMatch: Float = 0f,
    val ratingScore: Float = 0f,
    val providerBoost: Float = 0f,
    val synopsisMatch: Float = 0f,
    val popularityScore: Float = 0f
) {
    val total: Float
        get() = (tagSimilarity * 0.30f +
                userPreferenceMatch * 0.25f +
                authorMatch * 0.15f +
                ratingScore * 0.10f +
                providerBoost * 0.10f +
                synopsisMatch * 0.05f +
                popularityScore * 0.05f).coerceIn(0f, 1f)
}

/**
 * A group of recommendations with a common theme
 */
data class RecommendationGroup(
    val type: RecommendationType,
    val title: String,
    val subtitle: String? = null,
    val recommendations: List<Recommendation>,
    val sourceNovel: Novel? = null
)