package com.emptycastle.novery.util

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controller for managing immersive/fullscreen mode
 */
@Stable
class ImmersiveModeController(
    private val window: Window,
    private val view: View
) {
    private val insetsController: WindowInsetsControllerCompat? =
        WindowCompat.getInsetsController(window, view)

    init {
        // Allow content to extend behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * Enter immersive mode - hide system bars
     */
    fun enterImmersiveMode() {
        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Exit immersive mode - show system bars
     */
    fun exitImmersiveMode() {
        insetsController?.apply {
            show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    /**
     * Toggle immersive mode based on visibility flag
     */
    fun setImmersive(immersive: Boolean) {
        if (immersive) {
            enterImmersiveMode()
        } else {
            exitImmersiveMode()
        }
    }
}

/**
 * Remember an ImmersiveModeController instance
 */
@Composable
fun rememberImmersiveModeController(): ImmersiveModeController? {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    return remember(view, window) {
        window?.let { ImmersiveModeController(it, view) }
    }
}

/**
 * Effect that manages immersive mode based on a visibility flag.
 * Automatically restores normal mode when the composable leaves composition.
 *
 * @param showSystemBars When true, system bars are visible. When false, enters immersive mode.
 */
@Composable
fun ImmersiveModeEffect(showSystemBars: Boolean) {
    val controller = rememberImmersiveModeController()

    // Update immersive state when showSystemBars changes
    LaunchedEffect(showSystemBars) {
        controller?.setImmersive(!showSystemBars)
    }

    // Restore system bars when leaving composition (e.g., navigating away)
    DisposableEffect(Unit) {
        onDispose {
            controller?.exitImmersiveMode()
        }
    }
}