package com.emptycastle.novery.ui.screens.reader.logic

/**
 * Position of the author note within the chapter
 */
enum class AuthorNotePosition {
    BEFORE_CONTENT,
    INLINE,
    AFTER_CONTENT
}

/**
 * How to display author notes
 */
enum class AuthorNoteDisplayMode(val id: String) {
    EXPANDED("expanded"),
    COLLAPSED("collapsed"),
    HIDDEN("hidden");

    companion object {
        fun fromId(id: String): AuthorNoteDisplayMode {
            return entries.find { it.id == id } ?: COLLAPSED
        }
    }
}

/**
 * Detects author notes in web novel content.
 *
 * IMPORTANT: This detector is intentionally conservative to avoid false positives.
 * It primarily relies on CSS class detection for known platforms (RoyalRoad, etc.)
 * and only uses text-based detection for EXPLICIT author note markers.
 */
object AuthorNoteDetector {

    // =========================================================================
    // CSS-BASED DETECTION (Primary - Most Reliable)
    // =========================================================================

    /**
     * CSS classes that indicate author note CONTAINERS (the wrapper element).
     * These are specific to known platforms and should be trusted.
     */
    val authorNoteContainerClasses = setOf(
        "author-note-portlet",      // RoyalRoad
        "author-note-container",    // Generic / our own wrapper
        "authornote-container",
        "authors-note-container",
        "tl-note-container",
        "translator-note-container",
        "chapter-afterword",        // Some sites
        "chapter-foreword"
    )

    /**
     * CSS classes that indicate author note CONTENT (inside the container).
     * Must be combined with container detection or appear with "author-note" specifically.
     */
    private val authorNoteContentClasses = setOf(
        "author-note",              // RoyalRoad and others
        "author-note-content",
        "authornote",
        "authorsnote",
        "authors-note",
        "tl-note",
        "translator-note",
        "editor-note"
        // NOTE: Removed "portlet-body" - too generic, causes false positives
    )

    // =========================================================================
    // TEXT-BASED DETECTION (Secondary - Very Conservative)
    // =========================================================================

