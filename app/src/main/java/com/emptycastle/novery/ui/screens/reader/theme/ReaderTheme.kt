package com.emptycastle.novery.ui.screens.reader.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.ReaderTheme

/**
 * Default values and constants for the reader UI
 */
object ReaderDefaults {

    // =========================================================================
    // FONT SETTINGS
    // =========================================================================

    const val MIN_FONT_SIZE = 12
    const val MAX_FONT_SIZE = 32
    const val DEFAULT_FONT_SIZE = 16

    const val MIN_LINE_HEIGHT = 1.2f
    const val MAX_LINE_HEIGHT = 2.5f
    const val DEFAULT_LINE_HEIGHT = 1.6f

    const val MIN_LETTER_SPACING = -0.05f
    const val MAX_LETTER_SPACING = 0.15f
    const val DEFAULT_LETTER_SPACING = 0f

    const val MIN_PARAGRAPH_SPACING = 0f
    const val MAX_PARAGRAPH_SPACING = 48f
    const val DEFAULT_PARAGRAPH_SPACING = 16f

    // =========================================================================
    // SCROLL SETTINGS
    // =========================================================================

    const val SCROLL_OFFSET_PX = -100
    const val SMOOTH_SCROLL_DURATION_MS = 300
    const val PRELOAD_THRESHOLD_ITEMS = 5
    const val SCROLL_DEBOUNCE_MS = 150L

    // =========================================================================
    // PADDING & SPACING
    // =========================================================================

    val ContentHorizontalPadding = 24.dp
    val ContentHorizontalPaddingSmall = 16.dp
    val ContentHorizontalPaddingLarge = 32.dp

    val ContentVerticalPadding = 16.dp
    val ContentVerticalPaddingSmall = 8.dp
    val ContentVerticalPaddingLarge = 24.dp

    val SegmentSpacing = 8.dp
    val ChapterDividerVerticalPadding = 48.dp
    val FabBottomPadding = 32.dp
    val ControlsPadding = 16.dp
    val SettingsPanelPadding = 20.dp

    // =========================================================================
    // SIZES
    // =========================================================================

    val ProgressBarHeight = 3.dp
    val TopBarElevation = 4.dp
    val ThemeButtonHeight = 40.dp
    val ThemeButtonWidth = 48.dp
    val CornerRadius = 8.dp
    val CornerRadiusSmall = 4.dp
    val CornerRadiusLarge = 16.dp
    val CornerRadiusExtraLarge = 24.dp
    val ActiveSegmentBorderWidth = 2.dp
    val IconSize = 24.dp
    val IconSizeSmall = 20.dp
    val IconSizeLarge = 32.dp
    val ButtonMinHeight = 48.dp
    val BottomBarHeight = 80.dp
    val TopBarHeight = 56.dp
    val SettingsSliderHeight = 48.dp
    val ThemePreviewSize = 36.dp

    // =========================================================================
    // ALPHA VALUES
    // =========================================================================

    const val ControlsBackgroundAlpha = 0.98f
    const val LabelAlpha = 0.6f
    const val SecondaryTextAlpha = 0.7f
    const val DisabledAlpha = 0.38f
    const val ActiveSegmentBackgroundAlpha = 0.1f
    const val ActiveSegmentBorderAlpha = 0.3f
    const val TtsIndicatorAlpha = 0.1f
    const val OverlayAlpha = 0.5f
    const val SurfaceOverlayAlpha = 0.08f
    const val HoverAlpha = 0.04f
    const val PressedAlpha = 0.12f
    const val InactiveAlpha = 0.5f
    const val DividerAlpha = 0.12f

    // =========================================================================
    // ANIMATION DURATIONS
    // =========================================================================

    const val FastAnimationDuration = 150
    const val MediumAnimationDuration = 300
    const val SlowAnimationDuration = 500
    const val ControlsFadeDelay = 3000L
    const val ControlsAnimationDuration = 250
    const val SettingsExpandDuration = 300

    // =========================================================================
    // TTS SETTINGS
    // =========================================================================

    const val MIN_TTS_SPEED = 0.5f
    const val MAX_TTS_SPEED = 2.5f
    const val DEFAULT_TTS_SPEED = 1.0f
    const val TTS_SPEED_STEP = 0.1f

    const val MIN_TTS_PITCH = 0.5f
    const val MAX_TTS_PITCH = 2.0f
    const val DEFAULT_TTS_PITCH = 1.0f
    const val TTS_PITCH_STEP = 0.1f

    const val DEFAULT_TTS_VOLUME = 1.0f

    // =========================================================================
    // GESTURE SETTINGS
    // =========================================================================

    const val DOUBLE_TAP_TIMEOUT_MS = 300L
    const val LONG_PRESS_TIMEOUT_MS = 500L
    const val EDGE_TAP_ZONE_RATIO = 0.25f
}

/**
 * Metadata about a reader theme for UI display and categorization
 */
data class ThemeMetadata(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val isHighContrast: Boolean = false,
    val isOled: Boolean = false,
    val category: ThemeCategory = ThemeCategory.STANDARD,
    val description: String = ""
)

/**
 * Categories for organizing themes in settings UI
 */
enum class ThemeCategory(val displayName: String) {
    STANDARD("Standard"),
    WARM("Warm Tones"),
    COOL("Cool Tones"),
    HIGH_CONTRAST("High Contrast"),
    SPECIAL("Special")
}

/**
 * Comprehensive color scheme for the reader based on selected theme
 */
