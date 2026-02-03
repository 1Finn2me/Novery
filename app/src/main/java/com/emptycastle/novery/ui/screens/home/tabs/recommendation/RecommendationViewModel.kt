package com.emptycastle.novery.ui.screens.home.tabs.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.local.entity.BlockedAuthorEntity
import com.emptycastle.novery.data.local.entity.HiddenNovelEntity
import com.emptycastle.novery.data.local.entity.HideReason
import com.emptycastle.novery.data.local.entity.TagFilterType
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.recommendation.RecommendationEngine
import com.emptycastle.novery.recommendation.TagNormalizer
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.NovelVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RecommendationViewModel"
private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

class RecommendationViewModel : ViewModel() {

    private val recommendationEngine = RepositoryProvider.getRecommendationEngine()
    private val userPreferenceManager = RepositoryProvider.getUserPreferenceManager()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val discoveryManager = RepositoryProvider.getDiscoveryManager()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val userFilterManager = RepositoryProvider.getUserFilterManager()
    private val tagEnhancementManager = RepositoryProvider.getTagEnhancementManager()
    private val authorPreferenceManager = RepositoryProvider.getAuthorPreferenceManager()

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private val _tagFilters = MutableStateFlow<Map<TagCategory, TagFilterType>>(emptyMap())
    val tagFilters: StateFlow<Map<TagCategory, TagFilterType>> = _tagFilters.asStateFlow()

    private val _hiddenNovels = MutableStateFlow<List<HiddenNovelEntity>>(emptyList())
    val hiddenNovels: StateFlow<List<HiddenNovelEntity>> = _hiddenNovels.asStateFlow()

    private val _blockedAuthors = MutableStateFlow<List<BlockedAuthorEntity>>(emptyList())
    val blockedAuthors: StateFlow<List<BlockedAuthorEntity>> = _blockedAuthors.asStateFlow()

    private val _favoriteAuthors = MutableStateFlow<List<String>>(emptyList())
    val favoriteAuthors: StateFlow<List<String>> = _favoriteAuthors.asStateFlow()

    init {
        // Observe tag filters
        viewModelScope.launch {
            userFilterManager.observeTagFilters().collect { filters ->
                _tagFilters.value = filters
            }
        }

        // Observe hidden novels
        viewModelScope.launch {
            userFilterManager.observeHiddenNovels().collect { novels ->
                _hiddenNovels.value = novels
            }
        }

        // Observe blocked authors
        viewModelScope.launch {
            userFilterManager.observeBlockedAuthors().collect { authors ->
                _blockedAuthors.value = authors
            }
        }

        checkAndSeed()
    }

    // ================================================================
    // INITIALIZATION & SEEDING
    // ================================================================

