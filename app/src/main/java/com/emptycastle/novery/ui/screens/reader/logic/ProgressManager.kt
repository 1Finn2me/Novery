package com.emptycastle.novery.ui.screens.reader.logic

import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.ui.screens.reader.model.LoadedChapter
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReadingPosition
import com.emptycastle.novery.ui.screens.reader.model.ResolutionMethod
import com.emptycastle.novery.ui.screens.reader.model.ResolvedScrollPosition

/**
 * Manages reading position tracking, saving, and restoration.
 * Handles the complexity of mapping between scroll positions and content segments.
 */
class ProgressManager(
    private val preferencesManager: PreferencesManager
) {

    // =========================================================================
    // POSITION CAPTURE
    // =========================================================================

    /**
     * Captures the current reading position based on visible items.
     */
    fun capturePosition(
        displayItems: List<ReaderDisplayItem>,
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Int,
        loadedChapters: Map<Int, LoadedChapter>
    ): ReadingPosition? {
        if (displayItems.isEmpty() || firstVisibleItemIndex >= displayItems.size) {
            return null
        }

        val visibleItem = displayItems[firstVisibleItemIndex]

        return when (visibleItem) {
            is ReaderDisplayItem.Segment -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalSegments = chapter?.segments?.size ?: 1
                val progress = if (totalSegments > 0) {
                    (visibleItem.segmentIndexInChapter.toFloat() / totalSegments).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = visibleItem.chapterUrl,
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = visibleItem.segment.id,
                    segmentIndexInChapter = visibleItem.segmentIndexInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.Image -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalItems = chapter?.contentCount ?: 1
                val progress = if (totalItems > 0) {
                    (visibleItem.orderInChapter.toFloat() / totalItems).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = visibleItem.chapterUrl,
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "image_${visibleItem.imageIndexInChapter}",
                    segmentIndexInChapter = visibleItem.orderInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.HorizontalRule -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalItems = chapter?.contentCount ?: 1
                val progress = if (totalItems > 0) {
                    (visibleItem.orderInChapter.toFloat() / totalItems).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "hrule_${visibleItem.orderInChapter}",
                    segmentIndexInChapter = visibleItem.orderInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.SceneBreak -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalItems = chapter?.contentCount ?: 1
                val progress = if (totalItems > 0) {
                    (visibleItem.orderInChapter.toFloat() / totalItems).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "scenebreak_${visibleItem.orderInChapter}",
                    segmentIndexInChapter = visibleItem.orderInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.AuthorNote -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalItems = chapter?.contentCount ?: 1
                val progress = if (totalItems > 0) {
                    (visibleItem.orderInChapter.toFloat() / totalItems).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "authornote_${visibleItem.authorNote.id}",
                    segmentIndexInChapter = visibleItem.orderInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.ChapterHeader -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                ReadingPosition.fromHeader(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex
                ).copy(offsetPixels = firstVisibleItemOffset)
            }

            is ReaderDisplayItem.ChapterDivider -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                ReadingPosition(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "divider",
                    segmentIndexInChapter = chapter?.contentCount ?: 0,
                    approximateProgress = 1f,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.LoadingIndicator,
            is ReaderDisplayItem.ErrorIndicator -> null
        }
    }

    // =========================================================================
    // POSITION RESOLUTION
    // =========================================================================

    /**
     * Resolves a saved position to a display item index.
     * Uses multiple strategies with decreasing confidence.
     */
    fun resolvePosition(
        savedPosition: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition {
        val chapter = loadedChapters.values.find {
            it.chapter.url == savedPosition.chapterUrl && !it.isLoading
        }

        if (chapter == null) {
            return ResolvedScrollPosition(
                displayIndex = 0,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.NOT_FOUND,
                confidence = 0f
            )
        }

        // Try resolution strategies in order of confidence
        findBySegmentId(savedPosition, displayItems)?.let { return it }
        findBySegmentIndex(savedPosition, displayItems)?.let { return it }
        findByProgress(savedPosition, displayItems, loadedChapters)?.let { return it }
        findChapterStart(savedPosition, displayItems, loadedChapters)?.let { return it }

        return ResolvedScrollPosition(
            displayIndex = 0,
            offsetPixels = 0,
            resolutionMethod = ResolutionMethod.NOT_FOUND,
            confidence = 0f
        )
    }

    private fun findBySegmentId(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>
    ): ResolvedScrollPosition? {
        // Handle special segment IDs
        if (position.segmentId == "header") {
            val index = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterHeader &&
                        item.chapterIndex == position.chapterIndex
            }
            if (index >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = index,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                    confidence = 1f
                )
            }
        }

        if (position.segmentId == "divider") {
            val index = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterDivider &&
                        item.chapterIndex == position.chapterIndex
            }
            if (index >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = index,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                    confidence = 1f
                )
            }
        }

        if (position.segmentId.startsWith("image_")) {
            val imageIndex = position.segmentId.removePrefix("image_").toIntOrNull() ?: -1
            if (imageIndex >= 0) {
                val index = displayItems.indexOfFirst { item ->
                    item is ReaderDisplayItem.Image &&
                            item.chapterUrl == position.chapterUrl &&
                            item.imageIndexInChapter == imageIndex
                }
                if (index >= 0) {
                    return ResolvedScrollPosition(
                        displayIndex = index,
                        offsetPixels = position.offsetPixels,
                        resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                        confidence = 1f
                    )
                }
            }
        }

        // Handle author note segment IDs
        if (position.segmentId.startsWith("authornote_")) {
            val noteId = position.segmentId.removePrefix("authornote_")
            val index = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.AuthorNote &&
                        item.chapterIndex == position.chapterIndex &&
                        item.authorNote.id == noteId
            }
            if (index >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = index,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                    confidence = 1f
                )
            }
        }

        // Find by exact segment ID (existing code)
        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segment.id == position.segmentId
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = position.offsetPixels,
                resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                confidence = 1f
            )
        } else null
    }

    private fun findBySegmentIndex(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>
    ): ResolvedScrollPosition? {
        if (position.segmentIndexInChapter < 0) {
            val headerIndex = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterHeader &&
                        item.chapterIndex == position.chapterIndex
            }
            if (headerIndex >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = headerIndex,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.SEGMENT_INDEX,
                    confidence = 0.9f
                )
            }
        }

        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segmentIndexInChapter == position.segmentIndexInChapter
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = position.offsetPixels,
                resolutionMethod = ResolutionMethod.SEGMENT_INDEX,
                confidence = 0.85f
            )
        } else null
    }

    private fun findByProgress(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition? {
        val chapter = loadedChapters.values.find { it.chapter.url == position.chapterUrl }
            ?: return null

        if (chapter.segments.isEmpty()) return null

        val targetSegmentIndex = (position.approximateProgress * chapter.segments.size)
            .toInt()
            .coerceIn(0, chapter.segments.lastIndex)

        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segmentIndexInChapter >= targetSegmentIndex
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.PROGRESS_ESTIMATE,
                confidence = 0.6f
            )
        } else null
    }

    private fun findChapterStart(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition? {
        val headerIndex = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.ChapterHeader &&
                    loadedChapters[item.chapterIndex]?.chapter?.url == position.chapterUrl
        }

        if (headerIndex >= 0) {
            return ResolvedScrollPosition(
                displayIndex = headerIndex,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.CHAPTER_START,
                confidence = 0.4f
            )
        }

        val firstSegmentIndex = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl
        }

        return if (firstSegmentIndex >= 0) {
            ResolvedScrollPosition(
                displayIndex = firstSegmentIndex,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.CHAPTER_START,
                confidence = 0.4f
            )
        } else null
    }

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    /**
     * Saves the reading position to preferences.
     */
    fun savePosition(position: ReadingPosition) {
        preferencesManager.saveReadingPosition(
            chapterUrl = position.chapterUrl,
            segmentId = position.segmentId,
            segmentIndex = position.segmentIndexInChapter,
            progress = position.approximateProgress,
            offset = position.offsetPixels
        )
    }

    /**
     * Loads a previously saved position for a chapter.
     */
    fun loadPosition(chapterUrl: String, chapterIndex: Int): ReadingPosition? {
        return preferencesManager.getReadingPosition(chapterUrl)?.let { saved ->
            ReadingPosition(
                chapterUrl = chapterUrl,
                chapterIndex = chapterIndex,
                segmentId = saved.segmentId,
                segmentIndexInChapter = saved.segmentIndex,
                approximateProgress = saved.progress,
                offsetPixels = saved.offset,
                timestamp = saved.timestamp
            )
        }
    }

    // =========================================================================
    // PROGRESS CALCULATION
    // =========================================================================

    /**
     * Calculates chapter progress based on current position.
     */
    fun calculateChapterProgress(
        currentSegmentIndex: Int,
        totalSegments: Int
    ): Float {
        if (totalSegments == 0) return 0f
        return (currentSegmentIndex.toFloat() / totalSegments).coerceIn(0f, 1f)
    }

    /**
     * Calculates overall reading progress across all chapters.
     */
    fun calculateOverallProgress(
        currentChapterIndex: Int,
        totalChapters: Int,
        chapterProgress: Float
    ): Float {
        if (totalChapters == 0) return 0f
        val chapterWeight = 1f / totalChapters
        return (currentChapterIndex * chapterWeight) + (chapterProgress * chapterWeight)
    }
}