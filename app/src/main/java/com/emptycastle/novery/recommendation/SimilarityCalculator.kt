package com.emptycastle.novery.recommendation

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.NovelVector
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Calculates similarity between novels based on various features.
 */
object SimilarityCalculator {

    /**
     * Configuration for similarity weights
     */
    data class SimilarityConfig(
        val tagWeight: Float = 0.40f,
        val authorWeight: Float = 0.20f,
        val ratingWeight: Float = 0.10f,
        val synopsisWeight: Float = 0.15f,
        val lengthWeight: Float = 0.05f,
        val statusWeight: Float = 0.05f,
        val providerBoost: Float = 0.05f
    )

    private val defaultConfig = SimilarityConfig()

    /**
     * Calculate overall similarity between two novels.
     * Returns value between 0.0 and 1.0
     */
    fun calculateSimilarity(
        novel1: NovelVector,
        novel2: NovelVector,
        config: SimilarityConfig = defaultConfig
    ): Float {
        // Skip if same novel
        if (novel1.url == novel2.url) return 1f

        val tagSim = calculateTagSimilarity(novel1.tags, novel2.tags)
        val authorSim = calculateAuthorSimilarity(novel1.authorNormalized, novel2.authorNormalized)
        val ratingSim = calculateRatingSimilarity(novel1.rating, novel2.rating)
        val synopsisSim = calculateSynopsisSimilarity(novel1.synopsisKeywords, novel2.synopsisKeywords)
        val lengthSim = calculateLengthSimilarity(novel1.chapterCount, novel2.chapterCount)
        val statusSim = if (novel1.isCompleted == novel2.isCompleted) 1f else 0.5f
        val providerBoost = if (novel1.providerName == novel2.providerName) 1f else 0f

        return (tagSim * config.tagWeight +
                authorSim * config.authorWeight +
                ratingSim * config.ratingWeight +
                synopsisSim * config.synopsisWeight +
                lengthSim * config.lengthWeight +
                statusSim * config.statusWeight +
                providerBoost * config.providerBoost).coerceIn(0f, 1f)
    }

    /**
     * Calculate tag similarity using Jaccard index with related tag bonus
     */
    fun calculateTagSimilarity(
        tags1: Set<TagCategory>,
        tags2: Set<TagCategory>
    ): Float {
        return TagNormalizer.calculateTagSimilarity(tags1, tags2)
    }

    /**
     * Calculate author similarity (binary with fuzzy matching)
     */
    fun calculateAuthorSimilarity(author1: String?, author2: String?): Float {
        if (author1.isNullOrBlank() || author2.isNullOrBlank()) return 0f

        // Exact match
        if (author1 == author2) return 1f

        // Check if one contains the other (handles pen names, etc.)
        if (author1.contains(author2) || author2.contains(author1)) return 0.8f

        // Levenshtein distance for fuzzy matching
        val distance = levenshteinDistance(author1, author2)
        val maxLength = maxOf(author1.length, author2.length)
        val similarity = 1f - (distance.toFloat() / maxLength)

        return if (similarity > 0.8f) similarity else 0f
    }

    /**
     * Calculate rating proximity (closer ratings = more similar)
     */
    fun calculateRatingSimilarity(rating1: Int?, rating2: Int?): Float {
        if (rating1 == null || rating2 == null) return 0.5f // Neutral if unknown

        val diff = abs(rating1 - rating2)
        // 0 diff = 1.0, 1000 diff = 0.0
        return (1f - diff / 1000f).coerceIn(0f, 1f)
    }

    /**
     * Calculate synopsis similarity using keyword overlap
     */
    fun calculateSynopsisSimilarity(
        keywords1: Set<String>,
        keywords2: Set<String>
    ): Float {
        if (keywords1.isEmpty() || keywords2.isEmpty()) return 0f

        val intersection = keywords1.intersect(keywords2).size
        val union = keywords1.union(keywords2).size

        return if (union > 0) intersection.toFloat() / union else 0f
    }

    /**
     * Calculate length similarity (similar chapter counts = similar commitment)
     */
    fun calculateLengthSimilarity(chapters1: Int, chapters2: Int): Float {
        if (chapters1 == 0 || chapters2 == 0) return 0.5f

        val min = minOf(chapters1, chapters2).toFloat()
        val max = maxOf(chapters1, chapters2).toFloat()

        // Ratio-based similarity
        return (min / max).coerceIn(0f, 1f)
    }

    /**
     * Find most similar novels to a target novel
     */
    fun findSimilar(
        target: NovelVector,
        candidates: List<NovelVector>,
        limit: Int = 10,
        minSimilarity: Float = 0.2f,
        config: SimilarityConfig = defaultConfig
    ): List<Pair<NovelVector, Float>> {
        return candidates
            .filter { it.url != target.url }
            .map { candidate -> candidate to calculateSimilarity(target, candidate, config) }
            .filter { (_, similarity) -> similarity >= minSimilarity }
            .sortedByDescending { (_, similarity) -> similarity }
            .take(limit)
    }

    /**
     * Calculate cosine similarity between two feature vectors
     */
    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        require(vector1.size == vector2.size) { "Vectors must have same size" }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    /**
     * Levenshtein distance for fuzzy string matching
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }
}