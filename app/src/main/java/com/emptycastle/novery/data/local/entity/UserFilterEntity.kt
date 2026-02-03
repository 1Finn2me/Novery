package com.emptycastle.novery.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User's filter preferences for tags
 */
@Entity(
    tableName = "user_tag_filters",
    indices = [Index(value = ["filterType"])]
)
data class UserTagFilterEntity(
    @PrimaryKey
    val tag: String,  // TagCategory.name

    val filterType: String,  // "BLOCKED", "BOOSTED", "NEUTRAL"

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Novels the user has hidden/dismissed
 */
@Entity(
    tableName = "hidden_novels",
    indices = [Index(value = ["hiddenAt"])]
)
data class HiddenNovelEntity(
    @PrimaryKey
    val novelUrl: String,

    val novelName: String,

    val reason: String,  // "NOT_INTERESTED", "ALREADY_READ", "DISLIKED"

    val hiddenAt: Long = System.currentTimeMillis()
)

/**
 * Authors the user has blocked
 */
@Entity(
    tableName = "blocked_authors",
    indices = [Index(value = ["blockedAt"])]
)
data class BlockedAuthorEntity(
    @PrimaryKey
    val authorNormalized: String,

    val displayName: String,

    val blockedAt: Long = System.currentTimeMillis()
)

enum class TagFilterType {
    BLOCKED,    // Never show novels with this tag
    BOOSTED,    // Prioritize novels with this tag
    NEUTRAL     // Default behavior (not stored)
}

enum class HideReason {
    NOT_INTERESTED,
    ALREADY_READ_ELSEWHERE,
    DISLIKED
}