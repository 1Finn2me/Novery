package com.emptycastle.novery.ui.screens.reader.logic

import com.emptycastle.novery.ui.screens.reader.model.LoadedChapter

/**
 * Action to take for infinite scroll.
 */
sealed class ScrollAction {
    object None : ScrollAction()

    data class LoadNext(
        val chapterIndex: Int,
        val chaptersToUnload: Set<Int>
    ) : ScrollAction()

    data class LoadPrevious(
        val chapterIndex: Int,
        val chaptersToUnload: Set<Int>
    ) : ScrollAction()
}

/**
 * Configuration for infinite scroll behavior.
 */
data class InfiniteScrollConfig(
    val preloadThreshold: Int = 1,      // Load next when within N chapters of end
    val keepLoadedRange: Int = 2,        // Keep N chapters before/after current
    val triggerItemThreshold: Int = 5    // Trigger when within N items of edge
)

/**
 * Manages infinite scroll logic including:
 * - Detecting when to preload chapters
 * - Deciding which chapters to load/unload
 * - Memory management by unloading distant chapters
 */
class InfiniteScrollController(
    private val config: InfiniteScrollConfig = InfiniteScrollConfig()
) {

    // =========================================================================
    // SCROLL EDGE DETECTION
    // =========================================================================

    /**
     * Evaluates if action is needed when approaching the end of loaded content.
     */
    fun onApproachingEnd(
        lastVisibleChapterIndex: Int,
        loadedChapterIndices: Set<Int>,
        totalChapters: Int,
        isEnabled: Boolean
    ): ScrollAction {
        if (!isEnabled) return ScrollAction.None

        val maxLoadedIndex = loadedChapterIndices.maxOrNull() ?: return ScrollAction.None

        // Check if we're near the end of loaded chapters
        if (lastVisibleChapterIndex >= maxLoadedIndex - config.preloadThreshold) {
            val nextToLoad = maxLoadedIndex + 1

            if (nextToLoad < totalChapters && nextToLoad !in loadedChapterIndices) {
                // Use the current visible chapter index for unload calculations to avoid removing chapters the user is currently viewing
                val chaptersToUnload = calculateChaptersToUnload(
                    currentChapterIndex = lastVisibleChapterIndex,
                    loadedChapterIndices = loadedChapterIndices
                )

                return ScrollAction.LoadNext(
                    chapterIndex = nextToLoad,
                    chaptersToUnload = chaptersToUnload
                )
            }
        }

        return ScrollAction.None
    }

    /**
     * Evaluates if action is needed when approaching the beginning of loaded content.
     */
    fun onApproachingBeginning(
        firstVisibleChapterIndex: Int,
        loadedChapterIndices: Set<Int>,
        isEnabled: Boolean
    ): ScrollAction {
        if (!isEnabled) return ScrollAction.None

        val minLoadedIndex = loadedChapterIndices.minOrNull() ?: return ScrollAction.None

        // Check if we're near the beginning of loaded chapters
        if (firstVisibleChapterIndex <= minLoadedIndex + config.preloadThreshold) {
            val prevToLoad = minLoadedIndex - 1

            if (prevToLoad >= 0 && prevToLoad !in loadedChapterIndices) {
                // Use the current visible chapter index for unload calculations to avoid removing chapters the user is currently viewing
                val chaptersToUnload = calculateChaptersToUnload(
                    currentChapterIndex = firstVisibleChapterIndex,
                    loadedChapterIndices = loadedChapterIndices
                )

                return ScrollAction.LoadPrevious(
                    chapterIndex = prevToLoad,
                    chaptersToUnload = chaptersToUnload
                )
            }
        }

        return ScrollAction.None
    }

    // =========================================================================
    // MEMORY MANAGEMENT
    // =========================================================================

    /**
     * Calculates which chapters should be unloaded to free memory.
     */
    fun calculateChaptersToUnload(
        currentChapterIndex: Int,
        loadedChapterIndices: Set<Int>
    ): Set<Int> {
        val keepRange = (currentChapterIndex - config.keepLoadedRange)..(currentChapterIndex + config.keepLoadedRange)
        return loadedChapterIndices.filter { it !in keepRange }.toSet()
    }

    /**
     * Determines the optimal chapters to keep loaded based on current position.
     */
    fun getOptimalLoadedRange(
        currentChapterIndex: Int,
        totalChapters: Int
    ): IntRange {
        val start = (currentChapterIndex - config.keepLoadedRange).coerceAtLeast(0)
        val end = (currentChapterIndex + config.keepLoadedRange).coerceAtMost(totalChapters - 1)
        return start..end
    }

    // =========================================================================
    // SCROLL POSITION ADJUSTMENT
    // =========================================================================

    /**
     * Calculates how many display items a loaded chapter contributes.
     * Used for scroll position adjustment when loading chapters before current position.
     */
    fun calculateChapterItemCount(loadedChapter: LoadedChapter): Int {
        return when {
            loadedChapter.isLoading -> 2  // Header + loading indicator
            loadedChapter.error != null -> 2  // Header + error indicator
            else -> 1 + loadedChapter.segments.size + 1  // Header + segments + divider
        }
    }

    /**
     * Calculates the scroll adjustment needed when chapters are added before current position.
     */
    fun calculateScrollAdjustment(
        addedChapters: List<LoadedChapter>,
        currentChapterIndex: Int
    ): Int {
        return addedChapters
            .filter { it.chapterIndex < currentChapterIndex }
            .sumOf { calculateChapterItemCount(it) }
    }

    // =========================================================================
    // PRELOAD DECISIONS
    // =========================================================================

    /**
     * Determines chapters that should be preloaded for smooth reading.
     */
    fun getChaptersToPreload(
        currentChapterIndex: Int,
        loadedChapterIndices: Set<Int>,
        totalChapters: Int,
        isEnabled: Boolean
    ): List<Int> {
        if (!isEnabled) return emptyList()

        val toPreload = mutableListOf<Int>()
        val optimalRange = getOptimalLoadedRange(currentChapterIndex, totalChapters)

        for (index in optimalRange) {
            if (index !in loadedChapterIndices) {
                toPreload.add(index)
            }
        }

        // Prioritize forward loading
        return toPreload.sortedBy {
            if (it >= currentChapterIndex) it - currentChapterIndex
            else Int.MAX_VALUE - (currentChapterIndex - it)
        }
    }

    /**
     * Checks if a chapter index is within the visible/preload zone.
     */
    fun isChapterInActiveZone(
        chapterIndex: Int,
        currentChapterIndex: Int
    ): Boolean {
        return chapterIndex in getOptimalLoadedRange(currentChapterIndex, Int.MAX_VALUE)
    }
}