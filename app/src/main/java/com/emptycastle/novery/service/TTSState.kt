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
    val chapterName: String = "",
    val currentSegmentIndex: Int = 0,
    val totalSegments: Int = 0,
    val currentText: String = "",
    val speechRate: Float = 1.0f,
    val error: String? = null
) {
    val hasContent: Boolean
        get() = totalSegments > 0

    val isAtStart: Boolean
        get() = currentSegmentIndex == 0

    val isAtEnd: Boolean
        get() = currentSegmentIndex >= totalSegments - 1

    val progressText: String
        get() = if (totalSegments > 0) "${currentSegmentIndex + 1} / $totalSegments" else ""
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
    val coverUrl: String? = null
) {
    val totalSegments: Int get() = segments.size

    fun getSegment(index: Int): TTSSegment? = segments.getOrNull(index)
}