    /**
     * Patterns that EXPLICITLY indicate author notes at the START of text.
     * These must be unambiguous - we don't want to catch story content.
     */
    private val explicitAuthorNoteStartPatterns = listOf(
        // "Author's Note:" or "Author Note -" at start (with required punctuation after)
        Regex("""^[\s\n]*Author'?s?\s*Note\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE),

        // "A/N:" at start (must have colon/dash after)
        Regex("""^[\s\n]*A\s*/?\s*N\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE),

        // "[A/N]" or "(A/N)" as prefix (followed by content)
        Regex("""^[\s\n]*[\[\(]A\s*/?\s*N[\]\)]\s*[:：\-–—]?\s*""", RegexOption.IGNORE_CASE),

        // "TL Note:" or "Translator's Note:" at start
        Regex("""^[\s\n]*(?:TL|T/N|Translator'?s?)\s*(?:Note)?\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE),

        // "Editor's Note:" at start
        Regex("""^[\s\n]*(?:ED|E/N|Editor'?s?)\s*(?:Note)?\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE),

        // "PR Note:" (Proofreader) at start
        Regex("""^[\s\n]*(?:PR|P/R|Proofreader'?s?)\s*(?:Note)?\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE),

        // "Note from the author:" at start
        Regex("""^[\s\n]*Note\s+from\s+(?:the\s+)?(?:author|translator|editor|TL)\s*[:：\-–—]\s*""", RegexOption.IGNORE_CASE)
    )

    /**
     * Patterns for COMPLETELY wrapped notes like "[A/N: This is my note]"
     * The ENTIRE text must match, not just contain the pattern.
     */
    private val fullyWrappedNotePatterns = listOf(
        // [A/N: content] - entire text wrapped
        Regex("""^\s*[\[\(]A\s*/?\s*N\s*[:：]\s*.+[\]\)]\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

        // [TL: content] or [TN: content] - entire text wrapped
        Regex("""^\s*[\[\(]T\s*/?\s*[LN]\s*[:：]\s*.+[\]\)]\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

        // 【A/N: content】 (Asian brackets) - entire text wrapped
        Regex("""^\s*【A\s*/?\s*N\s*[:：]\s*.+】\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    // =========================================================================
    // SEPARATOR DETECTION (For positioning, NOT for classifying content)
    // =========================================================================

    private val separatorPatterns = listOf(
        Regex("""^[\s]*[─━═—\-]{5,}[\s]*$"""),  // Increased minimum to 5
        Regex("""^[\s]*[━]{5,}[\s]*$"""),
        Regex("""^[\s]*[*]{5,}[\s]*$"""),
        Regex("""^[\s]*[=]{5,}[\s]*$""")
    )

    // RoyalRoad specific separator (used in provider)
    private const val ROYALROAD_SEPARATOR = "━━━━━━━━━━━━━━━━━━━━"

    // Pattern to extract author name from RoyalRoad-style titles
    private val authorNamePattern = Regex(
        """(?:A\s+note\s+from|Note\s+from|By)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    // =========================================================================
    // PUBLIC DETECTION METHODS
    // =========================================================================

    /**
     * Check if an element is an author note container (the wrapper).
     * This is the PRIMARY and most reliable detection method.
     */
    fun isAuthorNoteContainer(classAttribute: String?): Boolean {
        if (classAttribute.isNullOrBlank()) return false
        val classes = classAttribute.lowercase().split(Regex("\\s+"))
        return classes.any { cssClass ->
            authorNoteContainerClasses.any { containerClass ->
                cssClass == containerClass || cssClass.contains(containerClass)
            }
        }
    }

    /**
     * Check if an element has author note content classes.
     * Should typically be used in conjunction with container detection.
     */
    fun hasAuthorNoteContentClass(classAttribute: String?): Boolean {
        if (classAttribute.isNullOrBlank()) return false
        val classes = classAttribute.lowercase().split(Regex("\\s+"))
        return classes.any { cssClass ->
            authorNoteContentClasses.any { noteClass ->
                cssClass == noteClass || cssClass.contains(noteClass)
            }
        }
    }

    /**
     * Check if text EXPLICITLY indicates an author note.
     * This is very conservative - only matches clear author note markers.
     */
    fun isExplicitAuthorNote(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false

        // Check for explicit start patterns (Author's Note:, A/N:, etc.)
        if (explicitAuthorNoteStartPatterns.any { it.containsMatchIn(trimmed) }) {
            return true
        }

        // Check for fully wrapped notes like "[A/N: This is my note]"
        if (fullyWrappedNotePatterns.any { it.matches(trimmed) }) {
            return true
        }

        return false
    }

    /**
     * Check if text is a separator line.
     * Used for positioning detection, NOT for classifying subsequent content.
     */
    fun isSeparatorLine(text: String): Boolean {
        val trimmed = text.trim()
        return separatorPatterns.any { it.matches(trimmed) } ||
                trimmed == ROYALROAD_SEPARATOR
    }

    /**
     * Detect the position of an author note based on its context.
     */
    fun detectPosition(
        itemIndex: Int,
        totalItems: Int,
        isAfterSeparator: Boolean = false
    ): AuthorNotePosition {
        return when {
            itemIndex <= 1 -> AuthorNotePosition.BEFORE_CONTENT
            isAfterSeparator || itemIndex >= totalItems - 2 -> AuthorNotePosition.AFTER_CONTENT
            else -> AuthorNotePosition.INLINE
        }
    }

    /**
     * Extract the note type label from text.
     */
    fun extractNoteTypeLabel(text: String): String {
        val trimmed = text.trim().lowercase()
        return when {
            trimmed.contains("translator") ||
                    Regex("""^[\s\n]*(?:tl|t/n|t\s*/\s*n)""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                "Translator's Note"

            trimmed.contains("editor") ||
                    Regex("""^[\s\n]*(?:ed|e/n|e\s*/\s*n)""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                "Editor's Note"

            trimmed.contains("proofreader") ||
                    Regex("""^[\s\n]*(?:pr|p/r|p\s*/\s*r)""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                "Proofreader's Note"

            else -> "Author's Note"
        }
    }

    /**
     * Extract author name from a title like "A note from Omega_93".
     */
    fun extractAuthorName(titleText: String): String? {
        val match = authorNamePattern.find(titleText.trim())
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Clean author note text by removing redundant markers at the start.
     */
    fun cleanNoteText(text: String): String {
        var result = text.trim()

        // Remove explicit markers at the start
        for (pattern in explicitAuthorNoteStartPatterns) {
            result = pattern.replace(result, "").trim()
        }

        return result.ifBlank { text.trim() }
    }

    // =========================================================================
    // DEPRECATED / REMOVED METHODS
    // =========================================================================

    /**
     * @deprecated Use isExplicitAuthorNote instead. This was too aggressive.
     */
    @Deprecated("Use isExplicitAuthorNote for conservative detection", ReplaceWith("isExplicitAuthorNote(text)"))
    fun isLikelyAuthorNote(text: String): Boolean = isExplicitAuthorNote(text)

    /**
     * @deprecated This method caused too many false positives by matching
     * common phrases like "If you" or "Please" after separators.
     * Now just delegates to isExplicitAuthorNote.
     */
    @Deprecated("Post-separator heuristics removed due to false positives")
    fun isPostSeparatorAuthorNote(text: String, previousTexts: List<String>): Boolean {
        // Only return true for EXPLICIT author note markers
        // We no longer try to guess based on common phrases
        return isExplicitAuthorNote(text)
    }
}