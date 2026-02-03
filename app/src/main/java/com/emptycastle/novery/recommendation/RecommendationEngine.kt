package com.emptycastle.novery.recommendation

import android.util.Log
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.dao.RecommendationDao
import com.emptycastle.novery.data.local.entity.AuthorPreferenceEntity
import com.emptycastle.novery.data.local.entity.DiscoveredNovelEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.repository.LibraryRepository
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.recommendation.model.NovelVector
import com.emptycastle.novery.recommendation.model.Recommendation
import com.emptycastle.novery.recommendation.model.RecommendationGroup
import com.emptycastle.novery.recommendation.model.RecommendationType
import com.emptycastle.novery.recommendation.model.ScoreBreakdown
import com.emptycastle.novery.recommendation.model.UserTasteProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "RecommendationEngine"

/**
 * Main recommendation engine that generates personalized recommendations.
 * Ensures variety across groups by tracking used novels.
 */
class RecommendationEngine(
    private val userPreferenceManager: UserPreferenceManager,
    private val libraryRepository: LibraryRepository,
    private val offlineDao: OfflineDao,
    private val recommendationDao: RecommendationDao,
    private val userFilterManager: UserFilterManager,
    private val authorPreferenceManager: AuthorPreferenceManager
) {

    data class RecommendationConfig(
        val maxPerGroup: Int = 10,
        val minScore: Float = 0.25f,
        val sameProviderBoost: Float = 1.15f,
        val includeCrossProvider: Boolean = true,
        val preferredProvider: String? = null,
        /** Allow some overlap between groups (0 = no overlap, 0.3 = 30% can overlap) */
        val allowedOverlapRatio: Float = 0.2f
    )

    // ================================================================
    // MAIN RECOMMENDATION GENERATION
    // ================================================================

    suspend fun generateRecommendations(
        config: RecommendationConfig = RecommendationConfig()
    ): List<RecommendationGroup> = withContext(Dispatchers.IO) {

        val userProfile = userPreferenceManager.getUserProfile()
        val libraryUrls = libraryRepository.getLibrary().map { it.novel.url }.toSet()

        // Get filter state
        val filterState = userFilterManager.getFilterState()

        // Get author data for scoring
        val likedAuthors = authorPreferenceManager.getLikedAuthors(50)
        val authorAffinities = likedAuthors.associate {
            it.authorNormalized to (it.affinityScore / 1000f)
        }
        val favoriteAuthors = likedAuthors
            .filter { it.isFavorite }
            .map { it.authorNormalized }
            .toSet()

        Log.d(TAG, "Filters: ${filterState.blockedTags.size} blocked tags, " +
                "${filterState.hiddenNovelUrls.size} hidden novels, " +
                "${filterState.blockedAuthors.size} blocked authors")

        Log.d(TAG, "Generating recommendations - ${likedAuthors.size} liked authors, ${favoriteAuthors.size} favorites")

        // Pass filterState to loadAllCandidates
        val allCandidates = loadAllCandidates(libraryUrls, filterState)

        Log.d(TAG, "Generating recommendations - Profile: ${userProfile.sampleSize} novels, Library: ${libraryUrls.size}")

        // Separate candidates by quality (has tags vs no tags)
        val (withTags, withoutTags) = allCandidates.partition { it.tags.isNotEmpty() }

        Log.d(TAG, "Candidates: ${allCandidates.size} total, ${withTags.size} with tags, ${withoutTags.size} without tags")

        if (allCandidates.isEmpty()) {
            return@withContext emptyList()
        }

        val groups = mutableListOf<RecommendationGroup>()

        // Track used novels to prevent duplicates across groups
        val usedNovelUrls = mutableSetOf<String>()
        val maxOverlapPerGroup = (config.maxPerGroup * config.allowedOverlapRatio).toInt()

        // 1. FOR YOU - Only if user has preferences AND we have tagged novels
        if (userProfile.sampleSize >= 1 && withTags.isNotEmpty()) {
            generateForYouGroup(
                userProfile, withTags, config, usedNovelUrls, maxOverlapPerGroup,
                authorAffinities, favoriteAuthors, filterState
            )?.let {
                groups.add(it)
                usedNovelUrls.addAll(it.recommendations.map { r -> r.novel.url })
            }
        }

        // 2. FROM AUTHORS YOU LIKE
        if (favoriteAuthors.isNotEmpty() || likedAuthors.size >= 2) {
            generateFromAuthorsYouLikeGroup(
                allCandidates, likedAuthors, config, usedNovelUrls, maxOverlapPerGroup, filterState
            )?.let {
                groups.add(it)
                usedNovelUrls.addAll(it.recommendations.map { r -> r.novel.url })
            }
        }

        // 3. BECAUSE YOU READ - Only if user has read something
        val recentlyRead = libraryRepository.getLibrary()
            .filter { it.lastReadPosition != null }
            .sortedByDescending { it.lastReadPosition?.timestamp ?: 0 }
            .take(3)

        if (recentlyRead.isNotEmpty()) {
            generateSimilarToRecentGroup(
                userProfile, allCandidates, recentlyRead, config, usedNovelUrls, maxOverlapPerGroup,
                authorAffinities, favoriteAuthors, filterState
            )?.let {
                groups.add(it)
                usedNovelUrls.addAll(it.recommendations.map { r -> r.novel.url })
            }
        }

        // 4. TOP RATED - Use different strategies based on data availability
        generateTopRatedGroup(
            userProfile, allCandidates, config, filterState, usedNovelUrls, maxOverlapPerGroup
        )?.let {
            groups.add(it)
            usedNovelUrls.addAll(it.recommendations.map { r -> r.novel.url })
        }

        // 5. BY GENRE - Create genre-specific groups from available tags
        if (withTags.isNotEmpty()) {
            generateGenreGroups(
                withTags, config, usedNovelUrls, maxOverlapPerGroup
            ).forEach { group ->
                groups.add(group)
                usedNovelUrls.addAll(group.recommendations.map { r -> r.novel.url })
            }
        }

        // 6. DISCOVER NEW SOURCE - Cross-provider discovery
        val preferredProvider = config.preferredProvider
            ?: allCandidates.groupingBy { it.providerName }.eachCount().maxByOrNull { it.value }?.key

        if (preferredProvider != null && config.includeCrossProvider) {
            generateDiscoverNewSourceGroup(
                allCandidates, preferredProvider, config, usedNovelUrls, maxOverlapPerGroup
            )?.let {
                groups.add(it)
                usedNovelUrls.addAll(it.recommendations.map { r -> r.novel.url })
            }
        }

        // 7. TRENDING - Different novels per provider
        generateProviderTrendingGroups(
            allCandidates, config, usedNovelUrls, maxOverlapPerGroup
        ).forEach { group ->
            groups.add(group)
            usedNovelUrls.addAll(group.recommendations.map { r -> r.novel.url })
        }

        // 8. NEW RELEASES - Ongoing novels (not completed)
        generateNewReleasesGroup(
            allCandidates, config, usedNovelUrls, maxOverlapPerGroup
        )?.let {
            groups.add(it)
        }

        Log.d(TAG, "Generated ${groups.size} groups with ${usedNovelUrls.size} unique novels")
        groups
    }

    // ================================================================
    // CANDIDATE LOADING
    // ================================================================

    private suspend fun loadAllCandidates(
        excludeUrls: Set<String>,
        filterState: UserFilterManager.FilterState? = null
    ): List<NovelVector> {
        val candidates = mutableListOf<NovelVector>()
        val seenUrls = mutableSetOf<String>()

        // Priority 1: NovelDetailsEntity
        offlineDao.getAllNovelDetails().forEach { entity ->
            if (entity.url !in excludeUrls && entity.url !in seenUrls) {
                entityToVector(entity)?.let { vector ->
                    // Apply filters if provided
                    val shouldInclude = filterState == null || !userFilterManager.shouldFilter(vector, filterState)
                    if (shouldInclude) {
                        candidates.add(vector)
                        seenUrls.add(entity.url)
                    }
                }
            }
        }

        // Priority 2: DiscoveredNovelEntity
        recommendationDao.getAllDiscoveredNovels().forEach { entity ->
            if (entity.url !in excludeUrls && entity.url !in seenUrls) {
                discoveredToVector(entity)?.let { vector ->
                    // Apply filters if provided
                    val shouldInclude = filterState == null || !userFilterManager.shouldFilter(vector, filterState)
                    if (shouldInclude) {
                        candidates.add(vector)
                        seenUrls.add(entity.url)
                    }
                }
            }
        }

        return candidates
    }


    private fun discoveredToVector(entity: DiscoveredNovelEntity): NovelVector? {
        // Start with normalized provider tags
        var tags = TagNormalizer.normalizeAll(entity.tags)

        // If no/few tags, extract from synopsis
        if (tags.size < 3) {
            val extractedTags = SynopsisTagExtractor.extractTags(entity.synopsis, maxTags = 8)
            tags = tags + extractedTags
        }

        return NovelVector(
            url = entity.url,
            name = entity.name,
            providerName = entity.apiName,
            tags = tags,
            rawTags = entity.tags,
            authorNormalized = NovelVector.normalizeAuthor(entity.author),
            rating = entity.rating,
            chapterCount = 0,
            isCompleted = entity.status?.lowercase() == "completed",
            synopsisKeywords = NovelVector.extractKeywords(entity.synopsis),
            posterUrl = entity.posterUrl
        )
    }

    private fun entityToVector(entity: NovelDetailsEntity): NovelVector? {
        // Start with normalized provider tags
        var tags = entity.tags?.let { TagNormalizer.normalizeAll(it) } ?: emptySet()

        // If no/few tags, extract from synopsis
        if (tags.size < 3) {
            val extractedTags = SynopsisTagExtractor.extractTags(entity.synopsis, maxTags = 8)
            tags = tags + extractedTags
        }

        return NovelVector(
            url = entity.url,
            name = entity.name,
            providerName = entity.apiName.ifBlank { extractProviderFromUrl(entity.url) },
            tags = tags,
            rawTags = entity.tags ?: emptyList(),
            authorNormalized = NovelVector.normalizeAuthor(entity.author),
            rating = entity.rating,
            chapterCount = entity.chapterCount,
            isCompleted = entity.status?.lowercase() == "completed",
            synopsisKeywords = NovelVector.extractKeywords(entity.synopsis),
            posterUrl = entity.posterUrl
        )
    }

    private fun extractProviderFromUrl(url: String): String {
        return when {
            url.contains("novelfire") -> "NovelFire"
            url.contains("royalroad") -> "Royal Road"
            url.contains("webnovel") -> "Webnovel"
            url.contains("novelbin") -> "NovelBin"
            url.contains("libread") -> "LibRead"
            url.contains("novelsonline") -> "NovelsOnline"
            else -> "Unknown"
        }
    }

    // ================================================================
    // GROUP GENERATORS - Each uses different selection strategy
    // ================================================================

    /**
     * FOR YOU - Based on user's tag preferences
     */
    private fun generateForYouGroup(
        userProfile: UserTasteProfile,
        candidates: List<NovelVector>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int,
        authorAffinities: Map<String, Float>,
        favoriteAuthors: Set<String>,
        filterState: UserFilterManager.FilterState
    ): RecommendationGroup? {
        // Create scorer with author data
        val scorer = NovelScorer(
            userProfile = userProfile,
            authorAffinities = authorAffinities,
            favoriteAuthors = favoriteAuthors
        )

        val available = candidates.filter { it.url !in usedUrls }
        val fromUsed = candidates.filter { it.url in usedUrls }.take(maxOverlap)
        val pool = available + fromUsed

        if (pool.isEmpty()) return null

        val scored = scorer.scoreAndRank(pool, config.preferredProvider, config.maxPerGroup * 2)

        val recommendations = scored
            .filter { (_, breakdown) -> breakdown.total >= config.minScore }
            .take(config.maxPerGroup)
            .map { (novel, breakdown) ->
                createRecommendation(
                    novel = novel,
                    type = RecommendationType.FOR_YOU,
                    breakdown = breakdown,
                    userProfile = userProfile,
                    filterState = filterState,
                    preferredProvider = config.preferredProvider
                )
            }

        if (recommendations.isEmpty()) return null

        return RecommendationGroup(
            type = RecommendationType.FOR_YOU,
            title = "For You",
            subtitle = "Based on your reading history",
            recommendations = recommendations
        )
    }

    /**
     * FROM AUTHORS YOU LIKE - Novels by authors the user has enjoyed
     */
    private fun generateFromAuthorsYouLikeGroup(
        candidates: List<NovelVector>,
        likedAuthors: List<AuthorPreferenceEntity>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int,
        filterState: UserFilterManager.FilterState
    ): RecommendationGroup? {
        if (likedAuthors.isEmpty()) return null

        // Get normalized names of liked authors
        val likedAuthorNames = likedAuthors
            .filter { it.affinityScore >= 600 }
            .map { it.authorNormalized }
            .toSet()

        if (likedAuthorNames.isEmpty()) return null

        // Find novels by liked authors that aren't in library
        val novelsByLikedAuthors = candidates
            .filter { novel ->
                novel.authorNormalized != null &&
                        novel.authorNormalized in likedAuthorNames &&
                        novel.url !in usedUrls &&
                        !userFilterManager.shouldFilter(novel, filterState)
            }
            .sortedWith(
                compareByDescending<NovelVector> {
                    likedAuthors.find { a -> a.authorNormalized == it.authorNormalized }?.affinityScore ?: 0
                }.thenByDescending { it.rating ?: 0 }
            )
            .take(config.maxPerGroup)

        if (novelsByLikedAuthors.size < 2) return null

        // Create author name lookup for display
        val authorDisplayNames = likedAuthors.associate { it.authorNormalized to it.displayName }

        val recommendations = novelsByLikedAuthors.map { novel ->
            val authorName = authorDisplayNames[novel.authorNormalized] ?: novel.authorNormalized ?: "Unknown"
            val authorEntity = likedAuthors.find { it.authorNormalized == novel.authorNormalized }

            val breakdown = ScoreBreakdown(
                authorMatch = (authorEntity?.affinityScore ?: 500) / 1000f,
                ratingScore = (novel.rating ?: 0) / 1000f,
                tagSimilarity = 0.5f,
                userPreferenceMatch = 0.6f
            )

            Recommendation(
                novel = Novel(
                    name = novel.name,
                    url = novel.url,
                    posterUrl = novel.posterUrl,
                    rating = novel.rating,
                    apiName = novel.providerName
                ),
                score = breakdown.total,
                type = RecommendationType.FROM_AUTHORS_YOU_LIKE,
                reason = "By $authorName",
                scoreBreakdown = breakdown,
                isCrossProvider = false
            )
        }

        // Generate title based on author count
        val authorCount = novelsByLikedAuthors
            .mapNotNull { it.authorNormalized }
            .distinct()
            .size

        val title = when {
            authorCount == 1 -> {
                val author = authorDisplayNames[novelsByLikedAuthors.first().authorNormalized]
                "More from $author"
            }
            authorCount <= 3 -> "From Authors You Love"
            else -> "From Your Favorite Authors"
        }

        return RecommendationGroup(
            type = RecommendationType.FROM_AUTHORS_YOU_LIKE,
            title = title,
            subtitle = "Other works by authors you've enjoyed",
            recommendations = recommendations
        )
    }

    /**
     * BECAUSE YOU READ - Similar to recently read novels
     */
    private suspend fun generateSimilarToRecentGroup(
        userProfile: UserTasteProfile,
        candidates: List<NovelVector>,
        recentlyRead: List<com.emptycastle.novery.data.repository.LibraryItem>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int,
        authorAffinities: Map<String, Float>,
        favoriteAuthors: Set<String>,
        filterState: UserFilterManager.FilterState
    ): RecommendationGroup? {
        val sourceNovel = recentlyRead.first()
        val sourceVector = loadNovelVector(sourceNovel.novel.url) ?: return null

        val available = candidates.filter { it.url !in usedUrls && it.url != sourceVector.url }
        val fromUsed = candidates.filter { it.url in usedUrls && it.url != sourceVector.url }.take(maxOverlap)
        val pool = available + fromUsed

        if (pool.isEmpty()) return null

        val similar = SimilarityCalculator.findSimilar(
            target = sourceVector,
            candidates = pool,
            limit = config.maxPerGroup,
            minSimilarity = 0.15f // Lower threshold for similarity
        )

        if (similar.isEmpty()) return null

        val recommendations = similar.map { (novel, _) ->
            val breakdown = ScoreBreakdown(
                tagSimilarity = SimilarityCalculator.calculateTagSimilarity(sourceVector.tags, novel.tags),
                authorMatch = SimilarityCalculator.calculateAuthorSimilarity(sourceVector.authorNormalized, novel.authorNormalized),
                ratingScore = SimilarityCalculator.calculateRatingSimilarity(sourceVector.rating, novel.rating),
                providerBoost = if (novel.providerName == sourceVector.providerName) 1f else 0f,
                synopsisMatch = SimilarityCalculator.calculateSynopsisSimilarity(sourceVector.synopsisKeywords, novel.synopsisKeywords)
            )

            createRecommendation(
                novel = novel,
                type = RecommendationType.BECAUSE_YOU_READ,
                breakdown = breakdown,
                userProfile = userProfile,
                filterState = filterState,
                preferredProvider = config.preferredProvider,
                sourceNovelUrl = sourceVector.url,
                sourceNovelName = sourceVector.name
            )
        }

        val displayName = if (sourceNovel.novel.name.length > 25) {
            sourceNovel.novel.name.take(22) + "..."
        } else {
            sourceNovel.novel.name
        }

        return RecommendationGroup(
            type = RecommendationType.BECAUSE_YOU_READ,
            title = "Because you read $displayName",
            subtitle = "Similar stories you might enjoy",
            recommendations = recommendations,
            sourceNovel = sourceNovel.novel
        )
    }

    /**
     * TOP RATED - Highest rated novels not yet shown
     */
    private fun generateTopRatedGroup(
        userProfile: UserTasteProfile,
        candidates: List<NovelVector>,
        config: RecommendationConfig,
        filterState: UserFilterManager.FilterState,
        usedUrls: Set<String>,
        maxOverlap: Int
    ): RecommendationGroup? {
        val available = candidates.filter { it.url !in usedUrls && it.rating != null }
        val fromUsed = candidates.filter { it.url in usedUrls && it.rating != null }.take(maxOverlap)
        val pool = (available + fromUsed).sortedByDescending { it.rating ?: 0 }

        if (pool.isEmpty()) return null

        // Take top rated that haven't been used much
        val topRated = pool.take(config.maxPerGroup)

        val recommendations = topRated.map { novel ->
            val breakdown = ScoreBreakdown(
                ratingScore = (novel.rating ?: 0) / 1000f,
                popularityScore = 0.7f
            )
            createRecommendation(
                novel = novel,
                type = RecommendationType.TOP_RATED_FOR_YOU,
                breakdown = breakdown,
                userProfile = userProfile,
                filterState = filterState,
                preferredProvider = config.preferredProvider
            )
        }

        return RecommendationGroup(
            type = RecommendationType.TOP_RATED_FOR_YOU,
            title = "Highest Rated",
            subtitle = "Top-rated stories across all sources",
            recommendations = recommendations
        )
    }

    /**
     * GENRE GROUPS - Create groups for each major genre found in candidates
     */
    private fun generateGenreGroups(
        candidates: List<NovelVector>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int
    ): List<RecommendationGroup> {
        // Count tag occurrences
        val tagCounts = mutableMapOf<TagNormalizer.TagCategory, Int>()
        candidates.forEach { novel ->
            novel.tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }

        // Get top 2-3 genres with enough novels
        val popularGenres = tagCounts.entries
            .filter { it.value >= 5 } // At least 5 novels in genre
            .sortedByDescending { it.value }
            .take(2) // Limit to 2 genre groups
            .map { it.key }

        val groups = mutableListOf<RecommendationGroup>()
        val genreUsedUrls = usedUrls.toMutableSet()

        for (genre in popularGenres) {
            val genreCandidates = candidates
                .filter { genre in it.tags }
                .filter { it.url !in genreUsedUrls || genreUsedUrls.size < usedUrls.size + maxOverlap }
                .sortedByDescending { it.rating ?: 0 }
                .take(config.maxPerGroup)

            if (genreCandidates.size >= 3) { // Need at least 3 for a group
                val recommendations = genreCandidates.map { novel ->
                    val breakdown = ScoreBreakdown(
                        tagSimilarity = 0.8f,
                        ratingScore = (novel.rating ?: 0) / 1000f
                    )
                    Recommendation(
                        novel = Novel(
                            name = novel.name,
                            url = novel.url,
                            posterUrl = novel.posterUrl,
                            rating = novel.rating,
                            apiName = novel.providerName
                        ),
                        score = breakdown.total,
                        type = RecommendationType.TRENDING_IN_YOUR_GENRES,
                        reason = novel.rating?.let { "%.1f★".format(it / 200f) } ?: "Popular ${TagNormalizer.getDisplayName(genre)}",
                        scoreBreakdown = breakdown,
                        isCrossProvider = false
                    )
                }

                groups.add(
                    RecommendationGroup(
                        type = RecommendationType.TRENDING_IN_YOUR_GENRES,
                        title = "Best in ${TagNormalizer.getDisplayName(genre)}",
                        subtitle = "Top ${TagNormalizer.getDisplayName(genre)} novels",
                        recommendations = recommendations
                    )
                )

                genreUsedUrls.addAll(genreCandidates.map { it.url })
            }
        }

        return groups
    }

    /**
     * DISCOVER NEW SOURCE - Novels from other providers
     */
    private fun generateDiscoverNewSourceGroup(
        candidates: List<NovelVector>,
        preferredProvider: String,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int
    ): RecommendationGroup? {
        val crossProvider = candidates
            .filter { it.providerName != preferredProvider }
            .filter { it.url !in usedUrls }
            .sortedByDescending { it.rating ?: 0 }
            .take(config.maxPerGroup)

        if (crossProvider.isEmpty()) return null

        val recommendations = crossProvider.map { novel ->
            val breakdown = ScoreBreakdown(
                ratingScore = (novel.rating ?: 0) / 1000f,
                providerBoost = 0f,
                popularityScore = 0.6f
            )
            Recommendation(
                novel = Novel(
                    name = novel.name,
                    url = novel.url,
                    posterUrl = novel.posterUrl,
                    rating = novel.rating,
                    apiName = novel.providerName
                ),
                score = breakdown.total,
                type = RecommendationType.DISCOVER_NEW_SOURCE,
                reason = "Popular on ${novel.providerName}",
                scoreBreakdown = breakdown,
                isCrossProvider = true
            )
        }

        return RecommendationGroup(
            type = RecommendationType.DISCOVER_NEW_SOURCE,
            title = "Discover New Sources",
            subtitle = "Great novels from other platforms",
            recommendations = recommendations
        )
    }

    /**
     * PROVIDER TRENDING - One trending group per provider (if we have multiple)
     */
    private fun generateProviderTrendingGroups(
        candidates: List<NovelVector>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int
    ): List<RecommendationGroup> {
        val byProvider = candidates.groupBy { it.providerName }

        // Only create provider groups if we have multiple providers with enough novels
        if (byProvider.size < 2) return emptyList()

        val groups = mutableListOf<RecommendationGroup>()
        val providerUsedUrls = usedUrls.toMutableSet()

        // Take top 2 providers by novel count
        val topProviders = byProvider.entries
            .sortedByDescending { it.value.size }
            .take(2)

        for ((provider, providerCandidates) in topProviders) {
            val available = providerCandidates
                .filter { it.url !in providerUsedUrls }
                .sortedWith(
                    compareByDescending<NovelVector> { it.rating ?: 0 }
                        .thenBy { it.name } // Secondary sort for consistency
                )
                .take(config.maxPerGroup)

            if (available.size >= 3) {
                val recommendations = available.map { novel ->
                    val breakdown = ScoreBreakdown(
                        ratingScore = (novel.rating ?: 0) / 1000f,
                        providerBoost = 1f,
                        popularityScore = 0.7f
                    )
                    Recommendation(
                        novel = Novel(
                            name = novel.name,
                            url = novel.url,
                            posterUrl = novel.posterUrl,
                            rating = novel.rating,
                            apiName = novel.providerName
                        ),
                        score = breakdown.total,
                        type = RecommendationType.TRENDING_IN_YOUR_GENRES,
                        reason = novel.rating?.let { "%.1f★".format(it / 200f) } ?: "Popular",
                        scoreBreakdown = breakdown,
                        isCrossProvider = false
                    )
                }

                groups.add(
                    RecommendationGroup(
                        type = RecommendationType.TRENDING_IN_YOUR_GENRES,
                        title = "Trending on $provider",
                        subtitle = "Popular novels from $provider",
                        recommendations = recommendations
                    )
                )

                providerUsedUrls.addAll(available.map { it.url })
            }
        }

        return groups
    }

    /**
     * NEW RELEASES - Ongoing (not completed) novels
     */
    private fun generateNewReleasesGroup(
        candidates: List<NovelVector>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int
    ): RecommendationGroup? {
        val newReleases = candidates
            .filter { !it.isCompleted }
            .filter { it.url !in usedUrls }
            .sortedByDescending { it.rating ?: 0 }
            .take(config.maxPerGroup)

        if (newReleases.size < 3) return null

        val recommendations = newReleases.map { novel ->
            val breakdown = ScoreBreakdown(
                ratingScore = (novel.rating ?: 0) / 1000f,
                popularityScore = 0.6f
            )
            Recommendation(
                novel = Novel(
                    name = novel.name,
                    url = novel.url,
                    posterUrl = novel.posterUrl,
                    rating = novel.rating,
                    apiName = novel.providerName
                ),
                score = breakdown.total,
                type = RecommendationType.NEW_FOR_YOU,
                reason = "Ongoing series",
                scoreBreakdown = breakdown,
                isCrossProvider = false
            )
        }

        return RecommendationGroup(
            type = RecommendationType.NEW_FOR_YOU,
            title = "Ongoing Series",
            subtitle = "Active stories still being updated",
            recommendations = recommendations
        )
    }

    // ================================================================
    // SINGLE NOVEL RECOMMENDATIONS
    // ================================================================

    suspend fun getSimilarTo(
        novelUrl: String,
        limit: Int = 10,
        excludeLibrary: Boolean = true
    ): List<Recommendation> = withContext(Dispatchers.IO) {
        val sourceVector = loadNovelVector(novelUrl) ?: return@withContext emptyList()
        val userProfile = userPreferenceManager.getUserProfile()
        val filterState = userFilterManager.getFilterState()

        val libraryUrls = if (excludeLibrary) {
            libraryRepository.getLibrary().map { it.novel.url }.toSet()
        } else emptySet()

        val allCandidates = loadAllCandidates(libraryUrls + novelUrl, filterState)

        val similar = SimilarityCalculator.findSimilar(
            target = sourceVector,
            candidates = allCandidates,
            limit = limit,
            minSimilarity = 0.15f
        )

        similar.map { (novel, _) ->
            val breakdown = ScoreBreakdown(
                tagSimilarity = SimilarityCalculator.calculateTagSimilarity(sourceVector.tags, novel.tags),
                authorMatch = SimilarityCalculator.calculateAuthorSimilarity(sourceVector.authorNormalized, novel.authorNormalized),
                ratingScore = SimilarityCalculator.calculateRatingSimilarity(sourceVector.rating, novel.rating),
                providerBoost = if (novel.providerName == sourceVector.providerName) 1f else 0f
            )

            createRecommendation(
                novel = novel,
                type = RecommendationType.SIMILAR_TO,
                breakdown = breakdown,
                userProfile = userProfile,
                filterState = filterState
            )
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private suspend fun loadNovelVector(url: String): NovelVector? {
        offlineDao.getNovelDetails(url)?.let { return entityToVector(it) }
        recommendationDao.getDiscoveredNovel(url)?.let { return discoveredToVector(it) }
        return null
    }

    private fun createRecommendation(
        novel: NovelVector,
        type: RecommendationType,
        breakdown: ScoreBreakdown,
        userProfile: UserTasteProfile,
        filterState: UserFilterManager.FilterState,
        preferredProvider: String? = null,
        sourceNovelUrl: String? = null,
        sourceNovelName: String? = null,
        isCrossProvider: Boolean = false
    ): Recommendation {
        // Apply boost factor
        val boostFactor = userFilterManager.calculateBoostFactor(novel, filterState)
        val boostedScore = (breakdown.total * boostFactor).coerceAtMost(1f)

        val reason = generateReason(novel, type, breakdown, userProfile, sourceNovelName)

        return Recommendation(
            novel = Novel(
                name = novel.name,
                url = novel.url,
                posterUrl = novel.posterUrl,
                rating = novel.rating,
                apiName = novel.providerName
            ),
            score = boostedScore,
            type = type,
            reason = reason,
            scoreBreakdown = breakdown,
            sourceNovelUrl = sourceNovelUrl,
            sourceNovelName = sourceNovelName,
            isCrossProvider = isCrossProvider
        )
    }

    private fun generateReason(
        novel: NovelVector,
        type: RecommendationType,
        breakdown: ScoreBreakdown,
        userProfile: UserTasteProfile,
        sourceNovelName: String? = null
    ): String {
        val matchingTags = novel.tags
            .filter { tag -> userProfile.preferredTags.any { it.tag == tag } }
            .take(2)
            .map { TagNormalizer.getDisplayName(it) }

        val ratingText = novel.rating?.let { "%.1f★".format(it / 200f) }

        return when (type) {
            RecommendationType.FOR_YOU -> {
                when {
                    matchingTags.isNotEmpty() -> "Matches your love of ${matchingTags.joinToString(" & ")}"
                    ratingText != null -> "$ratingText on ${novel.providerName}"
                    else -> "Recommended for you"
                }
            }
            RecommendationType.SIMILAR_TO, RecommendationType.BECAUSE_YOU_READ -> {
                when {
                    breakdown.authorMatch > 0.5f -> "Same author"
                    breakdown.tagSimilarity > 0.5f && matchingTags.isNotEmpty() -> "Similar ${matchingTags.first()}"
                    sourceNovelName != null -> "Similar vibes"
                    else -> "You might like this"
                }
            }
            RecommendationType.TOP_RATED_FOR_YOU -> {
                ratingText ?: "Highly rated"
            }
            RecommendationType.DISCOVER_NEW_SOURCE -> {
                if (ratingText != null) "$ratingText on ${novel.providerName}" else "Popular on ${novel.providerName}"
            }
            RecommendationType.TRENDING_IN_YOUR_GENRES -> {
                ratingText ?: "Trending"
            }
            RecommendationType.NEW_FOR_YOU -> {
                "Ongoing series"
            }
            RecommendationType.FROM_AUTHORS_YOU_LIKE -> {
                "From an author you like"
            }
        }
    }
}