package com.emptycastle.novery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.emptycastle.novery.data.backup.BackupManager
import com.emptycastle.novery.data.cache.CacheManager
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.ui.screens.about.AboutScreen
import com.emptycastle.novery.ui.screens.details.DetailsScreen
import com.emptycastle.novery.ui.screens.downloads.DownloadsScreen
import com.emptycastle.novery.ui.screens.home.HomeScreen
import com.emptycastle.novery.ui.screens.home.tabs.browse.ProviderBrowseScreen
import com.emptycastle.novery.ui.screens.home.tabs.browse.ProviderWebViewScreen
import com.emptycastle.novery.ui.screens.notification.NotificationScreen
import com.emptycastle.novery.ui.screens.onboarding.OnboardingScreen
import com.emptycastle.novery.ui.screens.profile.ProfileScreen
import com.emptycastle.novery.ui.screens.reader.ReaderScreen
import com.emptycastle.novery.ui.screens.reader.settings.ReaderSettingsScreen
import com.emptycastle.novery.ui.screens.settings.SettingsScreen
import com.emptycastle.novery.ui.screens.settings.StorageScreen

@Composable
fun NoveryNavGraph(
    navController: NavHostController,
    appSettings: AppSettings
) {
    // Check if onboarding is needed
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val needsOnboarding = remember {
        preferencesManager.isFirstRun() || !preferencesManager.hasCompletedOnboarding()
    }

    val startDestination = if (needsOnboarding) {
        NavRoutes.Onboarding.route
    } else {
        NavRoutes.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ================================================================
        // ONBOARDING
        // ================================================================
        composable(route = NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // HOME (with nested tab navigation)
        // ================================================================
        composable(route = NavRoutes.Home.route) {
            HomeScreen(
                appSettings = appSettings,
                onNavigateToDetails = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                },
                onNavigateToReader = { chapterUrl, novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName)
                    )
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                onNavigateToProviderBrowse = { providerName ->
                    navController.navigate(
                        NavRoutes.ProviderBrowse.createRoute(providerName)
                    )
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.Notifications.route)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.Profile.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(NavRoutes.Downloads.route)
                },
                onNavigateToAbout = {
                    navController.navigate(NavRoutes.About.route)
                },
                onNavigateToOnboarding = {
                    navController.navigate(NavRoutes.Onboarding.route)
                }
            )
        }

        // ================================================================
        // NOTIFICATIONS
        // ================================================================
        composable(route = NavRoutes.Notifications.route) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReader = { chapterUrl, novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName)
                    )
                },
                onNavigateToDetails = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // SETTINGS
        // ================================================================
        composable(route = NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToStorage = {
                    navController.navigate(NavRoutes.Storage.route)
                }
            )
        }

        // ================================================================
        // STORAGE & BACKUP
        // ================================================================
        composable(route = NavRoutes.Storage.route) {
            val context = LocalContext.current
            val database = remember { NovelDatabase.getInstance(context) }
            val prefsManager = remember { PreferencesManager(context) }
            val cacheManager = remember { CacheManager(context, database) }
            val backupManager = remember { BackupManager(context, database, prefsManager) }

            StorageScreen(
                cacheManager = cacheManager,
                backupManager = backupManager,
                onBack = { navController.popBackStack() }
            )
        }

        // ================================================================
        // PROFILE
        // ================================================================
        composable(route = NavRoutes.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNovelClick = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // DOWNLOADS
        // ================================================================
        composable(route = NavRoutes.Downloads.route) {
            DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onNovelClick = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // ABOUT
        // ================================================================
        composable(route = NavRoutes.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ================================================================
        // PROVIDER BROWSE (novels from specific provider)
        // ================================================================
        composable(
            route = NavRoutes.ProviderBrowse.route,
            arguments = listOf(
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            ProviderBrowseScreen(
                providerName = providerName,
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
                onNavigateToDetails = { novelUrl, provider ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, provider)
                    )
                },
                onNavigateToReader = { chapterUrl, novelUrl, provider ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, provider)
                    )
                },
                onNavigateToWebView = { provider, url ->
                    navController.navigate(
                        NavRoutes.ProviderWebView.createRoute(provider, url)
                    )
                }
            )
        }

        // ================================================================
        // PROVIDER WEBVIEW (for Cloudflare bypass, manual browsing)
        // ================================================================
        composable(
            route = NavRoutes.ProviderWebView.route,
            arguments = listOf(
                navArgument("providerName") { type = NavType.StringType },
                navArgument("initialUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("initialUrl") ?: ""

            val providerName = NavRoutes.decodeUrl(encodedProvider)
            val initialUrl = NavRoutes.decodeUrl(encodedUrl).takeIf { it.isNotBlank() }

            ProviderWebViewScreen(
                providerName = providerName,
                initialUrl = initialUrl,
                onBack = { navController.popBackStack() },
                onOpenNovelInApp = { novelUrl ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    ) {
                        popUpTo(NavRoutes.ProviderWebView.route) { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // DETAILS
        // ================================================================
        composable(
            route = NavRoutes.Details.route,
            arguments = listOf(
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("novelUrl") ?: ""
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""

            val novelUrl = NavRoutes.decodeUrl(encodedUrl)
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            DetailsScreen(
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() },
                onChapterClick = { chapterUrl, nUrl, provider ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, nUrl, provider)
                    )
                },
                onNovelClick = { relatedNovelUrl, relatedProviderName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(relatedNovelUrl, relatedProviderName)
                    )
                },
                onOpenInWebView = { provider, url ->
                    navController.navigate(
                        NavRoutes.ProviderWebView.createRoute(provider, url)
                    )
                },
                onNavigateToDownloads = {
                    navController.navigate(NavRoutes.Downloads.route)
                }
            )
        }

        // ================================================================
        // READER
        // ================================================================
        composable(
            route = NavRoutes.Reader.route,
            arguments = listOf(
                navArgument("chapterUrl") { type = NavType.StringType },
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedChapterUrl = backStackEntry.arguments?.getString("chapterUrl") ?: ""
            val encodedNovelUrl = backStackEntry.arguments?.getString("novelUrl") ?: ""
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""

            val chapterUrl = NavRoutes.decodeUrl(encodedChapterUrl)
            val novelUrl = NavRoutes.decodeUrl(encodedNovelUrl)
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            ReaderScreen(
                chapterUrl = chapterUrl,
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.ReaderSettings.route)
                }
            )
        }

        // ================================================================
        // READER SETTINGS
        // ================================================================
        composable(route = NavRoutes.ReaderSettings.route) {
            ReaderSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}