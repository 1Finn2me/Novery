package com.emptycastle.novery.ui.screens.home.tabs.recommendation

import com.emptycastle.novery.recommendation.model.ProfileMaturity
import com.emptycastle.novery.recommendation.model.RecommendationGroup

data class RecommendationUiState(
    // Loading states
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSeeding: Boolean = false,  // NEW
    val error: String? = null,

    // Seeding progress - NEW
    val seedingProgress: SeedingProgress? = null,

    // Recommendation data
    val recommendationGroups: List<RecommendationGroup> = emptyList(),

    // Pool info - NEW
    val poolSize: Int = 0,
    val poolByProvider: Map<String, Int> = emptyMap(),

    // User profile info
    val profileMaturity: ProfileMaturity = ProfileMaturity.NEW,
    val topPreferences: List<String> = emptyList(),
    val novelsInProfile: Int = 0,

    // UI state
    val preferredProvider: String? = null,
    val showCrossProvider: Boolean = true,

    // Cache info
    val lastRefreshTime: Long = 0,
    val isCacheStale: Boolean = true
) {
    val hasRecommendations: Boolean
        get() = recommendationGroups.any { it.recommendations.isNotEmpty() }

    val totalRecommendations: Int
        get() = recommendationGroups.sumOf { it.recommendations.size }

    val needsSeeding: Boolean
        get() = poolSize < 50 && !isSeeding

    val needsMoreData: Boolean
        get() = profileMaturity == ProfileMaturity.NEW && poolSize >= 50

    val isProfileDeveloping: Boolean
        get() = profileMaturity == ProfileMaturity.DEVELOPING
}

data class SeedingProgress(
    val currentProvider: String,
    val currentIndex: Int,
    val totalProviders: Int,
    val novelsDiscovered: Int = 0
)