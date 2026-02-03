// com/emptycastle/novery/data/local/dao/AuthorPreferenceDao.kt
package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emptycastle.novery.data.local.entity.AuthorPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorPreferenceDao {

    // ============ BASIC CRUD ============

    @Query("SELECT * FROM author_preferences WHERE authorNormalized = :authorNormalized")
    suspend fun getAuthor(authorNormalized: String): AuthorPreferenceEntity?

    @Query("SELECT * FROM author_preferences ORDER BY affinityScore DESC")
    suspend fun getAllAuthors(): List<AuthorPreferenceEntity>

    @Query("SELECT * FROM author_preferences ORDER BY affinityScore DESC")
    fun observeAllAuthors(): Flow<List<AuthorPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorPreferenceEntity)

    @Update
    suspend fun updateAuthor(author: AuthorPreferenceEntity)

    @Query("DELETE FROM author_preferences WHERE authorNormalized = :authorNormalized")
    suspend fun deleteAuthor(authorNormalized: String)

    @Query("DELETE FROM author_preferences")
    suspend fun clearAllAuthors()

    // ============ QUERIES FOR RECOMMENDATIONS ============

    /** Get top N authors by affinity score */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE affinityScore >= :minScore 
        ORDER BY affinityScore DESC 
        LIMIT :limit
    """)
    suspend fun getTopAuthors(minScore: Int = 600, limit: Int = 10): List<AuthorPreferenceEntity>

    /** Get favorite authors (high score + completed novels) */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE affinityScore >= 800 AND novelsCompleted >= 1
        ORDER BY affinityScore DESC, novelsCompleted DESC
        LIMIT :limit
    """)
    suspend fun getFavoriteAuthors(limit: Int = 5): List<AuthorPreferenceEntity>

    /** Get authors with liked status */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE affinityScore >= 600 AND novelsRead >= 1
        ORDER BY affinityScore DESC
        LIMIT :limit
    """)
    suspend fun getLikedAuthors(limit: Int = 20): List<AuthorPreferenceEntity>

    /** Get authors the user has dropped multiple novels from */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE novelsDropped >= 2 
        AND novelsDropped > (novelsRead * 0.5)
        ORDER BY novelsDropped DESC
    """)
    suspend fun getDislikedAuthors(): List<AuthorPreferenceEntity>

    /** Search authors by name */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE displayName LIKE '%' || :query || '%' 
        OR authorNormalized LIKE '%' || :query || '%'
        ORDER BY affinityScore DESC
        LIMIT :limit
    """)
    suspend fun searchAuthors(query: String, limit: Int = 20): List<AuthorPreferenceEntity>

    /** Get author count */
    @Query("SELECT COUNT(*) FROM author_preferences")
    suspend fun getAuthorCount(): Int

    /** Get authors with novels in library */
    @Query("""
        SELECT * FROM author_preferences 
        WHERE novelUrlsInLibrary != ''
        ORDER BY updatedAt DESC
    """)
    suspend fun getAuthorsWithNovelsInLibrary(): List<AuthorPreferenceEntity>

    // ============ STATISTICS ============

    /** Get total reading time across all authors */
    @Query("SELECT SUM(totalReadingTimeSeconds) FROM author_preferences")
    suspend fun getTotalReadingTime(): Long?

    /** Get total novels read across all authors */
    @Query("SELECT SUM(novelsRead) FROM author_preferences")
    suspend fun getTotalNovelsRead(): Int?

    /** Get average affinity score */
    @Query("SELECT AVG(affinityScore) FROM author_preferences WHERE novelsRead >= 1")
    suspend fun getAverageAffinityScore(): Float?

    // ============ UPDATES ============

    /** Increment novels read and update score */
    @Query("""
        UPDATE author_preferences SET 
            novelsRead = novelsRead + 1,
            affinityScore = CASE 
                WHEN :scoreDelta > 0 THEN MIN(affinityScore + :scoreDelta, 1000)
                ELSE MAX(affinityScore + :scoreDelta, 0)
            END,
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
    """)
    suspend fun incrementNovelsRead(
        authorNormalized: String,
        scoreDelta: Int = 50,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Increment completed count and boost score */
    @Query("""
        UPDATE author_preferences SET 
            novelsCompleted = novelsCompleted + 1,
            affinityScore = MIN(affinityScore + :scoreBoost, 1000),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
    """)
    suspend fun incrementCompleted(
        authorNormalized: String,
        scoreBoost: Int = 100,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Increment dropped count and reduce score */
    @Query("""
        UPDATE author_preferences SET 
            novelsDropped = novelsDropped + 1,
            affinityScore = MAX(affinityScore - :scorePenalty, 0),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
    """)
    suspend fun incrementDropped(
        authorNormalized: String,
        scorePenalty: Int = 100,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Update reading progress */
    @Query("""
        UPDATE author_preferences SET 
            totalChaptersRead = totalChaptersRead + :chapters,
            totalReadingTimeSeconds = totalReadingTimeSeconds + :seconds,
            affinityScore = MIN(affinityScore + :scoreBoost, 1000),
            updatedAt = :timestamp
        WHERE authorNormalized = :authorNormalized
    """)
    suspend fun updateReadingProgress(
        authorNormalized: String,
        chapters: Int,
        seconds: Long,
        scoreBoost: Int = 10,
        timestamp: Long = System.currentTimeMillis()
    )
}