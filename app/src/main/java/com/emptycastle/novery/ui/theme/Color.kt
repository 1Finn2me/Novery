package com.emptycastle.novery.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Novery Color Palette
 * Matches the React app's dark theme with orange accents
 */

// ============================================
// PRIMARY - Orange
// ============================================
val Orange50 = Color(0xFFFFF7ED)
val Orange100 = Color(0xFFFFEDD5)
val Orange200 = Color(0xFFFED7AA)
val Orange300 = Color(0xFFFDBA74)
val Orange400 = Color(0xFFFB923C)
val Orange500 = Color(0xFFF97316)
val Orange600 = Color(0xFFEA580C)  // Primary
val Orange700 = Color(0xFFC2410C)
val Orange800 = Color(0xFF9A3412)
val Orange900 = Color(0xFF7C2D12)

// ============================================
// NEUTRAL - Zinc
// ============================================
val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)  // Background

// ============================================
// SEMANTIC COLORS
// ============================================
val Success = Color(0xFF22C55E)      // Green-500
val SuccessLight = Color(0xFF4ADE80) // Green-400
val Error = Color(0xFFEF4444)        // Red-500
val ErrorLight = Color(0xFFF87171)   // Red-400
val Warning = Color(0xFFF59E0B)      // Amber-500
val Info = Color(0xFF3B82F6)         // Blue-500

// ============================================
// STATUS COLORS
// ============================================
val StatusReading = Info
val StatusCompleted = Success
val StatusOnHold = Warning
val StatusPlanToRead = Color(0xFF8B5CF6)
val StatusDROPPED = Error


// ============================================
// READER THEMES
// ============================================

// Dark Theme (Default)
val ReaderDarkBackground = Zinc950
val ReaderDarkText = Zinc300
val ReaderDarkSecondary = Zinc500

// Light Theme
val ReaderLightBackground = Zinc50
val ReaderLightText = Zinc900
val ReaderLightSecondary = Zinc600

// Sepia Theme
val ReaderSepiaBackground = Color(0xFFF4ECD8)
val ReaderSepiaText = Color(0xFF5B4636)
val ReaderSepiaSecondary = Color(0xFF8B7355)
