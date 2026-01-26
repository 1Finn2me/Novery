package com.emptycastle.novery.domain.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// READER SETTINGS
// =============================================================================

/**
 * Comprehensive user preferences for the chapter reader.
 */
data class ReaderSettings(
    // =========================================================================
    // TYPOGRAPHY
    // =========================================================================

    /** Font size in sp (12-32) */
    val fontSize: Int = DEFAULT_FONT_SIZE,

    /** Font family for body text */
    val fontFamily: FontFamily = FontFamily.SYSTEM_SERIF,

    /** Font weight (300-900) */
    val fontWeight: FontWeight = FontWeight.REGULAR,

    /** Line height multiplier (1.0-3.0) */
    val lineHeight: Float = DEFAULT_LINE_HEIGHT,

    /** Letter spacing in em (-0.05 to 0.15) */
    val letterSpacing: Float = DEFAULT_LETTER_SPACING,

    /** Word spacing multiplier (0.8-2.0) */
    val wordSpacing: Float = DEFAULT_WORD_SPACING,

    /** Text alignment */
    val textAlign: TextAlign = TextAlign.LEFT,

    /** Enable hyphenation for justified text */
    val hyphenation: Boolean = true,

    // =========================================================================
    // LAYOUT
    // =========================================================================

    /** Maximum content width */
    val maxWidth: MaxWidth = MaxWidth.LARGE,

    /** Horizontal margin in dp (0-48) */
    val marginHorizontal: Int = DEFAULT_MARGIN_HORIZONTAL,

    /** Vertical margin in dp (0-48) */
    val marginVertical: Int = DEFAULT_MARGIN_VERTICAL,

    /** Paragraph spacing multiplier (0.5-3.0) */
    val paragraphSpacing: Float = DEFAULT_PARAGRAPH_SPACING,

    /** First line indent in em (0-4) */
    val paragraphIndent: Float = DEFAULT_PARAGRAPH_INDENT,

    // =========================================================================
    // APPEARANCE
    // =========================================================================

    /** Reader color theme */
    val theme: ReaderTheme = ReaderTheme.DARK,

    /** Screen brightness override (-1 = system, 0.0-1.0 = manual) */
    val brightness: Float = BRIGHTNESS_SYSTEM,

    /** Use warm/sepia filter for night reading */
    val warmthFilter: Float = 0f,

    /** Show chapter progress indicator */
    val showProgress: Boolean = true,

    /** Progress indicator style */
    val progressStyle: ProgressStyle = ProgressStyle.BAR,

    /** Show reading time estimate */
    val showReadingTime: Boolean = true,

    /** Show chapter title in header */
    val showChapterTitle: Boolean = true,

    // =========================================================================
    // BEHAVIOR
    // =========================================================================

    /** Keep screen on while reading */
    val keepScreenOn: Boolean = true,

    /** Use volume keys for navigation */
    val volumeKeyNavigation: Boolean = false,

    /** Volume key navigation direction */
    val volumeKeyDirection: VolumeKeyDirection = VolumeKeyDirection.NATURAL,

    /** Reading direction */
    val readingDirection: ReadingDirection = ReadingDirection.LTR,

    /** Tap zone configuration */
    val tapZones: TapZoneConfig = TapZoneConfig.DEFAULT,

    /** Enable long press for text selection */
    val longPressSelection: Boolean = true,

    /** Auto-hide controls after delay (ms, 0 = never) */
    val autoHideControlsDelay: Long = DEFAULT_AUTO_HIDE_DELAY,

    // =========================================================================
    // SCROLL & NAVIGATION
    // =========================================================================

    /** Scroll mode */
    val scrollMode: ScrollMode = ScrollMode.CONTINUOUS,

    /** Page turn animation style (for paged mode) */
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,

    /** Smooth scroll animation */
    val smoothScroll: Boolean = true,

    /** Scroll sensitivity multiplier (0.5-2.0) */
    val scrollSensitivity: Float = 1.0f,

    /** Enable edge-to-edge gestures */
    val edgeGestures: Boolean = true,

    // =========================================================================
    // ACCESSIBILITY
    // =========================================================================

    /** High contrast mode override */
    val forceHighContrast: Boolean = false,

    /** Reduce motion/animations */
    val reduceMotion: Boolean = false,

    /** Larger touch targets */
    val largerTouchTargets: Boolean = false
) {

    // =========================================================================
    // COMPUTED PROPERTIES
    // =========================================================================

    /** Font size as TextUnit */
    val fontSizeSp: TextUnit get() = fontSize.sp

    /** Horizontal margin as Dp */
    val marginHorizontalDp: Dp get() = marginHorizontal.dp

    /** Vertical margin as Dp */
    val marginVerticalDp: Dp get() = marginVertical.dp

    /** Whether brightness is manually controlled */
    val hasManualBrightness: Boolean get() = brightness != BRIGHTNESS_SYSTEM

    /** Whether using a dark theme */
    val isDarkTheme: Boolean get() = theme.isDark

    /** Calculated paragraph spacing in dp based on font size */
    val calculatedParagraphSpacing: Dp get() = (fontSize * paragraphSpacing).dp

    /** Calculated line height as a multiplier */
    val calculatedLineHeight: Float get() = lineHeight

    /** Letter spacing as em */
    val letterSpacingEm: TextUnit get() = letterSpacing.sp

    // =========================================================================
    // VALIDATION
    // =========================================================================

    init {
        require(fontSize in MIN_FONT_SIZE..MAX_FONT_SIZE) {
            "Font size must be between $MIN_FONT_SIZE and $MAX_FONT_SIZE"
        }
        require(lineHeight in MIN_LINE_HEIGHT..MAX_LINE_HEIGHT) {
            "Line height must be between $MIN_LINE_HEIGHT and $MAX_LINE_HEIGHT"
        }
        require(letterSpacing in MIN_LETTER_SPACING..MAX_LETTER_SPACING) {
            "Letter spacing must be between $MIN_LETTER_SPACING and $MAX_LETTER_SPACING"
        }
        require(paragraphSpacing in MIN_PARAGRAPH_SPACING..MAX_PARAGRAPH_SPACING) {
            "Paragraph spacing must be between $MIN_PARAGRAPH_SPACING and $MAX_PARAGRAPH_SPACING"
        }
        require(marginHorizontal in MIN_MARGIN..MAX_MARGIN) {
            "Horizontal margin must be between $MIN_MARGIN and $MAX_MARGIN"
        }
        require(marginVertical in MIN_MARGIN..MAX_MARGIN) {
            "Vertical margin must be between $MIN_MARGIN and $MAX_MARGIN"
        }
        require(brightness == BRIGHTNESS_SYSTEM || brightness in 0f..1f) {
            "Brightness must be -1 (system) or between 0.0 and 1.0"
        }
        require(warmthFilter in 0f..1f) {
            "Warmth filter must be between 0.0 and 1.0"
        }
    }

    // =========================================================================
    // BUILDER METHODS
    // =========================================================================

    fun withFontSize(size: Int): ReaderSettings =
        copy(fontSize = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE))

    fun withFontFamily(family: FontFamily): ReaderSettings =
        copy(fontFamily = family)

    fun withTheme(theme: ReaderTheme): ReaderSettings =
        copy(theme = theme)

    fun withLineHeight(height: Float): ReaderSettings =
        copy(lineHeight = height.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT))

    fun withLetterSpacing(spacing: Float): ReaderSettings =
        copy(letterSpacing = spacing.coerceIn(MIN_LETTER_SPACING, MAX_LETTER_SPACING))

    fun withMargins(horizontal: Int = marginHorizontal, vertical: Int = marginVertical): ReaderSettings =
        copy(
            marginHorizontal = horizontal.coerceIn(MIN_MARGIN, MAX_MARGIN),
            marginVertical = vertical.coerceIn(MIN_MARGIN, MAX_MARGIN)
        )

    fun withBrightness(value: Float): ReaderSettings =
        copy(brightness = if (value < 0) BRIGHTNESS_SYSTEM else value.coerceIn(0f, 1f))

    fun resetBrightness(): ReaderSettings =
        copy(brightness = BRIGHTNESS_SYSTEM)

    fun increaseFontSize(step: Int = 1): ReaderSettings =
        withFontSize(fontSize + step)

    fun decreaseFontSize(step: Int = 1): ReaderSettings =
        withFontSize(fontSize - step)

    // =========================================================================
    // COMPANION OBJECT
    // =========================================================================

    companion object {
        // Limits
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 32
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 3.0f
        const val MIN_LETTER_SPACING = -0.05f
        const val MAX_LETTER_SPACING = 0.15f
        const val MIN_WORD_SPACING = 0.8f
        const val MAX_WORD_SPACING = 2.0f
        const val MIN_PARAGRAPH_SPACING = 0.5f
        const val MAX_PARAGRAPH_SPACING = 3.0f
        const val MIN_PARAGRAPH_INDENT = 0f
        const val MAX_PARAGRAPH_INDENT = 4f
        const val MIN_MARGIN = 0
        const val MAX_MARGIN = 48

        // Defaults
        const val DEFAULT_FONT_SIZE = 18
        const val DEFAULT_LINE_HEIGHT = 1.6f
        const val DEFAULT_LETTER_SPACING = 0f
        const val DEFAULT_WORD_SPACING = 1.0f
        const val DEFAULT_PARAGRAPH_SPACING = 1.2f
        const val DEFAULT_PARAGRAPH_INDENT = 0f
        const val DEFAULT_MARGIN_HORIZONTAL = 20
        const val DEFAULT_MARGIN_VERTICAL = 16
        const val DEFAULT_AUTO_HIDE_DELAY = 3000L

        // Special values
        const val BRIGHTNESS_SYSTEM = -1f

        // =====================================================================
        // PRESETS
        // =====================================================================

        /** Default comfortable reading settings */
        val DEFAULT = ReaderSettings()

        /** Compact settings for smaller screens */
        val COMPACT = ReaderSettings(
            fontSize = 14,
            lineHeight = 1.4f,
            paragraphSpacing = 1.0f,
            marginHorizontal = 12,
            marginVertical = 8,
            maxWidth = MaxWidth.FULL
        )

        /** Comfortable settings for long reading sessions */
        val COMFORTABLE = ReaderSettings(
            fontSize = 18,
            fontFamily = FontFamily.LITERATA,
            lineHeight = 1.8f,
            paragraphSpacing = 1.5f,
            marginHorizontal = 24,
            maxWidth = MaxWidth.MEDIUM,
            theme = ReaderTheme.SEPIA
        )

        /** Large text for accessibility */
        val LARGE_TEXT = ReaderSettings(
            fontSize = 24,
            fontFamily = FontFamily.ATKINSON,
            fontWeight = FontWeight.MEDIUM,
            lineHeight = 1.8f,
            letterSpacing = 0.02f,
            paragraphSpacing = 1.5f,
            marginHorizontal = 16,
            largerTouchTargets = true
        )

        /** E-reader style settings */
        val E_READER = ReaderSettings(
            fontSize = 18,
            fontFamily = FontFamily.BOOKERLY,
            lineHeight = 1.6f,
            textAlign = TextAlign.JUSTIFY,
            hyphenation = true,
            paragraphIndent = 1.5f,
            paragraphSpacing = 0.5f,
            marginHorizontal = 24,
            theme = ReaderTheme.SEPIA
        )

        /** Night reading settings */
        val NIGHT = ReaderSettings(
            fontSize = 18,
            fontFamily = FontFamily.SYSTEM_SERIF,
            lineHeight = 1.7f,
            theme = ReaderTheme.AMOLED,
            brightness = 0.3f,
            warmthFilter = 0.3f,
            keepScreenOn = true
        )

        /** Speed reading settings */
        val SPEED_READING = ReaderSettings(
            fontSize = 16,
            fontFamily = FontFamily.SYSTEM_SANS,
            lineHeight = 1.4f,
            letterSpacing = 0.01f,
            paragraphSpacing = 1.0f,
            marginHorizontal = 16,
            maxWidth = MaxWidth.MEDIUM,
            showProgress = true,
            showReadingTime = true
        )

        /** High contrast accessibility */
        val HIGH_CONTRAST = ReaderSettings(
            fontSize = 20,
            fontFamily = FontFamily.ATKINSON,
            fontWeight = FontWeight.MEDIUM,
            lineHeight = 1.7f,
            letterSpacing = 0.02f,
            theme = ReaderTheme.HIGH_CONTRAST_LIGHT,
            forceHighContrast = true,
            largerTouchTargets = true
        )

        /** Dyslexia-friendly settings */
        val DYSLEXIA_FRIENDLY = ReaderSettings(
            fontSize = 18,
            fontFamily = FontFamily.OPEN_DYSLEXIC,
            lineHeight = 2.0f,
            letterSpacing = 0.05f,
            wordSpacing = 1.5f,
            paragraphSpacing = 2.0f,
            textAlign = TextAlign.LEFT,
            hyphenation = false,
            marginHorizontal = 24
        )

        /** Minimal distraction-free settings */
        val MINIMAL = ReaderSettings(
            fontSize = 18,
            fontFamily = FontFamily.IA_WRITER,
            lineHeight = 1.7f,
            maxWidth = MaxWidth.MEDIUM,
            marginHorizontal = 32,
            showProgress = false,
            showReadingTime = false,
            showChapterTitle = false,
            autoHideControlsDelay = 1500L
        )

        /**
         * Get all available presets with their names
         */
        fun getPresets(): List<Pair<String, ReaderSettings>> = listOf(
            "Default" to DEFAULT,
            "Compact" to COMPACT,
            "Comfortable" to COMFORTABLE,
            "Large Text" to LARGE_TEXT,
            "E-Reader" to E_READER,
            "Night Mode" to NIGHT,
            "Speed Reading" to SPEED_READING,
            "High Contrast" to HIGH_CONTRAST,
            "Dyslexia Friendly" to DYSLEXIA_FRIENDLY,
            "Minimal" to MINIMAL
        )
    }
}

