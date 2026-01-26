// ProviderWebViewScreen.kt - Improved Version with Better URL Detection
package com.emptycastle.novery.ui.screens.home.tabs.browse

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.emptycastle.novery.data.remote.CloudflareManager
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// Color Constants
// ============================================================================

private object WebViewColors {
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF3B82F6)
    val Secure = Color(0xFF22C55E)
}

// ============================================================================
// URL Validation & Provider Detection - IMPROVED
// ============================================================================

/**
 * Result of URL validation
 */
private sealed class UrlValidationResult {
    data class Valid(
        val url: String,
        val provider: MainProvider,
        val isNovelUrl: Boolean
    ) : UrlValidationResult()

    data class InvalidProvider(val url: String) : UrlValidationResult()
    data class InvalidFormat(val input: String) : UrlValidationResult()
}

/**
 * Validates and normalizes a URL input
 */
private fun validateUrl(input: String): UrlValidationResult {
    val trimmed = input.trim()

    // Try to normalize the URL
    val normalizedUrl = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") -> "https://$trimmed"
        else -> return UrlValidationResult.InvalidFormat(trimmed)
    }

    // Find matching provider
    val provider = findProviderForUrl(normalizedUrl)
        ?: return UrlValidationResult.InvalidProvider(normalizedUrl)

    // Check if it's likely a novel URL
    val isNovelUrl = isLikelyNovelUrl(normalizedUrl, provider)

    return UrlValidationResult.Valid(
        url = normalizedUrl,
        provider = provider,
        isNovelUrl = isNovelUrl
    )
}

/**
 * Find which provider a URL belongs to
 */
private fun findProviderForUrl(url: String): MainProvider? {
    return MainProvider.getProviders().find { provider ->
        val providerDomain = extractDomain(provider.mainUrl)
        val urlDomain = extractDomain(url)

        // Match if domains are the same or URL domain ends with provider domain
        urlDomain == providerDomain ||
                urlDomain.endsWith(".$providerDomain")
    }
}

/**
 * Extract domain from URL
 */
private fun extractDomain(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .substringBefore("/")
        .substringBefore("?")
        .lowercase()
}

/**
 * Extract site name from domain (e.g., "novelbin" from "novelbin.com")
 */
private fun extractSiteName(url: String): String {
    return extractDomain(url)
        .substringBefore(".")
        .lowercase()
}

/**
 * Get provider-specific URL patterns
 * Each provider may have unique URL structures for novel pages
 */
