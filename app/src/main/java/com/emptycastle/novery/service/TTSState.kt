package com.emptycastle.novery.service

import com.emptycastle.novery.util.ParsedSentence
/**
 * Represents the current state of TTS playback
 */
data class TTSPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val novelName: String = "",
    val novelUrl: String = "",
    val chapterName: String = "",
    val chapterUrl: String = "",
    val currentSegmentIndex: Int = 0,
    val totalSegments: Int = 0,
    val currentText: String = "",
    val speechRate: Float = 1.0f,
    val error: String? = null,
    // Chapter navigation info
    val chapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val hasNextChapter: Boolean = false,
    val hasPreviousChapter: Boolean = false,
    // Background mode tracking
    val isInBackgroundMode: Boolean = false,
    val autoAdvanceEnabled: Boolean = true
) {
    val hasContent: Boolean
        get() = totalSegments > 0

    val isAtStart: Boolean
        get() = currentSegmentIndex == 0

    val isAtEnd: Boolean
        get() = currentSegmentIndex >= totalSegments - 1

    val progressText: String
        get() = if (totalSegments > 0) "${currentSegmentIndex + 1} / $totalSegments" else ""

    val chapterProgressText: String
        get() = if (totalChapters > 0) "Chapter ${chapterIndex + 1} / $totalChapters" else ""
}

/**
 * A TTS segment with pause hint (milliseconds) provided by the sentence parser
 */
data class TTSSegment(
    val text: String,
    val pauseAfterMs: Int = ParsedSentence.DEFAULT_PAUSE_MS
)

/**
 * Content to be read by TTS
 */
data class TTSContent(
    val novelName: String,
    val novelUrl: String,
    val chapterName: String,
    val chapterUrl: String,
    val segments: List<TTSSegment>,
    val coverUrl: String? = null,
    // Chapter navigation info
    val chapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val hasNextChapter: Boolean = false,
    val hasPreviousChapter: Boolean = false
) {
    val totalSegments: Int get() = segments.size

    fun getSegment(index: Int): TTSSegment? = segments.getOrNull(index)
}

/**
 * Event emitted when chapter changes during TTS playback
 */
data class TTSChapterChangeEvent(
    val chapterIndex: Int,
    val chapterUrl: String,
    val chapterName: String,
    val totalChapters: Int
)