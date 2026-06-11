package com.emptycastle.novery.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Network client for making HTTP requests with Cloudflare bypass support.
 *
 * Improvements over the previous version:
 * - CF challenge detection is more precise (checks status + markers together).
 * - User-Agent priority is explicit: caller header > CF saved UA > default.
 * - Cookie merge keeps the most recently-set value per name (CF wins).
 * - All response builder paths share a single helper to avoid duplication.
 */
object NetworkClient {

    private val cookieJar = MemoryCookieJar()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.toString()
                val domain = CloudflareManager.getDomain(url)
                val requestBuilder = original.newBuilder()

                // ── User-Agent ───────────────────────────────────────────────
                // Priority: caller-provided > CF saved > hardcoded default.
                if (original.header("User-Agent").isNullOrBlank()) {
                    val ua = runCatching { CloudflareManager.getUserAgent(domain) }.getOrNull()
                        ?: CloudflareManager.WEBVIEW_USER_AGENT
                    requestBuilder.header("User-Agent", ua)
                }

                // ── Cloudflare Cookie Injection ──────────────────────────────
                runCatching {
                    val cfCookies = CloudflareManager.getCookiesForDomain(domain)
                    if (cfCookies.isNotBlank()) {
                        val existing = original.header("Cookie") ?: ""
                        requestBuilder.header("Cookie", mergeCookies(existing, cfCookies))
                        android.util.Log.d("NetworkClient", "Injected CF cookies for $domain")
                    }
                }.onFailure {
                    android.util.Log.e("NetworkClient", "CF cookie injection failed", it)
                }

                // ── Standard Browser Headers (non-overwriting) ───────────────
                fun setIfAbsent(name: String, value: String) {
                    if (original.header(name) == null) requestBuilder.header(name, value)
                }
                setIfAbsent("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                setIfAbsent("Accept-Language", "en-US,en;q=0.9")
                setIfAbsent("Connection", "keep-alive")
                setIfAbsent("Upgrade-Insecure-Requests", "1")
                setIfAbsent("Cache-Control", "max-age=0")
                runCatching {
                    val uri = java.net.URI(url)
                    setIfAbsent("Referer", "${uri.scheme}://${uri.host}/")
                }

                val response = chain.proceed(requestBuilder.build())
                android.util.Log.d("NetworkClient", "${response.code} ← $url")
                response
            }
            .build()
    }

    // ========================================================================
    // Response Model
    // ========================================================================

    data class NetworkResponse(
        val document: Document,
        val text: String,
        val isSuccessful: Boolean,
        val code: Int,
        val isCloudflareBlocked: Boolean = false,
        val headers: Map<String, String> = emptyMap(),
        val effectiveUrl: String = ""
    )

    // ========================================================================
    // Cloudflare Detection
    // ========================================================================

    /**
     * Returns true only when the response both has a 4xx/5xx status AND
     * contains Cloudflare challenge markers in the body.
     * Previously this returned false positives on normal 403 pages.
     */
    private fun isCloudflareChallenge(code: Int, body: String): Boolean {
        if (code !in listOf(403, 503)) return false
        return CloudflareManager.isCloudflareChallengeHtml(body).also { blocked ->
            if (blocked) android.util.Log.w("NetworkClient", "CF challenge detected (HTTP $code)")
        }
    }

    // ========================================================================
    // Cookie Merging
    // ========================================================================

    /**
     * Merge two cookie header strings. When the same cookie name appears in
     * both, the [cfCookies] value wins (Cloudflare bypass cookies take
     * precedence over session cookies).
     */
    private fun mergeCookies(existing: String, cfCookies: String): String {
        if (existing.isBlank()) return cfCookies
        if (cfCookies.isBlank()) return existing

        val merged = mutableMapOf<String, String>()

        // Add existing first, then overwrite with CF cookies.
        sequenceOf(existing, cfCookies)
            .flatMap { it.split(";").asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains("=") }
            .forEach { cookie ->
                val name = cookie.substringBefore("=").trim()
                merged[name] = cookie
            }

        return merged.values.joinToString("; ")
    }

    // ========================================================================
    // Shared Response Builder
    // ========================================================================

    private fun buildResponse(
        code: Int,
        body: String,
        url: String,
        headers: okhttp3.Headers
    ): NetworkResponse = NetworkResponse(
        document = Jsoup.parse(body, url),
        text = body,
        isSuccessful = code in 200..299,
        code = code,
        isCloudflareBlocked = isCloudflareChallenge(code, body),
        headers = headers.toMap(),
        effectiveUrl = url
    )

    private fun okhttp3.Headers.toMap(): Map<String, String> =
        (0 until size).associate { name(it) to value(it) }

    // ========================================================================
    // Public API
    // ========================================================================

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): NetworkResponse =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get()
                    .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                buildResponse(response.code, body, url, response.headers)
            } catch (e: Exception) {
                android.util.Log.e("NetworkClient", "GET failed: $url", e)
                throw NetworkException("GET request failed: ${e.message}", e)
            }
        }

    suspend fun post(
        url: String,
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder().also { b -> data.forEach { (k, v) -> b.add(k, v) } }.build()
            val request = Request.Builder().url(url).post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            buildResponse(response.code, body, url, response.headers)
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "POST failed: $url", e)
            throw NetworkException("POST request failed: ${e.message}", e)
        }
    }

    suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType))
                .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            buildResponse(response.code, body, url, response.headers)
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "POST JSON failed: $url", e)
            throw NetworkException("POST JSON request failed: ${e.message}", e)
        }
    }

    suspend fun downloadBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get()
                    .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw NetworkException("Download failed: HTTP ${response.code}")
                response.body?.bytes() ?: throw NetworkException("Empty response body")
            } catch (e: NetworkException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("NetworkClient", "Download failed: $url", e)
                throw NetworkException("Download failed: ${e.message}", e)
            }
        }

    suspend fun isAccessible(url: String, headers: Map<String, String> = emptyMap()): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).head()
                    .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                    .build()
                httpClient.newCall(request).execute().isSuccessful
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get()
                    .also { b -> headers.forEach { (k, v) -> b.header(k, v) } }
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw NetworkException("HTTP ${response.code}")
                response.body?.string() ?: ""
            } catch (e: NetworkException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("NetworkClient", "getText failed: $url", e)
                throw NetworkException("Request failed: ${e.message}", e)
            }
        }

    fun clearSessionCookies() {
        cookieJar.clear()
        android.util.Log.d("NetworkClient", "Session cookies cleared")
    }

    fun getClient(): OkHttpClient = httpClient
}

// ============================================================================
// In-memory Cookie Jar
// ============================================================================

class MemoryCookieJar : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostCookies = store.getOrPut(host) { mutableListOf() }
        synchronized(hostCookies) {
            val now = System.currentTimeMillis()
            hostCookies.removeAll { existing ->
                cookies.any { it.name == existing.name } || existing.expiresAt < now
            }
            hostCookies.addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val hostCookies = store[host] ?: return emptyList()
        synchronized(hostCookies) {
            val now = System.currentTimeMillis()
            hostCookies.removeAll { it.expiresAt < now }
            return hostCookies.toList()
        }
    }

    fun clear() = store.clear()

    fun getCookiesForHost(host: String): List<Cookie> = store[host]?.toList() ?: emptyList()
}

// ============================================================================
// Exception
// ============================================================================

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)