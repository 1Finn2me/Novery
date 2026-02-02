package com.emptycastle.novery

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import com.emptycastle.novery.service.TTSNotifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.ui.navigation.NoveryNavGraph
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.AppLoadState
import com.emptycastle.novery.util.VolumeKeyManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.emptycastle.novery.ui.components.SplashScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No-op for now; TTSNotifications.hasNotificationPermission checks permissions before posting
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install system splash and keep it briefly
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { AppLoadState.isLoading.get() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ if not already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !TTSNotifications.hasNotificationPermission(this)
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
            val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()

            // Track if app is ready
            var showCustomSplash by remember { mutableStateOf(true) }

            // When settings are loaded, exit system splash but show custom one
            LaunchedEffect(appSettings) {
                AppLoadState.isLoading.set(false) // Exit system splash

                // Show custom splash for a bit, then transition
                delay(2000) // Show custom splash for 2 seconds
                showCustomSplash = false
            }

            NoveryTheme(appSettings = appSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show custom splash OR main content
                    AnimatedContent(
                        targetState = showCustomSplash,
                        transitionSpec = {
                            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                        }
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen() // Your beautiful custom splash!
                        } else {
                            val navController = rememberNavController()

                            var showNotificationDialog by remember {
                                mutableStateOf(
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !TTSNotifications.hasNotificationPermission(this@MainActivity)
                                )
                            }

                            if (showNotificationDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNotificationDialog = false },
                                    title = { Text(text = "Enable notifications") },
                                    text = { Text(text = "Novery uses notifications to show playback controls and timers. Please allow notifications to enable these features.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            showNotificationDialog = false
                                        }) {
                                            Text("Allow")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNotificationDialog = false }) {
                                            Text("Not now")
                                        }
                                    }
                                )
                            }

                            NoveryNavGraph(
                                navController = navController,
                                appSettings = appSettings
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (VolumeKeyManager.onVolumeKeyPressed(isVolumeUp = true)) {
                    return true // Consume the event
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (VolumeKeyManager.onVolumeKeyPressed(isVolumeUp = false)) {
                    return true // Consume the event
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        VolumeKeyManager.reset()
    }
}