// =============================================================================
// FONT FAMILY
// =============================================================================

/**
 * Available font families for the reader.
 * Organized by category for easy UI grouping.
 */
enum class FontFamily(
    val id: String,
    val displayName: String,
    val category: FontCategory,
    val description: String = "",
    val isSystemFont: Boolean = false,
    val supportsAllWeights: Boolean = true
) {
    // =========================================================================
    // SYSTEM FONTS
    // =========================================================================

    SYSTEM_DEFAULT(
        id = "system_default",
        displayName = "System Default",
        category = FontCategory.SYSTEM,
        description = "Default system font",
        isSystemFont = true
    ),

    SYSTEM_SERIF(
        id = "system_serif",
        displayName = "System Serif",
        category = FontCategory.SYSTEM,
        description = "System serif font",
        isSystemFont = true
    ),

    SYSTEM_SANS(
        id = "system_sans",
        displayName = "System Sans",
        category = FontCategory.SYSTEM,
        description = "System sans-serif font",
        isSystemFont = true
    ),

    SYSTEM_MONO(
        id = "system_mono",
        displayName = "System Mono",
        category = FontCategory.SYSTEM,
        description = "System monospace font",
        isSystemFont = true
    ),

    // =========================================================================
    // SERIF FONTS (Traditional Reading)
    // =========================================================================

    LITERATA(
        id = "literata",
        displayName = "Literata",
        category = FontCategory.SERIF,
        description = "Designed for long-form reading, optimized for e-books"
    ),

    MERRIWEATHER(
        id = "merriweather",
        displayName = "Merriweather",
        category = FontCategory.SERIF,
        description = "Pleasant to read with slightly condensed letterforms"
    ),

    LORA(
        id = "lora",
        displayName = "Lora",
        category = FontCategory.SERIF,
        description = "Well-balanced contemporary serif with moderate contrast"
    ),

    CRIMSON_PRO(
        id = "crimson_pro",
        displayName = "Crimson Pro",
        category = FontCategory.SERIF,
        description = "Inspired by classic book typography"
    ),

    SOURCE_SERIF(
        id = "source_serif",
        displayName = "Source Serif Pro",
        category = FontCategory.SERIF,
        description = "Adobe's open-source serif companion to Source Sans"
    ),

    LIBRE_BASKERVILLE(
        id = "libre_baskerville",
        displayName = "Libre Baskerville",
        category = FontCategory.SERIF,
        description = "Classic Baskerville optimized for body text on screens"
    ),

    BOOKERLY(
        id = "bookerly",
        displayName = "Bookerly",
        category = FontCategory.SERIF,
        description = "Amazon's Kindle font, designed for digital reading"
    ),

    GEORGIA(
        id = "georgia",
        displayName = "Georgia",
        category = FontCategory.SERIF,
        description = "Classic screen serif, highly readable at small sizes"
    ),

    CHARTER(
        id = "charter",
        displayName = "Charter",
        category = FontCategory.SERIF,
        description = "Designed for low-resolution output, excellent screen legibility"
    ),

    SPECTRAL(
        id = "spectral",
        displayName = "Spectral",
        category = FontCategory.SERIF,
        description = "Google's first response to screen-first serif design"
    ),

    NEWSREADER(
        id = "newsreader",
        displayName = "Newsreader",
        category = FontCategory.SERIF,
        description = "Inspired by typefaces from the golden age of newspapers"
    ),

    // =========================================================================
    // SANS-SERIF FONTS (Modern/Clean)
    // =========================================================================

    ROBOTO(
        id = "roboto",
        displayName = "Roboto",
        category = FontCategory.SANS_SERIF,
        description = "Google's signature font family"
    ),

    OPEN_SANS(
        id = "open_sans",
        displayName = "Open Sans",
        category = FontCategory.SANS_SERIF,
        description = "Humanist sans-serif with excellent legibility"
    ),

    LATO(
        id = "lato",
        displayName = "Lato",
        category = FontCategory.SANS_SERIF,
        description = "Semi-rounded details with strong structure"
    ),

    SOURCE_SANS(
        id = "source_sans",
        displayName = "Source Sans Pro",
        category = FontCategory.SANS_SERIF,
        description = "Adobe's first open-source font family"
    ),

    INTER(
        id = "inter",
        displayName = "Inter",
        category = FontCategory.SANS_SERIF,
        description = "Designed for computer screens with tall x-height"
    ),

    NUNITO(
        id = "nunito",
        displayName = "Nunito",
        category = FontCategory.SANS_SERIF,
        description = "Balanced sans-serif with rounded terminals"
    ),

    LEXEND(
        id = "lexend",
        displayName = "Lexend",
        category = FontCategory.SANS_SERIF,
        description = "Designed to improve reading proficiency"
    ),

    IBM_PLEX_SANS(
        id = "ibm_plex_sans",
        displayName = "IBM Plex Sans",
        category = FontCategory.SANS_SERIF,
        description = "IBM's corporate typeface, excellent readability"
    ),

    NOTO_SANS(
        id = "noto_sans",
        displayName = "Noto Sans",
        category = FontCategory.SANS_SERIF,
        description = "Google's universal font covering all languages"
    ),

    WORK_SANS(
        id = "work_sans",
        displayName = "Work Sans",
        category = FontCategory.SANS_SERIF,
        description = "Optimized for on-screen text usage"
    ),

    // =========================================================================
    // MONOSPACE FONTS (Code/Technical)
    // =========================================================================

    JETBRAINS_MONO(
        id = "jetbrains_mono",
        displayName = "JetBrains Mono",
        category = FontCategory.MONOSPACE,
        description = "Developer-focused with increased height for better readability"
    ),

    FIRA_CODE(
        id = "fira_code",
        displayName = "Fira Code",
        category = FontCategory.MONOSPACE,
        description = "Monospaced font with programming ligatures"
    ),

    SOURCE_CODE(
        id = "source_code",
        displayName = "Source Code Pro",
        category = FontCategory.MONOSPACE,
        description = "Adobe's monospaced companion to Source Sans/Serif"
    ),

    ROBOTO_MONO(
        id = "roboto_mono",
        displayName = "Roboto Mono",
        category = FontCategory.MONOSPACE,
        description = "Monospaced variant of Roboto"
    ),

    IBM_PLEX_MONO(
        id = "ibm_plex_mono",
        displayName = "IBM Plex Mono",
        category = FontCategory.MONOSPACE,
        description = "IBM's monospaced typeface"
    ),

    COUSINE(
        id = "cousine",
        displayName = "Cousine",
        category = FontCategory.MONOSPACE,
        description = "Designed to be metrically compatible with Courier New"
    ),

    // =========================================================================
    // HANDWRITING & CASUAL FONTS
    // =========================================================================

    CAVEAT(
        id = "caveat",
        displayName = "Caveat",
        category = FontCategory.HANDWRITING,
        description = "Handwritten style, good for casual reading",
        supportsAllWeights = false
    ),

    INDIE_FLOWER(
        id = "indie_flower",
        displayName = "Indie Flower",
        category = FontCategory.HANDWRITING,
        description = "Casual handwritten font",
        supportsAllWeights = false
    ),

    KALAM(
        id = "kalam",
        displayName = "Kalam",
        category = FontCategory.HANDWRITING,
        description = "Handwritten style inspired by Indian languages",
        supportsAllWeights = false
    ),

    PATRICK_HAND(
        id = "patrick_hand",
        displayName = "Patrick Hand",
        category = FontCategory.HANDWRITING,
        description = "Natural handwriting style",
        supportsAllWeights = false
    ),

    // =========================================================================
    // ACCESSIBILITY FONTS
    // =========================================================================

    ATKINSON(
        id = "atkinson",
        displayName = "Atkinson Hyperlegible",
        category = FontCategory.ACCESSIBILITY,
        description = "Designed for low vision readers, maximum character distinction"
    ),

    OPEN_DYSLEXIC(
        id = "open_dyslexic",
        displayName = "OpenDyslexic",
        category = FontCategory.ACCESSIBILITY,
        description = "Designed to mitigate some common reading errors caused by dyslexia",
        supportsAllWeights = false
    ),

    LEXIE_READABLE(
        id = "lexie_readable",
        displayName = "Lexie Readable",
        category = FontCategory.ACCESSIBILITY,
        description = "Designed for dyslexic readers",
        supportsAllWeights = false
    ),

    // =========================================================================
    // SPECIALTY/WRITING FONTS
    // =========================================================================

    IA_WRITER(
        id = "ia_writer",
        displayName = "iA Writer",
        category = FontCategory.SPECIALTY,
        description = "Designed for focused writing and reading"
    ),

    VOLLKORN(
        id = "vollkorn",
        displayName = "Vollkorn",
        category = FontCategory.SPECIALTY,
        description = "German design for body text, quiet and modest"
    ),

    ALEGREYA(
        id = "alegreya",
        displayName = "Alegreya",
        category = FontCategory.SPECIALTY,
        description = "Dynamic and varied rhythm for literature and long texts"
    ),

    CORMORANT(
        id = "cormorant",
        displayName = "Cormorant Garamond",
        category = FontCategory.SPECIALTY,
        description = "Display version of Garamond, elegant and refined"
    ),

    EB_GARAMOND(
        id = "eb_garamond",
        displayName = "EB Garamond",
        category = FontCategory.SPECIALTY,
        description = "Revival of Claude Garamont's humanist typeface"
    );

    companion object {
        /**
         * Get fonts grouped by category
         */
        fun getByCategory(): Map<FontCategory, List<FontFamily>> =
            entries.groupBy { it.category }

        /**
         * Get system fonts only
         */
        fun getSystemFonts(): List<FontFamily> =
            entries.filter { it.isSystemFont }

        /**
         * Get fonts suitable for body text
         */
        fun getBodyFonts(): List<FontFamily> =
            entries.filter {
                it.category in listOf(
                    FontCategory.SERIF,
                    FontCategory.SANS_SERIF,
                    FontCategory.ACCESSIBILITY,
                    FontCategory.SYSTEM
                )
            }

        /**
         * Get accessibility-focused fonts
         */
        fun getAccessibilityFonts(): List<FontFamily> =
            entries.filter { it.category == FontCategory.ACCESSIBILITY }

        /**
         * Find font by ID
         */
        fun fromId(id: String): FontFamily? =
            entries.find { it.id == id }

        /**
         * Get default font for a category
         */
        fun defaultForCategory(category: FontCategory): FontFamily = when (category) {
            FontCategory.SYSTEM -> SYSTEM_DEFAULT
            FontCategory.SERIF -> LITERATA
            FontCategory.SANS_SERIF -> INTER
            FontCategory.MONOSPACE -> JETBRAINS_MONO
            FontCategory.HANDWRITING -> CAVEAT
            FontCategory.ACCESSIBILITY -> ATKINSON
            FontCategory.SPECIALTY -> IA_WRITER
        }
    }
}