private fun getProviderNovelPatterns(providerName: String): List<String> {
    return when (providerName.lowercase()) {
        "novelbin" -> listOf(
            "^b/[^/]+$",                    // /b/novel-slug
            "^novel/[^/]+$",                // /novel/novel-slug (fallback)
        )
        "libread" -> listOf(
            "^libread/[^/]+(-\\d+)?$",      // /libread/novel-slug-123456
            "^book/[^/]+$",                 // /book/novel-slug
        )
        "novelfire" -> listOf(
            "^novel/[^/]+$",                // /novel/novel-slug
        )
        "webnovel" -> listOf(
            // Robust Webnovel patterns; support m. subdomain, www, slug_id forms, numeric IDs,
            // optional trailing segments, queries or hashes. Match both path-only and full-URL forms.
            // Preferred: slug + long numeric id (e.g., in-fate-with-unique-skill_34759885400745505)
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[a-z0-9\\-]+_[0-9]{5,}(?:[/?#].*)?$",
            // General slug + numeric id (shorter ids)
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[a-z0-9\\-]+_[0-9]+(?:[/?#].*)?$",
            // Numeric id + slug (rare)
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[0-9]+_[a-z0-9\\-]+(?:[/?#].*)?$",
            // Numeric-only id
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[0-9]+(?:[/?#].*)?$",
            // Slug-only under /book/
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[a-z0-9\\-]+(?:[/?#].*)?$",

            // Also accept path-only forms (when domain was stripped differently)
            "^book/[a-z0-9\\-]+_[0-9]{5,}(?:[/?#].*)?$",
            "^book/[a-z0-9\\-]+_[0-9]+(?:[/?#].*)?$",
            "^book/[0-9]+_[a-z0-9\\-]+(?:[/?#].*)?$",
            "^book/[0-9]+(?:[/?#].*)?$",
            "^book/[a-z0-9\\-]+(?:[/?#].*)?$"
        )
        "novelfull" -> listOf(
            "^[^/]+-novel/?$",              // /something-novel
            "^novel/[^/]+$",                // /novel/slug
        )
        "lightnovelpub", "lightnovelworld" -> listOf(
            "^novel/[^/]+$",                // /novel/novel-slug
        )
        "readnovelfull" -> listOf(
            "^[^/]+\\.html$",               // /novel-slug.html
            "^novel/[^/]+$",
        )
        "freewebnovel" -> listOf(
            "^[^/]+\\.html$",               // /novel-slug.html
        )
        "allnovelbin" -> listOf(
            "^novel/[^/]+$",
            "^book/[^/]+$",
        )
        else -> emptyList()
    }
}

/**
 * Check if URL is likely a novel detail page
 * Uses provider-specific patterns + generic fallbacks
 */
private fun isLikelyNovelUrl(url: String, provider: MainProvider): Boolean {
    // Clean and extract path
    val path = url
        .removePrefix(provider.mainUrl)
        .removePrefix("/")
        .removeSuffix("/")
        .substringBefore("?")  // Remove query params
        .substringBefore("#")  // Remove hash
        .lowercase()

    // Skip empty paths (homepage)
    if (path.isBlank()) return false

    // ========================================
    // 1. Check provider-specific patterns first
    // ========================================
    val providerPatterns = getProviderNovelPatterns(provider.name)
    for (pattern in providerPatterns) {
        if (Regex(pattern, RegexOption.IGNORE_CASE).matches(path)) {
            // Still need to check it's not a chapter
            if (!isChapterPath(path)) {
                android.util.Log.d("NovelDetection", "Matched provider pattern: $pattern for path: $path")
                return true
            }
        }
    }

    // ========================================
    // 2. Check common exclusion patterns
    // ========================================
    if (isExcludedPath(path)) {
        android.util.Log.d("NovelDetection", "Excluded path: $path")
        return false
    }

    // ========================================
    // 3. Check generic novel patterns
    // ========================================
    val genericPatterns = listOf(
        // Standard paths
        "^novel/[^/]+$",
        "^book/[^/]+$",
        "^series/[^/]+$",
        "^title/[^/]+$",
        "^story/[^/]+$",
        "^read/[^/]+$",

        // Short prefixes (single letter or two letters)
        "^[a-z]/[^/]+$",                    // /n/slug, /b/slug, etc.
        "^[a-z]{2}/[^/]+$",                 // /nr/slug, etc.

        // Plural forms
        "^novels/[^/]+$",
        "^books/[^/]+$",
        "^stories/[^/]+$",

        // Suffix patterns
        "^[^/]+-novel$",                    // /something-novel
        "^[^/]+-book$",                     // /something-book

        // HTML extension
        "^[^/]+\\.html$",                   // /novel-slug.html (only if single segment)
    )

    for (pattern in genericPatterns) {
        if (Regex(pattern, RegexOption.IGNORE_CASE).matches(path)) {
            if (!isChapterPath(path)) {
                android.util.Log.d("NovelDetection", "Matched generic pattern: $pattern for path: $path")
                return true
            }
        }
    }

    // ========================================
    // 4. Check if path matches site name pattern
    // ========================================
    // e.g., libread.com/libread/slug
    val siteName = extractSiteName(provider.mainUrl)
    if (path.startsWith("$siteName/")) {
        val afterSiteName = path.removePrefix("$siteName/")
        if (afterSiteName.isNotBlank() &&
            !afterSiteName.contains("/") &&
            !isChapterPath(afterSiteName)) {
            android.util.Log.d("NovelDetection", "Matched site-name pattern for: $path")
            return true
        }
    }

    // ========================================
    // 5. Fallback: Analyze path structure
    // ========================================
    val segments = path.split("/").filter {
        it.isNotBlank() && !it.startsWith("?")
    }

    // Single segment that looks like a novel slug
    if (segments.size == 1) {
        val segment = segments[0]
            .removeSuffix(".html")
            .removeSuffix(".htm")

        if (looksLikeNovelSlug(segment) && !isChapterPath(segment)) {
            android.util.Log.d("NovelDetection", "Single segment looks like novel slug: $segment")
            return true
        }
    }

    // Two segments: prefix + slug
    if (segments.size == 2) {
        val prefix = segments[0]
        val slug = segments[1]
            .removeSuffix(".html")
            .removeSuffix(".htm")

        // Short prefix (like /b/, /n/, /novel/, /book/) + long slug
        if (prefix.length <= 12 &&
            looksLikeNovelSlug(slug) &&
            !isExcludedPrefix(prefix) &&
            !isChapterPath(slug)) {
            android.util.Log.d("NovelDetection", "Two-segment pattern matched: $prefix/$slug")
            return true
        }
    }

    android.util.Log.d("NovelDetection", "No match for path: $path")
    return false
}

/**
 * Check if a string looks like a novel slug
 */
private fun looksLikeNovelSlug(slug: String): Boolean {
    // Must have hyphens (slugified)
    if (!slug.contains("-")) return false

    // Must be reasonably long
    if (slug.length < 8) return false

    // Shouldn't contain chapter indicators
    if (isChapterPath(slug)) return false

    // Shouldn't be just numbers
    if (slug.replace("-", "").all { it.isDigit() }) return false

    return true
}

/**
 * Check if path indicates a chapter page
 */
private fun isChapterPath(path: String): Boolean {
    val chapterIndicators = listOf(
        "chapter-",
        "-chapter-",
        "/chapter",
        "-ch-",
        "/ch/",
        "/c/",
        "chapter_",
        "-chuong-",      // Vietnamese
        "-capitulo-",    // Spanish
        "-chapitre-",    // French
    )

    val lowerPath = path.lowercase()

    // Direct indicators
    if (chapterIndicators.any { lowerPath.contains(it) }) return true

    // Pattern: ends with /chapter-N or -chapter-N
    if (Regex("[-/]chapter[-_]?\\d+", RegexOption.IGNORE_CASE).containsMatchIn(lowerPath)) return true

    // Pattern: ends with /cN or -cN (where N is a number)
    if (Regex("[-/]c\\d+$", RegexOption.IGNORE_CASE).containsMatchIn(lowerPath)) return true

    return false
}

/**
 * Check if path matches common non-novel patterns
 */
private fun isExcludedPath(path: String): Boolean {
    val excludePatterns = listOf(
        "^search",
        "^genre/",
        "^genres/",
        "^category/",
        "^categories/",
        "^tag/",
        "^tags/",
        "^author/",
        "^authors/",
        "^sort/",
        "^ranking",
        "^latest",
        "^popular",
        "^completed",
        "^ongoing",
        "^hot",
        "^new",
        "^login",
        "^register",
        "^signup",
        "^signin",
        "^user/",
        "^profile",
        "^account",
        "^ajax/",
        "^api/",
        "^wp-",
        "^admin",
        "^page/",
        "^browse",
        "^list",
        "^all-",
        "^top-",
        "^most-",
    )

    val lowerPath = path.lowercase()
    return excludePatterns.any {
        Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(lowerPath)
    }
}

/**
 * Check if a prefix is excluded (not a novel container)
 */
private fun isExcludedPrefix(prefix: String): Boolean {
    val excludedPrefixes = setOf(
        "search", "genre", "genres", "category", "categories",
        "tag", "tags", "author", "authors", "sort", "ranking",
        "latest", "popular", "completed", "ongoing", "hot", "new",
        "login", "register", "user", "profile", "account",
        "ajax", "api", "page", "browse", "list", "wp-admin",
        "chapter", "chapters", "ch", "c"
    )
    return prefix.lowercase() in excludedPrefixes
}

// ============================================================================
// Provider WebView Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderWebViewScreen(
    providerName: String,
    initialUrl: String?,
    onBack: () -> Unit,
    onOpenNovelInApp: (novelUrl: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl ?: "") }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    // URL editing state
    var isEditingUrl by remember { mutableStateOf(false) }
    var editUrlValue by remember { mutableStateOf(TextFieldValue("")) }

    // Novel detection state
    var detectedProvider by remember { mutableStateOf<MainProvider?>(null) }
    var isNovelUrl by remember { mutableStateOf(false) }

    // Cookie status
    var cookieStatus by remember { mutableStateOf(CookieDisplayStatus.NONE) }
    var showCookieSavedMessage by remember { mutableStateOf(false) }

    // Get initial provider
    val initialProvider = remember(providerName) {
        MainProvider.getProvider(providerName)
    }

    val startUrl = remember(initialProvider, initialUrl) {
        initialUrl ?: initialProvider?.mainUrl ?: "https://google.com"
    }

    val domain = remember(detectedProvider, initialProvider) {
        val provider = detectedProvider ?: initialProvider
        provider?.mainUrl?.let { CloudflareManager.getDomain(it) } ?: ""
    }

    // Check if URL is secure
    val isSecure by remember(currentUrl) {
        derivedStateOf { currentUrl.startsWith("https://") }
    }

    // Analyze current URL when it changes
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank()) {
            when (val result = validateUrl(currentUrl)) {
                is UrlValidationResult.Valid -> {
                    detectedProvider = result.provider
                    isNovelUrl = result.isNovelUrl
                    android.util.Log.d("WebView", "URL validated: ${result.url}, isNovel: ${result.isNovelUrl}")
                }
                else -> {
                    // Try with initial provider
                    detectedProvider = initialProvider
                    isNovelUrl = initialProvider?.let {
                        isLikelyNovelUrl(currentUrl, it)
                    } ?: false
                }
            }
        }
    }

    // Hide cookie saved message after delay
    LaunchedEffect(showCookieSavedMessage) {
        if (showCookieSavedMessage) {
            delay(4000)
            showCookieSavedMessage = false
        }
    }

    // Handle back - ensure cookies are saved before leaving
    val handleBack: () -> Unit = {
        CloudflareManager.flushWebViewCookies()

        val cookies = CloudflareManager.extractCookiesFromWebView(currentUrl)
        if (cookies != null && cookies.contains("cf_clearance")) {
            CloudflareManager.saveCookiesForDomain(
                domain = domain,
                cookies = cookies,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT
            )
        }

        onBack()
    }

    // Handle URL submission from edit mode
    val handleUrlSubmit: (String) -> Unit = { input ->
        when (val result = validateUrl(input)) {
            is UrlValidationResult.Valid -> {
                isEditingUrl = false
                webView?.loadUrl(result.url)
            }
            is UrlValidationResult.InvalidProvider -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "URL doesn't match any known provider"
                    )
                }
            }
            is UrlValidationResult.InvalidFormat -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Invalid URL format"
                    )
                }
            }
        }
    }

    // Handle opening novel in app
    val handleOpenInApp: () -> Unit = {
        when (val result = validateUrl(currentUrl)) {
            is UrlValidationResult.Valid -> {
                handleBack()
                onOpenNovelInApp(result.url)
            }
            else -> {
                // Fallback: try anyway with current URL
                handleBack()
                onOpenNovelInApp(currentUrl)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EnhancedWebViewTopBar(
                title = pageTitle,
                url = currentUrl,
                isLoading = isLoading,
                loadingProgress = loadingProgress,
                canGoBack = canGoBack,
                isSecure = isSecure,
                cookieStatus = cookieStatus,
                providerName = detectedProvider?.name ?: providerName,
                isEditingUrl = isEditingUrl,
                editUrlValue = editUrlValue,
                onEditUrlValueChange = { editUrlValue = it },
                onStartEditingUrl = {
                    editUrlValue = TextFieldValue(
                        text = currentUrl,
                        selection = TextRange(0, currentUrl.length)
                    )
                    isEditingUrl = true
                },
                onCancelEditingUrl = { isEditingUrl = false },
                onSubmitUrl = handleUrlSubmit,
                onBack = {
                    if (isEditingUrl) {
                        isEditingUrl = false
                    } else if (canGoBack) {
                        webView?.goBack()
                    } else {
                        handleBack()
                    }
                },
                onClose = handleBack,
                onRefresh = { webView?.reload() },
                onClearCookies = {
                    CloudflareManager.clearCookiesForDomain(domain)
                    cookieStatus = CookieDisplayStatus.NONE
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EnhancedProviderWebView(
                startUrl = startUrl,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT,
                onWebViewCreated = { webView = it },
                onPageStarted = { url ->
                    isLoading = true
                    loadingProgress = 0
                    currentUrl = url
                },
                onPageFinished = { url ->
                    isLoading = false
                    loadingProgress = 100
                    currentUrl = url
                    canGoBack = webView?.canGoBack() ?: false

                    CloudflareManager.flushWebViewCookies()

                    checkAndSaveCookies(
                        url = url,
                        domain = domain,
                        onCookiesSaved = {
                            cookieStatus = CookieDisplayStatus.VALID
                            showCookieSavedMessage = true
                        },
                        onCookieStatusUpdate = { status ->
                            cookieStatus = status
                        }
                    )
                },
                onTitleChanged = { pageTitle = it },
                onProgressChanged = { progress ->
                    loadingProgress = progress
                }
            )

            // Loading progress overlay
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                EnhancedProgressIndicator(progress = loadingProgress)
            }

            // Cookie saved notification
            AnimatedVisibility(
                visible = showCookieSavedMessage,
                enter = slideInVertically { -it } + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically { -it } + fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                EnhancedCookieSavedBanner()
            }

            // Open in app floating button
            AnimatedVisibility(
                visible = isNovelUrl && detectedProvider != null,
                enter = slideInVertically { it } + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutVertically { it } + fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                EnhancedOpenInAppButton(
                    providerName = detectedProvider?.name ?: providerName,
                    onClick = handleOpenInApp
                )
            }

            // Dismiss keyboard/edit mode when clicking outside
            if (isEditingUrl) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isEditingUrl = false
                        }
                )
            }
        }
    }
}

