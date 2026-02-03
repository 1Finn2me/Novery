package com.emptycastle.novery.recommendation

import com.emptycastle.novery.data.local.dao.RecommendationDao
import com.emptycastle.novery.data.local.entity.UserPreferenceEntity
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.NovelVector
import com.emptycastle.novery.recommendation.model.TagAffinity
import com.emptycastle.novery.recommendation.model.UserTasteProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Manages user preference calculation and updates.
 * Tracks what the user reads and builds their taste profile.
 */
class UserPreferenceManager(
    private val recommendationDao: RecommendationDao,
    private val authorPreferenceManager: AuthorPreferenceManager
) {

    // ================================================================
    // PROFILE RETRIEVAL
    // ================================================================

    /**
     * Get the user's current taste profile
     */
    suspend fun getUserProfile(): UserTasteProfile = withContext(Dispatchers.IO) {
        val preferences = recommendationDao.getAllPreferences()
        buildProfileFromPreferences(preferences)
    }

    /**
     * Observe user profile changes
     */
    fun observeUserProfile(): Flow<UserTasteProfile> {
        return recommendationDao.observeAllPreferences().map { preferences ->
            buildProfileFromPreferences(preferences)
        }
    }

    private fun buildProfileFromPreferences(preferences: List<UserPreferenceEntity>): UserTasteProfile {
        if (preferences.isEmpty()) return UserTasteProfile.EMPTY

        val affinities = preferences.mapNotNull { pref ->
            val tag = try {
                TagCategory.valueOf(pref.tag)
            } catch (e: Exception) {
                return@mapNotNull null
            }

            // Calculate confidence based on sample size
            val confidence = calculateConfidence(pref.novelCount, pref.readingTimeSeconds)

            TagAffinity(
                tag = tag,
                score = pref.affinityScore / 1000f,
                confidence = confidence,
                novelCount = pref.novelCount,
                completionRate = pref.completionRate,
                dropRate = pref.dropRate
            )
        }

        // Separate preferred vs avoided
        val preferred = affinities
            .filter { it.score >= 0.5f }
            .sortedByDescending { it.score * it.confidence }

        val avoided = affinities
            .filter { it.dropRate > 0.5f && it.novelCount >= 2 }
            .sortedByDescending { it.completionRate }

        // Calculate diversity (how spread out are the preferences?)
        val diversityScore = calculateDiversity(preferred)

        val totalNovels = preferences.maxOfOrNull { it.novelCount } ?: 0

        return UserTasteProfile(
            preferredTags = preferred,
            avoidedTags = avoided,
            diversityScore = diversityScore,
            preferredRatingMin = null, // Could add rating preference tracking
            sampleSize = totalNovels
        )
    }

    private fun calculateConfidence(novelCount: Int, readingTimeSeconds: Long): Float {
        // More novels and more reading time = higher confidence
        val novelFactor = (novelCount / 10f).coerceAtMost(1f)
        val timeFactor = (readingTimeSeconds / (10 * 60 * 60f)).coerceAtMost(1f) // 10 hours max
        return ((novelFactor + timeFactor) / 2).coerceIn(0.1f, 1f)
    }

    private fun calculateDiversity(preferences: List<TagAffinity>): Float {
        if (preferences.size <= 1) return 0f

        // Calculate variance in scores - high variance = focused, low = diverse
        val scores = preferences.map { it.score }
        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean) * (it - mean) }.average().toFloat()

        // Invert: low variance = high diversity score
        return (1f - variance).coerceIn(0f, 1f)
    }

    // ================================================================
    // PREFERENCE UPDATES
    // ================================================================

    /**
     * Update preferences when user adds a novel to library
     */
    suspend fun onNovelAddedToLibrary(
        novelDetails: NovelDetails,
        novelUrl: String
    ) = withContext(Dispatchers.IO) {
        val tags = TagNormalizer.normalizeAll(novelDetails.tags ?: emptyList())

        for (tag in tags) {
            val existing = recommendationDao.getPreference(tag.name)

            if (existing != null) {
                // Increment novel count, small score boost for adding
                val newScore = (existing.affinityScore + 50).coerceAtMost(1000)
                recommendationDao.updatePreference(
                    tag = tag.name,
                    score = newScore,
                    novelDelta = 1
                )
            } else {
                // Create new preference with initial score
                recommendationDao.insertPreference(
                    UserPreferenceEntity(
                        tag = tag.name,
                        affinityScore = 500, // Neutral starting point
                        novelCount = 1
                    )
                )
            }
        }

        // Track author
        authorPreferenceManager.onNovelAddedToLibrary(novelDetails, novelUrl)
    }

    /**
     * Update preferences when user reads chapters
     */
    suspend fun onChaptersRead(
        novelDetails: NovelDetails,
        chaptersRead: Int,
        readingTimeSeconds: Long
    ) = withContext(Dispatchers.IO) {
        val tags = TagNormalizer.normalizeAll(novelDetails.tags ?: emptyList())

        for (tag in tags) {
            val existing = recommendationDao.getPreference(tag.name)

            if (existing != null) {
                // Reading = positive signal, increase score
                val readingBoost = (chaptersRead * 10 + (readingTimeSeconds / 60).toInt())
                    .coerceAtMost(100)
                val newScore = (existing.affinityScore + readingBoost).coerceAtMost(1000)

                recommendationDao.updatePreference(
                    tag = tag.name,
                    score = newScore,
                    chaptersDelta = chaptersRead,
                    timeDelta = readingTimeSeconds
                )
            } else {
                // Shouldn't happen normally, but handle it
                recommendationDao.insertPreference(
                    UserPreferenceEntity(
                        tag = tag.name,
                        affinityScore = 550, // Slightly positive (they're reading it)
                        novelCount = 1,
                        chaptersRead = chaptersRead,
                        readingTimeSeconds = readingTimeSeconds
                    )
                )
            }
        }

        // Track author reading
        val authorNormalized = NovelVector.normalizeAuthor(novelDetails.author)
        authorPreferenceManager.onChaptersRead(
            authorNormalized = authorNormalized,
            authorDisplay = novelDetails.author,
            chaptersRead = chaptersRead,
            readingTimeSeconds = readingTimeSeconds
        )
    }

    /**
     * Update preferences when user changes reading status
     */
    suspend fun onStatusChanged(
        novelDetails: NovelDetails,
        novelUrl: String,
        newStatus: ReadingStatus,
        oldStatus: ReadingStatus? = null
    ) = withContext(Dispatchers.IO) {
        val tags = TagNormalizer.normalizeAll(novelDetails.tags ?: emptyList())

        val (scoreDelta, completedDelta, droppedDelta) = when (newStatus) {
            ReadingStatus.COMPLETED -> Triple(100, 1, 0)  // Big positive signal
            ReadingStatus.DROPPED -> Triple(-150, 0, 1)  // Negative signal
            ReadingStatus.READING -> Triple(25, 0, 0)    // Small positive
            ReadingStatus.ON_HOLD -> Triple(-25, 0, 0)   // Slight negative
            ReadingStatus.PLAN_TO_READ -> Triple(10, 0, 0) // Interest signal
        }

        // Undo old status effect if applicable
        val undoEffect = when (oldStatus) {
            ReadingStatus.COMPLETED -> Triple(-100, -1, 0)
            ReadingStatus.DROPPED -> Triple(150, 0, -1)
            else -> Triple(0, 0, 0)
        }

        for (tag in tags) {
            val existing = recommendationDao.getPreference(tag.name) ?: continue

            val newScore = (existing.affinityScore + scoreDelta + undoEffect.first)
                .coerceIn(0, 1000)

            recommendationDao.updatePreference(
                tag = tag.name,
                score = newScore,
                completedDelta = completedDelta + undoEffect.second,
                droppedDelta = droppedDelta + undoEffect.third
            )
        }

        // Track author status
        authorPreferenceManager.onStatusChanged(
            novelDetails = novelDetails,
            novelUrl = novelUrl,
            newStatus = newStatus,
            oldStatus = oldStatus
        )
    }

    /**
     * Update preferences when user removes novel from library
     */
    suspend fun onNovelRemoved(
        novelDetails: NovelDetails,
        novelUrl: String,
        wasCompleted: Boolean
    ) = withContext(Dispatchers.IO) {
        val tags = TagNormalizer.normalizeAll(novelDetails.tags ?: emptyList())

        for (tag in tags) {
            val existing = recommendationDao.getPreference(tag.name) ?: continue

            // Don't change score much - removal is ambiguous
            // But do update counts
            recommendationDao.updatePreference(
                tag = tag.name,
                score = existing.affinityScore,
                novelDelta = -1,
                completedDelta = if (wasCompleted) -1 else 0
            )
        }

        // Track author removal
        authorPreferenceManager.onNovelRemoved(
            novelDetails = novelDetails,
            novelUrl = novelUrl,
            wasCompleted = wasCompleted
        )
    }

    // ================================================================
    // FULL RECALCULATION
    // ================================================================

    /**
     * Recalculate all preferences from scratch based on library and history.
     * Call this occasionally or when user requests it.
     */
    suspend fun recalculateAllPreferences() = withContext(Dispatchers.IO) {
        // Clear existing
        recommendationDao.clearAllPreferences()

        // Get all data
        val libraryRepository = RepositoryProvider.getLibraryRepository()
        val offlineRepository = RepositoryProvider.getOfflineRepository()
        val historyRepository = RepositoryProvider.getHistoryRepository()
        val statsRepository = RepositoryProvider.getStatsRepository()

        val library = libraryRepository.getLibrary()
        val tagScores = mutableMapOf<TagCategory, MutablePreferenceData>()

        // Build library items for author recalculation
        val libraryItemsWithDetails = mutableListOf<LibraryItemWithDetails>()

        for (item in library) {
            // Get cached novel details
            val details = offlineRepository.getNovelDetails(item.novel.url) ?: continue
            val tags = TagNormalizer.normalizeAll(details.tags ?: emptyList())

            // Get reading data for this novel
            val readCount = historyRepository.getReadChapterCount(item.novel.url)
            val totalChapters = details.chapters.size
            val progress = if (totalChapters > 0) readCount.toFloat() / totalChapters else 0f

            // Build library item for author recalculation
            libraryItemsWithDetails.add(
                LibraryItemWithDetails(
                    novelUrl = item.novel.url,
                    author = details.author,
                    status = item.readingStatus,
                    chaptersRead = readCount,
                    readingTimeSeconds = 0L, // Per-novel reading time not tracked separately
                    progressPercent = progress
                )
            )

            for (tag in tags) {
                val data = tagScores.getOrPut(tag) { MutablePreferenceData() }
                data.novelCount++
                data.chaptersRead += readCount

                // Score based on status and progress
                when (item.readingStatus) {
                    ReadingStatus.COMPLETED -> {
                        data.completedCount++
                        data.scoreAccumulator += 900
                    }
                    ReadingStatus.DROPPED -> {
                        data.droppedCount++
                        data.scoreAccumulator += 200
                    }
                    ReadingStatus.READING -> {
                        data.scoreAccumulator += (400 + progress * 300).toInt()
                    }
                    ReadingStatus.ON_HOLD -> {
                        data.scoreAccumulator += 350
                    }
                    ReadingStatus.PLAN_TO_READ -> {
                        data.scoreAccumulator += 450
                    }
                }
            }
        }

        // Convert to entities and save
        val preferences = tagScores.map { (tag, data) ->
            UserPreferenceEntity(
                tag = tag.name,
                affinityScore = (data.scoreAccumulator / data.novelCount).coerceIn(0, 1000),
                novelCount = data.novelCount,
                chaptersRead = data.chaptersRead,
                readingTimeSeconds = data.readingTimeSeconds,
                completedCount = data.completedCount,
                droppedCount = data.droppedCount
            )
        }

        recommendationDao.insertPreferences(preferences)

        // Also recalculate author preferences
        authorPreferenceManager.recalculateAllPreferences(libraryItemsWithDetails)
    }

    private data class MutablePreferenceData(
        var novelCount: Int = 0,
        var chaptersRead: Int = 0,
        var readingTimeSeconds: Long = 0,
        var completedCount: Int = 0,
        var droppedCount: Int = 0,
        var scoreAccumulator: Int = 0
    )
}