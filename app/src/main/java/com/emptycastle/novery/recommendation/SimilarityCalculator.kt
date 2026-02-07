// com/emptycastle/novery/recommendation/SimilarityCalculator.kt
package com.emptycastle.novery.recommendation

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.NovelVector
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Calculates similarity between novels based on various features.
 * Enhanced to handle novels with sparse tag data by using title/synopsis matching.
 */
object SimilarityCalculator {

    /**
     * Configuration for similarity weights
     */
    data class SimilarityConfig(
        val tagWeight: Float = 0.35f,
        val authorWeight: Float = 0.15f,
        val ratingWeight: Float = 0.05f,
        val synopsisWeight: Float = 0.20f,
        val titleWeight: Float = 0.10f,
        val lengthWeight: Float = 0.05f,
        val statusWeight: Float = 0.05f,
        val providerBoost: Float = 0.05f
    )

    /**
     * Config for when both novels have good tag data
     */
    private val tagRichConfig = SimilarityConfig(
        tagWeight = 0.40f,
        authorWeight = 0.15f,
        ratingWeight = 0.05f,
        synopsisWeight = 0.15f,
        titleWeight = 0.10f,
        lengthWeight = 0.05f,
        statusWeight = 0.05f,
        providerBoost = 0.05f
    )

    /**
     * Config for when one or both novels have sparse tag data
     * Relies more on text-based similarity
     */
    private val tagSparseConfig = SimilarityConfig(
        tagWeight = 0.15f,
        authorWeight = 0.15f,
        ratingWeight = 0.05f,
        synopsisWeight = 0.35f,
        titleWeight = 0.15f,
        lengthWeight = 0.05f,
        statusWeight = 0.05f,
        providerBoost = 0.05f
    )

    /**
     * Calculate overall similarity between two novels.
     * Automatically adjusts weights based on data quality.
     * Returns value between 0.0 and 1.0
     */
    fun calculateSimilarity(
        novel1: NovelVector,
        novel2: NovelVector,
        config: SimilarityConfig? = null
    ): Float {
        // Skip if same novel
        if (novel1.url == novel2.url) return 1f

        // Choose config based on data quality
        val effectiveConfig = config ?: selectConfig(novel1, novel2)

        val tagSim = calculateTagSimilarity(novel1.tags, novel2.tags)
        val authorSim = calculateAuthorSimilarity(novel1.authorNormalized, novel2.authorNormalized)
        val ratingSim = calculateRatingSimilarity(novel1.rating, novel2.rating)
        val synopsisSim = calculateSynopsisSimilarity(novel1.synopsisKeywords, novel2.synopsisKeywords)
        val titleSim = calculateTitleSimilarity(novel1.titleKeywords, novel2.titleKeywords, novel1.name, novel2.name)
        val lengthSim = calculateLengthSimilarity(novel1.chapterCount, novel2.chapterCount)
        val statusSim = if (novel1.isCompleted == novel2.isCompleted) 1f else 0.5f
        val providerBoost = if (novel1.providerName == novel2.providerName) 1f else 0f

        val rawScore = (
                tagSim * effectiveConfig.tagWeight +
                        authorSim * effectiveConfig.authorWeight +
                        ratingSim * effectiveConfig.ratingWeight +
                        synopsisSim * effectiveConfig.synopsisWeight +
                        titleSim * effectiveConfig.titleWeight +
                        lengthSim * effectiveConfig.lengthWeight +
                        statusSim * effectiveConfig.statusWeight +
                        providerBoost * effectiveConfig.providerBoost
                )

        // Boost if author matches
        val authorBoost = if (authorSim > 0.8f) 1.15f else 1f

        return (rawScore * authorBoost).coerceIn(0f, 1f)
    }

    /**
     * Select appropriate config based on data quality
     */
    private fun selectConfig(novel1: NovelVector, novel2: NovelVector): SimilarityConfig {
        val bothHaveGoodTags = novel1.tags.size >= 3 && novel2.tags.size >= 3
        return if (bothHaveGoodTags) tagRichConfig else tagSparseConfig
    }

