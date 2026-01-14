// domain/model/ReaderSettings.kt
package com.emptycastle.novery.domain.model

/**
 * User preferences for the chapter reader.
 */
data class ReaderSettings(
    val fontSize: Int = 18,
    val fontFamily: FontFamily = FontFamily.SERIF,
    val maxWidth: MaxWidth = MaxWidth.LARGE,
    val lineHeight: Float = 1.6f,
    val textAlign: TextAlign = TextAlign.LEFT,
    val theme: ReaderTheme = ReaderTheme.DARK,
    val paragraphSpacing: Float = 1.2f,
    val marginHorizontal: Int = 20
)

enum class FontFamily {
    SERIF, SANS, MONO;

    fun displayName(): String = when (this) {
        SERIF -> "Serif"
        SANS -> "Sans Serif"
        MONO -> "Monospace"
    }
}

enum class MaxWidth {
    MEDIUM, LARGE, EXTRA_LARGE, FULL;

    fun displayName(): String = when (this) {
        MEDIUM -> "Medium"
        LARGE -> "Large"
        EXTRA_LARGE -> "Extra Large"
        FULL -> "Full Width"
    }
}

enum class TextAlign {
    LEFT, JUSTIFY;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class ReaderTheme {
    DARK, LIGHT, SEPIA;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}