/**
 * Categories for organizing font families
 */
enum class FontCategory(
    val displayName: String,
    val description: String,
    val sortOrder: Int
) {
    SYSTEM(
        displayName = "System",
        description = "Built-in system fonts",
        sortOrder = 0
    ),
    SERIF(
        displayName = "Serif",
        description = "Traditional fonts with decorative strokes",
        sortOrder = 1
    ),
    SANS_SERIF(
        displayName = "Sans Serif",
        description = "Clean, modern fonts without serifs",
        sortOrder = 2
    ),
    MONOSPACE(
        displayName = "Monospace",
        description = "Fixed-width fonts for code and technical content",
        sortOrder = 3
    ),
    HANDWRITING(
        displayName = "Handwriting",
        description = "Casual handwritten-style fonts",
        sortOrder = 4
    ),
    ACCESSIBILITY(
        displayName = "Accessibility",
        description = "Fonts designed for improved readability",
        sortOrder = 5
    ),
    SPECIALTY(
        displayName = "Specialty",
        description = "Unique fonts for specific purposes",
        sortOrder = 6
    )
}

// =============================================================================
// FONT WEIGHT
// =============================================================================

/**
 * Font weight options
 */
enum class FontWeight(
    val value: Int,
    val displayName: String
) {
    THIN(100, "Thin"),
    EXTRA_LIGHT(200, "Extra Light"),
    LIGHT(300, "Light"),
    REGULAR(400, "Regular"),
    MEDIUM(500, "Medium"),
    SEMI_BOLD(600, "Semi Bold"),
    BOLD(700, "Bold"),
    EXTRA_BOLD(800, "Extra Bold"),
    BLACK(900, "Black");

    companion object {
        fun fromValue(value: Int): FontWeight =
            entries.minByOrNull { kotlin.math.abs(it.value - value) } ?: REGULAR

        /** Common weights for quick access */
        val COMMON = listOf(LIGHT, REGULAR, MEDIUM, BOLD)
    }
}

