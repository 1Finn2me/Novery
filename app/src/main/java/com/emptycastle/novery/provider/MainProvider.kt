package com.emptycastle.novery.provider

import com.emptycastle.novery.data.remote.NetworkClient
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Abstract base class for novel providers/sources.
 * Equivalent to MainAPI in React.
 *
 * Each provider implements scraping logic for a specific novel website.
 */
abstract class MainProvider {

    /**
     * Display name of the provider
     */
    abstract val name: String

    /**
     * Base URL of the website
     */
    abstract val mainUrl: String

    /**
     * Whether this provider has a browsable main page
     */
    open val hasMainPage: Boolean = true

    /**
     * Available genre/category tags for filtering
     */
    open val tags: List<FilterOption> = emptyList()

    /**
     * Available sorting options
     */
    open val orderBys: List<FilterOption> = emptyList()

    /**
     * The rating scale used by this provider.
     * Used to normalize ratings to a common 0-1000 scale.
     */
    open val ratingScale: RatingScale = RatingScale.TEN_POINT

    // ============================================================
    // ABSTRACT METHODS - Must be implemented by each provider
    // ============================================================

    /**
     * Load the main catalog page with optional filters.
     *
     * @param page Page number (1-indexed)
     * @param orderBy Sort option value
     * @param tag Genre/category filter value
     * @return List of novels on this page
     */
    abstract suspend fun loadMainPage(
        page: Int,
        orderBy: String? = null,
        tag: String? = null
    ): MainPageResult

    /**
     * Search for novels by query.
     *
     * @param query Search term
     * @return List of matching novels
     */
    abstract suspend fun search(query: String): List<Novel>

    /**
     * Load detailed information about a novel including chapter list.
     *
     * @param url Novel page URL
     * @return Novel details with chapters
     */
    abstract suspend fun load(url: String): NovelDetails?

    /**
     * Load the HTML content of a chapter for reading.
     *
     * @param url Chapter URL
     * @return HTML content string
     */
    abstract suspend fun loadChapterContent(url: String): String?

    // ============================================================
    // HELPER METHODS - Available to all providers
    // ============================================================

    /**
     * Perform GET request
     */
    protected suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkClient.NetworkResponse {
        return NetworkClient.get(url, headers)
    }

    /**
     * Perform POST request
     */
    protected suspend fun post(
        url: String,
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): NetworkClient.NetworkResponse {
        return NetworkClient.post(url, data, headers)
    }

    /**
     * Fix relative URL to absolute URL
     */
    protected fun fixUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null

        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$mainUrl$url"
            else -> "$mainUrl/$url"
        }
    }

    /**
     * Convenience method to select first element matching CSS selector
     */
    protected fun Document.selectFirstOrNull(cssQuery: String): Element? {
        return this.select(cssQuery).firstOrNull()
    }

    /**
     * Convenience method to select first element matching CSS selector from Element
     */
    protected fun Element.selectFirstOrNull(cssQuery: String): Element? {
        return this.select(cssQuery).firstOrNull()
    }

    /**
     * Get text content, trimmed, or null if empty
     */
    protected fun Element.textOrNull(): String? {
        val text = this.text().trim()
        return if (text.isBlank()) null else text
    }

    /**
     * Get attribute value or null if empty
     */
    protected fun Element.attrOrNull(attributeKey: String): String? {
        val value = this.attr(attributeKey).trim()
        return if (value.isBlank()) null else value
    }

    // ============================================================
    // PROVIDER REGISTRY
    // ============================================================

    companion object {
        private val providers = mutableListOf<MainProvider>()

        /**
         * Register a provider to make it available in the app
         */
        fun register(provider: MainProvider) {
            if (providers.none { it.name == provider.name }) {
                providers.add(provider)
            }
        }

        /**
         * Get all registered providers
         */
        fun getProviders(): List<MainProvider> = providers.toList()

        /**
         * Get provider by name
         */
        fun getProvider(name: String): MainProvider? {
            return providers.find { it.name == name }
        }
    }
}
enum class RatingScale(val maxValue: Float) {
    FIVE_STAR(5f),
    TEN_POINT(10f),
    HUNDRED_POINT(100f)
}
