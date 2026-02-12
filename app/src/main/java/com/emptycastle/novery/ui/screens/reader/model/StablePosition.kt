package com.emptycastle.novery.ui.screens.reader.model

/**
 * Stable scroll position that survives content changes.
 * Uses character-offset approach from QuickNovel pattern.
 */
data class StableScrollPosition(
    val chapterIndex: Int,
    /** Character offset from start of chapter (stable identifier) */
    val characterOffset: Int,
    /** Segment index within chapter (for fast lookup) */
    val segmentIndex: Int = 0,
    /** Pixel offset within the segment for sub-item precision */
    val pixelOffset: Int = 0,
    /** Timestamp for debugging and cache management */
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun chapterStart(chapterIndex: Int) = StableScrollPosition(
            chapterIndex = chapterIndex,
            characterOffset = 0,
            segmentIndex = 0,
            pixelOffset = 0
        )
    }
}

/**
 * Separate TTS position tracking.
 * TTS and scroll positions can diverge when user scrolls during playback.
 */
data class StableTTSPosition(
    val chapterIndex: Int,
    val segmentIndex: Int,
    val sentenceIndex: Int,
    val globalSentenceIndex: Int = 0,
    val characterOffset: Int = 0
) {
    val isValid: Boolean get() = chapterIndex >= 0 && segmentIndex >= 0

    companion object {
        val INVALID = StableTTSPosition(-1, -1, -1, -1, -1)
    }
}

/**
 * Pre-computed character offsets for O(1) segment lookups.
 */
data class ChapterCharacterMap(
    val chapterIndex: Int,
    val segmentStartOffsets: List<Int>,
    val segmentEndOffsets: List<Int>,
    val totalCharacters: Int
) {
    fun findSegmentByCharOffset(charOffset: Int): Int {
        if (segmentStartOffsets.isEmpty()) return 0
        val clampedOffset = charOffset.coerceIn(0, totalCharacters)

        var low = 0
        var high = segmentStartOffsets.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val start = segmentStartOffsets[mid]
            val end = segmentEndOffsets[mid]

            when {
                clampedOffset < start -> high = mid - 1
                clampedOffset > end -> low = mid + 1
                else -> return mid
            }
        }

        return low.coerceIn(0, segmentStartOffsets.size - 1)
    }

    fun getCharOffsetForSegment(segmentIndex: Int): Int {
        return segmentStartOffsets.getOrElse(segmentIndex) { 0 }
    }

    companion object {
        fun build(segments: List<ContentSegment>, chapterIndex: Int): ChapterCharacterMap {
            if (segments.isEmpty()) {
                return ChapterCharacterMap(chapterIndex, emptyList(), emptyList(), 0)
            }

            val startOffsets = mutableListOf<Int>()
            val endOffsets = mutableListOf<Int>()
            var runningOffset = 0

            segments.forEach { segment ->
                startOffsets.add(runningOffset)
                runningOffset += segment.text.length
                endOffsets.add(runningOffset)
            }

            return ChapterCharacterMap(
                chapterIndex = chapterIndex,
                segmentStartOffsets = startOffsets,
                segmentEndOffsets = endOffsets,
                totalCharacters = runningOffset
            )
        }
    }
}

/**
 * Resolution result when converting StableScrollPosition to display index.
 */
sealed class PositionResolution {
    data class Found(
        val displayIndex: Int,
        val pixelOffset: Int,
        val confidence: Float
    ) : PositionResolution()

    data class ChapterNotLoaded(val chapterIndex: Int) : PositionResolution()
    data object NotFound : PositionResolution()
}