package com.emptycastle.novery.util

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple app-wide loading flag used to keep the Android SplashScreen visible
 * until the app's Compose content has finished initial setup.
 */
object AppLoadState {
    val isLoading = AtomicBoolean(true) // Start as true
}
