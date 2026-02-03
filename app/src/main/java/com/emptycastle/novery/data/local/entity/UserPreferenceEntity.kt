package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores user's affinity for each tag category.
 * Score is calculated from reading behavior.
 */
@Entity(
    tableName = "user_preferences",
    indices = [Index(value = ["tag"])]
)
data class UserPreferenceEntity(
    @PrimaryKey
    val tag: String,  // TagCategory.name

    /** Affinity score: 0-1000 scale, higher = user likes this more */
    val affinityScore: Int = 0,

    /** Number of novels with this tag the user has interacted with */
    val novelCount: Int = 0,

    /** Total chapters read in novels with this tag */
    val chaptersRead: Int = 0,

    /** Total reading time (seconds) spent on novels with this tag */
    val readingTimeSeconds: Long = 0,

    /** How many novels with this tag were completed */
    val completedCount: Int = 0,

    /** How many novels with this tag were dropped */
    val droppedCount: Int = 0,

    /** Last time this preference was updated */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Completion rate for novels with this tag (0.0 - 1.0)
     */
    val completionRate: Float
        get() = if (novelCount > 0) {
            completedCount.toFloat() / novelCount
        } else 0f

    /**
     * Drop rate for novels with this tag (0.0 - 1.0)
     */
    val dropRate: Float
        get() = if (novelCount > 0) {
            droppedCount.toFloat() / novelCount
        } else 0f
}