package com.emptycastle.novery.util

/**
 * Represents a sentence with its position and TTS hints
 */
data class ParsedSentence(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val sentenceIndex: Int,
    val pauseAfterMs: Int = DEFAULT_PAUSE_MS
) {
    companion object {
        const val DEFAULT_PAUSE_MS = 200
        const val SHORT_PAUSE_MS = 100
        const val LONG_PAUSE_MS = 300
        const val ELLIPSIS_PAUSE_MS = 350
    }
}

/**
 * Represents a paragraph with its sentences parsed
 */
data class ParsedParagraph(
    val fullText: String,
    val sentences: List<ParsedSentence>
) {
    val sentenceCount: Int get() = sentences.size

    fun getSentence(index: Int): ParsedSentence? = sentences.getOrNull(index)
}

/**
 * TTS-optimized sentence parser with intelligent handling of:
 * - Ellipsis (... and …)
 * - Multiple punctuation
 * - Abbreviations
 * - Decimal numbers
 * - Initials
 * - Quotation marks (stripped for Google TTS compatibility)
 */
object SentenceParser {

    // Common abbreviations that shouldn't end sentences
    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "rev", "hon",
        "capt", "col", "gen", "lt", "sgt",
        "vs", "etc", "inc", "ltd", "co", "corp",
        "st", "ave", "blvd", "rd", "ft", "mt", "apt",
        "vol", "pg", "pp", "ch", "pt", "no", "nos",
        "fig", "figs", "approx", "dept", "est", "govt", "misc",
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    )

    private val SENTENCE_ENDINGS = charArrayOf('.', '!', '?')
    private val CLOSING_QUOTES = charArrayOf('"', '\u201D', '\'', '\u2019', '\u300D', '\u300F', ')', ']', '\u00BB')
    private val OPENING_QUOTES = charArrayOf('"', '\u201C', '\'', '\u2018', '\u300C', '\u300E', '(', '[', '\u00AB')

    // Pattern to detect sentences that are just punctuation/ellipsis/quotes (no actual words)
    private val PUNCTUATION_ONLY_REGEX = Regex(
        "^[\"'\u201C\u201D\u2018\u2019\u201E\u201A" +
                "\u300C\u300D\u300E\u300F()\\[\\]\u00AB\u00BB" +
                ".,!?\u2026:;\\-\u2014\u2013_\\s]+$"
    )

    /**
     * Parse text into sentences
     */
    fun parse(text: String): ParsedParagraph {
        if (text.isBlank()) {
            return ParsedParagraph(text, emptyList())
        }

        val cleanedText = preprocessText(text.trim())
        val sentences = mutableListOf<ParsedSentence>()

        var sentenceStart = 0
        var i = 0
        var sentenceIndex = 0
        var lastI = -1

        while (i < cleanedText.length) {
            // Safety: ensure we always advance
            if (i == lastI) {
                i++
                continue
            }
            lastI = i

            val char = cleanedText[i]

            // Check for ellipsis first
            if (char == '.' || char == '\u2026') {
                val ellipsisEnd = findEllipsisEnd(cleanedText, i)

                if (ellipsisEnd > i) {
                    val shouldBreak = shouldBreakAtEllipsis(cleanedText, ellipsisEnd)

                    if (shouldBreak) {
                        var endIndex = ellipsisEnd
                        endIndex = skipClosingQuotes(cleanedText, endIndex)

                        val sentenceText = cleanedText.substring(sentenceStart, endIndex).trim()
                        if (isValidSentence(sentenceText)) {
                            val normalized = normalizeSentence(sentenceText)
                            if (normalized.isNotBlank()) {
                                sentences.add(
                                    ParsedSentence(
                                        text = normalized,
                                        startIndex = sentenceStart,
                                        endIndex = endIndex,
                                        sentenceIndex = sentenceIndex,
                                        pauseAfterMs = ParsedSentence.ELLIPSIS_PAUSE_MS
                                    )
                                )
                                sentenceIndex++
                            }
                        }

                        sentenceStart = skipWhitespace(cleanedText, endIndex)
                        i = sentenceStart
                    } else {
                        i = ellipsisEnd
                    }
                    continue
                }
            }

            // Check for regular sentence endings
            if (char in SENTENCE_ENDINGS) {
                if (isSentenceEnd(cleanedText, i)) {
                    var endIndex = i + 1
                    while (endIndex < cleanedText.length && cleanedText[endIndex] in SENTENCE_ENDINGS) {
                        endIndex++
                    }
                    endIndex = skipClosingQuotes(cleanedText, endIndex)

                    val sentenceText = cleanedText.substring(sentenceStart, endIndex).trim()
                    if (isValidSentence(sentenceText)) {
                        val normalized = normalizeSentence(sentenceText)
                        if (normalized.isNotBlank()) {
                            sentences.add(
                                ParsedSentence(
                                    text = normalized,
                                    startIndex = sentenceStart,
                                    endIndex = endIndex,
                                    sentenceIndex = sentenceIndex,
                                    pauseAfterMs = calculatePause(sentenceText)
                                )
                            )
                            sentenceIndex++
                        }
                    }

                    sentenceStart = skipWhitespace(cleanedText, endIndex)
                    i = sentenceStart
                    continue
                }
            }

            i++
        }

        // Add remaining text as final sentence
        if (sentenceStart < cleanedText.length) {
            val remainingText = cleanedText.substring(sentenceStart).trim()
            if (isValidSentence(remainingText)) {
                val normalized = normalizeSentence(remainingText)
                if (normalized.isNotBlank()) {
                    sentences.add(
                        ParsedSentence(
                            text = normalized,
                            startIndex = sentenceStart,
                            endIndex = cleanedText.length,
                            sentenceIndex = sentenceIndex
                        )
                    )
                }
            }
        }

        // Fallback: if no sentences found, use whole text
        if (sentences.isEmpty() && cleanedText.isNotBlank()) {
            val normalized = normalizeSentence(cleanedText)
            if (normalized.isNotBlank()) {
                sentences.add(
                    ParsedSentence(
                        text = normalized,
                        startIndex = 0,
                        endIndex = cleanedText.length,
                        sentenceIndex = 0
                    )
                )
            }
        }

        return ParsedParagraph(cleanedText, sentences)
    }

    /**
     * Check if text is valid sentence (not blank and has actual content)
     */
    private fun isValidSentence(text: String): Boolean {
        return text.isNotBlank() && !isPunctuationOnly(text)
    }

    /**
     * Check if text is only punctuation, quotes, ellipsis, and whitespace (no actual words)
     * This prevents TTS from saying things like "quote ellipsis quote"
     */
    private fun isPunctuationOnly(text: String): Boolean {
        return PUNCTUATION_ONLY_REGEX.matches(text)
    }

    /**
     * Find the end of an ellipsis starting at index
     * Returns index (unchanged) if not an ellipsis
     */
    private fun findEllipsisEnd(text: String, index: Int): Int {
        return when {
            // Unicode ellipsis
            text[index] == '\u2026' -> index + 1

            // ASCII ellipsis (...) - must be exactly 3 or more dots
            text[index] == '.' &&
                    index + 2 < text.length &&
                    text[index + 1] == '.' &&
                    text[index + 2] == '.' -> {
                var end = index + 3
                while (end < text.length && text[end] == '.') end++
                end
            }

            else -> index
        }
    }

    /**
     * Determine if we should break the sentence at an ellipsis
     */
    private fun shouldBreakAtEllipsis(text: String, afterEllipsis: Int): Boolean {
        // Skip closing quotes
        var i = afterEllipsis
        while (i < text.length && text[i] in CLOSING_QUOTES) i++

        // Skip whitespace
        val hadWhitespace = i < text.length && text[i].isWhitespace()
        while (i < text.length && text[i].isWhitespace()) i++

        // End of text = break
        if (i >= text.length) return true

        val nextChar = text[i]

        return when {
            // Capital letter after space = new sentence
            nextChar.isUpperCase() && hadWhitespace -> true

            // Opening quote after space = likely new sentence
            nextChar in OPENING_QUOTES && hadWhitespace -> true

            // Lowercase = continuation (e.g., "He was... uncertain")
            nextChar.isLowerCase() -> false

            // Digit after ellipsis without space = continuation
            nextChar.isDigit() && !hadWhitespace -> false

            // Default: break if there was whitespace
            else -> hadWhitespace
        }
    }

    /**
     * Check if punctuation at index is a real sentence end
     */
    private fun isSentenceEnd(text: String, index: Int): Boolean {
        val char = text[index]

        if (char == '.') {
            // Ellipsis check - handled separately
            if (index + 2 < text.length && text[index + 1] == '.' && text[index + 2] == '.') {
                return false
            }

            // Abbreviation check
            val wordStart = findWordStart(text, index)
            if (wordStart < index) {
                val word = text.substring(wordStart, index).lowercase()
                if (word in ABBREVIATIONS) {
                    val nextNonSpace = skipWhitespace(text, index + 1)
                    if (nextNonSpace < text.length && text[nextNonSpace].isLowerCase()) {
                        return false
                    }
                }
            }

            // Decimal number check (3.14)
            if (index > 0 && index < text.length - 1 &&
                text[index - 1].isDigit() && text[index + 1].isDigit()
            ) {
                return false
            }

            // Initial check (J. K. Rowling)
            if (isInitial(text, index)) return false
        }

        // Check what follows
        var nextIndex = index + 1
        while (nextIndex < text.length && text[nextIndex] == char) nextIndex++
        nextIndex = skipClosingQuotes(text, nextIndex)
        nextIndex = skipWhitespace(text, nextIndex)

        // End of text = sentence end
        if (nextIndex >= text.length) return true

        val nextChar = text[nextIndex]

        // Uppercase or opening quote = new sentence
        if (nextChar.isUpperCase() || nextChar in OPENING_QUOTES) return true

        // For ! and ? be lenient
        if (char == '!' || char == '?') return true

        // Period with space = likely end
        if (char == '.' && index + 1 < text.length && text[index + 1].isWhitespace()) {
            return true
        }

        return false
    }

    /**
     * Check if period at index is part of initials (e.g., J. K.)
     */
    private fun isInitial(text: String, periodIndex: Int): Boolean {
        if (periodIndex < 1) return false

        val charBefore = text[periodIndex - 1]
        if (!charBefore.isUpperCase()) return false

        if (periodIndex >= 2 && text[periodIndex - 2].isLetter()) return false

        val nextNonSpace = skipWhitespace(text, periodIndex + 1)
        if (nextNonSpace >= text.length) return false

        val nextChar = text[nextNonSpace]

        if (nextChar.isUpperCase()) {
            val afterNext = nextNonSpace + 1
            if (afterNext < text.length && text[afterNext] == '.') return true
            if (afterNext < text.length && text[afterNext].isLowerCase()) return true
        }

        return false
    }

    // ==================== Text Processing ====================

    private fun preprocessText(text: String): String {
        var result = text

        // Normalize line breaks within paragraph
        result = result.replace(Regex("[ \\t]*\\n[ \\t]*"), " ")

        // Normalize multiple spaces
        result = result.replace(Regex(" {2,}"), " ")

        // Normalize dashes
        result = result.replace("---", "\u2014")
        result = result.replace("--", "\u2014")

        return result.trim()
    }

    private fun normalizeSentence(text: String): String {
        var result = text.trim()

        // Remove all quotation marks — Google TTS reads them literally as "quote"
        result = result.replace(Regex("[\"'\u201C\u201D\u2018\u2019\u300C\u300D\u300E\u300F\u00AB\u00BB]"), "")

        // Normalize ellipsis to ASCII dots for consistent handling below
        result = result.replace("\u2026", "...")
        result = result.replace(Regex("\\.{4,}"), "...")

        // Remove leading ellipsis — Google TTS says "dot dot dot"
        // The inter-sentence pause already provides the dramatic effect
        result = result.replace(Regex("^\\.{3}\\s*"), "")

        // Remove trailing ellipsis — ELLIPSIS_PAUSE_MS already handles the pause
        result = result.replace(Regex("\\s*\\.{3}$"), "")

        // Replace mid-sentence ellipsis with comma for a natural TTS pause
        // e.g. "he was...uncertain" or "he was... uncertain" → "he was, uncertain"
        result = result.replace(Regex("\\s*\\.{3}\\s*(?=[A-Za-z])"), ", ")

        // Clean up artifacts left by stripping
        result = result.replace(Regex("^[,;:\\s]+"), "")    // leading comma/space
        result = result.replace(Regex("[,;:\\s]+$"), "")     // trailing comma/space
        result = result.replace(Regex(",\\s*,"), ",")        // double commas
        result = result.replace(Regex(" {2,}"), " ")         // multiple spaces

        return result.trim()
    }

    private fun calculatePause(sentence: String): Int {
        val trimmed = sentence.trim()
        if (trimmed.isEmpty()) return ParsedSentence.DEFAULT_PAUSE_MS

        // Check for ellipsis at end
        if (trimmed.endsWith("\u2026") || trimmed.endsWith("...")) {
            return ParsedSentence.ELLIPSIS_PAUSE_MS
        }

        val lastChar = trimmed.last()
        val effectiveLast = if (lastChar in CLOSING_QUOTES && trimmed.length > 1) {
            trimmed[trimmed.length - 2]
        } else {
            lastChar
        }

        return when (effectiveLast) {
            '?' -> ParsedSentence.DEFAULT_PAUSE_MS + 50   // 250ms total
            '!' -> ParsedSentence.DEFAULT_PAUSE_MS + 25   // 225ms total
            '\u2014', '\u2013' -> ParsedSentence.SHORT_PAUSE_MS  // 100ms
            '\u2026' -> ParsedSentence.ELLIPSIS_PAUSE_MS         // 350ms
            else -> ParsedSentence.DEFAULT_PAUSE_MS              // 200ms
        }
    }

    // ==================== Utility Functions ====================

    private fun findWordStart(text: String, endIndex: Int): Int {
        var start = endIndex - 1
        while (start >= 0 && text[start].isLetter()) start--
        return start + 1
    }

    private fun skipWhitespace(text: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < text.length && text[i].isWhitespace()) i++
        return i
    }

    private fun skipClosingQuotes(text: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < text.length && text[i] in CLOSING_QUOTES) i++
        return i
    }

    // ==================== Public API ====================

    /**
     * Split text into sentences (simple list)
     */
    fun splitIntoSentences(text: String): List<String> {
        return parse(text).sentences.map { it.text }
    }

    /**
     * Count sentences in text
     */
    fun countSentences(text: String): Int {
        return parse(text).sentenceCount
    }

    /**
     * Get sentences with pause hints for TTS
     */
    fun getSentencesWithPauses(text: String): List<Pair<String, Int>> {
        return parse(text).sentences.map { it.text to it.pauseAfterMs }
    }
}