// =============================================================================
// MAX WIDTH
// =============================================================================

/**
 * Maximum content width options
 */
enum class MaxWidth(
    val id: String,
    val displayName: String,
    val widthDp: Int,
    val description: String
) {
    NARROW(
        id = "narrow",
        displayName = "Narrow",
        widthDp = 480,
        description = "45-50 characters per line"
    ),
    MEDIUM(
        id = "medium",
        displayName = "Medium",
        widthDp = 600,
        description = "55-65 characters per line"
    ),
    LARGE(
        id = "large",
        displayName = "Large",
        widthDp = 720,
        description = "70-80 characters per line"
    ),
    EXTRA_LARGE(
        id = "extra_large",
        displayName = "Extra Large",
        widthDp = 900,
        description = "80-90 characters per line"
    ),
    FULL(
        id = "full",
        displayName = "Full Width",
        widthDp = Int.MAX_VALUE,
        description = "Use entire screen width"
    );

    val widthDpValue: Dp get() = if (this == FULL) Dp.Unspecified else widthDp.dp

    companion object {
        fun fromId(id: String): MaxWidth =
            entries.find { it.id == id } ?: LARGE
    }
}

// =============================================================================
// TEXT ALIGN
// =============================================================================

/**
 * Text alignment options
 */
enum class TextAlign(
    val id: String,
    val displayName: String,
    val description: String
) {
    LEFT(
        id = "left",
        displayName = "Left",
        description = "Align text to the left margin"
    ),
    RIGHT(
        id = "right",
        displayName = "Right",
        description = "Align text to the right margin"
    ),
    CENTER(
        id = "center",
        displayName = "Center",
        description = "Center text on each line"
    ),
    JUSTIFY(
        id = "justify",
        displayName = "Justify",
        description = "Stretch text to fill the full width"
    );

    companion object {
        fun fromId(id: String): TextAlign =
            entries.find { it.id == id } ?: LEFT
    }
}

