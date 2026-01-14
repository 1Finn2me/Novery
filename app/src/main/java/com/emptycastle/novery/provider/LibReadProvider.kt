package com.emptycastle.novery.provider

import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.atomic.AtomicLong

/**
 * Provider for LibRead.com
 * Improved based on the JavaScript reference implementation with:
 * - POST-based search
 * - Search throttling
 * - Multiple fallback selectors
 * - Fallback chapter parsing from HTML
 * - Novel type filters (Chinese, Korean, Japanese, English)
 */
class LibReadProvider : MainProvider() {

    override val name = "LibRead"
    override val mainUrl = "https://libread.com"
    override val hasMainPage = true

    // ================================================================
    // THROTTLING & PATTERNS
    // ================================================================

    // Throttling for search requests (3.4 seconds as per JS implementation)
    private val searchInterval = 3400L
    private val lastSearchTime = AtomicLong(0)

    // Paths that don't support pagination
    private val noPagesPaths = listOf("sort/most-popular")

    // Regex patterns
    private val alertRegex = Regex("alert\\(['\"]?(.*?)['\"]?\\)")
    private val aidRegex = Regex("([0-9]+)s\\.jpg")

    // Content cleaning patterns
    private val cleaningPatterns = listOf(
        Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<iframe[^>]*>.*?</iframe>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<div[^>]*class=\"[^\"]*(?:unlock-buttons|ads)[^\"]*\"[^>]*>.*?</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("<sub>.*?</sub>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "Action"),
        FilterOption("Adult", "Adult"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Eastern", "Eastern"),
        FilterOption("Ecchi", "Ecchi"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Game", "Game"),
        FilterOption("Gender Bender", "Gender+Bender"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Josei", "Josei"),
        FilterOption("Martial Arts", "Martial+Arts"),
        FilterOption("Mature", "Mature"),
        FilterOption("Mecha", "Mecha"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Reincarnation", "Reincarnation"),
        FilterOption("Romance", "Romance"),
        FilterOption("School Life", "School+Life"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Seinen", "Seinen"),
        FilterOption("Shoujo", "Shoujo"),
        FilterOption("Shounen Ai", "Shounen+Ai"),
        FilterOption("Shounen", "Shounen"),
        FilterOption("Slice of Life", "Slice+of+Life"),
        FilterOption("Smut", "Smut"),
        FilterOption("Sports", "Sports"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy"),
        FilterOption("Wuxia", "Wuxia"),
        FilterOption("Xianxia", "Xianxia"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Yaoi", "Yaoi")
    )

    override val orderBys = listOf(
        FilterOption("Latest Release", "sort/latest-release"),
        FilterOption("Chinese Novel", "sort/latest-release/chinese-novel"),
        FilterOption("Korean Novel", "sort/latest-release/korean-novel"),
        FilterOption("Japanese Novel", "sort/latest-release/japanese-novel"),
        FilterOption("English Novel", "sort/latest-release/english-novel"),
        FilterOption("Latest Novels", "sort/latest-novels"),
        FilterOption("Completed Novels", "sort/completed-novel"),
        FilterOption("Most Popular", "sort/most-popular")
    )

    // ================================================================
    // SELECTOR CONFIGURATIONS
    // ================================================================

    private object Selectors {
        // Novel list containers
        val novelContainers = listOf(
            "div.ul-list1.ul-list1-2.ss-custom > div.li-row",
            "div.archive div.li-row",
            "div.col-content div.li-row",
            "div.li-row"
        )

        // Novel title in list
        val novelTitle = listOf(
            "h3.tit > a",
            ".tit > a",
            "h3 > a"
        )

        // Novel detail title
        val novelDetailTitle = listOf(
            "h1.tit",
            "div.m-desc h1",
            "div.books h1"
        )

        // Chapter content
        val chapterContent = listOf(
            "div.txt",
            "#chr-content",
            "#chapter-content",
            ".chapter-content"
        )

        // Synopsis
        val synopsis = listOf(
            "div.inner",
            "div.desc-text",
            ".summary .content"
        )

        // Poster
        val poster = listOf(
            "div.pic > img",
            "div.m-imgtxt img",
            "div.books img"
        )

        // Chapter list (for noAjax fallback)
        val chapterList = listOf(
            "ul#idData > li > a",
            "#idData li a",
            ".list-chapter li a"
        )

        // Search results
        val searchResults = listOf(
            "div.li-row > div.li > div.con",
            "div.archive div.con",
            "div.li-row div.con"
        )

        val searchTitle = listOf(
            "div.txt > h3.tit > a",
            ".txt .tit > a",
            "h3.tit > a"
        )
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

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
     * Clean chapter content HTML
     */
    private fun cleanChapterHtml(html: String): String {
        var cleaned = html

        // Apply all cleaning patterns
        for (pattern in cleaningPatterns) {
            cleaned = cleaned.replace(pattern, " ")
        }

        // Use HtmlUtils for additional provider-specific cleaning
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "libread")

        // Remove site name watermarks
        cleaned = cleaned.replace("libread.com", "", ignoreCase = true)
        cleaned = cleaned.replace("libread", "", ignoreCase = true)

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

        // Get name from title attribute first, then text content
        val name = titleElement.attrOrNull("title")?.takeIf { it.isNotBlank() }
            ?: titleElement.textOrNull()?.trim()

        if (name.isNullOrBlank()) return null

        val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return null

        // Get poster image
        val imgElement = element.selectFirstOrNull("img")
        val rawSrc = imgElement?.attrOrNull("data-src")
            ?: imgElement?.attrOrNull("src")
        val posterUrl = fixUrl(rawSrc)

        // Get latest chapter if available (from third item div)
        val latestChapter = element.select("div.item")
            .getOrNull(2)
            ?.selectFirstOrNull("div > a")
            ?.textOrNull()

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            latestChapter = latestChapter,
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
        // Handle paths that don't support pagination (as per JS: noPages)
        val currentPath = orderBy ?: "sort/latest-release"
        if (page > 1 && noPagesPaths.any { currentPath.contains(it) } && tag.isNullOrEmpty()) {
            return MainPageResult(url = "", novels = emptyList())
        }

        val url = when {
            // If genre/tag filter selected
            !tag.isNullOrEmpty() && tag != "All" -> {
                // Use + for spaces as per JS implementation
                "$mainUrl/genre/$tag/$page"
            }
            // Otherwise use sort option with page as path (pageAsPath: true in JS)
            else -> {
                if (page > 1) {
                    "$mainUrl/$currentPath/$page"
                } else {
                    "$mainUrl/$currentPath"
                }
            }
        }

        val response = get(url)
        val document = response.document

        val novels = parseNovels(document)

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH (POST-based as per JS implementation)
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        // Throttle search requests (as per JS implementation)
        val now = System.currentTimeMillis()
        val lastSearch = lastSearchTime.get()
        if (lastSearch > 0 && (now - lastSearch) < searchInterval) {
            delay(searchInterval - (now - lastSearch))
        }
        lastSearchTime.set(System.currentTimeMillis())

        // POST search as per JS: postSearch: true, searchKey: "searchkey"
        val url = "$mainUrl/search"

        val response = post(
            url = url,
            data = mapOf("searchkey" to query)
        )

        val html = response.text

        // Check for anti-bot alerts
        val alertMatch = alertRegex.find(html)
        if (alertMatch != null) {
            val message = alertMatch.groupValues.getOrNull(1) ?: "Search blocked"
            throw Exception("Search blocked: $message")
        }

        val document = response.document

        // Use search-specific selectors
        val results = document.selectAny(Selectors.searchResults)

        return results.mapNotNull { element ->
            val titleElement = element.selectFirst(Selectors.searchTitle) ?: return@mapNotNull null

            val name = titleElement.attrOrNull("title")?.takeIf { it.isNotBlank() }
                ?: titleElement.textOrNull()?.trim()

            if (name.isNullOrBlank()) return@mapNotNull null

            val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return@mapNotNull null

            // Get poster image
            val imgElement = element.selectFirstOrNull("div.pic > img")
                ?: element.selectFirstOrNull("img")
            val rawSrc = imgElement?.attrOrNull("data-src")
                ?: imgElement?.attrOrNull("src")
            val posterUrl = fixUrl(rawSrc)

            Novel(
                name = name,
                url = novelUrl,
                posterUrl = posterUrl,
                apiName = this.name
            )
        }
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val trimmedUrl = url.trim().trimEnd('/')

        val response = get(url)
        val document = response.document
        val html = response.text

        // Get novel title - try multiple selectors
        val name = document.selectFirst(Selectors.novelDetailTitle)?.textOrNull()?.trim()
            ?: "Unknown"

        // Try to load chapters via API first
        var chapters = loadChaptersViaApi(html, trimmedUrl)

        // Fallback: parse chapters from HTML (noAjax style as per JS)
        if (chapters.isEmpty()) {
            chapters = loadChaptersFromHtml(document, trimmedUrl)
        }

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
        // Author - look for glyphicon-user or info list
        val author = document.selectFirstOrNull("span.glyphicon.glyphicon-user")
            ?.nextElementSibling()?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("ul.info li:contains(Author) a")?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("a[href*='/author/']")?.textOrNull()?.trim()

        // Poster
        val posterUrl = document.selectFirst(Selectors.poster)?.let { img ->
            val src = img.attrOrNull("data-src") ?: img.attrOrNull("src")
            fixUrl(src)
        }

        // Synopsis
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            element.select("br").append("\\n")
            element.select("p").prepend("\\n")
            element.text()
                .replace("\\n", "\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        }

        // Tags/Genres - look for glyphicon-th-list or info list
        val tagsText = document.selectFirstOrNull("span.glyphicon.glyphicon-th-list")
            ?.nextElementSibling()?.textOrNull()
            ?: document.selectFirstOrNull("ul.info li:contains(Genre)")?.textOrNull()

        val tags = tagsText?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() && !it.contains("Genre", ignoreCase = true) }
            ?.distinct()
            ?: emptyList()

        // Status
        val statusText = document.selectFirstOrNull("span.s1.s3 > a")?.textOrNull()
            ?: document.selectFirstOrNull("span.s1.s2 > a")?.textOrNull()
            ?: document.selectFirstOrNull("ul.info li:contains(Status) a")?.textOrNull()
        val status = parseStatus(statusText)

        // Rating and votes
        val votesP = document.selectFirstOrNull("div.m-desc > div.score > p:nth-child(2)")
            ?: document.selectFirstOrNull("div.score p")
        val votesText = votesP?.textOrNull()

        var rating: Int? = null
        var peopleVoted: Int? = null

        if (votesText != null) {
            val ratingMatch = Regex("([0-9.]+)").find(votesText)
            val rawRating = ratingMatch?.groupValues?.getOrNull(1)?.toFloatOrNull()

            // Detect the scale from the text format
            rating = rawRating?.let { value ->
                when {
                    votesText.contains("/10") -> RatingUtils.from10Points(value)
                    votesText.contains("/5") -> RatingUtils.from5Stars(value)
                    value > 5f -> RatingUtils.from10Points(value)  // Assume 10-point if > 5
                    else -> RatingUtils.from5Stars(value)          // Otherwise assume 5-star
                }
            }

            val voteMatch = Regex("\\(([0-9,]+)").find(votesText)
            peopleVoted = voteMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
        }

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
     * Load chapters via API endpoint
     */
    private suspend fun loadChaptersViaApi(html: String, baseUrl: String): List<Chapter> {
        // Extract aid from response: pattern [0-9]+s.jpg
        val aidMatch = aidRegex.find(html)
        val aid = aidMatch?.groupValues?.getOrNull(1) ?: return emptyList()

        val chapters = mutableListOf<Chapter>()

        try {
            val chapterResponse = post(
                url = "$mainUrl/api/chapterlist.php",
                data = mapOf("aid" to aid)
            )

            // Clean up response text (remove backslashes)
            val cleanText = chapterResponse.text.replace("\\", "")
            val chapterDoc = Jsoup.parse(cleanText)

            // Determine URL prefix
            val prefix = baseUrl.removeSuffix(".html")

            // Parse chapter options
            chapterDoc.select("option").forEach { option ->
                val value = option.attrOrNull("value") ?: return@forEach

                // Skip empty or placeholder values
                if (value.isBlank() || value == "#" || value == "0") return@forEach

                val lastPart = value.split("/").lastOrNull() ?: return@forEach

                val chapterUrl = "$prefix/$lastPart"
                val chapterName = option.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Chapter ${chapters.size + 1}"

                chapters.add(Chapter(name = chapterName, url = chapterUrl))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return chapters
    }

    /**
     * Fallback: Load chapters from HTML (noAjax style as per JS implementation)
     */
    private fun loadChaptersFromHtml(document: Document, baseUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        var chapterIndex = 0

        // Try multiple selectors for chapter list
        for (selector in Selectors.chapterList) {
            val chapterElements = document.select(selector)

            if (chapterElements.isNotEmpty()) {
                chapterElements.forEach { element ->
                    val href = element.attrOrNull("href") ?: return@forEach

                    // Skip empty or placeholder values
                    if (href.isBlank() || href == "#") return@forEach

                    chapterIndex++

                    val chapterUrl = if (href.startsWith("http")) {
                        href
                    } else if (href.startsWith("/")) {
                        "$mainUrl$href"
                    } else {
                        // Relative to novel URL
                        val prefix = baseUrl.removeSuffix(".html")
                        "$prefix/$href"
                    }

                    val chapterName = element.attrOrNull("title")?.takeIf { it.isNotBlank() }
                        ?: element.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Chapter $chapterIndex"

                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }

                if (chapters.isNotEmpty()) break
            }
        }

        return chapters
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val response = get(url)

        // Clean site name from text before parsing
        val cleanedText = response.text
            .replace("libread.com", "", ignoreCase = true)
            .replace("libread", "", ignoreCase = true)

        // Re-parse the cleaned text
        val document = Jsoup.parse(cleanedText)

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