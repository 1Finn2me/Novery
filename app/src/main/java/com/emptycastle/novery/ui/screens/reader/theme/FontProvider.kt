package com.emptycastle.novery.ui.screens.reader.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.FontCategory
import com.emptycastle.novery.domain.model.FontFamily as ReaderFontFamily

/**
 * Provides font families for the reader.
 * All fonts are bundled in res/font/ directory.
 */
object FontProvider {

    // =========================================================================
    // FONT CACHE
    // =========================================================================

    private val fontCache = mutableMapOf<ReaderFontFamily, FontFamily>()

    // =========================================================================
    // SYSTEM FONTS
    // =========================================================================

    private val systemDefault = FontFamily.Default
    private val systemSerif = FontFamily.Serif
    private val systemSansSerif = FontFamily.SansSerif
    private val systemMonospace = FontFamily.Monospace
    private val systemCursive = FontFamily.Cursive

    // =========================================================================
    // SERIF FONTS
    // =========================================================================

    private val literataFamily = FontFamily(
        Font(R.font.literata_regular, FontWeight.Normal),
        Font(R.font.literata_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.literata_bold, FontWeight.Bold),
        Font(R.font.literata_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val merriweatherFamily = FontFamily(
        Font(R.font.merriweather_regular, FontWeight.Normal),
        Font(R.font.merriweather_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.merriweather_bold, FontWeight.Bold),
        Font(R.font.merriweather_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val loraFamily = FontFamily(
        Font(R.font.lora_regular, FontWeight.Normal),
        Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.lora_bold, FontWeight.Bold),
        Font(R.font.lora_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val crimsonProFamily = FontFamily(
        Font(R.font.crimson_pro_regular, FontWeight.Normal),
        Font(R.font.crimson_pro_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.crimson_pro_bold, FontWeight.Bold),
        Font(R.font.crimson_pro_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val sourceSerifFamily = FontFamily(
        Font(R.font.source_serif_4_regular, FontWeight.Normal),
        Font(R.font.source_serif_4_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.source_serif_4_bold, FontWeight.Bold),
        Font(R.font.source_serif_4_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val libreBaskervilleFamily = FontFamily(
        Font(R.font.libre_baskerville_regular, FontWeight.Normal),
        Font(R.font.libre_baskerville_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.libre_baskerville_bold, FontWeight.Bold),
        Font(R.font.libre_baskerville_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val charterFamily = FontFamily(
        Font(R.font.charter_regular, FontWeight.Normal),
        Font(R.font.charter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.charter_bold, FontWeight.Bold),
        Font(R.font.charter_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val spectralFamily = FontFamily(
        Font(R.font.spectral_regular, FontWeight.Normal),
        Font(R.font.spectral_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.spectral_bold, FontWeight.Bold),
        Font(R.font.spectral_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val newsreaderFamily = FontFamily(
        Font(R.font.newsreader_regular, FontWeight.Normal),
        Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.newsreader_bold, FontWeight.Bold),
        Font(R.font.newsreader_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val ebGaramondFamily = FontFamily(
        Font(R.font.eb_garamond_regular, FontWeight.Normal),
        Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.eb_garamond_bold, FontWeight.Bold),
        Font(R.font.eb_garamond_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val cormorantGaramondFamily = FontFamily(
        Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
        Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.cormorant_garamond_bold, FontWeight.Bold),
        Font(R.font.cormorant_garamond_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    // =========================================================================
    // SANS-SERIF FONTS
    // =========================================================================

    private val robotoFamily = FontFamily(
        Font(R.font.roboto_regular, FontWeight.Normal),
        Font(R.font.roboto_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.roboto_bold, FontWeight.Bold),
        Font(R.font.roboto_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val openSansFamily = FontFamily(
        Font(R.font.open_sans_regular, FontWeight.Normal),
        Font(R.font.open_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.open_sans_bold, FontWeight.Bold),
        Font(R.font.open_sans_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val latoFamily = FontFamily(
        Font(R.font.lato_regular, FontWeight.Normal),
        Font(R.font.lato_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.lato_bold, FontWeight.Bold),
        Font(R.font.lato_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val sourceSansFamily = FontFamily(
        Font(R.font.source_sans_3_regular, FontWeight.Normal),
        Font(R.font.source_sans_3_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.source_sans_3_bold, FontWeight.Bold),
        Font(R.font.source_sans_3_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val interFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val nunitoFamily = FontFamily(
        Font(R.font.nunito_regular, FontWeight.Normal),
        Font(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val lexendFamily = FontFamily(
        Font(R.font.lexend_regular, FontWeight.Normal),
        Font(R.font.lexend_bold, FontWeight.Bold)
    )

    private val ibmPlexSansFamily = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
        Font(R.font.ibm_plex_sans_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val notoSansFamily = FontFamily(
        Font(R.font.noto_sans_regular, FontWeight.Normal),
        Font(R.font.noto_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.noto_sans_bold, FontWeight.Bold),
        Font(R.font.noto_sans_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val workSansFamily = FontFamily(
        Font(R.font.work_sans_regular, FontWeight.Normal),
        Font(R.font.work_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.work_sans_bold, FontWeight.Bold),
        Font(R.font.work_sans_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    // =========================================================================
    // MONOSPACE FONTS
    // =========================================================================

    private val jetbrainsMonoFamily = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
        Font(R.font.jetbrains_mono_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val firaCodeFamily = FontFamily(
        Font(R.font.firacode_light, FontWeight.Light),
        Font(R.font.firacode_regular, FontWeight.Normal),
        Font(R.font.firacode_medium, FontWeight.Medium),
        Font(R.font.firacode_bold, FontWeight.Bold)
    )

    private val sourceCodeProFamily = FontFamily(
        Font(R.font.source_code_pro_regular, FontWeight.Normal),
        Font(R.font.source_code_pro_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.source_code_pro_bold, FontWeight.Bold),
        Font(R.font.source_code_pro_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val robotoMonoFamily = FontFamily(
        Font(R.font.roboto_mono_regular, FontWeight.Normal),
        Font(R.font.roboto_mono_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.roboto_mono_bold, FontWeight.Bold),
        Font(R.font.roboto_mono_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val ibmPlexMonoFamily = FontFamily(
        Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_mono_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
        Font(R.font.ibm_plex_mono_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val cousineFamily = FontFamily(
        Font(R.font.cousine_regular, FontWeight.Normal),
        Font(R.font.cousine_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.cousine_bold, FontWeight.Bold),
        Font(R.font.cousine_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    // =========================================================================
    // HANDWRITING FONTS
    // =========================================================================

    private val caveatFamily = FontFamily(
        Font(R.font.caveat_regular, FontWeight.Normal),
        Font(R.font.caveat_bold, FontWeight.Bold)
    )

    private val indieFlowerFamily = FontFamily(
        Font(R.font.indie_flower_regular, FontWeight.Normal)
    )

    private val kalamFamily = FontFamily(
        Font(R.font.kalam_regular, FontWeight.Normal),
        Font(R.font.kalam_bold, FontWeight.Bold)
    )

    private val patrickHandFamily = FontFamily(
        Font(R.font.patrick_hand_regular, FontWeight.Normal)
    )

    // =========================================================================
    // ACCESSIBILITY FONTS
    // =========================================================================

    private val atkinsonFamily = FontFamily(
        Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
        Font(R.font.atkinson_hyperlegible_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
        Font(R.font.atkinson_hyperlegible_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    // OpenDyslexic - add font files: open_dyslexic_regular.ttf, open_dyslexic_bold.ttf
    private val openDyslexicFamily = FontFamily(
        Font(R.font.open_dyslexic_regular, FontWeight.Normal),
        Font(R.font.open_dyslexic_bold, FontWeight.Bold)
    )

    // =========================================================================
    // SPECIALTY FONTS
    // =========================================================================

    // iA Writer Quattro - best for reading (proportional)
    private val iaWriterFamily = FontFamily(
        Font(R.font.iawriterquattros_regular, FontWeight.Normal),
        Font(R.font.iawriterquattros_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.iawriterquattros_bold, FontWeight.Bold),
        Font(R.font.iawriterquattros_bolditalic, FontWeight.Bold, FontStyle.Italic)
    )

    private val vollkornFamily = FontFamily(
        Font(R.font.vollkorn_regular, FontWeight.Normal),
        Font(R.font.vollkorn_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.vollkorn_bold, FontWeight.Bold),
        Font(R.font.vollkorn_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    private val alegreyaFamily = FontFamily(
        Font(R.font.alegreya_regular, FontWeight.Normal),
        Font(R.font.alegreya_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.alegreya_bold, FontWeight.Bold),
        Font(R.font.alegreya_bold_italic, FontWeight.Bold, FontStyle.Italic)
    )

    // =========================================================================
    // FONT MAP
    // =========================================================================

    private val bundledFonts: Map<ReaderFontFamily, FontFamily> = mapOf(
        // Serif
        ReaderFontFamily.LITERATA to literataFamily,
        ReaderFontFamily.MERRIWEATHER to merriweatherFamily,
        ReaderFontFamily.LORA to loraFamily,
        ReaderFontFamily.CRIMSON_PRO to crimsonProFamily,
        ReaderFontFamily.SOURCE_SERIF to sourceSerifFamily,
        ReaderFontFamily.LIBRE_BASKERVILLE to libreBaskervilleFamily,
        ReaderFontFamily.CHARTER to charterFamily,
        ReaderFontFamily.SPECTRAL to spectralFamily,
        ReaderFontFamily.NEWSREADER to newsreaderFamily,
        ReaderFontFamily.CORMORANT to cormorantGaramondFamily,
        ReaderFontFamily.EB_GARAMOND to ebGaramondFamily,

        // Sans-Serif
        ReaderFontFamily.ROBOTO to robotoFamily,
        ReaderFontFamily.OPEN_SANS to openSansFamily,
        ReaderFontFamily.LATO to latoFamily,
        ReaderFontFamily.SOURCE_SANS to sourceSansFamily,
        ReaderFontFamily.INTER to interFamily,
        ReaderFontFamily.NUNITO to nunitoFamily,
        ReaderFontFamily.LEXEND to lexendFamily,
        ReaderFontFamily.IBM_PLEX_SANS to ibmPlexSansFamily,
        ReaderFontFamily.NOTO_SANS to notoSansFamily,
        ReaderFontFamily.WORK_SANS to workSansFamily,

        // Monospace
        ReaderFontFamily.JETBRAINS_MONO to jetbrainsMonoFamily,
        ReaderFontFamily.FIRA_CODE to firaCodeFamily,
        ReaderFontFamily.SOURCE_CODE to sourceCodeProFamily,
        ReaderFontFamily.ROBOTO_MONO to robotoMonoFamily,
        ReaderFontFamily.IBM_PLEX_MONO to ibmPlexMonoFamily,
        ReaderFontFamily.COUSINE to cousineFamily,

        // Handwriting
        ReaderFontFamily.CAVEAT to caveatFamily,
        ReaderFontFamily.INDIE_FLOWER to indieFlowerFamily,
        ReaderFontFamily.KALAM to kalamFamily,
        ReaderFontFamily.PATRICK_HAND to patrickHandFamily,

        // Accessibility
        ReaderFontFamily.ATKINSON to atkinsonFamily,
        ReaderFontFamily.OPEN_DYSLEXIC to openDyslexicFamily,

        // Specialty
        ReaderFontFamily.IA_WRITER to iaWriterFamily,
        ReaderFontFamily.VOLLKORN to vollkornFamily,
        ReaderFontFamily.ALEGREYA to alegreyaFamily
    )

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Gets the Compose FontFamily for a given reader font family.
     * Returns bundled font if available, otherwise falls back to system font.
     */
    fun getFontFamily(readerFont: ReaderFontFamily): FontFamily {
        // Check cache first
        fontCache[readerFont]?.let { return it }

        val fontFamily = when {
            // Check if we have a bundled version
            bundledFonts.containsKey(readerFont) -> bundledFonts[readerFont]!!

            // Handle system fonts
            readerFont == ReaderFontFamily.SYSTEM_DEFAULT -> systemDefault
            readerFont == ReaderFontFamily.SYSTEM_SERIF -> systemSerif
            readerFont == ReaderFontFamily.SYSTEM_SANS -> systemSansSerif
            readerFont == ReaderFontFamily.SYSTEM_MONO -> systemMonospace

            // Fall back based on category
            else -> getFallbackForCategory(readerFont.category)
        }

        // Cache and return
        fontCache[readerFont] = fontFamily
        return fontFamily
    }

    /**
     * Gets the appropriate system font fallback for a category.
     */
    private fun getFallbackForCategory(category: FontCategory): FontFamily {
        return when (category) {
            FontCategory.SYSTEM -> systemDefault
            FontCategory.SERIF -> systemSerif
            FontCategory.SANS_SERIF -> systemSansSerif
            FontCategory.MONOSPACE -> systemMonospace
            FontCategory.HANDWRITING -> systemCursive
            FontCategory.ACCESSIBILITY -> systemSansSerif
            FontCategory.SPECIALTY -> systemSerif
        }
    }

    /**
     * Checks if a font is available as a bundled font.
     */
    fun isFontBundled(readerFont: ReaderFontFamily): Boolean {
        return bundledFonts.containsKey(readerFont)
    }

    /**
     * Gets list of all available fonts (bundled + system).
     */
    fun getAvailableFonts(): List<ReaderFontFamily> {
        val bundled = bundledFonts.keys.toList()
        val system = listOf(
            ReaderFontFamily.SYSTEM_DEFAULT,
            ReaderFontFamily.SYSTEM_SERIF,
            ReaderFontFamily.SYSTEM_SANS,
            ReaderFontFamily.SYSTEM_MONO
        )
        return system + bundled
    }

    /**
     * Clears the font cache (call if fonts are dynamically loaded).
     */
    fun clearCache() {
        fontCache.clear()
    }
}