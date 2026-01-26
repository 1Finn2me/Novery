package com.emptycastle.novery.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import android.webkit.CookieManager as WebViewCookieManager

/**
 * Manages Cloudflare cookies for bypassing protection.
 */
object CloudflareManager {

    private const val PREFS_NAME = "cloudflare_cookies"
    private const val COOKIE_MAX_AGE_MINUTES = 25

    // This MUST match exactly what's used in WebView
    const val WEBVIEW_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private var prefs: SharedPreferences? = null

    private val _cookieStateChanged = MutableStateFlow(0L)
    val cookieStateChanged: StateFlow<Long> = _cookieStateChanged.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cleanupExpiredCookies()
        }
    }

    // ========================================================================
    // Domain Normalization - CRITICAL for matching
    // ========================================================================

    /**
     * Normalize domain for consistent storage and retrieval
     * Removes www., protocol, paths, etc.
     */
    fun getDomain(url: String): String {
        return try {
            val cleanUrl = url.trim()
            val uri = URI(
                if (cleanUrl.startsWith("http")) cleanUrl
                else "https://$cleanUrl"
            )
            uri.host
                ?.removePrefix("www.")
                ?.lowercase()
                ?: cleanUrl.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .split("/")[0]
                    .lowercase()
        } catch (e: Exception) {
            url.removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .split("/")[0]
                .lowercase()
        }
    }

    // ========================================================================
    // Cookie Storage
    // ========================================================================

    fun getCookiesForDomain(domain: String): String {
        val normalizedDomain = getDomain(domain)
        return prefs?.getString("cookies_$normalizedDomain", "") ?: ""
    }

    fun getUserAgent(domain: String): String? {
        val normalizedDomain = getDomain(domain)
        return prefs?.getString("ua_$normalizedDomain", null)
    }

    fun saveCookiesForDomain(domain: String, cookies: String, userAgent: String) {
        if (!cookies.contains("cf_clearance")) {
            android.util.Log.d("CloudflareManager", "No cf_clearance in cookies, not saving")
            return
        }

        val normalizedDomain = getDomain(domain)
        android.util.Log.d("CloudflareManager", "Saving cookies for domain: $normalizedDomain")
        android.util.Log.d("CloudflareManager", "Cookies contain cf_clearance: ${cookies.contains("cf_clearance")}")

        prefs?.edit()
            ?.putString("cookies_$normalizedDomain", cookies)
            ?.putString("ua_$normalizedDomain", userAgent)
            ?.putLong("time_$normalizedDomain", System.currentTimeMillis())
            ?.apply()

        _cookieStateChanged.value = System.currentTimeMillis()
    }

    fun clearCookiesForDomain(domain: String) {
        val normalizedDomain = getDomain(domain)
        prefs?.edit()
            ?.remove("cookies_$normalizedDomain")
            ?.remove("ua_$normalizedDomain")
            ?.remove("time_$normalizedDomain")
            ?.apply()

        _cookieStateChanged.value = System.currentTimeMillis()
    }

    fun clearAllCookies() {
        prefs?.edit()?.clear()?.apply()
        _cookieStateChanged.value = System.currentTimeMillis()
    }

    fun getAllStoredDomains(): List<String> {
        return prefs?.all?.keys
            ?.filter { it.startsWith("cookies_") }
            ?.map { it.removePrefix("cookies_") }
            ?: emptyList()
    }

    // ========================================================================
    // Cookie Status
    // ========================================================================

    fun hasClearanceCookie(url: String): Boolean {
        val domain = getDomain(url)
        val cookies = getCookiesForDomain(domain)
        val hasCookie = cookies.contains("cf_clearance=")
        val notExpired = !areCookiesExpired(domain)
        android.util.Log.d("CloudflareManager", "hasClearanceCookie for $domain: hasCookie=$hasCookie, notExpired=$notExpired")
        return hasCookie && notExpired
    }

    fun areCookiesExpired(domain: String, maxAgeMinutes: Int = COOKIE_MAX_AGE_MINUTES): Boolean {
        val normalizedDomain = getDomain(domain)
        val savedTime = prefs?.getLong("time_$normalizedDomain", 0) ?: 0
        if (savedTime == 0L) return true

        val ageMs = System.currentTimeMillis() - savedTime
        val maxAgeMs = maxAgeMinutes * 60 * 1000L
        return ageMs > maxAgeMs
    }

    fun getCookieAgeMinutes(domain: String): Int? {
        val normalizedDomain = getDomain(domain)
        val savedTime = prefs?.getLong("time_$normalizedDomain", 0) ?: 0
        if (savedTime == 0L) return null

        val ageMs = System.currentTimeMillis() - savedTime
        return (ageMs / 60000).toInt()
    }

    fun getCookieSavedTime(domain: String): Long {
        val normalizedDomain = getDomain(domain)
        return prefs?.getLong("time_$normalizedDomain", 0) ?: 0
    }

    fun getRemainingMinutes(domain: String, maxAgeMinutes: Int = COOKIE_MAX_AGE_MINUTES): Int? {
        val age = getCookieAgeMinutes(domain) ?: return null
        val remaining = maxAgeMinutes - age
        return if (remaining > 0) remaining else 0
    }

    private fun cleanupExpiredCookies() {
        val domains = getAllStoredDomains()
        domains.forEach { domain ->
            if (areCookiesExpired(domain, maxAgeMinutes = 60)) {
                clearCookiesForDomain(domain)
            }
        }
    }

    fun getCookieStatus(url: String): CookieStatus {
        val domain = getDomain(url)
        val cookies = getCookiesForDomain(domain)

        return when {
            cookies.isBlank() || !cookies.contains("cf_clearance") -> CookieStatus.NONE
            areCookiesExpired(domain) -> CookieStatus.EXPIRED
            else -> CookieStatus.VALID
        }
    }

    enum class CookieStatus {
        NONE,
        VALID,
        EXPIRED
    }

    // ========================================================================
    // WebView Cookie Extraction
    // ========================================================================

    /**
     * Extract cookies from WebView for a URL
     * Tries multiple URL variants to ensure we get the cookies
     */
    fun extractCookiesFromWebView(url: String): String? {
        return try {
            val cookieManager = WebViewCookieManager.getInstance()

            // Try the exact URL first
            var cookies = cookieManager.getCookie(url)

            // If no cookies, try with/without www
            if (cookies.isNullOrBlank() || !cookies.contains("cf_clearance")) {
                val domain = getDomain(url)
                cookies = cookieManager.getCookie("https://$domain")
                    ?: cookieManager.getCookie("https://www.$domain")
                            ?: cookieManager.getCookie("http://$domain")
            }

            android.util.Log.d("CloudflareManager", "Extracted cookies for $url: ${cookies?.take(100)}...")
            cookies
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Failed to extract cookies", e)
            null
        }
    }

    /**
     * Flush WebView cookies to persistent storage
     */
    fun flushWebViewCookies() {
        try {
            WebViewCookieManager.getInstance().flush()
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Failed to flush cookies", e)
        }
    }
}