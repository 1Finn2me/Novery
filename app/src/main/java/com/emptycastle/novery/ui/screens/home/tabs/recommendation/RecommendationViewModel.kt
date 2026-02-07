package com.emptycastle.novery.ui.screens.home.tabs.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.local.entity.BlockedAuthorEntity
import com.emptycastle.novery.data.local.entity.HiddenNovelEntity
import com.emptycastle.novery.data.local.entity.HideReason
import com.emptycastle.novery.data.local.entity.TagFilterType
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.recommendation.RecommendationEngine
import com.emptycastle.novery.recommendation.TagNormalizer
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.LibrarySourceNovel
import com.emptycastle.novery.recommendation.model.NovelVector
import com.emptycastle.novery.recommendation.model.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val historyRepository = RepositoryProvider.getHistoryRepository()

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

    // Track the last known disabled providers to detect changes
    private var lastKnownDisabledProviders: Set<String> = emptySet()

    // Cache for source-specific recommendations
    private val sourceRecommendationCache = mutableMapOf<String, List<Recommendation>>()

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

        // Observe app settings for disabled provider changes
        viewModelScope.launch {
            preferencesManager.appSettings
                .map { it.disabledProviders }
                .distinctUntilChanged()
                .collect { disabledProviders ->
                    val previousDisabled = lastKnownDisabledProviders
                    lastKnownDisabledProviders = disabledProviders

                    _uiState.update { it.copy(disabledProviderCount = disabledProviders.size) }

                    if (previousDisabled != disabledProviders && _uiState.value.hasRecommendations) {
                        Log.d(TAG, "Disabled providers changed, refreshing recommendations")
                        loadRecommendations(forceRefresh = true)
                    }
                }
        }

        // Observe library changes to update source novels
        viewModelScope.launch {
            libraryRepository.observeLibrary().collect { libraryItems ->
                updateLibrarySources(libraryItems)
            }
        }

        checkAndSeed()
    }

    // ================================================================
    // LIBRARY SOURCE MANAGEMENT (NEW)
    // ================================================================

    /**
     * Update the list of library novels that can be used as recommendation sources
     */
    private suspend fun updateLibrarySources(libraryItems: List<LibraryItem>) {
        // Get read chapter counts for each novel
        val sourceNovels = libraryItems
            .filter { it.lastReadPosition != null } // Only novels that have been read
            .map { item ->
                val chaptersRead = getChaptersReadCount(item.novel.url)
                LibrarySourceNovel.fromLibraryItem(item, chaptersRead)
            }
            .sortedByDescending { it.lastReadAt } // Most recently read first

        val currentSelected = _uiState.value.selectedSourceNovel
        val defaultSource = sourceNovels.firstOrNull()

        // If no source is selected, select the most recently read
        val newSelected = when {
            currentSelected != null && sourceNovels.any { it.novel.url == currentSelected.novel.url } -> {
                // Keep current selection if still valid
                sourceNovels.find { it.novel.url == currentSelected.novel.url }
            }
            else -> defaultSource
        }

        _uiState.update { state ->
            state.copy(
                librarySourceNovels = sourceNovels,
                selectedSourceNovel = newSelected
            )
        }

        // Load recommendations for the selected source if changed
        if (newSelected != null && currentSelected?.novel?.url != newSelected.novel.url) {
            loadRecommendationsForSource(newSelected.novel.url)
        }
    }

    /**
     * Get the number of chapters read for a novel
     */
    private suspend fun getChaptersReadCount(novelUrl: String): Int {
        return try {
            val dao = RepositoryProvider.getDatabase().historyDao()
            dao.getReadChapterCount(novelUrl)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Select a library novel as the source for recommendations
     */
    fun selectSourceNovel(sourceNovel: LibrarySourceNovel) {
        if (_uiState.value.selectedSourceNovel?.novel?.url == sourceNovel.novel.url) {
            return // Already selected
        }

        _uiState.update { it.copy(
            selectedSourceNovel = sourceNovel,
            isSourceSelectorExpanded = false
        )}

        viewModelScope.launch {
            loadRecommendationsForSource(sourceNovel.novel.url)
        }
    }

    /**
     * Toggle the source selector expansion
     */
    fun toggleSourceSelector() {
        _uiState.update { it.copy(isSourceSelectorExpanded = !it.isSourceSelectorExpanded) }
    }

    /**
     * Collapse the source selector
     */
    fun collapseSourceSelector() {
        _uiState.update { it.copy(isSourceSelectorExpanded = false) }
    }

    /**
     * Load recommendations for a specific source novel
     */
    private suspend fun loadRecommendationsForSource(novelUrl: String) {
        // Check cache first
        sourceRecommendationCache[novelUrl]?.let { cached ->
            _uiState.update { it.copy(sourceNovelRecommendations = cached) }
            return
        }

        _uiState.update { it.copy(isLoadingSourceRecommendations = true) }

        try {
            val disabledProviders = getDisabledProviders()

            val recommendations = recommendationEngine.getSimilarTo(
                novelUrl = novelUrl,
                limit = 15,
                excludeLibrary = true,
                disabledProviders = disabledProviders
            )

            // Cache the results
            sourceRecommendationCache[novelUrl] = recommendations

            _uiState.update { it.copy(
                sourceNovelRecommendations = recommendations,
                isLoadingSourceRecommendations = false
            )}

        } catch (e: Exception) {
            Log.e(TAG, "Error loading recommendations for source $novelUrl", e)
            _uiState.update { it.copy(
                sourceNovelRecommendations = emptyList(),
                isLoadingSourceRecommendations = false
            )}
        }
    }

    /**
     * Clear the recommendation cache (call when data changes significantly)
     */
    fun clearRecommendationCache() {
        sourceRecommendationCache.clear()
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
                    loadLibrarySources()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndSeed", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to initialize: ${e.message}")
                }
            }
        }
    }

    /**
     * Load library sources on init
     */
    private suspend fun loadLibrarySources() {
        val libraryItems = libraryRepository.getLibrary()
        updateLibrarySources(libraryItems)
    }

    private suspend fun seedDiscoveryPool() {
        _uiState.update { it.copy(isSeeding = true, isLoading = true) }

        try {
            val disabledProviders = getDisabledProviders()

            val result = discoveryManager.seedDiscoveryPool(
                disabledProviders = disabledProviders
            ) { provider, current, total ->
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
            loadLibrarySources()

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

                // Clear cache since data changed
                clearRecommendationCache()

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
            clearRecommendationCache()
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
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun blockAuthor(authorNormalized: String, displayName: String) {
        viewModelScope.launch {
            userFilterManager.blockAuthor(authorNormalized, displayName)
            removeNovelsByAuthor(authorNormalized)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun unblockAuthor(authorNormalized: String) {
        viewModelScope.launch {
            userFilterManager.unblockAuthor(authorNormalized)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllHiddenNovels() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllHiddenNovels()
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllBlockedAuthors() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllBlockedAuthors()
            clearRecommendationCache()
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
                }.filter { it.recommendations.isNotEmpty() },
                // Also remove from source recommendations
                sourceNovelRecommendations = state.sourceNovelRecommendations.filter {
                    it.novel.url != novelUrl
                }
            )
        }

        // Update cache
        sourceRecommendationCache.forEach { (key, recommendations) ->
            sourceRecommendationCache[key] = recommendations.filter { it.novel.url != novelUrl }
        }
    }

    private fun removeNovelsByAuthor(authorNormalized: String) {
        _uiState.update { state ->
            state.copy(
                recommendationGroups = state.recommendationGroups.map { group ->
                    group.copy(
                        recommendations = group.recommendations.filter { rec ->
                            true // TODO: Filter by author when we have author info in Recommendation
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
                val disabledProviders = getDisabledProviders()
                val preferredProvider = determinePreferredProvider()

                val config = RecommendationEngine.RecommendationConfig(
                    maxPerGroup = 12,
                    minScore = 0.3f,
                    includeCrossProvider = _uiState.value.showCrossProvider,
                    preferredProvider = preferredProvider,
                    disabledProviders = disabledProviders
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
                        poolSize = poolSize,
                        disabledProviderCount = disabledProviders.size
                    )
                }

                loadUserProfile()

                // Also refresh source recommendations if we have a selected source
                _uiState.value.selectedSourceNovel?.let { source ->
                    loadRecommendationsForSource(source.novel.url)
                }

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
        clearRecommendationCache()
        loadRecommendations(forceRefresh = true)
    }

    fun refreshDiscoveryPool() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val disabledProviders = getDisabledProviders()
                val newCount = discoveryManager.refreshPool(disabledProviders = disabledProviders)
                Log.d(TAG, "Discovered $newCount new novels")

                if (newCount > 0) {
                    val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                    Log.d(TAG, "Enhanced ${enhancementResult.novelsEnhanced} new novels")
                }

                val poolSize = discoveryManager.getPoolSize()
                _uiState.update { it.copy(poolSize = poolSize) }

                clearRecommendationCache()
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
        clearRecommendationCache()
        loadRecommendations(forceRefresh = true)
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun getDisabledProviders(): Set<String> {
        return preferencesManager.appSettings.value.disabledProviders
    }

    private suspend fun determinePreferredProvider(): String? {
        return try {
            val disabledProviders = getDisabledProviders()
            val library = libraryRepository.getLibrary()
            library
                .filter { it.novel.apiName !in disabledProviders }
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
        // Could track which recommendations are clicked for analytics
    }
}