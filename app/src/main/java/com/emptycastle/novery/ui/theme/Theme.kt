package com.emptycastle.novery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.ThemeMode

/**
 * Dark color scheme - Primary theme for Novery
 */
private val DarkColorScheme = darkColorScheme(
    primary = Orange600,
    onPrimary = Color.White,
    primaryContainer = Orange800,
    onPrimaryContainer = Orange100,
    secondary = Orange400,
    onSecondary = Color.Black,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc100,
    tertiary = Orange300,
    onTertiary = Color.Black,
    tertiaryContainer = Zinc700,
    onTertiaryContainer = Zinc100,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc950,
    onSurface = Zinc100,
    surfaceVariant = Zinc900,
    onSurfaceVariant = Zinc400,
    surfaceContainerLowest = Zinc950,
    surfaceContainerLow = Zinc900,
    surfaceContainer = Zinc800,
    surfaceContainerHigh = Zinc700,
    surfaceContainerHighest = Zinc600,
    inverseSurface = Zinc100,
    inverseOnSurface = Zinc900,
    inversePrimary = Orange700,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Zinc700,
    outlineVariant = Zinc800,
    scrim = Color.Black
)

/**
 * AMOLED Black color scheme
 */
private val AmoledDarkColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222)
)

/**
 * Light color scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = Orange600,
    onPrimary = Color.White,
    primaryContainer = Orange100,
    onPrimaryContainer = Orange900,
    secondary = Orange500,
    onSecondary = Color.White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange900,
    background = Zinc50,
    onBackground = Zinc900,
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc700,
    error = Error,
    onError = Color.White,
    outline = Zinc300,
    outlineVariant = Zinc200
)

/**
 * Main theme composable for Novery with settings support
 */
@Composable
fun NoveryTheme(
    appSettings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Determine if dark theme based on settings
    val useDarkTheme = when (appSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Select color scheme
    val colorScheme = when {
        // Dynamic color (Material You)
        appSettings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // AMOLED black (only in dark mode)
        useDarkTheme && appSettings.amoledBlack -> AmoledDarkColorScheme
        // Standard dark
        useDarkTheme -> DarkColorScheme
        // Light
        else -> LightColorScheme
    }

    // Update system bars
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundColor = if (useDarkTheme) {
                if (appSettings.amoledBlack) Color.Black else Zinc950
            } else {
                Zinc50
            }

            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    // Provide dimensions based on density
    ProvideDimensions(density = appSettings.uiDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoveryTypography,
            content = content
        )
    }
}

/**
 * Legacy theme for backward compatibility (always dark)
 */
@Composable
fun NoveryTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    NoveryTheme(
        appSettings = AppSettings(
            themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
        ),
        content = content
    )
}