// =============================================================================
// READER THEME
// =============================================================================

/**
 * Available color themes for the reader
 */
enum class ReaderTheme(
    val id: String,
    val displayName: String,
    val category: ThemeCategory,
    val isDark: Boolean,
    val isOled: Boolean = false,
    val isHighContrast: Boolean = false,
    val description: String = ""
) {
    // =========================================================================
    // STANDARD THEMES
    // =========================================================================

    LIGHT(
        id = "light",
        displayName = "Light",
        category = ThemeCategory.STANDARD,
        isDark = false,
        description = "Clean white background"
    ),

    DARK(
        id = "dark",
        displayName = "Dark",
        category = ThemeCategory.STANDARD,
        isDark = true,
        description = "Dark gray background"
    ),

    // =========================================================================
    // WARM THEMES
    // =========================================================================

    PAPER(
        id = "paper",
        displayName = "Paper",
        category = ThemeCategory.WARM,
        isDark = false,
        description = "Warm off-white like quality paper"
    ),

    CREAM(
        id = "cream",
        displayName = "Cream",
        category = ThemeCategory.WARM,
        isDark = false,
        description = "Soft cream-colored background"
    ),

    SEPIA(
        id = "sepia",
        displayName = "Sepia",
        category = ThemeCategory.WARM,
        isDark = false,
        description = "Classic aged paper tone"
    ),

    PARCHMENT(
        id = "parchment",
        displayName = "Parchment",
        category = ThemeCategory.WARM,
        isDark = false,
        description = "Old parchment manuscript style"
    ),

    // =========================================================================
    // COOL THEMES
    // =========================================================================

    MIDNIGHT(
        id = "midnight",
        displayName = "Midnight Blue",
        category = ThemeCategory.COOL,
        isDark = true,
        description = "Deep blue night reading"
    ),

    OCEAN(
        id = "ocean",
        displayName = "Ocean",
        category = ThemeCategory.COOL,
        isDark = true,
        description = "Deep ocean blue theme"
    ),

    // =========================================================================
    // SPECIAL THEMES
    // =========================================================================

    AMOLED(
        id = "amoled",
        displayName = "AMOLED Black",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        isOled = true,
        description = "Pure black for OLED displays"
    ),

    CHARCOAL(
        id = "charcoal",
        displayName = "Charcoal",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Neutral dark gray"
    ),

    NORD(
        id = "nord",
        displayName = "Nord",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Arctic north-bluish palette"
    ),

    SOLARIZED_LIGHT(
        id = "solarized_light",
        displayName = "Solarized Light",
        category = ThemeCategory.SPECIAL,
        isDark = false,
        description = "Precision colors for developers"
    ),

    SOLARIZED_DARK(
        id = "solarized_dark",
        displayName = "Solarized Dark",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Dark variant of Solarized"
    ),

    FOREST(
        id = "forest",
        displayName = "Forest",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Calming dark green theme"
    ),

    ROSE(
        id = "rose",
        displayName = "Rose",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Soft rose pink dark theme"
    ),

    LAVENDER(
        id = "lavender",
        displayName = "Lavender",
        category = ThemeCategory.SPECIAL,
        isDark = true,
        description = "Soft purple dark theme"
    ),

    // =========================================================================
    // HIGH CONTRAST THEMES
    // =========================================================================

    HIGH_CONTRAST_LIGHT(
        id = "high_contrast_light",
        displayName = "High Contrast",
        category = ThemeCategory.HIGH_CONTRAST,
        isDark = false,
        isHighContrast = true,
        description = "Maximum readability"
    ),

    HIGH_CONTRAST_DARK(
        id = "high_contrast_dark",
        displayName = "High Contrast Dark",
        category = ThemeCategory.HIGH_CONTRAST,
        isDark = true,
        isHighContrast = true,
        description = "High contrast dark mode"
    );

    companion object {
        fun fromId(id: String): ReaderTheme =
            entries.find { it.id == id } ?: LIGHT

        fun getByCategory(): Map<ThemeCategory, List<ReaderTheme>> =
            entries.groupBy { it.category }

        fun getLightThemes(): List<ReaderTheme> =
            entries.filter { !it.isDark }

        fun getDarkThemes(): List<ReaderTheme> =
            entries.filter { it.isDark }

        fun getOledThemes(): List<ReaderTheme> =
            entries.filter { it.isOled }

        fun getHighContrastThemes(): List<ReaderTheme> =
            entries.filter { it.isHighContrast }
    }
}

