package com.emptycastle.novery.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * Network client for making HTTP requests.
 * Unlike the React version, we don't need CORS proxies!
 * Android apps can make direct requests to any server.
 */
object NetworkClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Connection", "keep-alive")

                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    /**
     * Result class containing both parsed document and raw text
     */
    data class NetworkResponse(
        val document: Document,
        val text: String,
        val isSuccessful: Boolean,
        val code: Int
    )

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

            // Add custom headers
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            NetworkResponse(
                document = Jsoup.parse(body, url),
                text = body,
                isSuccessful = response.isSuccessful,
                code = response.code
            )
        } catch (e: Exception) {
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
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")

            // Add custom headers
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            NetworkResponse(
                document = Jsoup.parse(body, url),
                text = body,
                isSuccessful = response.isSuccessful,
                code = response.code
            )
        } catch (e: Exception) {
            throw NetworkException("POST request failed: ${e.message}", e)
        }
    }

    /**
     * Download raw bytes (for images, etc.)
     */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.body?.bytes() ?: throw NetworkException("Empty response body")
        } catch (e: Exception) {
            throw NetworkException("Download failed: ${e.message}", e)
        }
    }

    /**
     * Check if URL is accessible
     */
    suspend fun isAccessible(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()

            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}

/**
 * Custom exception for network errors
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)