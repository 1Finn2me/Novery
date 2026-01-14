package com.emptycastle.novery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.ui.screens.details.DetailsScreen
import com.emptycastle.novery.ui.screens.home.HomeScreen
import com.emptycastle.novery.ui.screens.reader.ReaderScreen
import com.emptycastle.novery.ui.screens.settings.SettingsScreen

@Composable
fun NoveryNavGraph(
    navController: NavHostController,
    appSettings: AppSettings
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.createRoute()
    ) {
        // Home Screen with tabs
        composable(
            route = NavRoutes.Home.route,
            arguments = listOf(
                navArgument("initialTab") {
                    type = NavType.StringType
                    defaultValue = "library"
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "library"

            HomeScreen(
                initialTab = initialTab,
                appSettings = appSettings,
                onNovelClick = { novel, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novel.url, providerName)
                    )
                },
                onSettingsClick = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                onChapterClick = { chapterUrl, novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName)
                    )
                }
            )
        }

        // Settings Screen
        composable(route = NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Details Screen
        composable(
            route = NavRoutes.Details.route,
            arguments = listOf(
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("novelUrl") ?: ""
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""

            val novelUrl = NavRoutes.Details.decodeUrl(encodedUrl)
            val providerName = NavRoutes.Details.decodeUrl(encodedProvider)

            DetailsScreen(
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() },
                onChapterClick = { chapterUrl, novelUrl, provider ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, provider)
                    )
                }
            )
        }

        // Reader Screen
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

            val chapterUrl = NavRoutes.Details.decodeUrl(encodedChapterUrl)
            val novelUrl = NavRoutes.Details.decodeUrl(encodedNovelUrl)
            val providerName = NavRoutes.Details.decodeUrl(encodedProvider)

            ReaderScreen(
                chapterUrl = chapterUrl,
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}