/**
 * Categories for organizing themes
 */
enum class ThemeCategory(
    val displayName: String,
    val sortOrder: Int
) {
    STANDARD("Standard", 0),
    WARM("Warm Tones", 1),
    COOL("Cool Tones", 2),
    SPECIAL("Special", 3),
    HIGH_CONTRAST("Accessibility", 4)
}

// =============================================================================
// PROGRESS STYLE
// =============================================================================

/**
 * Progress indicator display style
 */
enum class ProgressStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    NONE(
        id = "none",
        displayName = "None",
        description = "Hide progress indicator"
    ),
    BAR(
        id = "bar",
        displayName = "Progress Bar",
        description = "Thin bar at the bottom"
    ),
    PERCENTAGE(
        id = "percentage",
        displayName = "Percentage",
        description = "Show percentage complete"
    ),
    PAGES(
        id = "pages",
        displayName = "Page Numbers",
        description = "Show current/total pages"
    ),
    DOTS(
        id = "dots",
        displayName = "Dot Indicator",
        description = "Dot-based progress"
    ),
    TIME_LEFT(
        id = "time_left",
        displayName = "Time Remaining",
        description = "Estimated time to finish"
    );

    companion object {
        fun fromId(id: String): ProgressStyle =
            entries.find { it.id == id } ?: BAR
    }
}

