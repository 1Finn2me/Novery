// com/emptycastle/novery/recommendation/TagEnhancementManager.kt
package com.emptycastle.novery.recommendation

import android.util.Log
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.dao.RecommendationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "TagEnhancementManager"

/**
 * Manages tag enhancement for novels that have synopses but few/no tags.
 * Uses SynopsisTagExtractor to infer tags from text to improve recommendation quality.
 */
class TagEnhancementManager(
    private val recommendationDao: RecommendationDao,
    private val offlineDao: OfflineDao
) {

    data class EnhancementResult(
        val novelsProcessed: Int,
        val novelsEnhanced: Int,
        val tagsAdded: Int,
        val errors: Int
    )

    data class TagCoverageStats(
        val totalNovels: Int,
        val withTags: Int,
        val withSynopsis: Int,
        val tagCoveragePercent: Float,
        val topTags: List<Pair<TagNormalizer.TagCategory, Int>>
    )

    /**
     * Enhance novels that have synopsis but few/no tags.
     * This is a local operation - no network required.
     *
     * @param minTagThreshold Novels with fewer tags than this will be enhanced.
     * @param forceReprocess If true, reprocess all novels regardless of existing tags.
     * @param batchSize Number of novels to process (currently processes all, param reserved for future pagination).
     */
    suspend fun enhanceNovelsWithSynopsis(
        minTagThreshold: Int = 3,
        forceReprocess: Boolean = false,
        batchSize: Int = 100
    ): EnhancementResult = withContext(Dispatchers.IO) {
        var processed = 0
        var enhanced = 0
        var totalTagsAdded = 0
        var errors = 0

        try {
            // Get all discovered novels
            val allNovels = recommendationDao.getAllDiscoveredNovels()

            Log.d(TAG, "Starting tag enhancement for ${allNovels.size} novels")

            for (novel in allNovels) {
                processed++

                try {
                    // Parse existing tags from CSV string
                    val existingTags = novel.tagsString
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()

                    // Skip if enough tags already (unless force reprocess)
                    if (!forceReprocess && existingTags.size >= minTagThreshold) {
                        continue
                    }

                    // Skip if no synopsis available to analyze
                    if (novel.synopsis.isNullOrBlank()) {
                        continue
                    }

                    // Extract tags from synopsis
                    val extractedCategories = SynopsisTagExtractor.extractTags(
                        synopsis = novel.synopsis,
                        maxTags = 8
                    )

                    if (extractedCategories.isEmpty()) {
                        continue
                    }

                    // Convert categories to display names
                    val extractedTagNames = extractedCategories.map {
                        TagNormalizer.getDisplayName(it)
                    }

                    // Merge with existing tags (avoid duplicates, case-insensitive check)
                    val existingLower = existingTags.map { it.lowercase() }.toSet()
                    val newTags = extractedTagNames.filter {
                        it.lowercase() !in existingLower
                    }

                    if (newTags.isEmpty()) {
                        continue
                    }

                    // Combine tags
                    val allTags = existingTags + newTags
                    val updatedTagsString = allTags.joinToString(",")

                    // Update the entity
                    val updatedNovel = novel.copy(
                        tagsString = updatedTagsString,
                        lastVerifiedAt = System.currentTimeMillis()
                    )
                    recommendationDao.insertDiscoveredNovel(updatedNovel)

                    enhanced++
                    totalTagsAdded += newTags.size

                    if (enhanced % 50 == 0) {
                        Log.d(TAG, "Enhanced $enhanced novels so far...")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error enhancing novel ${novel.url}", e)
                    errors++
                }
            }

            Log.d(TAG, "Tag enhancement complete: $enhanced/$processed novels enhanced, $totalTagsAdded tags added")

        } catch (e: Exception) {
            Log.e(TAG, "Error in tag enhancement batch process", e)
            errors++
        }

        EnhancementResult(
            novelsProcessed = processed,
            novelsEnhanced = enhanced,
            tagsAdded = totalTagsAdded,
            errors = errors
        )
    }

    /**
     * Get statistics about tag coverage in the discovery pool.
     */
    suspend fun getTagCoverageStats(): TagCoverageStats = withContext(Dispatchers.IO) {
        val allNovels = recommendationDao.getAllDiscoveredNovels()

        var withTags = 0
        var withSynopsis = 0
        val tagCounts = mutableMapOf<TagNormalizer.TagCategory, Int>()

        for (novel in allNovels) {
            val tags = novel.tagsString
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (tags.isNotEmpty()) {
                withTags++

                // Normalize and count tags for distribution stats
                val normalizedTags = TagNormalizer.normalizeAll(tags)
                for (tag in normalizedTags) {
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }

            if (!novel.synopsis.isNullOrBlank()) {
                withSynopsis++
            }
        }

        val totalNovels = allNovels.size
        val tagCoveragePercent = if (totalNovels > 0) {
            (withTags.toFloat() / totalNovels) * 100f
        } else 0f

        val topTags = tagCounts.entries
            .sortedByDescending { it.value }
            .take(20) // Increased to top 20 for better visibility
            .map { it.key to it.value }

        TagCoverageStats(
            totalNovels = totalNovels,
            withTags = withTags,
            withSynopsis = withSynopsis,
            tagCoveragePercent = tagCoveragePercent,
            topTags = topTags
        )
    }

    /**
     * Enhance a single novel's tags from its synopsis.
     * Useful when a user manually refreshes a novel or opens details.
     */
    suspend fun enhanceSingleNovel(novelUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val novel = recommendationDao.getDiscoveredNovel(novelUrl) ?: return@withContext false

            if (novel.synopsis.isNullOrBlank()) {
                return@withContext false
            }

            val extractedCategories = SynopsisTagExtractor.extractTags(novel.synopsis, maxTags = 8)
            if (extractedCategories.isEmpty()) {
                return@withContext false
            }

            val existingTags = novel.tagsString
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val extractedTagNames = extractedCategories.map { TagNormalizer.getDisplayName(it) }
            val existingLower = existingTags.map { it.lowercase() }.toSet()
            val newTags = extractedTagNames.filter { it.lowercase() !in existingLower }

            if (newTags.isEmpty()) {
                return@withContext false
            }

            val allTags = existingTags + newTags
            val updatedNovel = novel.copy(
                tagsString = allTags.joinToString(","),
                lastVerifiedAt = System.currentTimeMillis()
            )
            recommendationDao.insertDiscoveredNovel(updatedNovel)

            Log.d(TAG, "Enhanced ${novel.name} with ${newTags.size} new tags")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error enhancing single novel $novelUrl", e)
            false
        }
    }
}