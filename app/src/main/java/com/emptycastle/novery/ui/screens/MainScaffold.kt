package com.emptycastle.novery.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.ui.components.NoveryBottomNavBarWithInsets

/**
 * Main scaffold with bottom navigation only (no top bar)
 */
@Composable
fun MainScaffold(
    currentTab: String,
    onTabChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    appSettings: AppSettings,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        // No topBar - removed to maximize screen space
        bottomBar = {
            NoveryBottomNavBarWithInsets(
                selectedRoute = currentTab,
                onItemSelected = { route ->
                    if (route == "settings") {
                        onSettingsClick()
                    } else {
                        onTabChange(route)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        content(paddingValues)
    }
}