// =============================================================================
// SCROLL MODE
// =============================================================================

/**
 * Reading scroll/page mode
 */
enum class ScrollMode(
    val id: String,
    val displayName: String,
    val description: String
) {
    CONTINUOUS(
        id = "continuous",
        displayName = "Continuous Scroll",
        description = "Smooth infinite scrolling"
    ),
    PAGED(
        id = "paged",
        displayName = "Paged",
        description = "Page-by-page reading like a book"
    ),
    PAGED_VERTICAL(
        id = "paged_vertical",
        displayName = "Vertical Pages",
        description = "Vertical page transitions"
    );

    companion object {
        fun fromId(id: String): ScrollMode =
            entries.find { it.id == id } ?: CONTINUOUS
    }
}

// =============================================================================
// PAGE ANIMATION
// =============================================================================

/**
 * Page turn animation styles (for paged mode)
 */
enum class PageAnimation(
    val id: String,
    val displayName: String,
    val description: String
) {
    NONE(
        id = "none",
        displayName = "None",
        description = "Instant page change"
    ),
    SLIDE(
        id = "slide",
        displayName = "Slide",
        description = "Slide horizontally"
    ),
    FADE(
        id = "fade",
        displayName = "Fade",
        description = "Crossfade between pages"
    ),
    CURL(
        id = "curl",
        displayName = "Page Curl",
        description = "Realistic page curl effect"
    ),
    FLIP(
        id = "flip",
        displayName = "Flip",
        description = "3D flip animation"
    );

    companion object {
        fun fromId(id: String): PageAnimation =
            entries.find { it.id == id } ?: SLIDE
    }
}

// =============================================================================
// READING DIRECTION
// =============================================================================

/**
 * Reading/text direction
 */
