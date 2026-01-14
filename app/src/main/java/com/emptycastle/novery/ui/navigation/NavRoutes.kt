package com.emptycastle.novery.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Navigation routes for the app
 */
sealed class NavRoutes(val route: String) {

    // Main home screen with tabs
    object Home : NavRoutes("home/{initialTab}") {
        fun createRoute(initialTab: String = "library"): String {
            return "home/$initialTab"
        }
    }

    // Settings screen
    object Settings : NavRoutes("settings")

    // Novel details screen
    object Details : NavRoutes("details/{novelUrl}/{providerName}") {
        fun createRoute(novelUrl: String, providerName: String): String {
            val encodedUrl = URLEncoder.encode(novelUrl, "UTF-8")
            val encodedProvider = URLEncoder.encode(providerName, "UTF-8")
            return "details/$encodedUrl/$encodedProvider"
        }

        fun decodeUrl(encodedUrl: String): String {
            return URLDecoder.decode(encodedUrl, "UTF-8")
        }
    }

    // Chapter reader screen
    object Reader : NavRoutes("reader/{chapterUrl}/{novelUrl}/{providerName}") {
        fun createRoute(chapterUrl: String, novelUrl: String, providerName: String): String {
            val encodedChapterUrl = URLEncoder.encode(chapterUrl, "UTF-8")
            val encodedNovelUrl = URLEncoder.encode(novelUrl, "UTF-8")
            val encodedProvider = URLEncoder.encode(providerName, "UTF-8")
            return "reader/$encodedChapterUrl/$encodedNovelUrl/$encodedProvider"
        }
    }
}