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
 * Types of user interactions that affect preference scoring
 */
enum class InteractionType {
    READ_CHAPTER,
    COMPLETED_NOVEL,
    DROPPED_NOVEL,
    ADDED_TO_LIBRARY,
    REMOVED_FROM_LIBRARY,
    LONG_READ,
    EXPLICIT_LIKE,
    EXPLICIT_DISLIKE,
    STATUS_READING,
    STATUS_ON_HOLD,
    STATUS_PLAN_TO_READ,

    // Undo variants for status changes
    UNDO_COMPLETED,
    UNDO_DROPPED
}

/**
 * Manages user preference calculation and updates.
 * Tracks what the user reads and builds their taste profile.
 */
class UserPreferenceManager(
    private val recommendationDao: RecommendationDao,
    private val authorPreferenceManager: AuthorPreferenceManager
) {

    companion object {
        // Long read threshold: 30 minutes of continuous reading
        private const val LONG_READ_THRESHOLD_SECONDS = 30 * 60L
    }

    // ================================================================
    // SCORE CONFIGURATION
    // ================================================================

    /**
     * Get score change for each interaction type
     */
    private fun getScoreChange(interactionType: InteractionType, weight: Float = 1.0f): Int {
        val baseScore = when (interactionType) {
            InteractionType.EXPLICIT_LIKE -> 200
            InteractionType.EXPLICIT_DISLIKE -> -150
            InteractionType.READ_CHAPTER -> 10
            InteractionType.COMPLETED_NOVEL -> 100
            InteractionType.DROPPED_NOVEL -> -150
            InteractionType.ADDED_TO_LIBRARY -> 50
            InteractionType.REMOVED_FROM_LIBRARY -> 0 // Neutral - ambiguous signal
            InteractionType.LONG_READ -> 30
            InteractionType.STATUS_READING -> 25
            InteractionType.STATUS_ON_HOLD -> -25
            InteractionType.STATUS_PLAN_TO_READ -> 10
            InteractionType.UNDO_COMPLETED -> -100
            InteractionType.UNDO_DROPPED -> 150
        }
        return (baseScore * weight).toInt()
    }

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

        val preferred = affinities
            .filter { it.score >= 0.5f }
            .sortedByDescending { it.score * it.confidence }

        val avoided = affinities
            .filter { it.dropRate > 0.5f && it.novelCount >= 2 }
            .sortedByDescending { it.completionRate }

        val diversityScore = calculateDiversity(preferred)
        val totalNovels = preferences.maxOfOrNull { it.novelCount } ?: 0

        return UserTasteProfile(
            preferredTags = preferred,
            avoidedTags = avoided,
            diversityScore = diversityScore,
            preferredRatingMin = null,
            sampleSize = totalNovels
        )
    }

    private fun calculateConfidence(novelCount: Int, readingTimeSeconds: Long): Float {
        val novelFactor = (novelCount / 10f).coerceAtMost(1f)
        val timeFactor = (readingTimeSeconds / (10 * 60 * 60f)).coerceAtMost(1f)
        return ((novelFactor + timeFactor) / 2).coerceIn(0.1f, 1f)
    }

    private fun calculateDiversity(preferences: List<TagAffinity>): Float {
        if (preferences.size <= 1) return 0f

        val scores = preferences.map { it.score }
        val mean = scores.average().toFloat()
        val variance = scores.map { (it - mean) * (it - mean) }.average().toFloat()

        return (1f - variance).coerceIn(0f, 1f)
    }

    // ================================================================
    // TAG INTERACTION RECORDING
    // ================================================================

    /**
     * Record a tag interaction from onboarding or explicit user action.
     * Use this for direct user feedback (likes/dislikes, onboarding selections).
     */
    suspend fun recordTagInteraction(
        tag: TagCategory,
        interactionType: InteractionType,
        weight: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        recordInteractionInternal(
            tagName = tag.name,
            interactionType = interactionType,
            weight = weight
        )
    }

    /**
     * Record multiple tag interactions at once (e.g., from onboarding)
     */
    suspend fun recordTagInteractions(
        tags: List<TagCategory>,
        interactionType: InteractionType,
        weight: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        tags.forEach { tag ->
            recordInteractionInternal(
                tagName = tag.name,
                interactionType = interactionType,
                weight = weight
            )
        }
    }

    /**
     * Internal method to record an interaction with full metadata control
     */
    private suspend fun recordInteractionInternal(
        tagName: String,
        interactionType: InteractionType,
        weight: Float = 1.0f,
        novelDelta: Int = 0,
        chaptersDelta: Int = 0,
        timeDelta: Long = 0,
        completedDelta: Int = 0,
        droppedDelta: Int = 0
    ) {
        val existing = recommendationDao.getPreference(tagName)
        val scoreChange = getScoreChange(interactionType, weight)

        if (existing != null) {
            val newScore = (existing.affinityScore + scoreChange).coerceIn(0, 1000)
            recommendationDao.updatePreference(
                tag = tagName,
                score = newScore,
                novelDelta = novelDelta,
                chaptersDelta = chaptersDelta,
                timeDelta = timeDelta,
                completedDelta = completedDelta,
                droppedDelta = droppedDelta
            )
        } else {
            val initialScore = (500 + scoreChange).coerceIn(0, 1000)
            recommendationDao.insertPreference(
                UserPreferenceEntity(
                    tag = tagName,
                    affinityScore = initialScore,
                    novelCount = novelDelta.coerceAtLeast(0),
                    chaptersRead = chaptersDelta.coerceAtLeast(0),
                    readingTimeSeconds = timeDelta.coerceAtLeast(0),
                    completedCount = completedDelta.coerceAtLeast(0),
                    droppedCount = droppedDelta.coerceAtLeast(0)
                )
            )
        }
    }

    // ================================================================
    // BEHAVIOR-BASED PREFERENCE UPDATES
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
            recordInteractionInternal(
                tagName = tag.name,
                interactionType = InteractionType.ADDED_TO_LIBRARY,
                novelDelta = 1
            )
        }

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

        // Calculate weight based on reading intensity
        val chapterWeight = chaptersRead.coerceIn(1, 10).toFloat()
        val isLongRead = readingTimeSeconds >= LONG_READ_THRESHOLD_SECONDS

        for (tag in tags) {
            // Record chapter reads with weighted score
            recordInteractionInternal(
                tagName = tag.name,
                interactionType = InteractionType.READ_CHAPTER,
                weight = chapterWeight,
                chaptersDelta = chaptersRead,
                timeDelta = readingTimeSeconds
            )

            // Bonus for long reading sessions
            if (isLongRead) {
                val longReadWeight = (readingTimeSeconds / LONG_READ_THRESHOLD_SECONDS.toFloat())
                    .coerceAtMost(3f)
                recordInteractionInternal(
                    tagName = tag.name,
                    interactionType = InteractionType.LONG_READ,
                    weight = longReadWeight
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

        // Map status to interaction type and deltas
        val (interactionType, completedDelta, droppedDelta) = when (newStatus) {
            ReadingStatus.COMPLETED -> Triple(InteractionType.COMPLETED_NOVEL, 1, 0)
            ReadingStatus.DROPPED -> Triple(InteractionType.DROPPED_NOVEL, 0, 1)
            ReadingStatus.READING -> Triple(InteractionType.STATUS_READING, 0, 0)
            ReadingStatus.ON_HOLD -> Triple(InteractionType.STATUS_ON_HOLD, 0, 0)
            ReadingStatus.PLAN_TO_READ -> Triple(InteractionType.STATUS_PLAN_TO_READ, 0, 0)
        }

        // Undo old status effect if applicable
        val undoInteraction = when (oldStatus) {
            ReadingStatus.COMPLETED -> InteractionType.UNDO_COMPLETED to (-1 to 0)
            ReadingStatus.DROPPED -> InteractionType.UNDO_DROPPED to (0 to -1)
            else -> null
        }

        for (tag in tags) {
            // Apply undo if needed
            undoInteraction?.let { (undoType, deltas) ->
                recordInteractionInternal(
                    tagName = tag.name,
                    interactionType = undoType,
                    completedDelta = deltas.first,
                    droppedDelta = deltas.second
                )
            }

            // Apply new status
            recordInteractionInternal(
                tagName = tag.name,
                interactionType = interactionType,
                completedDelta = completedDelta,
                droppedDelta = droppedDelta
            )
        }

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
            recordInteractionInternal(
                tagName = tag.name,
                interactionType = InteractionType.REMOVED_FROM_LIBRARY,
                novelDelta = -1,
                completedDelta = if (wasCompleted) -1 else 0
            )
        }

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
        recommendationDao.clearAllPreferences()

        val libraryRepository = RepositoryProvider.getLibraryRepository()
        val offlineRepository = RepositoryProvider.getOfflineRepository()
        val historyRepository = RepositoryProvider.getHistoryRepository()

        val library = libraryRepository.getLibrary()
        val tagScores = mutableMapOf<TagCategory, MutablePreferenceData>()
        val libraryItemsWithDetails = mutableListOf<LibraryItemWithDetails>()

        for (item in library) {
            val details = offlineRepository.getNovelDetails(item.novel.url) ?: continue
            val tags = TagNormalizer.normalizeAll(details.tags ?: emptyList())

            val readCount = historyRepository.getReadChapterCount(item.novel.url)
            val totalChapters = details.chapters.size
            val progress = if (totalChapters > 0) readCount.toFloat() / totalChapters else 0f

            libraryItemsWithDetails.add(
                LibraryItemWithDetails(
                    novelUrl = item.novel.url,
                    author = details.author,
                    status = item.readingStatus,
                    chaptersRead = readCount,
                    readingTimeSeconds = 0L,
                    progressPercent = progress
                )
            )

            for (tag in tags) {
                val data = tagScores.getOrPut(tag) { MutablePreferenceData() }
                data.novelCount++
                data.chaptersRead += readCount

                // Calculate score using interaction type scoring
                val statusScore = when (item.readingStatus) {
                    ReadingStatus.COMPLETED -> {
                        data.completedCount++
                        getScoreChange(InteractionType.COMPLETED_NOVEL) +
                                getScoreChange(InteractionType.ADDED_TO_LIBRARY)
                    }
                    ReadingStatus.DROPPED -> {
                        data.droppedCount++
                        getScoreChange(InteractionType.DROPPED_NOVEL) +
                                getScoreChange(InteractionType.ADDED_TO_LIBRARY)
                    }
                    ReadingStatus.READING -> {
                        getScoreChange(InteractionType.STATUS_READING) +
                                getScoreChange(InteractionType.ADDED_TO_LIBRARY) +
                                (progress * getScoreChange(InteractionType.READ_CHAPTER) * 10).toInt()
                    }
                    ReadingStatus.ON_HOLD -> {
                        getScoreChange(InteractionType.STATUS_ON_HOLD) +
                                getScoreChange(InteractionType.ADDED_TO_LIBRARY)
                    }
                    ReadingStatus.PLAN_TO_READ -> {
                        getScoreChange(InteractionType.STATUS_PLAN_TO_READ) +
                                getScoreChange(InteractionType.ADDED_TO_LIBRARY)
                    }
                }

                // Add reading chapter bonus
                val chapterBonus = (readCount * getScoreChange(InteractionType.READ_CHAPTER))
                    .coerceAtMost(200)

                data.scoreAccumulator += statusScore + chapterBonus
            }
        }

        val preferences = tagScores.map { (tag, data) ->
            // Calculate average score and normalize to 0-1000 range
            val avgScore = if (data.novelCount > 0) {
                (500 + data.scoreAccumulator / data.novelCount).coerceIn(0, 1000)
            } else {
                500
            }

            UserPreferenceEntity(
                tag = tag.name,
                affinityScore = avgScore,
                novelCount = data.novelCount,
                chaptersRead = data.chaptersRead,
                readingTimeSeconds = data.readingTimeSeconds,
                completedCount = data.completedCount,
                droppedCount = data.droppedCount
            )
        }

        recommendationDao.insertPreferences(preferences)
        authorPreferenceManager.recalculateAllPreferences(libraryItemsWithDetails)
    }

    /**
     * Clear all preferences (for testing or reset)
     */
    suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        recommendationDao.clearAllPreferences()
        authorPreferenceManager.clearAllPreferences()
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