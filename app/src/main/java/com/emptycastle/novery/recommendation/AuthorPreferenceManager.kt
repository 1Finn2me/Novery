package com.emptycastle.novery.recommendation

import android.util.Log
import com.emptycastle.novery.data.local.dao.AuthorPreferenceDao
import com.emptycastle.novery.data.local.entity.AuthorPreferenceEntity
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.recommendation.model.NovelVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val TAG = "AuthorPreferenceManager"

/**
 * Manages author preference tracking and calculations.
 * Tracks which authors users read, complete, or drop.
 */
class AuthorPreferenceManager(
    private val authorDao: AuthorPreferenceDao
) {

    // ================================================================
    // AUTHOR AFFINITY LOOKUP
    // ================================================================

    /**
     * Get affinity score for an author (0.0 - 1.0)
     * Returns null if author is unknown
     */
    suspend fun getAuthorAffinity(authorNormalized: String?): Float? = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext null

        val author = authorDao.getAuthor(authorNormalized)
        author?.let { it.affinityScore / 1000f }
    }

    /**
     * Get affinity for multiple authors at once (for batch processing)
     */
    suspend fun getAuthorAffinities(
        authorNormalized: List<String>
    ): Map<String, Float> = withContext(Dispatchers.IO) {
        val all = authorDao.getAllAuthors()
        all.filter { it.authorNormalized in authorNormalized }
            .associate { it.authorNormalized to (it.affinityScore / 1000f) }
    }

    /**
     * Check if an author is liked (for filtering/boosting)
     */
    suspend fun isAuthorLiked(authorNormalized: String?): Boolean = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext false
        val author = authorDao.getAuthor(authorNormalized) ?: return@withContext false
        author.isLiked
    }

    /**
     * Check if an author is a favorite
     */
    suspend fun isAuthorFavorite(authorNormalized: String?): Boolean = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank()) return@withContext false
        val author = authorDao.getAuthor(authorNormalized) ?: return@withContext false
        author.isFavorite
    }

    /**
     * Get all favorite authors
     */
    suspend fun getFavoriteAuthors(limit: Int = 10): List<AuthorPreferenceEntity> = withContext(Dispatchers.IO) {
        authorDao.getFavoriteAuthors(limit)
    }

    /**
     * Get all liked authors
     */
    suspend fun getLikedAuthors(limit: Int = 20): List<AuthorPreferenceEntity> = withContext(Dispatchers.IO) {
        authorDao.getLikedAuthors(limit)
    }

    /**
     * Get top authors by affinity score
     */
    suspend fun getTopAuthors(minScore: Int = 600, limit: Int = 10): List<AuthorPreferenceEntity> = withContext(Dispatchers.IO) {
        authorDao.getTopAuthors(minScore, limit)
    }

    /**
     * Observe all authors (for UI)
     */
    fun observeAllAuthors(): Flow<List<AuthorPreferenceEntity>> {
        return authorDao.observeAllAuthors()
    }

    // ================================================================
    // TRACKING EVENTS
    // ================================================================

    /**
     * Track when user adds a novel to library
     */
    suspend fun onNovelAddedToLibrary(
        novelDetails: NovelDetails,
        novelUrl: String
    ) = withContext(Dispatchers.IO) {
        val authorDisplay = novelDetails.author ?: return@withContext
        val authorNormalized = NovelVector.normalizeAuthor(authorDisplay) ?: return@withContext

        val existing = authorDao.getAuthor(authorNormalized)

        if (existing != null) {
            // Update existing author
            val updated = existing.copy(
                novelsRead = existing.novelsRead + 1,
                affinityScore = (existing.affinityScore + 50).coerceAtMost(1000),
                updatedAt = System.currentTimeMillis()
            ).withNovelUrl(novelUrl)

            authorDao.updateAuthor(updated)
            Log.d(TAG, "Updated author ${authorDisplay}: novelsRead=${updated.novelsRead}, score=${updated.affinityScore}")
        } else {
            // Create new author preference
            val newAuthor = AuthorPreferenceEntity(
                authorNormalized = authorNormalized,
                displayName = authorDisplay,
                affinityScore = 550, // Slightly positive - they added it to library
                novelsRead = 1,
                novelUrlsInLibrary = novelUrl
            )
            authorDao.insertAuthor(newAuthor)
            Log.d(TAG, "Created author preference for ${authorDisplay}")
        }
    }

    /**
     * Track reading progress for an author
     */
    suspend fun onChaptersRead(
        authorNormalized: String?,
        authorDisplay: String?,
        chaptersRead: Int,
        readingTimeSeconds: Long
    ) = withContext(Dispatchers.IO) {
        if (authorNormalized.isNullOrBlank() || authorDisplay.isNullOrBlank()) return@withContext

        val existing = authorDao.getAuthor(authorNormalized)

        if (existing != null) {
            // Calculate score boost based on reading
            val scoreBoost = calculateReadingBoost(chaptersRead, readingTimeSeconds)
            authorDao.updateReadingProgress(
                authorNormalized = authorNormalized,
                chapters = chaptersRead,
                seconds = readingTimeSeconds,
                scoreBoost = scoreBoost
            )
            Log.d(TAG, "Updated reading progress for ${authorDisplay}: +$chaptersRead chapters, +$scoreBoost score")
        } else {
            // Create new author with reading data
            val newAuthor = AuthorPreferenceEntity(
                authorNormalized = authorNormalized,
                displayName = authorDisplay,
                affinityScore = 500 + calculateReadingBoost(chaptersRead, readingTimeSeconds),
                novelsRead = 1,
                totalChaptersRead = chaptersRead,
                totalReadingTimeSeconds = readingTimeSeconds
            )
            authorDao.insertAuthor(newAuthor)
        }
    }

    /**
     * Track status changes (completed, dropped, etc.)
     */
    suspend fun onStatusChanged(
        novelDetails: NovelDetails,
        novelUrl: String,
        newStatus: ReadingStatus,
        oldStatus: ReadingStatus?
    ) = withContext(Dispatchers.IO) {
        val authorDisplay = novelDetails.author ?: return@withContext
        val authorNormalized = NovelVector.normalizeAuthor(authorDisplay) ?: return@withContext

        val existing = authorDao.getAuthor(authorNormalized)
        if (existing == null) {
            // Create if doesn't exist
            onNovelAddedToLibrary(novelDetails, novelUrl)
            return@withContext
        }

        // Calculate changes based on status transition
        var completedDelta = 0
        var droppedDelta = 0
        var scoreDelta = 0

        // Undo old status effects
        when (oldStatus) {
            ReadingStatus.COMPLETED -> {
                completedDelta -= 1
                scoreDelta -= 100
            }
            ReadingStatus.DROPPED -> {
                droppedDelta -= 1
                scoreDelta += 100
            }
            else -> {}
        }

        // Apply new status effects
        when (newStatus) {
            ReadingStatus.COMPLETED -> {
                completedDelta += 1
                scoreDelta += 150 // Big boost for completing
            }
            ReadingStatus.DROPPED -> {
                droppedDelta += 1
                scoreDelta -= 100
            }
            ReadingStatus.READING -> {
                scoreDelta += 25 // Small boost for actively reading
            }
            ReadingStatus.ON_HOLD -> {
                scoreDelta -= 15 // Small penalty for putting on hold
            }
            ReadingStatus.PLAN_TO_READ -> {
                scoreDelta += 10 // Interest signal
            }
        }

        val updated = existing.copy(
            novelsCompleted = (existing.novelsCompleted + completedDelta).coerceAtLeast(0),
            novelsDropped = (existing.novelsDropped + droppedDelta).coerceAtLeast(0),
            affinityScore = (existing.affinityScore + scoreDelta).coerceIn(0, 1000),
            updatedAt = System.currentTimeMillis()
        )

        authorDao.updateAuthor(updated)
        Log.d(TAG, "Status changed for ${authorDisplay}: $oldStatus -> $newStatus, score: ${existing.affinityScore} -> ${updated.affinityScore}")
    }

    /**
     * Track when user removes a novel from library
     */
    suspend fun onNovelRemoved(
        novelDetails: NovelDetails,
        novelUrl: String,
        wasCompleted: Boolean
    ) = withContext(Dispatchers.IO) {
        val authorDisplay = novelDetails.author ?: return@withContext
        val authorNormalized = NovelVector.normalizeAuthor(authorDisplay) ?: return@withContext

        val existing = authorDao.getAuthor(authorNormalized) ?: return@withContext

        val updated = existing.copy(
            novelsRead = (existing.novelsRead - 1).coerceAtLeast(0),
            novelsCompleted = if (wasCompleted) (existing.novelsCompleted - 1).coerceAtLeast(0) else existing.novelsCompleted,
            // Don't change affinity much - removal is ambiguous
            updatedAt = System.currentTimeMillis()
        ).withoutNovelUrl(novelUrl)

        authorDao.updateAuthor(updated)
    }

    // ================================================================
    // FULL RECALCULATION
    // ================================================================

    /**
     * Recalculate all author preferences from library data.
     * Call this occasionally or when user requests it.
     */
    suspend fun recalculateAllPreferences(
        libraryItems: List<LibraryItemWithDetails>
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Recalculating author preferences from ${libraryItems.size} library items")

        // Clear existing
        authorDao.clearAllAuthors()

        // Group by author
        val byAuthor = mutableMapOf<String, MutableList<LibraryItemWithDetails>>()

        for (item in libraryItems) {
            val authorDisplay = item.author ?: continue
            val authorNormalized = NovelVector.normalizeAuthor(authorDisplay) ?: continue

            byAuthor.getOrPut(authorNormalized) { mutableListOf() }.add(item)
        }

        // Calculate preferences for each author
        for ((authorNormalized, items) in byAuthor) {
            val displayName = items.first().author ?: continue

            var totalScore = 500 // Start neutral
            var novelsRead = 0
            var novelsCompleted = 0
            var novelsDropped = 0
            var totalChapters = 0
            var totalTime = 0L
            val novelUrls = mutableListOf<String>()

            for (item in items) {
                novelsRead++
                novelUrls.add(item.novelUrl)
                totalChapters += item.chaptersRead
                totalTime += item.readingTimeSeconds

                // Score based on status
                when (item.status) {
                    ReadingStatus.COMPLETED -> {
                        novelsCompleted++
                        totalScore += 150
                    }
                    ReadingStatus.DROPPED -> {
                        novelsDropped++
                        totalScore -= 100
                    }
                    ReadingStatus.READING -> {
                        totalScore += 50 + (item.progressPercent * 50).toInt()
                    }
                    ReadingStatus.ON_HOLD -> {
                        totalScore += 25
                    }
                    ReadingStatus.PLAN_TO_READ -> {
                        totalScore += 30
                    }
                }

                // Score based on reading amount
                totalScore += calculateReadingBoost(item.chaptersRead, item.readingTimeSeconds)
            }

            val author = AuthorPreferenceEntity(
                authorNormalized = authorNormalized,
                displayName = displayName,
                affinityScore = totalScore.coerceIn(0, 1000),
                novelsRead = novelsRead,
                novelsCompleted = novelsCompleted,
                novelsDropped = novelsDropped,
                totalChaptersRead = totalChapters,
                totalReadingTimeSeconds = totalTime,
                novelUrlsInLibrary = novelUrls.joinToString(",")
            )

            authorDao.insertAuthor(author)
        }

        Log.d(TAG, "Recalculated preferences for ${byAuthor.size} authors")
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Calculate score boost based on reading activity
     */
    private fun calculateReadingBoost(chapters: Int, timeSeconds: Long): Int {
        // Up to +50 for chapters (more = better up to a point)
        val chapterBoost = (chapters * 2).coerceAtMost(50)

        // Up to +30 for time (shows engagement)
        val timeHours = timeSeconds / 3600f
        val timeBoost = (timeHours * 10).toInt().coerceAtMost(30)

        return chapterBoost + timeBoost
    }

    /**
     * Get statistics about author tracking
     */
    suspend fun getAuthorStats(): AuthorStats = withContext(Dispatchers.IO) {
        AuthorStats(
            totalAuthors = authorDao.getAuthorCount(),
            favoriteCount = authorDao.getFavoriteAuthors(100).size,
            likedCount = authorDao.getLikedAuthors(100).size,
            averageAffinity = authorDao.getAverageAffinityScore() ?: 0f,
            totalReadingTime = authorDao.getTotalReadingTime() ?: 0L,
            totalNovelsRead = authorDao.getTotalNovelsRead() ?: 0
        )
    }
}

/**
 * Simplified library item for recalculation
 */
data class LibraryItemWithDetails(
    val novelUrl: String,
    val author: String?,
    val status: ReadingStatus,
    val chaptersRead: Int,
    val readingTimeSeconds: Long,
    val progressPercent: Float
)

/**
 * Statistics about author tracking
 */
data class AuthorStats(
    val totalAuthors: Int,
    val favoriteCount: Int,
    val likedCount: Int,
    val averageAffinity: Float,
    val totalReadingTime: Long,
    val totalNovelsRead: Int
)