    /**
     * Calculate tag similarity using Jaccard index with related tag bonus
     */
    fun calculateTagSimilarity(
        tags1: Set<TagCategory>,
        tags2: Set<TagCategory>
    ): Float {
        if (tags1.isEmpty() || tags2.isEmpty()) return 0f
        return TagNormalizer.calculateTagSimilarity(tags1, tags2)
    }

    /**
     * Calculate title similarity
     * Considers both keyword overlap and fuzzy string matching
     */
    fun calculateTitleSimilarity(
        titleKeywords1: Set<String>,
        titleKeywords2: Set<String>,
        title1: String,
        title2: String
    ): Float {
        // Keyword overlap
        val keywordSim = if (titleKeywords1.isNotEmpty() && titleKeywords2.isNotEmpty()) {
            val intersection = titleKeywords1.intersect(titleKeywords2).size
            val union = titleKeywords1.union(titleKeywords2).size
            if (union > 0) intersection.toFloat() / union else 0f
        } else 0f

        // Also check for similar title patterns
        val patternSim = calculateTitlePatternSimilarity(title1, title2)

        return (keywordSim * 0.7f + patternSim * 0.3f).coerceIn(0f, 1f)
    }

    /**
     * Check for similar title patterns (e.g., "Reincarnated as X" vs "Reincarnated as Y")
     */
    private fun calculateTitlePatternSimilarity(title1: String, title2: String): Float {
        val lower1 = title1.lowercase()
        val lower2 = title2.lowercase()

        // Common title patterns
        val patterns = listOf(
            "reincarnated as", "transmigrated", "became a", "i am a", "i'm a",
            "my", "the", "a", "return of", "rise of", "fall of", "legend of",
            "story of", "tale of", "chronicles of", "system", "dungeon",
            "tower", "level", "class", "skill"
        )

        var sharedPatterns = 0
        for (pattern in patterns) {
            if (lower1.contains(pattern) && lower2.contains(pattern)) {
                sharedPatterns++
            }
        }

        return (sharedPatterns.toFloat() / 3f).coerceAtMost(1f)
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
     * Enhanced with weighted keywords
     */
    fun calculateSynopsisSimilarity(
        keywords1: Set<String>,
        keywords2: Set<String>
    ): Float {
        if (keywords1.isEmpty() || keywords2.isEmpty()) return 0f

        val intersection = keywords1.intersect(keywords2)
        val union = keywords1.union(keywords2)

        if (union.isEmpty()) return 0f

        // Basic Jaccard
        val jaccardScore = intersection.size.toFloat() / union.size

        // Boost for multiple matches
        val matchBoost = when {
            intersection.size >= 10 -> 1.3f
            intersection.size >= 5 -> 1.15f
            else -> 1f
        }

        return (jaccardScore * matchBoost).coerceIn(0f, 1f)
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
        config: SimilarityConfig? = null
    ): List<Pair<NovelVector, Float>> {
        return candidates
            .filter { it.url != target.url }
            .map { candidate -> candidate to calculateSimilarity(target, candidate, config) }
            .filter { (_, similarity) -> similarity >= minSimilarity }
            .sortedByDescending { (_, similarity) -> similarity }
            .take(limit)
    }

    /**
     * Find similar novels with quality-based filtering
     * Prioritizes novels with good data quality
     */
    fun findSimilarWithQuality(
        target: NovelVector,
        candidates: List<NovelVector>,
        limit: Int = 10,
        minSimilarity: Float = 0.15f
    ): List<Pair<NovelVector, Float>> {
        // Separate candidates by quality
        val (highQuality, lowQuality) = candidates.partition { it.hasQualityData }

        // Calculate similarity for all
        val allScored = candidates
            .filter { it.url != target.url }
            .map { candidate ->
                val baseSim = calculateSimilarity(target, candidate)
                // Slight boost for high quality data novels
                val qualityBoost = if (candidate.hasQualityData) 1.05f else 1f
                candidate to (baseSim * qualityBoost).coerceAtMost(1f)
            }
            .filter { (_, similarity) -> similarity >= minSimilarity }
            .sortedByDescending { (_, similarity) -> similarity }

        return allScored.take(limit)
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