data class ReaderColors(
    // =========================================================================
    // CORE COLORS
    // =========================================================================

    /** Main background color */
    val background: Color,
    /** Elevated surface color (cards, dialogs) */
    val surface: Color,
    /** Primary text color */
    val text: Color,
    /** Secondary/dimmed text color */
    val textSecondary: Color,
    /** Tertiary/hint text color */
    val textTertiary: Color = textSecondary.copy(alpha = 0.6f),

    // =========================================================================
    // ACCENT COLORS
    // =========================================================================

    /** Primary accent color */
    val accent: Color,
    /** Darker variant of accent */
    val accentDark: Color,
    /** Lighter variant of accent */
    val accentLight: Color,
    /** On-accent text color */
    val onAccent: Color = Color.White,

    // =========================================================================
    // UI ELEMENT COLORS
    // =========================================================================

    /** Divider/separator color */
    val divider: Color,
    /** Border color */
    val border: Color,
    /** Primary icon color */
    val icon: Color,
    /** Secondary icon color */
    val iconSecondary: Color,

    // =========================================================================
    // INTERACTIVE STATE COLORS
    // =========================================================================

    /** Button background color */
    val buttonBackground: Color,
    /** Button text color */
    val buttonText: Color,
    /** Secondary button background */
    val buttonSecondaryBackground: Color = surface,
    /** Ripple effect color */
    val ripple: Color,

    // =========================================================================
    // TTS & HIGHLIGHTING COLORS
    // =========================================================================

    /** Currently spoken sentence highlight */
    val sentenceHighlight: Color,
    /** Active segment background highlight */
    val segmentHighlight: Color,
    /** Text selection background */
    val selectionBackground: Color,
    /** Text selection handles */
    val selectionHandle: Color,

    // =========================================================================
    // STATUS COLORS
    // =========================================================================

    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color = accent,

    // =========================================================================
    // CHAPTER NAVIGATION COLORS
    // =========================================================================

    /** Chapter divider/separator background */
    val chapterDividerBackground: Color,
    /** Chapter header text color */
    val chapterHeaderText: Color,
    /** Read chapter indicator */
    val readChapterIndicator: Color = success.copy(alpha = 0.7f),

    // =========================================================================
    // PROGRESS INDICATOR COLORS
    // =========================================================================

    val progressTrack: Color,
    val progressIndicator: Color,

    // =========================================================================
    // OVERLAY COLORS
    // =========================================================================

    /** Modal scrim/dim background */
    val scrim: Color,
    /** Control bar background */
    val controlsBackground: Color,

    val linkColor: Color = accent,

    // =========================================================================
    // METADATA
    // =========================================================================

    val metadata: ThemeMetadata,

    // Code block colors
    val codeBackground: Color = Color(0xFF1E1E1E),
    val codeText: Color = Color(0xFFD4D4D4),

    // System message colors (LitRPG)
    val systemMessageBackground: Color = Color(0xFF0D1117),
    val systemMessageBorder: Color = Color(0xFF30363D),
    val systemMessageText: Color = Color(0xFFC9D1D9),
    val systemMessageAccent: Color = Color(0xFF58A6FF)

) {
    val isDarkTheme: Boolean get() = metadata.isDark
    val isHighContrast: Boolean get() = metadata.isHighContrast
    val isOled: Boolean get() = metadata.isOled

    /**
     * Creates a copy with optional parameter overrides.
     * Used for high contrast mode and other adjustments.
     */
    fun withOverrides(
        text: Color = this.text,
        textSecondary: Color = this.textSecondary,
        textTertiary: Color = this.textTertiary,
        background: Color = this.background,
        surface: Color = this.surface,
        accent: Color = this.accent
    ): ReaderColors = ReaderColors(
        background = background,
        surface = surface,
        text = text,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        accent = accent,
        accentDark = this.accentDark,
        accentLight = this.accentLight,
        onAccent = this.onAccent,
        divider = this.divider,
        border = this.border,
        icon = this.icon,
        iconSecondary = this.iconSecondary,
        buttonBackground = this.buttonBackground,
        buttonText = this.buttonText,
        buttonSecondaryBackground = this.buttonSecondaryBackground,
        ripple = this.ripple,
        sentenceHighlight = this.sentenceHighlight,
        segmentHighlight = this.segmentHighlight,
        selectionBackground = this.selectionBackground,
        selectionHandle = this.selectionHandle,
        success = this.success,
        warning = this.warning,
        error = this.error,
        info = this.info,
        chapterDividerBackground = this.chapterDividerBackground,
        chapterHeaderText = this.chapterHeaderText,
        readChapterIndicator = this.readChapterIndicator,
        progressTrack = this.progressTrack,
        progressIndicator = this.progressIndicator,
        scrim = this.scrim,
        controlsBackground = this.controlsBackground,
        metadata = this.metadata
    )

    companion object {

        // =====================================================================
        // ACCENT COLOR PALETTES
        // =====================================================================

        private object AccentColors {
            // Orange (Default)
            val Orange = Color(0xFFFF6B35)
            val OrangeDark = Color(0xFFE55A2B)
            val OrangeLight = Color(0xFFFF8A5B)

            // Blue
            val Blue = Color(0xFF2196F3)
            val BlueDark = Color(0xFF1976D2)
            val BlueLight = Color(0xFF64B5F6)

            // Green
            val Green = Color(0xFF4CAF50)
            val GreenDark = Color(0xFF388E3C)
            val GreenLight = Color(0xFF81C784)

            // Purple
            val Purple = Color(0xFF9C27B0)
            val PurpleDark = Color(0xFF7B1FA2)
            val PurpleLight = Color(0xFFBA68C8)

            // Teal
            val Teal = Color(0xFF009688)
            val TealDark = Color(0xFF00796B)
            val TealLight = Color(0xFF4DB6AC)

            // Amber
            val Amber = Color(0xFFFFC107)
            val AmberDark = Color(0xFFFFA000)
            val AmberLight = Color(0xFFFFD54F)

            // Pink
            val Pink = Color(0xFFE91E63)
            val PinkDark = Color(0xFFC2185B)
            val PinkLight = Color(0xFFF06292)

            // Cyan
            val Cyan = Color(0xFF00BCD4)
            val CyanDark = Color(0xFF0097A7)
            val CyanLight = Color(0xFF4DD0E1)
        }

        // =====================================================================
        // THEME FACTORY
        // =====================================================================

        fun fromTheme(theme: ReaderTheme): ReaderColors = when (theme) {
            ReaderTheme.LIGHT -> lightTheme()
            ReaderTheme.DARK -> darkTheme()
            ReaderTheme.PAPER -> paperTheme()
            ReaderTheme.CREAM -> creamTheme()
            ReaderTheme.SEPIA -> sepiaTheme()
            ReaderTheme.PARCHMENT -> parchmentTheme()
            ReaderTheme.MIDNIGHT -> midnightTheme()
            ReaderTheme.OCEAN -> oceanTheme()
            ReaderTheme.AMOLED -> amoledTheme()
            ReaderTheme.CHARCOAL -> charcoalTheme()
            ReaderTheme.NORD -> nordTheme()
            ReaderTheme.SOLARIZED_LIGHT -> solarizedLightTheme()
            ReaderTheme.SOLARIZED_DARK -> solarizedDarkTheme()
            ReaderTheme.FOREST -> forestTheme()
            ReaderTheme.ROSE -> roseTheme()
            ReaderTheme.LAVENDER -> lavenderTheme()
            ReaderTheme.HIGH_CONTRAST_LIGHT -> highContrastLightTheme()
            ReaderTheme.HIGH_CONTRAST_DARK -> highContrastDarkTheme()
        }

        // =====================================================================
        // LIGHT THEMES
        // =====================================================================

        fun lightTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFF5F5F5),
            text = Color(0xFF1C1B1F),
            textSecondary = Color(0xFF49454F),

            accent = AccentColors.Orange,
            accentDark = AccentColors.OrangeDark,
            accentLight = AccentColors.OrangeLight,

            divider = Color(0xFFE0E0E0),
            border = Color(0xFFCAC4D0),
            icon = Color(0xFF49454F),
            iconSecondary = Color(0xFF79747E),

            buttonBackground = AccentColors.Orange,
            buttonText = Color.White,
            ripple = Color(0xFF49454F),

            sentenceHighlight = AccentColors.Orange.copy(alpha = 0.25f),
            segmentHighlight = AccentColors.Orange.copy(alpha = 0.08f),
            selectionBackground = AccentColors.Blue.copy(alpha = 0.3f),
            selectionHandle = AccentColors.Blue,

            success = AccentColors.Green,
            warning = Color(0xFFFFA000),
            error = Color(0xFFD32F2F),

            chapterDividerBackground = Color(0xFFF0F0F0),
            chapterHeaderText = Color(0xFF49454F),

            codeBackground = Color(0xFFF6F8FA),
            codeText = Color(0xFF24292F),
            systemMessageBackground = Color(0xFFF0F6FC),
            systemMessageBorder = Color(0xFFD0D7DE),
            systemMessageText = Color(0xFF24292F),
            systemMessageAccent = Color(0xFF0969DA),

            progressTrack = Color(0xFFE0E0E0),
            progressIndicator = AccentColors.Orange,

            scrim = Color.Black.copy(alpha = 0.32f),
            controlsBackground = Color(0xFFFFFBFE).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "light",
                displayName = "Light",
                isDark = false,
                category = ThemeCategory.STANDARD,
                description = "Clean white background with dark text"
            )
        )

        fun paperTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFFAF8F5),
            surface = Color(0xFFF2F0ED),
            text = Color(0xFF2D2D2D),
            textSecondary = Color(0xFF5A5A5A),

            accent = Color(0xFF8B4513),
            accentDark = Color(0xFF6B3410),
            accentLight = Color(0xFFA0522D),

            divider = Color(0xFFE5E2DD),
            border = Color(0xFFD4D1CC),
            icon = Color(0xFF5A5A5A),
            iconSecondary = Color(0xFF7A7A7A),

            buttonBackground = Color(0xFF8B4513),
            buttonText = Color.White,
            ripple = Color(0xFF5A5A5A),

            sentenceHighlight = Color(0xFF8B4513).copy(alpha = 0.2f),
            segmentHighlight = Color(0xFF8B4513).copy(alpha = 0.06f),
            selectionBackground = Color(0xFF8B4513).copy(alpha = 0.25f),
            selectionHandle = Color(0xFF8B4513),

            success = Color(0xFF558B2F),
            warning = Color(0xFFE65100),
            error = Color(0xFFC62828),

            chapterDividerBackground = Color(0xFFEBE8E3),
            chapterHeaderText = Color(0xFF5A5A5A),

            progressTrack = Color(0xFFE5E2DD),
            progressIndicator = Color(0xFF8B4513),

            scrim = Color.Black.copy(alpha = 0.32f),
            controlsBackground = Color(0xFFFAF8F5).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "paper",
                displayName = "Paper",
                isDark = false,
                category = ThemeCategory.WARM,
                description = "Warm off-white like quality paper"
            )
        )

        fun creamTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFFFFDD0),
            surface = Color(0xFFF5F3C0),
            text = Color(0xFF3D3D3D),
            textSecondary = Color(0xFF666666),

            accent = Color(0xFFB8860B),
            accentDark = Color(0xFF8B6914),
            accentLight = Color(0xFFDAA520),

            divider = Color(0xFFE8E6B8),
            border = Color(0xFFD4D2A4),
            icon = Color(0xFF666666),
            iconSecondary = Color(0xFF888888),

            buttonBackground = Color(0xFFB8860B),
            buttonText = Color.White,
            ripple = Color(0xFF666666),

            sentenceHighlight = Color(0xFFB8860B).copy(alpha = 0.25f),
            segmentHighlight = Color(0xFFB8860B).copy(alpha = 0.08f),
            selectionBackground = Color(0xFFB8860B).copy(alpha = 0.3f),
            selectionHandle = Color(0xFFB8860B),

            success = Color(0xFF689F38),
            warning = Color(0xFFEF6C00),
            error = Color(0xFFD32F2F),

            chapterDividerBackground = Color(0xFFF0EEB0),
            chapterHeaderText = Color(0xFF666666),

            progressTrack = Color(0xFFE8E6B8),
            progressIndicator = Color(0xFFB8860B),

            scrim = Color.Black.copy(alpha = 0.32f),
            controlsBackground = Color(0xFFFFFDD0).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "cream",
                displayName = "Cream",
                isDark = false,
                category = ThemeCategory.WARM,
                description = "Soft cream-colored background"
            )
        )

        // =====================================================================
        // SEPIA THEMES
        // =====================================================================

        fun sepiaTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFF4ECD8),
            surface = Color(0xFFEAE2CE),
            text = Color(0xFF5B4636),
            textSecondary = Color(0xFF7A6B5A),

            accent = Color(0xFFD4A574),
            accentDark = Color(0xFFB8895A),
            accentLight = Color(0xFFE5C8A8),

            divider = Color(0xFFD4C4A8),
            border = Color(0xFFC4B498),
            icon = Color(0xFF7A6B5A),
            iconSecondary = Color(0xFF9A8B7A),

            buttonBackground = Color(0xFFD4A574),
            buttonText = Color(0xFF3D2E1F),
            onAccent = Color(0xFF3D2E1F),
            ripple = Color(0xFF5B4636),

            sentenceHighlight = Color(0xFFD4A574).copy(alpha = 0.35f),
            segmentHighlight = Color(0xFFD4A574).copy(alpha = 0.12f),
            selectionBackground = Color(0xFFD4A574).copy(alpha = 0.4f),
            selectionHandle = Color(0xFFD4A574),

            success = Color(0xFF7CB342),
            warning = Color(0xFFFF8F00),
            error = Color(0xFFD84315),

            chapterDividerBackground = Color(0xFFE6DCC8),
            chapterHeaderText = Color(0xFF7A6B5A),

            progressTrack = Color(0xFFD4C4A8),
            progressIndicator = Color(0xFFD4A574),

            scrim = Color(0xFF3D2E1F).copy(alpha = 0.32f),
            controlsBackground = Color(0xFFF4ECD8).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            codeBackground = Color(0xFFE8DCC8),
            codeText = Color(0xFF433422),
            systemMessageBackground = Color(0xFFEDE5D6),
            systemMessageBorder = Color(0xFFCDC4B5),
            systemMessageText = Color(0xFF433422),
            systemMessageAccent = Color(0xFF8B7355),
            metadata = ThemeMetadata(
                id = "sepia",
                displayName = "Sepia",
                isDark = false,
                category = ThemeCategory.WARM,
                description = "Classic aged paper sepia tone"
            )
        )

        fun parchmentTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFF5E6C8),
            surface = Color(0xFFEBDCBE),
            text = Color(0xFF4A3C2E),
            textSecondary = Color(0xFF6B5D4E),

            accent = Color(0xFF8B5E34),
            accentDark = Color(0xFF6B4A28),
            accentLight = Color(0xFFA67C52),

            divider = Color(0xFFD5C6A8),
            border = Color(0xFFC5B698),
            icon = Color(0xFF6B5D4E),
            iconSecondary = Color(0xFF8B7D6E),

            buttonBackground = Color(0xFF8B5E34),
            buttonText = Color.White,
            ripple = Color(0xFF4A3C2E),

            sentenceHighlight = Color(0xFF8B5E34).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFF8B5E34).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF8B5E34).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF8B5E34),

            success = Color(0xFF6B8E23),
            warning = Color(0xFFD4A017),
            error = Color(0xFFC41E3A),

            chapterDividerBackground = Color(0xFFE5D6B8),
            chapterHeaderText = Color(0xFF6B5D4E),

            progressTrack = Color(0xFFD5C6A8),
            progressIndicator = Color(0xFF8B5E34),

            scrim = Color(0xFF4A3C2E).copy(alpha = 0.32f),
            controlsBackground = Color(0xFFF5E6C8).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "parchment",
                displayName = "Parchment",
                isDark = false,
                category = ThemeCategory.WARM,
                description = "Old parchment manuscript style"
            )
        )

        // =====================================================================
        // DARK THEMES
        // =====================================================================

        fun darkTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF26272C),
            surface = Color(0xFF39363F),
            text = Color(0xFFE6E1E5),
            textSecondary = Color(0xFFCAC4D0),

            accent = AccentColors.Orange,
            accentDark = AccentColors.OrangeDark,
            accentLight = AccentColors.OrangeLight,

            divider = Color(0xFF49454F),
            border = Color(0xFF49454F),
            icon = Color(0xFFCAC4D0),
            iconSecondary = Color(0xFF938F99),

            buttonBackground = AccentColors.Orange,
            buttonText = Color.White,
            ripple = Color(0xFFE6E1E5),

            sentenceHighlight = AccentColors.Orange.copy(alpha = 0.35f),
            segmentHighlight = AccentColors.Orange.copy(alpha = 0.12f),
            selectionBackground = AccentColors.Blue.copy(alpha = 0.4f),
            selectionHandle = AccentColors.Blue,

            success = AccentColors.GreenLight,
            warning = Color(0xFFFFB74D),
            error = Color(0xFFEF5350),

            chapterDividerBackground = Color(0xFF252328),
            chapterHeaderText = Color(0xFFCAC4D0),

            progressTrack = Color(0xFF49454F),
            progressIndicator = AccentColors.Orange,

            codeBackground = Color(0xFF1E1E1E),
            codeText = Color(0xFFD4D4D4),
            systemMessageBackground = Color(0xFF0D1117),
            systemMessageBorder = Color(0xFF30363D),
            systemMessageText = Color(0xFFC9D1D9),
            systemMessageAccent = Color(0xFF58A6FF),

            scrim = Color.Black.copy(alpha = 0.5f),
            controlsBackground = Color(0xFF1C1B1F).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "dark",
                displayName = "Dark",
                isDark = true,
                category = ThemeCategory.STANDARD,
                description = "Dark gray background, easy on the eyes"
            )
        )

        fun amoledTheme(): ReaderColors = ReaderColors(
            background = Color.Black,
            surface = Color(0xFF0D0D0D),
            text = Color(0xFFE8E8E8),
            textSecondary = Color(0xFFB0B0B0),

            accent = AccentColors.Orange,
            accentDark = AccentColors.OrangeDark,
            accentLight = AccentColors.OrangeLight,

            divider = Color(0xFF2A2A2A),
            border = Color(0xFF3A3A3A),
            icon = Color(0xFFB0B0B0),
            iconSecondary = Color(0xFF808080),

            buttonBackground = AccentColors.Orange,
            buttonText = Color.White,
            ripple = Color(0xFFE8E8E8),

            sentenceHighlight = AccentColors.Orange.copy(alpha = 0.4f),
            segmentHighlight = AccentColors.Orange.copy(alpha = 0.15f),
            selectionBackground = AccentColors.Blue.copy(alpha = 0.45f),
            selectionHandle = AccentColors.Blue,

            success = AccentColors.GreenLight,
            warning = Color(0xFFFFB74D),
            error = Color(0xFFFF5252),

            chapterDividerBackground = Color(0xFF0A0A0A),
            chapterHeaderText = Color(0xFFB0B0B0),

            progressTrack = Color(0xFF2A2A2A),
            progressIndicator = AccentColors.Orange,

            scrim = Color.Black.copy(alpha = 0.7f),
            controlsBackground = Color.Black.copy(alpha = 0.95f),

            metadata = ThemeMetadata(
                id = "amoled",
                displayName = "AMOLED Black",
                isDark = true,
                isOled = true,
                category = ThemeCategory.SPECIAL,
                description = "Pure black for OLED displays"
            )
        )

        fun midnightTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF121620),
            surface = Color(0xFF1A2030),
            text = Color(0xFFE0E4EA),
            textSecondary = Color(0xFFA8B0C0),

            accent = Color(0xFF6B9FFF),
            accentDark = Color(0xFF4A7FDF),
            accentLight = Color(0xFF8BB8FF),

            divider = Color(0xFF2A3444),
            border = Color(0xFF3A4454),
            icon = Color(0xFFA8B0C0),
            iconSecondary = Color(0xFF788090),

            buttonBackground = Color(0xFF6B9FFF),
            buttonText = Color(0xFF121620),
            onAccent = Color(0xFF121620),
            ripple = Color(0xFFE0E4EA),

            sentenceHighlight = Color(0xFF6B9FFF).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFF6B9FFF).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF6B9FFF).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF6B9FFF),

            success = Color(0xFF66BB6A),
            warning = Color(0xFFFFCA28),
            error = Color(0xFFEF5350),

            chapterDividerBackground = Color(0xFF161C28),
            chapterHeaderText = Color(0xFFA8B0C0),

            progressTrack = Color(0xFF2A3444),
            progressIndicator = Color(0xFF6B9FFF),

            scrim = Color.Black.copy(alpha = 0.55f),
            controlsBackground = Color(0xFF121620).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "midnight",
                displayName = "Midnight Blue",
                isDark = true,
                category = ThemeCategory.COOL,
                description = "Deep blue night reading theme"
            )
        )

        fun charcoalTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF232323),
            surface = Color(0xFF2D2D2D),
            text = Color(0xFFDDDDDD),
            textSecondary = Color(0xFFAAAAAA),

            accent = Color(0xFF5C9CE6),
            accentDark = Color(0xFF3D7DC7),
            accentLight = Color(0xFF7BB5F5),

            divider = Color(0xFF3D3D3D),
            border = Color(0xFF4A4A4A),
            icon = Color(0xFFAAAAAA),
            iconSecondary = Color(0xFF888888),

            buttonBackground = Color(0xFF5C9CE6),
            buttonText = Color.White,
            ripple = Color(0xFFDDDDDD),

            sentenceHighlight = Color(0xFF5C9CE6).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFF5C9CE6).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF5C9CE6).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF5C9CE6),

            success = Color(0xFF81C784),
            warning = Color(0xFFFFB74D),
            error = Color(0xFFE57373),

            chapterDividerBackground = Color(0xFF292929),
            chapterHeaderText = Color(0xFFAAAAAA),

            progressTrack = Color(0xFF3D3D3D),
            progressIndicator = Color(0xFF5C9CE6),

            scrim = Color.Black.copy(alpha = 0.5f),
            controlsBackground = Color(0xFF232323).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "charcoal",
                displayName = "Charcoal",
                isDark = true,
                category = ThemeCategory.STANDARD,
                description = "Neutral dark gray theme"
            )
        )

        // =====================================================================
        // SPECIAL THEMES
        // =====================================================================

        fun nordTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF2E3440),
            surface = Color(0xFF3B4252),
            text = Color(0xFFECEFF4),
            textSecondary = Color(0xFFD8DEE9),

            accent = Color(0xFF88C0D0),
            accentDark = Color(0xFF5E81AC),
            accentLight = Color(0xFF8FBCBB),

            divider = Color(0xFF4C566A),
            border = Color(0xFF4C566A),
            icon = Color(0xFFD8DEE9),
            iconSecondary = Color(0xFF9BA4B4),

            buttonBackground = Color(0xFF88C0D0),
            buttonText = Color(0xFF2E3440),
            onAccent = Color(0xFF2E3440),
            ripple = Color(0xFFECEFF4),

            sentenceHighlight = Color(0xFF88C0D0).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFF88C0D0).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF88C0D0).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF88C0D0),

            success = Color(0xFFA3BE8C),
            warning = Color(0xFFEBCB8B),
            error = Color(0xFFBF616A),

            chapterDividerBackground = Color(0xFF353C4A),
            chapterHeaderText = Color(0xFFD8DEE9),

            progressTrack = Color(0xFF4C566A),
            progressIndicator = Color(0xFF88C0D0),

            scrim = Color(0xFF2E3440).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF2E3440).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "nord",
                displayName = "Nord",
                isDark = true,
                category = ThemeCategory.SPECIAL,
                description = "Arctic, north-bluish color palette"
            )
        )

        fun solarizedDarkTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF002B36),
            surface = Color(0xFF073642),
            text = Color(0xFF839496),
            textSecondary = Color(0xFF657B83),

            accent = Color(0xFFB58900),
            accentDark = Color(0xFF8B6914),
            accentLight = Color(0xFFD4A017),

            divider = Color(0xFF094555),
            border = Color(0xFF0A5568),
            icon = Color(0xFF839496),
            iconSecondary = Color(0xFF657B83),

            buttonBackground = Color(0xFFB58900),
            buttonText = Color(0xFF002B36),
            onAccent = Color(0xFF002B36),
            ripple = Color(0xFF839496),

            sentenceHighlight = Color(0xFFB58900).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFFB58900).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF268BD2).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF268BD2),

            success = Color(0xFF859900),
            warning = Color(0xFFCB4B16),
            error = Color(0xFFDC322F),

            chapterDividerBackground = Color(0xFF053540),
            chapterHeaderText = Color(0xFF657B83),

            progressTrack = Color(0xFF094555),
            progressIndicator = Color(0xFFB58900),

            scrim = Color(0xFF002B36).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF002B36).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "solarized_dark",
                displayName = "Solarized Dark",
                isDark = true,
                category = ThemeCategory.SPECIAL,
                description = "Precision colors for developers"
            )
        )

        fun solarizedLightTheme(): ReaderColors = ReaderColors(
            background = Color(0xFFFDF6E3),
            surface = Color(0xFFEEE8D5),
            text = Color(0xFF657B83),
            textSecondary = Color(0xFF839496),

            accent = Color(0xFFB58900),
            accentDark = Color(0xFF8B6914),
            accentLight = Color(0xFFD4A017),

            divider = Color(0xFFE0DACC),
            border = Color(0xFFD0CABC),
            icon = Color(0xFF657B83),
            iconSecondary = Color(0xFF839496),

            buttonBackground = Color(0xFFB58900),
            buttonText = Color.White,
            ripple = Color(0xFF657B83),

            sentenceHighlight = Color(0xFFB58900).copy(alpha = 0.25f),
            segmentHighlight = Color(0xFFB58900).copy(alpha = 0.08f),
            selectionBackground = Color(0xFF268BD2).copy(alpha = 0.3f),
            selectionHandle = Color(0xFF268BD2),

            success = Color(0xFF859900),
            warning = Color(0xFFCB4B16),
            error = Color(0xFFDC322F),

            chapterDividerBackground = Color(0xFFF5EFDC),
            chapterHeaderText = Color(0xFF839496),

            progressTrack = Color(0xFFE0DACC),
            progressIndicator = Color(0xFFB58900),

            scrim = Color(0xFF002B36).copy(alpha = 0.32f),
            controlsBackground = Color(0xFFFDF6E3).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "solarized_light",
                displayName = "Solarized Light",
                isDark = false,
                category = ThemeCategory.SPECIAL,
                description = "Light variant of Solarized"
            )
        )

        fun forestTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF1A2318),
            surface = Color(0xFF232D20),
            text = Color(0xFFD4DDD2),
            textSecondary = Color(0xFFA8B5A6),

            accent = Color(0xFF7CB342),
            accentDark = Color(0xFF558B2F),
            accentLight = Color(0xFF9CCC65),

            divider = Color(0xFF2D3A2A),
            border = Color(0xFF3A4A37),
            icon = Color(0xFFA8B5A6),
            iconSecondary = Color(0xFF7A8A77),

            buttonBackground = Color(0xFF7CB342),
            buttonText = Color.White,
            ripple = Color(0xFFD4DDD2),

            sentenceHighlight = Color(0xFF7CB342).copy(alpha = 0.3f),
            segmentHighlight = Color(0xFF7CB342).copy(alpha = 0.1f),
            selectionBackground = Color(0xFF7CB342).copy(alpha = 0.35f),
            selectionHandle = Color(0xFF7CB342),

            success = Color(0xFF8BC34A),
            warning = Color(0xFFFFB300),
            error = Color(0xFFE57373),

            chapterDividerBackground = Color(0xFF1E271C),
            chapterHeaderText = Color(0xFFA8B5A6),

            progressTrack = Color(0xFF2D3A2A),
            progressIndicator = Color(0xFF7CB342),

            scrim = Color(0xFF1A2318).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF1A2318).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "forest",
                displayName = "Forest",
                isDark = true,
                category = ThemeCategory.SPECIAL,
                description = "Calming dark green theme"
            )
        )

        fun oceanTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF0D1B2A),
            surface = Color(0xFF1B2838),
            text = Color(0xFFE0E7EF),
            textSecondary = Color(0xFFB0BEC5),

            accent = AccentColors.Cyan,
            accentDark = AccentColors.CyanDark,
            accentLight = AccentColors.CyanLight,

            divider = Color(0xFF263545),
            border = Color(0xFF324555),
            icon = Color(0xFFB0BEC5),
            iconSecondary = Color(0xFF78909C),

            buttonBackground = AccentColors.Cyan,
            buttonText = Color(0xFF0D1B2A),
            onAccent = Color(0xFF0D1B2A),
            ripple = Color(0xFFE0E7EF),

            sentenceHighlight = AccentColors.Cyan.copy(alpha = 0.3f),
            segmentHighlight = AccentColors.Cyan.copy(alpha = 0.1f),
            selectionBackground = AccentColors.Cyan.copy(alpha = 0.35f),
            selectionHandle = AccentColors.Cyan,

            success = AccentColors.TealLight,
            warning = Color(0xFFFFB74D),
            error = Color(0xFFE57373),

            chapterDividerBackground = Color(0xFF142330),
            chapterHeaderText = Color(0xFFB0BEC5),

            progressTrack = Color(0xFF263545),
            progressIndicator = AccentColors.Cyan,

            scrim = Color(0xFF0D1B2A).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF0D1B2A).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "ocean",
                displayName = "Ocean",
                isDark = true,
                category = ThemeCategory.COOL,
                description = "Deep ocean blue theme"
            )
        )

        fun roseTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF1A1518),
            surface = Color(0xFF251E22),
            text = Color(0xFFF0E4E8),
            textSecondary = Color(0xFFC4B0B8),

            accent = AccentColors.Pink,
            accentDark = AccentColors.PinkDark,
            accentLight = AccentColors.PinkLight,

            divider = Color(0xFF352A30),
            border = Color(0xFF453A40),
            icon = Color(0xFFC4B0B8),
            iconSecondary = Color(0xFF948088),

            buttonBackground = AccentColors.Pink,
            buttonText = Color.White,
            ripple = Color(0xFFF0E4E8),

            sentenceHighlight = AccentColors.Pink.copy(alpha = 0.3f),
            segmentHighlight = AccentColors.Pink.copy(alpha = 0.1f),
            selectionBackground = AccentColors.Pink.copy(alpha = 0.35f),
            selectionHandle = AccentColors.Pink,

            success = Color(0xFF81C784),
            warning = Color(0xFFFFB74D),
            error = Color(0xFFE57373),

            chapterDividerBackground = Color(0xFF1E1A1C),
            chapterHeaderText = Color(0xFFC4B0B8),

            progressTrack = Color(0xFF352A30),
            progressIndicator = AccentColors.Pink,

            scrim = Color(0xFF1A1518).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF1A1518).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "rose",
                displayName = "Rose",
                isDark = true,
                category = ThemeCategory.SPECIAL,
                description = "Soft rose pink dark theme"
            )
        )

        fun lavenderTheme(): ReaderColors = ReaderColors(
            background = Color(0xFF17151A),
            surface = Color(0xFF211E25),
            text = Color(0xFFE8E4F0),
            textSecondary = Color(0xFFB8B0C8),

            accent = AccentColors.Purple,
            accentDark = AccentColors.PurpleDark,
            accentLight = AccentColors.PurpleLight,

            divider = Color(0xFF302A38),
            border = Color(0xFF403848),
            icon = Color(0xFFB8B0C8),
            iconSecondary = Color(0xFF888098),

            buttonBackground = AccentColors.Purple,
            buttonText = Color.White,
            ripple = Color(0xFFE8E4F0),

            sentenceHighlight = AccentColors.Purple.copy(alpha = 0.3f),
            segmentHighlight = AccentColors.Purple.copy(alpha = 0.1f),
            selectionBackground = AccentColors.Purple.copy(alpha = 0.35f),
            selectionHandle = AccentColors.Purple,

            success = Color(0xFF81C784),
            warning = Color(0xFFFFB74D),
            error = Color(0xFFE57373),

            chapterDividerBackground = Color(0xFF1C1A20),
            chapterHeaderText = Color(0xFFB8B0C8),

            progressTrack = Color(0xFF302A38),
            progressIndicator = AccentColors.Purple,

            scrim = Color(0xFF17151A).copy(alpha = 0.6f),
            controlsBackground = Color(0xFF17151A).copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),

            metadata = ThemeMetadata(
                id = "lavender",
                displayName = "Lavender",
                isDark = true,
                category = ThemeCategory.SPECIAL,
                description = "Soft purple dark theme"
            )
        )

        // =====================================================================
        // HIGH CONTRAST THEMES
        // =====================================================================

        fun highContrastLightTheme(): ReaderColors = ReaderColors(
            background = Color.White,
            surface = Color(0xFFF5F5F5),
            text = Color.Black,
            textSecondary = Color(0xFF333333),

            accent = Color(0xFF0066CC),
            accentDark = Color(0xFF004C99),
            accentLight = Color(0xFF3399FF),

            divider = Color(0xFFCCCCCC),
            border = Color(0xFF999999),
            icon = Color.Black,
            iconSecondary = Color(0xFF333333),

            buttonBackground = Color(0xFF0066CC),
            buttonText = Color.White,
            ripple = Color.Black,

            sentenceHighlight = Color(0xFFFFFF00),
            segmentHighlight = Color(0xFFFFFF00).copy(alpha = 0.3f),
            selectionBackground = Color(0xFF0066CC).copy(alpha = 0.4f),
            selectionHandle = Color(0xFF0066CC),

            success = Color(0xFF008000),
            warning = Color(0xFFCC6600),
            error = Color(0xFFCC0000),

            chapterDividerBackground = Color(0xFFEEEEEE),
            chapterHeaderText = Color(0xFF333333),

            progressTrack = Color(0xFFCCCCCC),
            progressIndicator = Color(0xFF0066CC),

            scrim = Color.Black.copy(alpha = 0.4f),
            controlsBackground = Color.White.copy(alpha = 0.98f),

            metadata = ThemeMetadata(
                id = "high_contrast_light",
                displayName = "High Contrast",
                isDark = false,
                isHighContrast = true,
                category = ThemeCategory.HIGH_CONTRAST,
                description = "Maximum readability with high contrast"
            )
        )

        fun highContrastDarkTheme(): ReaderColors = ReaderColors(
            background = Color.Black,
            surface = Color(0xFF0A0A0A),
            text = Color.White,
            textSecondary = Color(0xFFCCCCCC),

            accent = Color(0xFF66B3FF),
            accentDark = Color(0xFF3399FF),
            accentLight = Color(0xFF99CCFF),

            divider = Color(0xFF444444),
            border = Color(0xFF666666),
            icon = Color.White,
            iconSecondary = Color(0xFFCCCCCC),

            buttonBackground = Color(0xFF66B3FF),
            buttonText = Color.Black,
            onAccent = Color.Black,
            ripple = Color.White,

            sentenceHighlight = Color(0xFFFFFF00).copy(alpha = 0.8f),
            segmentHighlight = Color(0xFFFFFF00).copy(alpha = 0.2f),
            selectionBackground = Color(0xFF66B3FF).copy(alpha = 0.5f),
            selectionHandle = Color(0xFF66B3FF),

            success = Color(0xFF00FF00),
            warning = Color(0xFFFFAA00),
            error = Color(0xFFFF4444),

            chapterDividerBackground = Color(0xFF111111),
            chapterHeaderText = Color(0xFFCCCCCC),

            progressTrack = Color(0xFF444444),
            progressIndicator = Color(0xFF66B3FF),

            scrim = Color.Black.copy(alpha = 0.7f),
            controlsBackground = Color.Black.copy(alpha = 0.98f),

            metadata = ThemeMetadata(
                id = "high_contrast_dark",
                displayName = "High Contrast Dark",
                isDark = true,
                isHighContrast = true,
                category = ThemeCategory.HIGH_CONTRAST,
                description = "High contrast dark mode for accessibility"
            )
        )

        // =====================================================================
        // THEME COLLECTIONS
        // =====================================================================

        /**
         * Get all available themes with their metadata
         */
        fun getAllThemes(): List<ReaderColors> = listOf(
            // Standard
            lightTheme(),
            darkTheme(),

            // Warm
            paperTheme(),
            creamTheme(),
            sepiaTheme(),
            parchmentTheme(),

            // Cool
            midnightTheme(),
            oceanTheme(),

            // Special
            amoledTheme(),
            charcoalTheme(),
            nordTheme(),
            solarizedLightTheme(),
            solarizedDarkTheme(),
            forestTheme(),
            roseTheme(),
            lavenderTheme(),

            // High Contrast
            highContrastLightTheme(),
            highContrastDarkTheme()
        )

        /**
         * Get themes filtered by category
         */
        fun getThemesByCategory(category: ThemeCategory): List<ReaderColors> =
            getAllThemes().filter { it.metadata.category == category }

        /**
         * Get light themes only
         */
        fun getLightThemes(): List<ReaderColors> =
            getAllThemes().filter { !it.isDarkTheme }

        /**
         * Get dark themes only
         */
        fun getDarkThemes(): List<ReaderColors> =
            getAllThemes().filter { it.isDarkTheme }

        /**
         * Get OLED-optimized themes
         */
        fun getOledThemes(): List<ReaderColors> =
            getAllThemes().filter { it.isOled }

        /**
         * Get a theme by its ID
         */
        fun getThemeById(id: String): ReaderColors? =
            getAllThemes().find { it.metadata.id == id }

        /**
         * Get themes grouped by category
         */
        fun getThemesGroupedByCategory(): Map<ThemeCategory, List<ReaderColors>> =
            getAllThemes().groupBy { it.metadata.category }
    }
}

// =============================================================================
// EXTENSION FUNCTIONS
// =============================================================================

/**
 * Get appropriate shimmer colors for loading states
 */
fun ReaderColors.getShimmerColors(): List<Color> = if (isDarkTheme) {
    listOf(
        surface,
        surface.copy(alpha = 0.7f),
        surface
    )
} else {
    listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0)
    )
}

/**
 * Get appropriate color for disabled elements
 */
fun ReaderColors.getDisabledColor(): Color =
    text.copy(alpha = ReaderDefaults.DisabledAlpha)

/**
 * Get appropriate color for placeholder text
 */
fun ReaderColors.getPlaceholderColor(): Color =
    textSecondary.copy(alpha = ReaderDefaults.LabelAlpha)

/**
 * Get the status bar color appropriate for this theme
 */
fun ReaderColors.getStatusBarColor(): Color =
    if (isDarkTheme) background else background

/**
 * Get the navigation bar color appropriate for this theme
 */
fun ReaderColors.getNavigationBarColor(): Color = background

/**
 * Convenience function to check if we should use light status bar icons
 */
fun ReaderColors.useLightStatusBarIcons(): Boolean = isDarkTheme