// ============================================================================
// Cookie Status Display
// ============================================================================

enum class CookieDisplayStatus {
    NONE,
    CHECKING,
    VALID,
    EXPIRED
}

@Composable
private fun EnhancedCookieStatusIndicator(status: CookieDisplayStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "cookie_pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (color, icon, bgColor) = when (status) {
        CookieDisplayStatus.NONE -> Triple(
            MaterialTheme.colorScheme.outline,
            Icons.Rounded.Cookie,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
        CookieDisplayStatus.CHECKING -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Rounded.HourglassEmpty,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        CookieDisplayStatus.VALID -> Triple(
            WebViewColors.Success,
            Icons.Rounded.VerifiedUser,
            WebViewColors.Success.copy(alpha = 0.15f)
        )
        CookieDisplayStatus.EXPIRED -> Triple(
            WebViewColors.Warning,
            Icons.Rounded.Warning,
            WebViewColors.Warning.copy(alpha = 0.15f)
        )
    }

    val scale by animateFloatAsState(
        targetValue = if (status == CookieDisplayStatus.VALID) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "status_scale"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (status == CookieDisplayStatus.CHECKING) pulseAlpha else 1f
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (status) {
                    CookieDisplayStatus.NONE -> "No bypass cookies"
                    CookieDisplayStatus.CHECKING -> "Checking cookies"
                    CookieDisplayStatus.VALID -> "Bypass cookies active"
                    CookieDisplayStatus.EXPIRED -> "Cookies expired"
                },
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
    }
}

