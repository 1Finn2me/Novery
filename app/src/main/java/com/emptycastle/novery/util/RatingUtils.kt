package com.emptycastle.novery.util

/**
 * Utility object for handling ratings across different providers.
 * All ratings are normalized to a 0-1000 scale internally (for precision).
 * Display can be as stars (out of 5), percentage, or X/10.
 */
object RatingUtils {

    /**
     * Normalize a rating to internal 0-1000 scale
     *
     * @param value The raw rating value
     * @param maxValue The maximum possible value for this rating system
     * @return Normalized rating (0-1000)
     */
    fun normalize(value: Float, maxValue: Float): Int {
        if (maxValue <= 0) return 0
        return ((value / maxValue) * 1000).toInt().coerceIn(0, 1000)
    }

    /**
     * Normalize from a 5-star rating system
     */
    fun from5Stars(value: Float): Int = normalize(value, 5f)

    /**
     * Normalize from a 10-point rating system
     */
    fun from10Points(value: Float): Int = normalize(value, 10f)

    /**
     * Normalize from a 100-point rating system
     */
    fun from100Points(value: Float): Int = normalize(value, 100f)

    /**
     * Convert normalized rating to 5-star display
     */
    fun to5Stars(normalized: Int): Float = (normalized / 1000f) * 5f

    /**
     * Convert normalized rating to 10-point display
     */
    fun to10Points(normalized: Int): Float = (normalized / 1000f) * 10f

    /**
     * Convert normalized rating to percentage
     */
    fun toPercentage(normalized: Int): Int = (normalized / 10f).toInt()

    /**
     * Format rating for display
     *
     * @param normalized The normalized rating (0-1000)
     * @param style The display style
     * @return Formatted string
     */
    fun format(normalized: Int?, style: RatingDisplayStyle = RatingDisplayStyle.STARS): String {
        if (normalized == null || normalized <= 0) return ""

        return when (style) {
            RatingDisplayStyle.STARS -> {
                val stars = to5Stars(normalized)
                String.format("%.1f", stars)
            }
            RatingDisplayStyle.STARS_WITH_MAX -> {
                val stars = to5Stars(normalized)
                String.format("%.1f/5", stars)
            }
            RatingDisplayStyle.POINTS_10 -> {
                val points = to10Points(normalized)
                String.format("%.1f", points)
            }
            RatingDisplayStyle.POINTS_10_WITH_MAX -> {
                val points = to10Points(normalized)
                String.format("%.1f/10", points)
            }
            RatingDisplayStyle.PERCENTAGE -> {
                "${toPercentage(normalized)}%"
            }
        }
    }

    /**
     * Get a color tint based on rating
     */
    fun getRatingColor(normalized: Int?): RatingColor {
        if (normalized == null || normalized <= 0) return RatingColor.NONE

        return when {
            normalized >= 800 -> RatingColor.EXCELLENT  // 4+ stars
            normalized >= 600 -> RatingColor.GOOD       // 3+ stars
            normalized >= 400 -> RatingColor.AVERAGE    // 2+ stars
            normalized >= 200 -> RatingColor.POOR       // 1+ stars
            else -> RatingColor.VERY_POOR
        }
    }
}

enum class RatingDisplayStyle {
    STARS,              // "4.5"
    STARS_WITH_MAX,     // "4.5/5"
    POINTS_10,          // "9.0"
    POINTS_10_WITH_MAX, // "9.0/10"
    PERCENTAGE          // "90%"
}

enum class RatingColor {
    NONE,
    VERY_POOR,
    POOR,
    AVERAGE,
    GOOD,
    EXCELLENT
}