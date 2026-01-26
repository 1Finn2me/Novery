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

                // ============================================================
                // USER-AGENT HANDLING - CRITICAL FIX
                // Only set User-Agent if the request doesn't already have one
                // This allows providers to specify their own UA (e.g., desktop)
                // ============================================================
                val existingUserAgent = original.header("User-Agent")
                if (existingUserAgent.isNullOrBlank()) {
                    // No UA provided by the caller, use CF cookie UA or default
                    val cfUserAgent = try {
                        CloudflareManager.getUserAgent(domain)
                    } catch (e: Exception) {
                        null
                    }
                    val userAgent = cfUserAgent ?: CloudflareManager.WEBVIEW_USER_AGENT
                    requestBuilder.header("User-Agent", userAgent)
                    android.util.Log.d("NetworkClient", "Using default/CF User-Agent for $domain")
                } else {
                    android.util.Log.d("NetworkClient", "Using provider-specified User-Agent for $domain")
                }

                // ============================================================
                // CLOUDFLARE COOKIE INJECTION
                // Inject stored CF cookies for the domain
                // ============================================================
                try {
                    val cfCookies = CloudflareManager.getCookiesForDomain(domain)
                    if (cfCookies.isNotBlank()) {
                        android.util.Log.d("NetworkClient", "Injecting CF cookies for $domain")
                        val existingCookies = original.header("Cookie") ?: ""
                        val allCookies = mergeCookies(existingCookies, cfCookies)
                        requestBuilder.header("Cookie", allCookies)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetworkClient", "Error getting CF cookies", e)
                }

                // ============================================================
                // STANDARD BROWSER HEADERS
                // Only add if not already present
                // ============================================================
                if (original.header("Accept") == null) {
                    requestBuilder.header(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    )
                }
                if (original.header("Accept-Language") == null) {
                    requestBuilder.header("Accept-Language", "en-US,en;q=0.5")
                }
                if (original.header("Connection") == null) {
                    requestBuilder.header("Connection", "keep-alive")
                }
                if (original.header("Upgrade-Insecure-Requests") == null) {
                    requestBuilder.header("Upgrade-Insecure-Requests", "1")
                }

                // Add Referer if not present (some sites need this)
                if (original.header("Referer") == null) {
                    try {
                        val uri = java.net.URI(url)
                        val referer = "${uri.scheme}://${uri.host}"
                        requestBuilder.header("Referer", referer)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                val response = chain.proceed(requestBuilder.build())

                android.util.Log.d("NetworkClient", "Request to $url returned ${response.code}")

                response
            }
            .build()
    }

    /**
     * Merge existing cookies with Cloudflare cookies
     * CF cookies take precedence (overwrite existing with same name)
     */
    private fun mergeCookies(existing: String, cfCookies: String): String {
        if (existing.isBlank()) return cfCookies
        if (cfCookies.isBlank()) return existing

        val existingMap = existing.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { cookie ->
                val parts = cookie.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to cookie else null
            }
            .toMap()
            .toMutableMap()

        // CF cookies overwrite existing cookies with same name
        cfCookies.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { cookie ->
                val parts = cookie.split("=", limit = 2)
                if (parts.size == 2) {
                    existingMap[parts[0].trim()] = cookie
                }
            }

        return existingMap.values.joinToString("; ")
    }

    /**
     * Result class containing response data and Cloudflare status
     */
    data class NetworkResponse(
        val document: Document,
        val text: String,
        val isSuccessful: Boolean,
        val code: Int,
        val isCloudflareBlocked: Boolean = false,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Check if response indicates Cloudflare block
     */
    private fun checkCloudflareBlock(responseCode: Int, responseBody: String): Boolean {
        // Only check for CF block on 403/503 responses
        if (responseCode !in listOf(403, 503)) {
            return false
        }

        val cfMarkers = listOf(
            "cf-browser-verification",
            "cf_chl_opt",
            "challenge-platform",
            "Checking your browser",
            "Just a moment",
            "Verify you are human",
            "cf-turnstile",
            "challenges.cloudflare.com",
            "cf-spinner",
            "cf_chl_prog"
        )

        val bodyLower = responseBody.lowercase()
        val isBlocked = cfMarkers.any { bodyLower.contains(it.lowercase()) }

        if (isBlocked) {
            android.util.Log.w("NetworkClient", "Cloudflare block detected! Response code: $responseCode")
        }

        return isBlocked
    }

    /**
     * Perform GET request and return parsed HTML document
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            // Add custom headers from the caller
            // These will be preserved by the interceptor (won't be overwritten)
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val isBlocked = checkCloudflareBlock(response.code, body)

            // Extract response headers
            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }

            NetworkResponse(
                document = Jsoup.parse(body, url),
                text = body,
                isSuccessful = response.isSuccessful,
                code = response.code,
                isCloudflareBlocked = isBlocked,
                headers = responseHeaders
            )
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "GET request failed for $url", e)
            throw NetworkException("GET request failed: ${e.message}", e)
        }
    }

    /**
     * Perform POST request with form data
     */
    suspend fun post(
        url: String,
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val formBuilder = FormBody.Builder()
            data.forEach { (key, value) ->
                formBuilder.add(key, value)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(formBuilder.build())

            // Add content type for form data
            requestBuilder.header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")

            // Add custom headers from the caller
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val isBlocked = checkCloudflareBlock(response.code, body)

            // Extract response headers
            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }

            NetworkResponse(
                document = Jsoup.parse(body, url),
                text = body,
                isSuccessful = response.isSuccessful,
                code = response.code,
                isCloudflareBlocked = isBlocked,
                headers = responseHeaders
            )
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "POST request failed for $url", e)
            throw NetworkException("POST request failed: ${e.message}", e)
        }
    }

    /**
     * Perform POST request with JSON body
     */
    suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)

            // Add custom headers from the caller
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val isBlocked = checkCloudflareBlock(response.code, responseBody)

            // Extract response headers
            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }

            NetworkResponse(
                document = Jsoup.parse(responseBody, url),
                text = responseBody,
                isSuccessful = response.isSuccessful,
                code = response.code,
                isCloudflareBlocked = isBlocked,
                headers = responseHeaders
            )
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "POST JSON request failed for $url", e)
            throw NetworkException("POST JSON request failed: ${e.message}", e)
        }
    }

    /**
     * Download raw bytes (for images, etc.)
     */
    suspend fun downloadBytes(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw NetworkException("Download failed with code: ${response.code}")
            }

            response.body?.bytes() ?: throw NetworkException("Empty response body")
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "Download failed for $url", e)
            throw NetworkException("Download failed: ${e.message}", e)
        }
    }

    /**
     * Check if URL is accessible (HEAD request)
     */
    suspend fun isAccessible(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .head()

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            httpClient.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "Accessibility check failed for $url", e)
            false
        }
    }

    /**
     * Get raw text response without parsing as HTML
     */
    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw NetworkException("Request failed with code: ${response.code}")
            }

            response.body?.string() ?: ""
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("NetworkClient", "getText failed for $url", e)
            throw NetworkException("Request failed: ${e.message}", e)
        }
    }

    /**
     * Clear all session cookies from the cookie jar
     */
    fun clearSessionCookies() {
        cookieJar.clear()
        android.util.Log.d("NetworkClient", "Session cookies cleared")
    }

    /**
     * Get the OkHttpClient instance (for advanced use cases)
     */
    fun getClient(): OkHttpClient = httpClient
}

/**
 * In-memory cookie jar for session cookies
 */
class MemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }

        synchronized(hostCookies) {
            // Remove existing cookies with same name or expired cookies
            hostCookies.removeAll { existing ->
                cookies.any { it.name == existing.name } ||
                        existing.expiresAt < System.currentTimeMillis()
            }
            hostCookies.addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val hostCookies = cookieStore[host] ?: return emptyList()

        synchronized(hostCookies) {
            // Remove expired cookies
            hostCookies.removeAll { it.expiresAt < System.currentTimeMillis() }
            return hostCookies.toList()
        }
    }

    fun clear() {
        cookieStore.clear()
    }

    fun getCookiesForHost(host: String): List<Cookie> {
        return cookieStore[host]?.toList() ?: emptyList()
    }
}

/**
 * Custom exception for network errors
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)