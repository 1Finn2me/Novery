// com/emptycastle/novery/domain/model/UserReview.kt
package com.emptycastle.novery.domain.model

/**
 * Enhanced review model with support for replies, spoilers, badges, etc.
 */
data class UserReview(
    // Core fields
    val id: String,
    val content: String,
    val title: String? = null,

    // User info
    val username: String? = null,
    val userId: String? = null,
    val avatarUrl: String? = null,
    val userLevel: Int? = null,
    val userBadgeUrl: String? = null,
    val userTier: String? = null,
    val isModerator: Boolean = false,
    val isAuthor: Boolean = false,

    // Rating
    val overallScore: Int? = null,
    val advancedScores: List<ReviewScore> = emptyList(),

    // Engagement
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val replyCount: Int = 0,
    val isLikedByAuthor: Boolean = false,

    // Timestamps
    val time: String? = null,
    val isEdited: Boolean = false,

    // Content flags
    val isSpoiler: Boolean = false,
    val isPinned: Boolean = false,
    val images: List<String> = emptyList(),

    // Reply threading
    val parentReviewId: String? = null,
    val parentUsername: String? = null,
    val parentContentPreview: String? = null,
    val replies: List<UserReview> = emptyList(),
    val hasMoreReplies: Boolean = false,

    // Provider-specific metadata for loading replies
    val providerData: Map<String, String> = emptyMap()
)

data class ReviewScore(
    val category: String,
    val score: Int  // 0-1000 scale
)

/**
 * Result wrapper for paginated review loading
 */
data class ReviewsResult(
    val reviews: List<UserReview>,
    val hasMore: Boolean,
    val nextCursor: String? = null,
    val totalCount: Int? = null
)

/**
 * Result for loading replies to a specific review
 */
data class RepliesResult(
    val replies: List<UserReview>,
    val hasMore: Boolean,
    val nextPage: Int? = null
)