enum class ReadingDirection(
    val id: String,
    val displayName: String,
    val description: String
) {
    LTR(
        id = "ltr",
        displayName = "Left to Right",
        description = "Standard Western reading direction"
    ),
    RTL(
        id = "rtl",
        displayName = "Right to Left",
        description = "For Arabic, Hebrew, etc."
    ),
    VERTICAL(
        id = "vertical",
        displayName = "Vertical",
        description = "Traditional CJK vertical reading"
    );

    companion object {
        fun fromId(id: String): ReadingDirection =
            entries.find { it.id == id } ?: LTR
    }
}

// =============================================================================
// VOLUME KEY DIRECTION
// =============================================================================

/**
 * Volume key navigation direction preference
 */
enum class VolumeKeyDirection(
    val id: String,
    val displayName: String,
    val description: String
) {
    NATURAL(
        id = "natural",
        displayName = "Natural",
        description = "Up = previous, Down = next"
    ),
    INVERTED(
        id = "inverted",
        displayName = "Inverted",
        description = "Up = next, Down = previous"
    );

    companion object {
        fun fromId(id: String): VolumeKeyDirection =
            entries.find { it.id == id } ?: NATURAL
    }
}

// =============================================================================
// TAP ZONE CONFIGURATION
// =============================================================================

/**
 * Configuration for tap zone behavior
 */
data class TapZoneConfig(
    /** Width of left/right tap zones (0.0-0.5) */
    val horizontalZoneRatio: Float = 0.25f,

    /** Height of top/bottom tap zones (0.0-0.5) */
    val verticalZoneRatio: Float = 0.2f,

    /** Action for left zone tap */
    val leftZoneAction: TapAction = TapAction.PREVIOUS_PAGE,

    /** Action for right zone tap */
    val rightZoneAction: TapAction = TapAction.NEXT_PAGE,

    /** Action for top zone tap */
    val topZoneAction: TapAction = TapAction.TOGGLE_CONTROLS,

    /** Action for bottom zone tap */
    val bottomZoneAction: TapAction = TapAction.TOGGLE_CONTROLS,

    /** Action for center zone tap */
    val centerZoneAction: TapAction = TapAction.TOGGLE_CONTROLS,

    /** Action for double tap */
    val doubleTapAction: TapAction = TapAction.TOGGLE_FULLSCREEN
) {
    companion object {
        val DEFAULT = TapZoneConfig()

        val KINDLE_STYLE = TapZoneConfig(
            horizontalZoneRatio = 0.33f,
            leftZoneAction = TapAction.PREVIOUS_PAGE,
            rightZoneAction = TapAction.NEXT_PAGE,
            centerZoneAction = TapAction.TOGGLE_CONTROLS
        )

        val SIMPLE = TapZoneConfig(
            horizontalZoneRatio = 0f,
            verticalZoneRatio = 0f,
            centerZoneAction = TapAction.TOGGLE_CONTROLS
        )

        val INVERTED = TapZoneConfig(
            leftZoneAction = TapAction.NEXT_PAGE,
            rightZoneAction = TapAction.PREVIOUS_PAGE
        )
    }
}

/**
 * Available tap zone actions
 */
enum class TapAction(
    val id: String,
    val displayName: String
) {
    NONE("none", "None"),
    PREVIOUS_PAGE("previous", "Previous Page"),
    NEXT_PAGE("next", "Next Page"),
    TOGGLE_CONTROLS("controls", "Toggle Controls"),
    TOGGLE_FULLSCREEN("fullscreen", "Toggle Fullscreen"),
    BOOKMARK("bookmark", "Add Bookmark"),
    OPEN_SETTINGS("settings", "Open Settings"),
    OPEN_CHAPTERS("chapters", "Chapter List"),
    START_TTS("tts", "Start TTS"),
    SCROLL_UP("scroll_up", "Scroll Up"),
    SCROLL_DOWN("scroll_down", "Scroll Down");

    companion object {
        fun fromId(id: String): TapAction =
            entries.find { it.id == id } ?: NONE
    }
}

// =============================================================================
// EXTENSIONS
// =============================================================================

/**
 * Check if settings are optimized for accessibility
 */
fun ReaderSettings.isAccessibilityOptimized(): Boolean =
    fontFamily.category == FontCategory.ACCESSIBILITY ||
            forceHighContrast ||
            theme.isHighContrast ||
            fontSize >= 22 ||
            largerTouchTargets

/**
 * Get estimated words per minute based on settings
 */
fun ReaderSettings.estimatedWpm(): Int {
    val baseWpm = 250
    val fontSizeModifier = when {
        fontSize < 14 -> 1.1f
        fontSize > 20 -> 0.9f
        else -> 1.0f
    }
    val lineHeightModifier = when {
        lineHeight < 1.4f -> 1.05f
        lineHeight > 2.0f -> 0.95f
        else -> 1.0f
    }
    return (baseWpm * fontSizeModifier * lineHeightModifier).toInt()
}

/**
 * Create settings optimized for the current time of day
 */
fun ReaderSettings.Companion.forTimeOfDay(hour: Int): ReaderSettings {
    return when (hour) {
        in 6..11 -> COMFORTABLE.copy(theme = ReaderTheme.LIGHT)
        in 12..17 -> COMFORTABLE.copy(theme = ReaderTheme.PAPER)
        in 18..21 -> COMFORTABLE.copy(theme = ReaderTheme.SEPIA, warmthFilter = 0.2f)
        else -> NIGHT
    }
}