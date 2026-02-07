package com.emptycastle.novery.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Type-safe navigation routes
 */
sealed class NavRoutes(val route: String) {

    // ================================================================
    // ROOT DESTINATIONS
    // ================================================================

    object Home : NavRoutes("home")

    object Settings : NavRoutes("settings")

    object Storage : NavRoutes("settings/storage")

    object Notifications : NavRoutes("notifications")

    // ================================================================
    // MORE TAB DESTINATIONS
    // ================================================================

    object Profile : NavRoutes("more/profile")

    object Downloads : NavRoutes("more/downloads")

    // ================================================================
    // READER DESTINATIONS
    // ================================================================

    object ReaderSettings : NavRoutes("reader_settings")

    // ================================================================
    // PROVIDER DESTINATIONS
    // ================================================================

    /**
     * Browse novels from a specific provider
     */
    object ProviderBrowse : NavRoutes("provider_browse/{providerName}") {
        fun createRoute(providerName: String): String {
            val encodedProvider = encodeUrl(providerName)
            return "provider_browse/$encodedProvider"
        }
    }

    /**
     * WebView for a provider (for Cloudflare bypass, manual browsing, etc.)
     */
    object ProviderWebView : NavRoutes("provider_webview/{providerName}/{initialUrl}") {
        fun createRoute(providerName: String, initialUrl: String? = null): String {
            val encodedProvider = encodeUrl(providerName)
            val encodedUrl = encodeUrl(initialUrl ?: "")
            return "provider_webview/$encodedProvider/$encodedUrl"
        }
    }

    // ================================================================
    // SHARED DESTINATIONS (accessible from any tab)
    // ================================================================

    object Details : NavRoutes("details/{novelUrl}/{providerName}") {
        fun createRoute(novelUrl: String, providerName: String): String {
            val encodedUrl = encodeUrl(novelUrl)
            val encodedProvider = encodeUrl(providerName)
            return "details/$encodedUrl/$encodedProvider"
        }
    }

    object Reader : NavRoutes("reader/{chapterUrl}/{novelUrl}/{providerName}") {
        fun createRoute(chapterUrl: String, novelUrl: String, providerName: String): String {
            val encodedChapterUrl = encodeUrl(chapterUrl)
            val encodedNovelUrl = encodeUrl(novelUrl)
            val encodedProvider = encodeUrl(providerName)
            return "reader/$encodedChapterUrl/$encodedNovelUrl/$encodedProvider"
        }
    }

    // ================================================================
    // TAB ROUTES (for bottom navigation)
    // ================================================================

    sealed class Tab(route: String) : NavRoutes(route) {
        object Library : Tab("tab_library")
        object Browse : Tab("tab_browse")
        object ForYou : Tab("tab_foryou")
        object History : Tab("tab_history")
        object More : Tab("tab_more")
    }

    companion object {
        fun encodeUrl(url: String): String = URLEncoder.encode(url, "UTF-8")
        fun decodeUrl(encodedUrl: String): String = URLDecoder.decode(encodedUrl, "UTF-8")
    }
}

/**
 * Bottom navigation tab configuration
 */
enum class HomeTabs(
    val route: String,
    val title: String
) {
    LIBRARY("tab_library", "Library"),
    BROWSE("tab_browse", "Browse"),
    FOR_YOU("tab_foryou", "For You"),
    HISTORY("tab_history", "History"),
    MORE("tab_more", "More");

    companion object {
        fun fromRoute(route: String): HomeTabs? {
            return entries.find { it.route == route }
        }
    }
}