@Composable
private fun EnhancedCookieSavedBanner() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WebViewColors.Success,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }

            Column {
                Text(
                    text = "Bypass Cookies Saved!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "You can go back to browsing now",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ============================================================================
// Enhanced Progress Indicator
// ============================================================================

@Composable
private fun EnhancedProgressIndicator(progress: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(200, easing = EaseOutCubic),
        label = "progress_animation"
    )

    if (animatedProgress < 0.05f) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    } else {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

// ============================================================================
// Enhanced Top Bar with Editable URL
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedWebViewTopBar(
    title: String,
    url: String,
    isLoading: Boolean,
    loadingProgress: Int,
    canGoBack: Boolean,
    isSecure: Boolean,
    cookieStatus: CookieDisplayStatus,
    providerName: String,
    isEditingUrl: Boolean,
    editUrlValue: TextFieldValue,
    onEditUrlValueChange: (TextFieldValue) -> Unit,
    onStartEditingUrl: () -> Unit,
    onCancelEditingUrl: () -> Unit,
    onSubmitUrl: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onClearCookies: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back/Close button
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (isEditingUrl) Icons.Rounded.Close
                        else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = when {
                            isEditingUrl -> "Cancel"
                            canGoBack -> "Go back"
                            else -> "Close"
                        }
                    )
                }

                // URL Bar - Animated between display and edit modes
                AnimatedContent(
                    targetState = isEditingUrl,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    modifier = Modifier.weight(1f),
                    label = "url_bar_mode"
                ) { editing ->
                    if (editing) {
                        EditableUrlBar(
                            value = editUrlValue,
                            onValueChange = onEditUrlValueChange,
                            onSubmit = { onSubmitUrl(editUrlValue.text) },
                            onCancel = onCancelEditingUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        DisplayUrlBar(
                            title = title,
                            url = url,
                            isSecure = isSecure,
                            cookieStatus = cookieStatus,
                            onClick = onStartEditingUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Refresh button (hidden when editing)
                if (!isEditingUrl) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isLoading) rotation else 0f
                            },
                            tint = if (isLoading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Go button when editing
                if (isEditingUrl) {
                    IconButton(
                        onClick = { onSubmitUrl(editUrlValue.text) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Go",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // More menu
                if (!isEditingUrl) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        EnhancedDropdownMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            cookieStatus = cookieStatus,
                            onClearCookies = {
                                onClearCookies()
                                showMenu = false
                            },
                            onEditUrl = {
                                showMenu = false
                                onStartEditingUrl()
                            },
                            onClose = {
                                onClose()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Display URL Bar (Non-editing mode)
// ============================================================================

@Composable
private fun DisplayUrlBar(
    title: String,
    url: String,
    isSecure: Boolean,
    cookieStatus: CookieDisplayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Security indicator
            Surface(
                shape = CircleShape,
                color = if (isSecure)
                    WebViewColors.Secure.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isSecure) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = if (isSecure) "Secure" else "Not secure",
                        modifier = Modifier.size(14.dp),
                        tint = if (isSecure)
                            WebViewColors.Secure
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            // Title and URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Loading..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formatDisplayUrl(url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            // Cookie status
            EnhancedCookieStatusIndicator(status = cookieStatus)
        }
    }
}

// ============================================================================
// Editable URL Bar
// ============================================================================

@Composable
private fun EditableUrlBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search/URL icon
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Text field
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        onSubmit()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = "Enter URL...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Clear button
            if (value.text.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange(TextFieldValue("")) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// Dropdown Menu
// ============================================================================

@Composable
private fun EnhancedDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    cookieStatus: CookieDisplayStatus,
    onClearCookies: () -> Unit,
    onEditUrl: () -> Unit,
    onClose: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp)
    ) {
        // Cookie status info
        if (cookieStatus != CookieDisplayStatus.NONE) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = when (cookieStatus) {
                                CookieDisplayStatus.VALID -> "Bypass Active"
                                CookieDisplayStatus.EXPIRED -> "Bypass Expired"
                                CookieDisplayStatus.CHECKING -> "Checking..."
                                else -> ""
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (cookieStatus) {
                                CookieDisplayStatus.VALID -> "Cloudflare cookies saved"
                                CookieDisplayStatus.EXPIRED -> "Please refresh the page"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = { },
                enabled = false,
                leadingIcon = {
                    Icon(
                        imageVector = when (cookieStatus) {
                            CookieDisplayStatus.VALID -> Icons.Rounded.VerifiedUser
                            CookieDisplayStatus.EXPIRED -> Icons.Rounded.Warning
                            else -> Icons.Rounded.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = when (cookieStatus) {
                            CookieDisplayStatus.VALID -> WebViewColors.Success
                            CookieDisplayStatus.EXPIRED -> WebViewColors.Warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        EnhancedMenuItem(
            text = "Edit URL",
            description = "Enter a custom URL",
            icon = Icons.Rounded.Edit,
            onClick = onEditUrl
        )

        EnhancedMenuItem(
            text = "Clear Bypass Cookies",
            description = "Remove saved Cloudflare cookies",
            icon = Icons.Rounded.DeleteOutline,
            iconTint = MaterialTheme.colorScheme.error,
            onClick = onClearCookies
        )

        EnhancedMenuItem(
            text = "Close Browser",
            description = "Return to app",
            icon = Icons.Rounded.Close,
            onClick = onClose
        )
    }
}

@Composable
private fun EnhancedMenuItem(
    text: String,
    description: String? = null,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = text,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    )
}

// ============================================================================
// Enhanced Open in App Button
// ============================================================================

@Composable
private fun EnhancedOpenInAppButton(
    providerName: String,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 16.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon container
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            Column {
                Text(
                    text = "Open in Novery",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "via $providerName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
    }
}

// ============================================================================
// Enhanced WebView Component
// ============================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EnhancedProviderWebView(
    startUrl: String,
    userAgent: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable cookies (CRITICAL for Cloudflare)
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = userAgent
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_DEFAULT
                    databaseEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onPageFinished(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }

                onWebViewCreated(this)
                loadUrl(startUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ============================================================================
// Cookie Extraction
// ============================================================================

private fun checkAndSaveCookies(
    url: String,
    domain: String,
    onCookiesSaved: () -> Unit,
    onCookieStatusUpdate: (CookieDisplayStatus) -> Unit
) {
    if (domain.isBlank()) return

    try {
        CloudflareManager.flushWebViewCookies()

        val cookies = CloudflareManager.extractCookiesFromWebView(url)

        android.util.Log.d("ProviderWebView", "Checking cookies for $domain")
        android.util.Log.d("ProviderWebView", "Cookies found: ${cookies?.contains("cf_clearance")}")

        if (cookies != null && cookies.contains("cf_clearance")) {
            val existingCookies = CloudflareManager.getCookiesForDomain(domain)
            val isNewOrUpdated = !existingCookies.contains("cf_clearance") ||
                    CloudflareManager.areCookiesExpired(domain)

            CloudflareManager.saveCookiesForDomain(
                domain = domain,
                cookies = cookies,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT
            )

            if (isNewOrUpdated) {
                android.util.Log.d("ProviderWebView", "New cookies saved for $domain")
                onCookiesSaved()
            }
            onCookieStatusUpdate(CookieDisplayStatus.VALID)
        } else {
            val status = CloudflareManager.getCookieStatus(url)
            val displayStatus = when (status) {
                CloudflareManager.CookieStatus.VALID -> CookieDisplayStatus.VALID
                CloudflareManager.CookieStatus.EXPIRED -> CookieDisplayStatus.EXPIRED
                CloudflareManager.CookieStatus.NONE -> CookieDisplayStatus.NONE
            }
            onCookieStatusUpdate(displayStatus)
        }
    } catch (e: Exception) {
        android.util.Log.e("ProviderWebView", "Error checking cookies", e)
    }
}

// ============================================================================
// Utility Functions
// ============================================================================

private fun formatDisplayUrl(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .take(50)
        .let { if (url.length > 50) "$it..." else it }
}