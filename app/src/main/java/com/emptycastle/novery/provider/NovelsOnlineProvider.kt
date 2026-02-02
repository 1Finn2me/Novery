// com/emptycastle/novery/provider/NovelsOnlineProvider.kt
package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NovelsOnlineProvider : MainProvider() {

    override val name = "NovelsOnline"
    override val mainUrl = "https://novelsonline.org"
    override val hasMainPage = true
    override val hasReviews = false
    override val iconRes: Int = R.drawable.ic_provider_novelsonline

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Celebrity", "celebrity"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Josei", "josei"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shotacon", "shotacon"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shoujo Ai", "shoujo-ai"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Shounen Ai", "shounen-ai"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    override val orderBys = listOf(
        FilterOption("Popular", "")
    )

    // ================================================================
    // SELECTOR CONFIGURATIONS
    // ================================================================

    private object Selectors {
        const val novelContainer = "div.top-novel-block"
        const val novelTitleLink = "div.top-novel-header > h2 > a, h2 > a"
        const val novelCover = "div.top-novel-content > div.top-novel-cover > a > img, .top-novel-cover img"
        const val detailTitle = "h1"
        const val detailCover = "div.novel-left > div.novel-cover > a > img, div.novel-cover > a > img, .novel-cover a > img"
        const val chapterList = "ul.chapter-chs > li > a"
        const val chapterContent = "#contentall"
        const val novelDetailItem = ".novel-detail-item"
        const val searchResultItem = "li"
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("src")
            ?: imgElement.attrOrNull("data-src")
            ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null
        return if (rawSrc.startsWith("http")) rawSrc else "$mainUrl$rawSrc"
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "hiatus", "on hiatus" -> "On Hiatus"
            "dropped", "cancelled", "canceled" -> "Cancelled"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        // Don't pass siteName to avoid breaking URLs
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "")
        // Remove common noise text
        cleaned = cleaned.replace("Your browser does not support JavaScript!", "")
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    /**
     * Parse view count string to integer
     * Handles formats like: "714453", "1,234,567", "1.5M", "500K"
     */
    private fun parseViewCount(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.replace(",", "").trim().uppercase()

        return when {
            cleaned.endsWith("K") -> {
                cleaned.dropLast(1).toFloatOrNull()?.times(1_000)?.toInt()
            }
            cleaned.endsWith("M") -> {
                cleaned.dropLast(1).toFloatOrNull()?.times(1_000_000)?.toInt()
            }
            cleaned.endsWith("B") -> {
                cleaned.dropLast(1).toFloatOrNull()?.times(1_000_000_000)?.toInt()
            }
            else -> cleaned.toIntOrNull()
        }
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.select(Selectors.novelContainer)
        return elements.mapNotNull { element -> parseNovelElement(element) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirstOrNull("div.top-novel-header > h2 > a")
            ?: element.selectFirstOrNull("h2 > a")
            ?: return null

        val name = titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null

        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return null

        val imgElement = element.selectFirstOrNull("div.top-novel-content > div.top-novel-cover > a > img")
            ?: element.selectFirstOrNull(".top-novel-cover img")
        val posterUrl = fixPosterUrl(imgElement)

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            apiName = this.name
        )
    }

    private fun parseSearchNovels(document: Document): List<Novel> {
        return document.select(Selectors.searchResultItem).mapNotNull { element ->
            val linkElement = element.selectFirstOrNull("a") ?: return@mapNotNull null
            val name = element.textOrNull()?.trim()
            if (name.isNullOrBlank()) return@mapNotNull null

            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return@mapNotNull null

            val posterUrl = fixPosterUrl(element.selectFirstOrNull("img"))

            Novel(
                name = name,
                url = novelUrl,
                posterUrl = posterUrl,
                apiName = this.name
            )
        }
    }

    // ================================================================
    // RELATED NOVELS PARSING
    // ================================================================

    /**
     * Parse "You May Also Like" section for related novels
     */
    private fun parseRelatedNovels(document: Document): List<Novel> {
        val novels = mutableListOf<Novel>()

        document.select(Selectors.novelDetailItem).forEach { item ->
            val label = item.selectFirstOrNull("h6")?.textOrNull()?.trim()?.lowercase() ?: return@forEach

            if (label.contains("you may also like")) {
                val body = item.selectFirstOrNull(".novel-detail-body") ?: return@forEach

                body.select("ul > li").forEach { li ->
                    val linkElement = li.selectFirstOrNull("a") ?: return@forEach
                    val href = linkElement.attrOrNull("href")
                    val name = linkElement.textOrNull()?.trim()

                    // Skip empty links
                    if (name.isNullOrBlank() || href.isNullOrBlank() || href == "$mainUrl/") {
                        return@forEach
                    }

                    val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return@forEach

                    // Extract status from span if available (e.g., "(Ongoing)")
                    val statusSpan = li.selectFirstOrNull("span")?.textOrNull()
                        ?.replace("(", "")?.replace(")", "")?.trim()

                    novels.add(
                        Novel(
                            name = name,
                            url = novelUrl,
                            posterUrl = null, // Cover not available in this section
                            apiName = this.name
                        )
                    )
                }
            }
        }

        return novels
    }

    /**
     * Parse "Related Series" section
     */
    private fun parseRelatedSeries(document: Document): List<Novel> {
        val novels = mutableListOf<Novel>()

        document.select(Selectors.novelDetailItem).forEach { item ->
            val label = item.selectFirstOrNull("h6")?.textOrNull()?.trim()?.lowercase() ?: return@forEach

            if (label.contains("related series")) {
                val body = item.selectFirstOrNull(".novel-detail-body") ?: return@forEach

                body.select("ul > li > a").forEach { linkElement ->
                    val href = linkElement.attrOrNull("href")
                    val name = linkElement.textOrNull()?.trim()

                    // Skip empty links
                    if (name.isNullOrBlank() || href.isNullOrBlank() || href == "$mainUrl/") {
                        return@forEach
                    }

                    val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return@forEach

                    novels.add(
                        Novel(
                            name = name,
                            url = novelUrl,
                            posterUrl = null,
                            apiName = this.name
                        )
                    )
                }
            }
        }

        return novels
    }

    /**
     * Parse "Alternative Names" section
     */
    private fun parseAlternativeNames(document: Document): List<String> {
        val altNames = mutableListOf<String>()

        document.select(Selectors.novelDetailItem).forEach { item ->
            val label = item.selectFirstOrNull("h6")?.textOrNull()?.trim()?.lowercase() ?: return@forEach

            if (label.contains("alternative names")) {
                val body = item.selectFirstOrNull(".novel-detail-body") ?: return@forEach

                body.select("ul > li").forEach { li ->
                    // Get text from either link or direct text
                    val name = li.selectFirstOrNull("a")?.textOrNull()?.trim()
                        ?: li.textOrNull()?.trim()

                    if (!name.isNullOrBlank()) {
                        altNames.add(name)
                    }
                }
            }
        }

        return altNames
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(page: Int, orderBy: String?, tag: String?): MainPageResult {
        val url = if (tag.isNullOrBlank()) {
            "$mainUrl/top-novel/$page"
        } else {
            "$mainUrl/category/$tag/$page"
        }

        val response = get(url)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please try opening in WebView.")
        }

        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val response = post(
            url = "$mainUrl/sResults.php",
            data = mapOf("q" to query)
        )
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please try opening in WebView.")
        }

        return parseSearchNovels(document)
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val response = get(fullUrl)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please try opening in WebView.")
        }

        val name = document.selectFirstOrNull(Selectors.detailTitle)?.textOrNull()?.trim() ?: "Unknown"

        // Parse chapters
        val chapters = document.select(Selectors.chapterList).mapNotNull { chapterElement ->
            val chapterName = chapterElement.textOrNull()?.trim()
            val chapterHref = chapterElement.attrOrNull("href")
            if (chapterName.isNullOrBlank() || chapterHref.isNullOrBlank()) return@mapNotNull null

            val chapterUrl = fixUrl(chapterHref.removePrefix(mainUrl).removePrefix("/")) ?: return@mapNotNull null
            Chapter(name = chapterName, url = chapterUrl)
        }

        // Extract metadata (includes views and rating)
        val metadata = extractMetadata(document)

        // Parse related novels from "You May Also Like" and "Related Series"
        val youMayAlsoLike = parseRelatedNovels(document)
        val relatedSeries = parseRelatedSeries(document)

        // Combine both lists, with related series first (they're more closely related)
        val allRelatedNovels = (relatedSeries + youMayAlsoLike).distinctBy { it.url }

        // Parse alternative names (could be used for display or search)
        val alternativeNames = parseAlternativeNames(document)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating,
            status = metadata.status,
            views = metadata.views,
            relatedNovels = allRelatedNovels.ifEmpty { null },
        )
    }

    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val rating: Int? = null,
        val status: String? = null,
        val views: Int? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        var author: String? = null
        var synopsis: String? = null
        var tags: List<String> = emptyList()
        var status: String? = null
        var rating: Int? = null
        var views: Int? = null

        // Parse novel detail items
        document.select(Selectors.novelDetailItem).forEach { item ->
            val label = item.selectFirstOrNull("h6")?.textOrNull()?.trim()?.lowercase() ?: return@forEach
            val body = item.selectFirstOrNull(".novel-detail-body")

            when {
                label.contains("description") -> {
                    synopsis = body?.textOrNull()?.trim()
                }
                label.contains("genre") -> {
                    tags = body?.select("li")?.mapNotNull {
                        it.textOrNull()?.trim()
                    }?.filter { it.isNotBlank() } ?: emptyList()
                }
                label.contains("author") -> {
                    author = body?.select("li")?.mapNotNull {
                        it.textOrNull()?.trim()
                    }?.filter { it.isNotBlank() && it != "N/A" }?.joinToString(", ")
                }
                label.contains("status") -> {
                    status = parseStatus(body?.textOrNull()?.trim())
                }
                label.contains("total views") -> {
                    views = parseViewCount(body?.textOrNull()?.trim())
                }
                label.contains("rating") -> {
                    // Rating is out of 10 on this site based on the example (8.0)
                    val ratingValue = body?.textOrNull()?.trim()?.toFloatOrNull()
                    rating = ratingValue?.let { RatingUtils.from10Points(it) }
                }
            }
        }

        // Get poster URL using multiple selectors
        val posterElement = document.selectFirstOrNull("div.novel-left > div.novel-cover > a > img")
            ?: document.selectFirstOrNull("div.novel-cover > a > img")
            ?: document.selectFirstOrNull(".novel-cover a > img")
        val posterUrl = fixPosterUrl(posterElement)

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis ?: "No Summary Found",
            tags = tags,
            rating = rating,
            status = status,
            views = views
        )
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val response = get(fullUrl)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please try opening in WebView.")
        }

        val contentElement = document.selectFirstOrNull(Selectors.chapterContent) ?: return null

        // Remove unwanted elements
        contentElement.select(
            ".ads, .adsbygoogle, script, style, .ads-holder, .ads-middle, " +
                    "[id*='ads'], [class*='ads'], .hidden, " +
                    "[style*='display:none'], [style*='display: none'], " +
                    "iframe, .social-share, .chapter-nav, .pagination"
        ).remove()

        // Fix relative image URLs to absolute URLs before processing
        fixRelativeUrls(contentElement)

        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }

    /**
     * Convert all relative URLs (images, links) to absolute URLs
     */
    private fun fixRelativeUrls(element: Element) {
        // Fix image src attributes
        element.select("img").forEach { img ->
            listOf("src", "data-src", "data-original", "data-lazy-src").forEach { attr ->
                val value = img.attr(attr)
                if (value.isNotBlank()) {
                    img.attr(attr, makeAbsoluteUrl(value))
                }
            }
        }

        // Fix link href attributes
        element.select("a[href]").forEach { link ->
            val href = link.attr("href")
            if (href.isNotBlank() && !href.startsWith("#") && !href.startsWith("javascript:")) {
                link.attr("href", makeAbsoluteUrl(href))
            }
        }
    }

    /**
     * Convert a potentially relative URL to an absolute URL
     */
    private fun makeAbsoluteUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$mainUrl$url"
            url.startsWith("data:") -> url
            url.startsWith("#") -> url
            url.isBlank() -> url
            else -> "$mainUrl/$url"
        }
    }

}