package com.emptycastle.novery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.ui.components.SplashScreen
import com.emptycastle.novery.ui.navigation.NoveryNavGraph
import com.emptycastle.novery.ui.theme.NoveryTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Observe app settings
            val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
            val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()

            NoveryTheme(appSettings = appSettings) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }

                // Use MaterialTheme.colorScheme.background instead of hardcoded Zinc950
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        val navController = rememberNavController()
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