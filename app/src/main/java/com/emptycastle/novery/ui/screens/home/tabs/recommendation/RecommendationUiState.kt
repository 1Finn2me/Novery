package com.emptycastle.novery.ui.screens.home.tabs.recommendation

import com.emptycastle.novery.recommendation.model.LibrarySourceNovel
import com.emptycastle.novery.recommendation.model.ProfileMaturity
import com.emptycastle.novery.recommendation.model.Recommendation
import com.emptycastle.novery.recommendation.model.RecommendationGroup

data class RecommendationUiState(
    // Loading states
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSeeding: Boolean = false,
    val error: String? = null,

    // Seeding progress
    val seedingProgress: SeedingProgress? = null,

    // Recommendation data
    val recommendationGroups: List<RecommendationGroup> = emptyList(),

    // Pool info
    val poolSize: Int = 0,
    val poolByProvider: Map<String, Int> = emptyMap(),

    // User profile info
    val profileMaturity: ProfileMaturity = ProfileMaturity.NEW,
    val topPreferences: List<String> = emptyList(),
    val novelsInProfile: Int = 0,

    // UI state
    val preferredProvider: String? = null,
    val showCrossProvider: Boolean = true,

    // Track disabled provider count for UI messaging
    val disabledProviderCount: Int = 0,

    // Cache info
    val lastRefreshTime: Long = 0,
    val isCacheStale: Boolean = true,

    // === NEW: Library-based recommendation source ===

    /** All novels in library that can be used as recommendation sources */
    val librarySourceNovels: List<LibrarySourceNovel> = emptyList(),

    /** Currently selected source novel for "Because You Read" */
    val selectedSourceNovel: LibrarySourceNovel? = null,

    /** Recommendations based on the selected source novel */
    val sourceNovelRecommendations: List<Recommendation> = emptyList(),

    /** Loading state for source-specific recommendations */
    val isLoadingSourceRecommendations: Boolean = false,

    /** Whether the library source selector is expanded */
    val isSourceSelectorExpanded: Boolean = false

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

    val hasDisabledProviders: Boolean
        get() = disabledProviderCount > 0

    /** Whether we have library novels to choose from */
    val hasLibrarySources: Boolean
        get() = librarySourceNovels.isNotEmpty()

    /** The default source (last read novel) */
    val defaultSourceNovel: LibrarySourceNovel?
        get() = librarySourceNovels.maxByOrNull { it.lastReadAt }

    /** Other available sources (excluding selected) */
    val otherSourceNovels: List<LibrarySourceNovel>
        get() = librarySourceNovels.filter { it.novel.url != selectedSourceNovel?.novel?.url }
}

data class SeedingProgress(
    val currentProvider: String,
    val currentIndex: Int,
    val totalProviders: Int,
    val novelsDiscovered: Int = 0
)