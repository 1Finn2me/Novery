// com/emptycastle/novery/domain/model/UserReview.kt
package com.emptycastle.novery.domain.model

data class UserReview(
    val content: String,
    val title: String? = null,
    val username: String? = null,
    val time: String? = null,
    val avatarUrl: String? = null,
    val overallScore: Int? = null,
    val advancedScores: List<ReviewScore> = emptyList()
)

data class ReviewScore(
    val category: String,
    val score: Int
)