    private fun checkAndSeed() {
        viewModelScope.launch {
            try {
                val poolSize = discoveryManager.getPoolSize()
                _uiState.update { it.copy(poolSize = poolSize) }

                if (discoveryManager.needsSeeding()) {
                    seedDiscoveryPool()
                } else {
                    enhanceTagsIfNeeded()
                    loadUserProfile()
                    loadRecommendations()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndSeed", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to initialize: ${e.message}")
                }
            }
        }
    }

    private suspend fun seedDiscoveryPool() {
        _uiState.update { it.copy(isSeeding = true, isLoading = true) }

        try {
            val result = discoveryManager.seedDiscoveryPool { provider, current, total ->
                _uiState.update {
                    it.copy(
                        seedingProgress = SeedingProgress(
                            currentProvider = provider,
                            currentIndex = current,
                            totalProviders = total
                        )
                    )
                }
            }

            Log.d(TAG, "Seeding complete: ${result.totalDiscovered} novels discovered")

            val newPoolSize = discoveryManager.getPoolSize()
            val poolByProvider = discoveryManager.getPoolSizeByProvider()

            _uiState.update {
                it.copy(
                    isSeeding = false,
                    seedingProgress = null,
                    poolSize = newPoolSize,
                    poolByProvider = poolByProvider
                )
            }

            val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
            Log.d(TAG, "Tag enhancement: ${enhancementResult.novelsEnhanced} novels, " +
                    "${enhancementResult.tagsAdded} tags added")

            logTagCoverageStats()

            loadUserProfile()
            loadRecommendations()

        } catch (e: Exception) {
            Log.e(TAG, "Error seeding discovery pool", e)
            _uiState.update {
                it.copy(
                    isSeeding = false,
                    isLoading = false,
                    seedingProgress = null,
                    error = "Failed to discover novels: ${e.message}"
                )
            }
        }
    }

    private suspend fun enhanceTagsIfNeeded() {
        try {
            val stats = tagEnhancementManager.getTagCoverageStats()

            if (stats.tagCoveragePercent < 70f) {
                Log.d(TAG, "Tag coverage is ${stats.tagCoveragePercent.toInt()}%, running enhancement...")

                val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                Log.d(TAG, "Tag enhancement: ${enhancementResult.novelsEnhanced} novels, " +
                        "${enhancementResult.tagsAdded} tags added")
            } else {
                Log.d(TAG, "Tag coverage is ${stats.tagCoveragePercent.toInt()}%, no enhancement needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/running tag enhancement", e)
        }
    }

    private suspend fun logTagCoverageStats() {
        try {
            val stats = tagEnhancementManager.getTagCoverageStats()
            Log.d(TAG, """
                Tag Coverage Stats:
                - Total novels: ${stats.totalNovels}
                - With tags: ${stats.withTags} (${stats.tagCoveragePercent.toInt()}%)
                - With synopsis: ${stats.withSynopsis}
                - Top tags: ${stats.topTags.take(5).map { it.first.name }}
            """.trimIndent())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tag coverage stats", e)
        }
    }

    fun getTagCoverageStats() {
        viewModelScope.launch {
            logTagCoverageStats()
        }
    }

    fun forceTagEnhancement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val result = tagEnhancementManager.enhanceNovelsWithSynopsis(forceReprocess = true)
                Log.d(TAG, "Force tag enhancement: ${result.novelsEnhanced} novels, " +
                        "${result.tagsAdded} tags added")

                logTagCoverageStats()

                loadRecommendations(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error in force tag enhancement", e)
                _uiState.update {
                    it.copy(isRefreshing = false, error = "Enhancement failed: ${e.message}")
                }
            }
        }
    }

    // ================================================================
    // USER FILTERS (Tags, Hiding, Blocking)
    // ================================================================

    fun setTagFilter(tag: TagCategory, filterType: TagFilterType) {
        viewModelScope.launch {
            userFilterManager.setTagFilter(tag, filterType)
            loadRecommendations(forceRefresh = true)
        }
    }

    fun hideNovel(novelUrl: String, novelName: String) {
        viewModelScope.launch {
            userFilterManager.hideNovel(novelUrl, novelName, HideReason.NOT_INTERESTED)
            removeNovelFromRecommendations(novelUrl)
        }
    }

    fun unhideNovel(novelUrl: String) {
        viewModelScope.launch {
            userFilterManager.unhideNovel(novelUrl)
            loadRecommendations(forceRefresh = true)
        }
    }

    fun blockAuthor(authorNormalized: String, displayName: String) {
        viewModelScope.launch {
            userFilterManager.blockAuthor(authorNormalized, displayName)
            removeNovelsByAuthor(authorNormalized)
            loadRecommendations(forceRefresh = true)
        }
    }

    fun unblockAuthor(authorNormalized: String) {
        viewModelScope.launch {
            userFilterManager.unblockAuthor(authorNormalized)
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllHiddenNovels() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllHiddenNovels()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllBlockedAuthors() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllBlockedAuthors()
            loadRecommendations(forceRefresh = true)
        }
    }

    suspend fun getAuthorForNovel(novelUrl: String): Pair<String, String>? {
        return try {
            val discovered = RepositoryProvider.getDatabase().recommendationDao()
                .getDiscoveredNovel(novelUrl)

            if (discovered?.author != null) {
                val normalized = NovelVector.normalizeAuthor(discovered.author)
                if (normalized != null) {
                    return normalized to discovered.author
                }
            }

            val details = RepositoryProvider.getDatabase().offlineDao()
                .getNovelDetails(novelUrl)

            if (details?.author != null) {
                val normalized = NovelVector.normalizeAuthor(details.author)
                if (normalized != null) {
                    return normalized to details.author
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun removeNovelFromRecommendations(novelUrl: String) {
        _uiState.update { state ->
            state.copy(
                recommendationGroups = state.recommendationGroups.map { group ->
                    group.copy(
                        recommendations = group.recommendations.filter {
                            it.novel.url != novelUrl
                        }
                    )
                }.filter { it.recommendations.isNotEmpty() }
            )
        }
    }

    private fun removeNovelsByAuthor(authorNormalized: String) {
        _uiState.update { state ->
            state.copy(
                recommendationGroups = state.recommendationGroups.map { group ->
                    group.copy(
                        recommendations = group.recommendations.filter { rec ->
                            true
                        }
                    )
                }
            )
        }
    }

    // ================================================================
    // PROFILE LOADING
    // ================================================================

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = userPreferenceManager.getUserProfile()

                val topPrefs = profile.getTopPreferences(5).map { tag ->
                    TagNormalizer.getDisplayName(tag)
                }

                // Load favorite authors
                val favAuthors = authorPreferenceManager.getFavoriteAuthors(5)
                    .map { it.displayName }

                _uiState.update {
                    it.copy(
                        profileMaturity = profile.maturity,
                        topPreferences = topPrefs,
                        novelsInProfile = profile.sampleSize
                    )
                }

                _favoriteAuthors.value = favAuthors

            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
            }
        }
    }

    // Add method to get author stats
    fun getAuthorStats() {
        viewModelScope.launch {
            try {
                val stats = authorPreferenceManager.getAuthorStats()
                Log.d(TAG, """
                    Author Stats:
                    - Total authors tracked: ${stats.totalAuthors}
                    - Favorites: ${stats.favoriteCount}
                    - Liked: ${stats.likedCount}
                    - Avg affinity: ${stats.averageAffinity}
                """.trimIndent())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting author stats", e)
            }
        }
    }

    // ================================================================
    // RECOMMENDATIONS LOADING
    // ================================================================

    fun loadRecommendations(forceRefresh: Boolean = false) {
        val state = _uiState.value

        if (state.isSeeding) return

        val now = System.currentTimeMillis()
        val cacheAge = now - state.lastRefreshTime
        val isCacheValid = cacheAge < CACHE_DURATION_MS && state.hasRecommendations

        if (isCacheValid && !forceRefresh) {
            _uiState.update { it.copy(isLoading = false, isCacheStale = false) }
            return
        }

        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val preferredProvider = determinePreferredProvider()

                val config = RecommendationEngine.RecommendationConfig(
                    maxPerGroup = 12,
                    minScore = 0.3f,
                    includeCrossProvider = _uiState.value.showCrossProvider,
                    preferredProvider = preferredProvider
                )

                val groups = recommendationEngine.generateRecommendations(config)
                val poolSize = discoveryManager.getPoolSize()

                _uiState.update {
                    it.copy(
                        recommendationGroups = groups,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        preferredProvider = preferredProvider,
                        lastRefreshTime = System.currentTimeMillis(),
                        isCacheStale = false,
                        poolSize = poolSize
                    )
                }

                loadUserProfile()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommendations", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load recommendations"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadRecommendations(forceRefresh = true)
    }

    fun refreshDiscoveryPool() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val newCount = discoveryManager.refreshPool()
                Log.d(TAG, "Discovered $newCount new novels")

                if (newCount > 0) {
                    val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                    Log.d(TAG, "Enhanced ${enhancementResult.novelsEnhanced} new novels")
                }

                val poolSize = discoveryManager.getPoolSize()
                _uiState.update { it.copy(poolSize = poolSize) }

                loadRecommendations(forceRefresh = true)

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing discovery pool", e)
                _uiState.update {
                    it.copy(isRefreshing = false, error = "Refresh failed: ${e.message}")
                }
            }
        }
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    fun toggleCrossProvider() {
        _uiState.update { it.copy(showCrossProvider = !it.showCrossProvider) }
        loadRecommendations(forceRefresh = true)
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private suspend fun determinePreferredProvider(): String? {
        return try {
            val library = libraryRepository.getLibrary()
            library
                .groupingBy { it.novel.apiName }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        } catch (e: Exception) {
            null
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onRecommendationClicked(novelUrl: String) {
        // Could track which recommendations are clicked
    }
}