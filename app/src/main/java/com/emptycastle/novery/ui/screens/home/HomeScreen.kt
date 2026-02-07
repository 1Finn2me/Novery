package com.emptycastle.novery.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.ui.components.NoveryBottomNavBarWithInsets
import com.emptycastle.novery.ui.navigation.HomeTabs
import com.emptycastle.novery.ui.navigation.rememberTabNavigationState
import com.emptycastle.novery.ui.screens.home.shared.LibraryStateHolder
import com.emptycastle.novery.ui.screens.home.tabs.browse.BrowseTab
import com.emptycastle.novery.ui.screens.home.tabs.history.HistoryTab
import com.emptycastle.novery.ui.screens.home.tabs.library.LibraryTab
import com.emptycastle.novery.ui.screens.home.tabs.more.MoreTab
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.RecommendationTab

@Composable
fun HomeScreen(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProviderBrowse: (providerName: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    // Initialize shared state
    LaunchedEffect(Unit) {
        LibraryStateHolder.initialize()
    }

    val tabNavState = rememberTabNavigationState()
    val navBackStackEntry by tabNavState.navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = currentRoute?.let { HomeTabs.fromRoute(it) } ?: HomeTabs.LIBRARY

    // Handle system back button
    BackHandler(enabled = currentTab != HomeTabs.LIBRARY) {
        tabNavState.navigateToTab(HomeTabs.LIBRARY)
    }

    Scaffold(
        bottomBar = {
            NoveryBottomNavBarWithInsets(
                selectedRoute = currentRoute ?: HomeTabs.LIBRARY.route,
                onItemSelected = { route ->
                    val tabRoute = "tab_$route"
                    HomeTabs.fromRoute(tabRoute)?.let { tab ->
                        tabNavState.navigateToTab(tab)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = tabNavState.navController,
                startDestination = HomeTabs.LIBRARY.route
            ) {
                composable(HomeTabs.LIBRARY.route) {
                    LibraryTab(
                        appSettings = appSettings,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader,
                        onNavigateToNotifications = onNavigateToNotifications
                    )
                }

                composable(HomeTabs.BROWSE.route) {
                    BrowseTab(
                        appSettings = appSettings,
                        onNavigateToProvider = onNavigateToProviderBrowse,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader
                    )
                }

                composable(HomeTabs.FOR_YOU.route) {
                    RecommendationTab(
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToBrowse = {
                            // Navigate to Browse tab
                            tabNavState.navigateToTab(HomeTabs.BROWSE)
                        }
                    )
                }

                composable(HomeTabs.HISTORY.route) {
                    HistoryTab(
                        appSettings = appSettings,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader
                    )
                }

                composable(HomeTabs.MORE.route) {
                    MoreTab(
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToDownloads = onNavigateToDownloads,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}