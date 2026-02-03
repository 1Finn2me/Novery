package com.emptycastle.novery.recommendation

import com.emptycastle.novery.data.local.dao.UserFilterDao
import com.emptycastle.novery.data.local.entity.BlockedAuthorEntity
import com.emptycastle.novery.data.local.entity.HiddenNovelEntity
import com.emptycastle.novery.data.local.entity.HideReason
import com.emptycastle.novery.data.local.entity.TagFilterType
import com.emptycastle.novery.data.local.entity.UserTagFilterEntity
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.NovelVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Manages user's content filters (blocked tags, hidden novels, blocked authors)
 */
class UserFilterManager(
    private val filterDao: UserFilterDao
) {

    // ================================================================
    // FILTER STATE
    // ================================================================

    data class FilterState(
        val blockedTags: Set<TagCategory> = emptySet(),
        val boostedTags: Set<TagCategory> = emptySet(),
        val hiddenNovelUrls: Set<String> = emptySet(),
        val blockedAuthors: Set<String> = emptySet()  // Normalized names
    )

    /**
     * Get current filter state for recommendation filtering
     */
    suspend fun getFilterState(): FilterState = withContext(Dispatchers.IO) {
        val blockedTagStrings = filterDao.getBlockedTags()
        val boostedTagStrings = filterDao.getBoostedTags()

        FilterState(
            blockedTags = blockedTagStrings.mapNotNull {
                runCatching { TagCategory.valueOf(it) }.getOrNull()
            }.toSet(),
            boostedTags = boostedTagStrings.mapNotNull {
                runCatching { TagCategory.valueOf(it) }.getOrNull()
            }.toSet(),
            hiddenNovelUrls = filterDao.getHiddenNovelUrls().toSet(),
            blockedAuthors = filterDao.getBlockedAuthors().toSet()
        )
    }

    /**
     * Check if a novel should be filtered out
     */
    fun shouldFilter(novel: NovelVector, filterState: FilterState): Boolean {
        // Check hidden novels
        if (novel.url in filterState.hiddenNovelUrls) return true

        // Check blocked authors
        if (novel.authorNormalized != null &&
            novel.authorNormalized in filterState.blockedAuthors) return true

        // Check blocked tags
        if (novel.tags.any { it in filterState.blockedTags }) return true

        return false
    }

    /**
     * Calculate boost factor for a novel based on boosted tags
     * Returns 1.0 for no boost, >1.0 for boosted
     */
    fun calculateBoostFactor(novel: NovelVector, filterState: FilterState): Float {
        val matchingBoostedTags = novel.tags.count { it in filterState.boostedTags }
        return when {
            matchingBoostedTags >= 3 -> 1.3f
            matchingBoostedTags == 2 -> 1.2f
            matchingBoostedTags == 1 -> 1.1f
            else -> 1.0f
        }
    }

    // ================================================================
    // TAG FILTER MANAGEMENT
    // ================================================================

    fun observeTagFilters(): Flow<Map<TagCategory, TagFilterType>> {
        return filterDao.observeAllTagFilters().map { filters ->
            filters.mapNotNull { entity ->
                val tag = runCatching { TagCategory.valueOf(entity.tag) }.getOrNull()
                val type = runCatching { TagFilterType.valueOf(entity.filterType) }.getOrNull()
                if (tag != null && type != null) tag to type else null
            }.toMap()
        }
    }

    suspend fun setTagFilter(tag: TagCategory, filterType: TagFilterType) = withContext(Dispatchers.IO) {
        if (filterType == TagFilterType.NEUTRAL) {
            filterDao.removeTagFilter(tag.name)
        } else {
            filterDao.setTagFilter(
                UserTagFilterEntity(
                    tag = tag.name,
                    filterType = filterType.name
                )
            )
        }
    }

    suspend fun blockTag(tag: TagCategory) = setTagFilter(tag, TagFilterType.BLOCKED)

    suspend fun boostTag(tag: TagCategory) = setTagFilter(tag, TagFilterType.BOOSTED)

    suspend fun clearTagFilter(tag: TagCategory) = setTagFilter(tag, TagFilterType.NEUTRAL)

    suspend fun getBlockedTags(): Set<TagCategory> = withContext(Dispatchers.IO) {
        filterDao.getBlockedTags().mapNotNull {
            runCatching { TagCategory.valueOf(it) }.getOrNull()
        }.toSet()
    }

    suspend fun getBoostedTags(): Set<TagCategory> = withContext(Dispatchers.IO) {
        filterDao.getBoostedTags().mapNotNull {
            runCatching { TagCategory.valueOf(it) }.getOrNull()
        }.toSet()
    }

    // ================================================================
    // HIDDEN NOVELS MANAGEMENT
    // ================================================================

    fun observeHiddenNovels(): Flow<List<HiddenNovelEntity>> {
        return filterDao.observeHiddenNovels()
    }

    suspend fun hideNovel(
        novelUrl: String,
        novelName: String,
        reason: HideReason = HideReason.NOT_INTERESTED
    ) = withContext(Dispatchers.IO) {
        filterDao.hideNovel(
            HiddenNovelEntity(
                novelUrl = novelUrl,
                novelName = novelName,
                reason = reason.name
            )
        )
    }

    suspend fun unhideNovel(novelUrl: String) = withContext(Dispatchers.IO) {
        filterDao.unhideNovel(novelUrl)
    }

    suspend fun isNovelHidden(novelUrl: String): Boolean = withContext(Dispatchers.IO) {
        filterDao.isNovelHidden(novelUrl)
    }

    // ================================================================
    // BLOCKED AUTHORS MANAGEMENT
    // ================================================================

    fun observeBlockedAuthors(): Flow<List<BlockedAuthorEntity>> {
        return filterDao.observeBlockedAuthors()
    }

    suspend fun blockAuthor(authorNormalized: String, displayName: String) = withContext(Dispatchers.IO) {
        filterDao.blockAuthor(
            BlockedAuthorEntity(
                authorNormalized = authorNormalized,
                displayName = displayName
            )
        )
    }

    suspend fun unblockAuthor(authorNormalized: String) = withContext(Dispatchers.IO) {
        filterDao.unblockAuthor(authorNormalized)
    }

    // ================================================================
    // BULK OPERATIONS
    // ================================================================

    suspend fun clearAllFilters() = withContext(Dispatchers.IO) {
        filterDao.clearAllTagFilters()
        filterDao.clearAllHiddenNovels()
        filterDao.clearAllBlockedAuthors()
    }

    suspend fun getFilterStats(): FilterStats = withContext(Dispatchers.IO) {
        FilterStats(
            blockedTagCount = filterDao.getBlockedTags().size,
            boostedTagCount = filterDao.getBoostedTags().size,
            hiddenNovelCount = filterDao.getHiddenNovelUrls().size,
            blockedAuthorCount = filterDao.getBlockedAuthors().size
        )
    }
}

data class FilterStats(
    val blockedTagCount: Int,
    val boostedTagCount: Int,
    val hiddenNovelCount: Int,
    val blockedAuthorCount: Int
) {
    val hasAnyFilters: Boolean
        get() = blockedTagCount > 0 || boostedTagCount > 0 ||
                hiddenNovelCount > 0 || blockedAuthorCount > 0
}