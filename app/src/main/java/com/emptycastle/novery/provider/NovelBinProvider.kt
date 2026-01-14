package com.emptycastle.novery.provider

import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import kotlinx.coroutines.delay
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.atomic.AtomicLong

/**
 * Provider for NovelBin.com
 * Improved based on the JavaScript reference implementation with better parsing,
 * error handling, and multiple fallback selectors.
 */
class NovelBinProvider : MainProvider() {

    override val name = "NovelBin"
    override val mainUrl = "https://novelbin.com"
    override val hasMainPage = true

    // ================================================================
    // THROTTLING & PATTERNS
    // ================================================================

    // Throttling for search requests (3.4 seconds as per JS implementation)
    private val searchInterval = 3400L
    private val lastSearchTime = AtomicLong(0)

    // Regex patterns for URL and content cleaning
    private val fullPosterRegex = Regex("/novel_[0-9]*_[0-9]*/")
    private val novelIdRegex = Regex("\\d+")
    private val alertRegex = Regex("alert\\(['\"]?(.*?)['\"]?\\)")

    // Content cleaning patterns
    private val cleaningPatterns = listOf(
        Regex("<iframe[^>]*>.*?</iframe>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<div[^>]*class=\"[^\"]*(?:unlock-buttons|ads|adsbygoogle)[^\"]*\"[^>]*>.*?</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<sub>.*?</sub>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", "All"),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Anime & Comics", "anime-&-comics"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Fan-fiction", "fan-fiction"),
        FilterOption("Fanfiction", "fanfiction"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Game", "game"),
        FilterOption("Games", "games"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("General", "general"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("LitRPG", "litrpg"),
        FilterOption("Magic", "magic"),
        FilterOption("Magical Realism", "magical-realism"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Military", "military"),
        FilterOption("Modern Life", "modern-life"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Other", "other"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shoujo Ai", "shoujo-ai"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Shounen Ai", "shounen-ai"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Smut", "smut"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("System", "system"),
        FilterOption("Thriller", "thriller"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Urban", "urban"),
        FilterOption("Urban Life", "urban-life"),
        FilterOption("Video Games", "video-games"),
        FilterOption("War", "war"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    override val orderBys = listOf(
        FilterOption("Genre", ""),
        FilterOption("Latest Release", "sort/latest"),
        FilterOption("Hot Novel", "sort/top-hot-novel"),
        FilterOption("Completed Novel", "sort/completed"),
        FilterOption("Most Popular", "sort/top-view-novel")
    )

    // ================================================================
    // SELECTOR CONFIGURATIONS
    // ================================================================

    // Multiple fallback selectors for different page layouts
    private object Selectors {
        val novelContainers = listOf(
            "div.archive div.list > div.row",
            "div.col-content div.list > div.row",
            "#list-page .archive .list .row",
            "div.list > div.row"
        )

        val novelTitle = listOf(
            "h3.novel-title > a",
            "h3.truyen-title > a",
            ".novel-title > a",
            ".truyen-title > a",
            "h3 > a"
        )

        val novelDetailTitle = listOf(
            "h3.title",
            "div.books h3",
            "div.m-imgtxt h3",
            ".book-info h3"
        )

        val chapterContent = listOf(
            "#chapter-content",
            "#chr-content",
            ".txt",
            ".chapter-content",
            "#content"
        )

        val chapterList = listOf(
            "select > option[value]",
            ".list-chapter > li > a",
            "ul.list-chapter li a",
            "#list-chapter a"
        )

        val synopsis = listOf(
            "div.desc-text",
            "div.inner",
            ".summary .content",
            "#editdescription"
        )

        val poster = listOf(
            "div.book > img",
            "div.books img",
            "div.m-imgtxt img",
            ".book-info img"
        )

        val author = listOf(
            "ul.info > li:nth-child(1) > a",
            "ul.info-meta li:contains(Author) a",
            ".info li:contains(Author) a",
            "span[title=Author] + a",
            "a[href*='/author/']"
        )

        val genres = listOf(
            "ul.info > li:nth-child(5) a",
            "ul.info-meta li:contains(Genre) a",
            ".info li:contains(Genre) a",
            "a[href*='/genre/']"
        )

        val status = listOf(
            "ul.info > li:nth-child(3) > a",
            "ul.info-meta li:contains(Status) a",
            ".info li:contains(Status) a",
            "span[title=Status] + a"
        )

        val novelId = listOf(
            "#rating[data-novel-id]",
            "[data-novel-id]"
        )

        val ratingValue = listOf(
            "div.small > em > strong:nth-child(1) > span",
            ".rating-value",
            "[itemprop=ratingValue]"
        )

        val ratingCount = listOf(
            "div.small > em > strong:nth-child(3) > span",
            ".rating-count",
            "[itemprop=ratingCount]"
        )
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    /**
     * Try multiple selectors until one returns results
     */
    private fun Document.selectFirst(selectors: List<String>): Element? {
        for (selector in selectors) {
            val element = this.selectFirstOrNull(selector)
            if (element != null) return element
        }
        return null
    }

    private fun Element.selectFirst(selectors: List<String>): Element? {
        for (selector in selectors) {
            val element = this.selectFirstOrNull(selector)
            if (element != null) return element
        }
        return null
    }

    private fun Document.selectAny(selectors: List<String>): Elements {
        for (selector in selectors) {
            val elements = this.select(selector)
            if (elements.isNotEmpty()) return elements
        }
        return Elements()
    }

    /**
     * Fix and normalize cover image URLs
     * Checks multiple possible sources and cleans up the URL
     */
    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null

        // Try multiple image source attributes (as per JS: data-src, src, data-cfsrc)
        val rawSrc = imgElement.attrOrNull("data-src")
            ?: imgElement.attrOrNull("src")
            ?: imgElement.attrOrNull("data-cfsrc")
            ?: return null

        // Skip placeholder/loading images
        if (rawSrc.contains("loading") || rawSrc.contains("placeholder")) return null

        // Clean up thumbnail URLs to get full-size images
        val cleanedSrc = rawSrc.replace(fullPosterRegex, "/novel/")
        return fixUrl(cleanedSrc)
    }

    /**
     * Map status string to standardized status
     */
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

    /**
     * Extract novel ID from various sources
     */
    private fun extractNovelId(document: Document, url: String): String? {
        // Try to get from data attribute first
        for (selector in Selectors.novelId) {
            val element = document.selectFirstOrNull(selector)
            val id = element?.attrOrNull("data-novel-id")
            if (!id.isNullOrBlank()) return id
        }

        // Fallback: extract from URL
        return novelIdRegex.find(url)?.value
    }

    /**
     * Clean chapter content HTML
     */
    private fun cleanChapterHtml(html: String): String {
        var cleaned = html

        // Apply all cleaning patterns
        for (pattern in cleaningPatterns) {
            cleaned = cleaned.replace(pattern, " ")
        }

        // Use HtmlUtils for additional provider-specific cleaning
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "novelbin")

        // Clean up excessive whitespace
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")

        return cleaned.trim()
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    /**
     * Parse novels from HTML document using multiple fallback selectors
     */
    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.selectAny(Selectors.novelContainers)

        return elements.mapNotNull { element ->
            parseNovelElement(element)
        }
    }

    /**
     * Parse a single novel element from the list
     */
    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst(Selectors.novelTitle) ?: return null

        val name = titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null

        val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return null

        // Get poster image
        val imgElement = element.selectFirstOrNull("img")
        val posterUrl = fixPosterUrl(imgElement)

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            apiName = this.name
        )
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?
    ): MainPageResult {
        val url = when {
            // If genre filter selected (orderBy is empty = "Genre" mode)
            orderBy.isNullOrEmpty() && !tag.isNullOrEmpty() && tag != "All" -> {
                "$mainUrl/genre/$tag?page=$page"
            }
            // Otherwise use sort option
            else -> {
                val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "sort/top-hot-novel"
                "$mainUrl/$sort?page=$page"
            }
        }

        val response = get(url)
        val document = response.document

        val novels = parseNovels(document)

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        // Throttle search requests (as per JS implementation)
        val now = System.currentTimeMillis()
        val lastSearch = lastSearchTime.get()
        if (lastSearch > 0 && (now - lastSearch) < searchInterval) {
            delay(searchInterval - (now - lastSearch))
        }
        lastSearchTime.set(System.currentTimeMillis())

        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search?keyword=$encodedQuery"

        val response = get(url)
        val html = response.text

        // Check for anti-bot alerts (as per JS implementation)
        val alertMatch = alertRegex.find(html)
        if (alertMatch != null) {
            val message = alertMatch.groupValues.getOrNull(1) ?: "Search blocked"
            throw Exception("Search blocked: $message")
        }

        val document = response.document
        return parseNovels(document)
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val response = get(url)
        val document = response.document

        // Get novel title - try multiple selectors
        val name = document.selectFirst(Selectors.novelDetailTitle)?.textOrNull()?.trim()
            ?: "Unknown"

        // Get novel ID for chapter list AJAX call
        val dataNovelId = extractNovelId(document, url)

        // Load chapters via AJAX
        val chapters = loadChaptersViaAjax(dataNovelId)

        // Extract all metadata
        val metadata = extractMetadata(document)

        return NovelDetails(
            url = url,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating,
            peopleVoted = metadata.peopleVoted,
            status = metadata.status
        )
    }

    /**
     * Container for novel metadata
     */
    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val rating: Int? = null,
        val peopleVoted: Int? = null,
        val status: String? = null
    )

    /**
     * Extract all metadata from novel page
     */
    private fun extractMetadata(document: Document): NovelMetadata {
        // Author
        val author = document.selectFirst(Selectors.author)?.textOrNull()?.trim()

        // Poster
        val posterUrl = document.selectFirst(Selectors.poster)?.let { fixPosterUrl(it) }

        // Synopsis
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            // Get text content, preserving some formatting
            element.select("br").append("\\n")
            element.select("p").prepend("\\n")
            element.text()
                .replace("\\n", "\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        }

        // Tags/Genres
        val tags = document.selectAny(Selectors.genres)
            .mapNotNull { it.textOrNull()?.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // Status
        val status = document.selectFirst(Selectors.status)?.textOrNull()?.let { parseStatus(it) }

        // Rating - NovelBin uses 10-point scale
        val ratingText = document.selectFirst(Selectors.ratingValue)?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let {
            RatingUtils.from10Points(it)  // ← Use RatingUtils!
        }


        // Vote count
        val votedText = document.selectFirst(Selectors.ratingCount)?.textOrNull()
        val peopleVoted = votedText?.replace(Regex("[^0-9]"), "")?.toIntOrNull()

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status
        )
    }

    /**
     * Load chapters via AJAX endpoint
     */
    private suspend fun loadChaptersViaAjax(novelId: String?): List<Chapter> {
        if (novelId.isNullOrBlank()) return emptyList()

        val chapters = mutableListOf<Chapter>()

        try {
            val ajaxUrl = "$mainUrl/ajax/chapter-archive?novelId=$novelId"
            val chapterResponse = get(ajaxUrl)
            val chapterDoc = chapterResponse.document

            // Try multiple chapter list formats
            for (selector in Selectors.chapterList) {
                val chapterElements = chapterDoc.select(selector)

                if (chapterElements.isNotEmpty()) {
                    var chapterIndex = 0

                    chapterElements.forEach { element ->
                        // Get URL from value attribute (for <option>) or href (for <a>)
                        val chapterUrl = element.attrOrNull("value")
                            ?: element.attrOrNull("href")
                            ?: return@forEach

                        // Skip empty or placeholder values
                        if (chapterUrl.isBlank() || chapterUrl == "#" || chapterUrl == "0") {
                            return@forEach
                        }

                        val fixedUrl = fixUrl(chapterUrl) ?: return@forEach

                        chapterIndex++

                        // Get chapter name with fallbacks
                        val chapterName = element.attrOrNull("title")?.takeIf { it.isNotBlank() }
                            ?: element.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                            ?: "Chapter $chapterIndex"

                        chapters.add(
                            Chapter(
                                name = chapterName,
                                url = fixedUrl
                            )
                        )
                    }

                    // If we found chapters, don't try other selectors
                    if (chapters.isNotEmpty()) break
                }
            }
        } catch (e: Exception) {
            // Log but don't fail - some novels might not have chapters via AJAX
            e.printStackTrace()
        }

        return chapters
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val response = get(url)
        val document = response.document

        // Try multiple selectors for content container
        val contentElement = document.selectFirst(Selectors.chapterContent) ?: return null

        // Remove unwanted elements before getting HTML
        contentElement.select(
            ".unlock-buttons, .ads, .adsbygoogle, sub, script, style, " +
                    ".ads-holder, .ads-middle, [id*='ads'], [class*='ads'], " +
                    ".hidden, [style*='display:none'], [style*='display: none']"
        ).remove()

        val rawHtml = contentElement.html()

        // Clean and return
        return cleanChapterHtml(rawHtml)
    }
}