package com.emptycastle.novery.provider

import androidx.annotation.DrawableRes
import com.emptycastle.novery.data.remote.NetworkClient
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.UserReview
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Abstract base class for novel providers/sources.
 */
abstract class MainProvider {

    abstract val name: String
    abstract val mainUrl: String

    open val hasMainPage: Boolean = true
    open val hasReviews: Boolean = false

    open val tags: List<FilterOption> = emptyList()
    open val orderBys: List<FilterOption> = emptyList()
    open val ratingScale: RatingScale = RatingScale.TEN_POINT

    /**
     * Rate limiting (milliseconds between requests)
     */
    open val rateLimitTime: Long = 0L

    /**
     * Optional icon resource for the provider.
     * If null, initials will be displayed instead.
     */
    @get:DrawableRes
    open val iconRes: Int? = null

    // ============================================================
    // ABSTRACT METHODS - Must be implemented by each provider
    // ============================================================

    abstract suspend fun loadMainPage(
        page: Int,
        orderBy: String? = null,
        tag: String? = null
    ): MainPageResult

    abstract suspend fun search(query: String): List<Novel>

    abstract suspend fun load(url: String): NovelDetails?

    abstract suspend fun loadChapterContent(url: String): String?

    // ============================================================
    // OPTIONAL METHODS - Override if provider supports reviews
    // ============================================================

    /**
     * Load user reviews for a novel.
     * Override this method if the provider supports reviews.
     *
     * @param url Novel URL
     * @param page Page number (1-indexed)
     * @param showSpoilers Whether to include spoiler content
     * @return List of user reviews
     */
    open suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean = false
    ): List<UserReview> = emptyList()

    // ============================================================
    // HELPER METHODS
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

    // ============================================================
    // JSOUP EXTENSION HELPERS
    // ============================================================

    protected fun Document.selectFirstOrNull(cssQuery: String): Element? {
        return this.select(cssQuery).firstOrNull()
    }

    protected fun Element.selectFirstOrNull(cssQuery: String): Element? {
        return this.select(cssQuery).firstOrNull()
    }

    protected fun Element.textOrNull(): String? {
        val text = this.text().trim()
        return if (text.isBlank()) null else text
    }

    protected fun Element.attrOrNull(attributeKey: String): String? {
        val value = this.attr(attributeKey).trim()
        return if (value.isBlank()) null else value
    }

    // ============================================================
    // PROVIDER REGISTRY
    // ============================================================

    companion object {
        private val providers = mutableListOf<MainProvider>()

        fun register(provider: MainProvider) {
            if (providers.none { it.name == provider.name }) {
                providers.add(provider)
            }
        }

        fun getProviders(): List<MainProvider> = providers.toList()

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