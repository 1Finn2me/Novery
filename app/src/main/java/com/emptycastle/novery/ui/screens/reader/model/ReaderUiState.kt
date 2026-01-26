package com.emptycastle.novery.ui.screens.reader.model

import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.util.ParsedSentence

// =============================================================================
// READING POSITION
// =============================================================================

data class ReadingPosition(
    val chapterUrl: String,
    val chapterIndex: Int,
    val segmentId: String,
    val segmentIndexInChapter: Int,
    val approximateProgress: Float,
    val offsetPixels: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromHeader(chapterUrl: String, chapterIndex: Int): ReadingPosition {
            return ReadingPosition(
                chapterUrl = chapterUrl,
                chapterIndex = chapterIndex,
                segmentId = "header",
                segmentIndexInChapter = -1,
                approximateProgress = 0f,
                offsetPixels = 0
            )
        }
    }
}

data class ResolvedScrollPosition(
    val displayIndex: Int,
    val offsetPixels: Int,
    val resolutionMethod: ResolutionMethod,
    val confidence: Float
)

enum class ResolutionMethod {
    EXACT_SEGMENT_ID,
    SEGMENT_INDEX,
    PROGRESS_ESTIMATE,
    CHAPTER_START,
    NOT_FOUND
}

data class ScrollRestorationState(
    val pendingPosition: ReadingPosition? = null,
    val isWaitingForChapter: Boolean = false,
    val restorationAttempts: Int = 0,
    val lastAttemptTime: Long = 0,
    val hasSuccessfullyRestored: Boolean = false
) {
    val maxAttempts = 5
    val shouldRetry: Boolean
        get() = pendingPosition != null &&
                restorationAttempts < maxAttempts &&
                !hasSuccessfullyRestored
}

data class TargetScrollPosition(
    val displayIndex: Int,
    val offsetPixels: Int,
    val id: Long = System.currentTimeMillis()
)

// =============================================================================
// LOADED CHAPTER
// =============================================================================

data class LoadedChapter(
    val chapter: Chapter,
    val chapterIndex: Int,
    val segments: List<ContentSegment>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFromCache: Boolean = false,
    val wordCount: Int = 0
) {
    val totalSentences: Int get() = segments.sumOf { it.sentenceCount }

    val displayItemCount: Int get() = when {
        isLoading -> 2
        error != null -> 2
        else -> 1 + segments.size + 1
    }
}

// =============================================================================
// TTS STATE
// =============================================================================

data class TTSPosition(
    val segmentIndex: Int = -1,
    val sentenceIndexInSegment: Int = 0,
    val globalSentenceIndex: Int = 0
) {
    val isValid: Boolean get() = segmentIndex >= 0
}

data class SentenceHighlight(
    val segmentDisplayIndex: Int,
    val sentenceIndex: Int,
    val sentence: ParsedSentence
)

data class TTSSettingsState(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val voiceId: String? = null,
    val autoScroll: Boolean = true,
    val highlightSentence: Boolean = true,
    val pauseOnCalls: Boolean = true
)

// =============================================================================
// MAIN UI STATE
// =============================================================================

data class ReaderUiState(
    // Loading & Error
    val isLoading: Boolean = true,
    val error: String? = null,

    // Chapters
    val allChapters: List<Chapter> = emptyList(),
    val loadedChapters: Map<Int, LoadedChapter> = emptyMap(),
    val initialChapterIndex: Int = 0,
    val displayItems: List<ReaderDisplayItem> = emptyList(),

    // Current Chapter Info
    val currentChapterIndex: Int = 0,
    val currentChapterUrl: String = "",
    val currentChapterName: String = "",
    val previousChapter: Chapter? = null,
    val nextChapter: Chapter? = null,

    // Scroll State
    val targetScrollPosition: TargetScrollPosition? = null,
    val hasRestoredScroll: Boolean = false,
    val currentScrollIndex: Int = 0,
    val currentScrollOffset: Int = 0,

    // Reader Settings
    val settings: ReaderSettings = ReaderSettings(),

    // UI Controls
    val showControls: Boolean = true,
    val showSettings: Boolean = false,
    val showQuickSettings: Boolean = false,
    val showChapterList: Boolean = false,
    val showTTSSettings: Boolean = false,

    // Features
    val infiniteScrollEnabled: Boolean = false,
    val isPreloading: Boolean = false,
    val isOfflineMode: Boolean = false,

    // Progress
    val readingProgress: Float = 0f,
    val chapterProgress: Float = 0f,
    val readChapterUrls: Set<String> = emptySet(),

    // Bookmarks
    val isCurrentChapterBookmarked: Boolean = false,
    val bookmarkedPositions: List<BookmarkPosition> = emptyList(),

    // Word count for reading time estimation
    val estimatedTotalWords: Int = 0,

    // TTS State
    val isTTSActive: Boolean = false,
    val ttsStatus: TTSStatus = TTSStatus.STOPPED,
    val currentSegmentIndex: Int = -1,
    val currentTTSChapterIndex: Int = -1,
    val ttsPosition: TTSPosition = TTSPosition(),
    val currentSentenceHighlight: SentenceHighlight? = null,
    val totalTTSSentences: Int = 0,
    val currentGlobalSentenceIndex: Int = 0,
    val ttsSettings: TTSSettingsState = TTSSettingsState()
) {
    val savedScrollPosition: TargetScrollPosition? get() = targetScrollPosition

    fun getAllSegments(): List<ReaderDisplayItem.Segment> {
        return displayItems.filterIsInstance<ReaderDisplayItem.Segment>()
    }

    fun getTotalSentenceCount(): Int {
        return getAllSegments().sumOf { it.segment.sentenceCount }
    }
}

/**
 * Represents a bookmarked position in a chapter
 */
data class BookmarkPosition(
    val chapterUrl: String,
    val chapterName: String,
    val segmentIndex: Int,
    val timestamp: Long
)