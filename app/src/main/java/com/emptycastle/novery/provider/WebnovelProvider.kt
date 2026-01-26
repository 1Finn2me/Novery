package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReviewScore
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URLEncoder

/**
 * Provider for Webnovel.com
 * One of the largest English web novel platforms with both original and translated content.
 *
 * Features:
 * - Male/Female genre categories
 * - Original, Translated, and MTL content
 * - Novel and Fanfic sections
 * - Locked chapters indicated with 🔒
 * - User reviews via API
 * - Related novels ("You May Also Like")
 */
class WebnovelProvider : MainProvider() {

    override val name = "Webnovel"
    override val mainUrl = "https://www.webnovel.com"
    override val hasMainPage = true
    override val hasReviews = true
    override val iconRes: Int = R.drawable.ic_provider_webnovel
    override val ratingScale: RatingScale = RatingScale.FIVE_STAR

    // Store CSRF token for API calls
    private var csrfToken: String? = null

    // Store bookId mapping for reviews
    private val bookIdCache = mutableMapOf<String, String>()

    // ================================================================
    // CUSTOM HEADERS
    // ================================================================

    private val customHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to mainUrl
    )

    private fun getAjaxHeaders(): Map<String, String> {
        return customHeaders + mapOf(
            "Accept" to "application/json, text/javascript, */*; q=0.01",
            "X-Requested-With" to "XMLHttpRequest"
        )
    }

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        // Male Genres
        FilterOption("All (Male)", "novel?gender=1"),
        FilterOption("Action", "novel-action-male"),
        FilterOption("ACG", "novel-acg-male"),
        FilterOption("Eastern", "novel-eastern-male"),
        FilterOption("Fantasy (Male)", "novel-fantasy-male"),
        FilterOption("Games", "novel-games-male"),
        FilterOption("History (Male)", "novel-history-male"),
        FilterOption("Horror", "novel-horror-male"),
        FilterOption("Realistic", "novel-realistic-male"),
        FilterOption("Sci-fi (Male)", "novel-scifi-male"),
        FilterOption("Sports", "novel-sports-male"),
        FilterOption("Urban (Male)", "novel-urban-male"),
        FilterOption("War", "novel-war-male"),
        // Female Genres
        FilterOption("All (Female)", "novel?gender=2"),
        FilterOption("Fantasy (Female)", "novel-fantasy-female"),
        FilterOption("General", "novel-general-female"),
        FilterOption("History (Female)", "novel-history-female"),
        FilterOption("LGBT+", "novel-lgbt-female"),
        FilterOption("Sci-fi (Female)", "novel-scifi-female"),
        FilterOption("Teen", "novel-teen-female"),
        FilterOption("Urban (Female)", "novel-urban-female"),
    )

    override val orderBys = listOf(
        FilterOption("Popular", "1"),
        FilterOption("Recommended", "2"),
        FilterOption("Most Collections", "3"),
        FilterOption("Rating", "4"),
        FilterOption("Time Updated", "5"),
    )

    // ================================================================
    // SELECTOR CONFIGURATIONS
    // ================================================================

    private object Selectors {
        // Novel list (category page)
        val categoryContainer = ".j_category_wrapper li"
        val categoryThumb = ".g_thumb"
        val categoryCover = ".g_thumb > img"

        // Novel list (search page)
        val searchContainer = ".j_list_container li"
        val searchThumb = ".g_thumb"
        val searchCover = ".g_thumb > img"

        // Novel detail page
        val detailCover = ".g_thumb > img"
        val detailTitle = listOf(
            ".g_thumb > img",  // alt attribute
            ".det-hd-detail h2",
            "div.g_col h2"
        )
        val detailGenres = ".det-hd-detail > .det-hd-tag"
        val detailSynopsis = ".j_synopsis > p"
        val detailAuthorLabel = ".det-info .c_s"
        val detailStatus = ".det-hd-detail svg[title=Status]"

        // Related novels
        val relatedContainer = "ul.j_books_you_also_like li"
        val relatedThumb = ".g_thumb"
        val relatedCover = ".g_thumb img"
        val relatedTitle = "a.m-book-title h3"
        val relatedRating = ".g_star_num small"

        // Chapter catalog
        val volumeContainer = ".volume-item"
        val chapterItem = "li"
        val chapterLink = "a"
        val chapterLocked = "svg"

        // Chapter content
        val chapterTitle = ".cha-tit"
        val chapterContent = listOf(
            ".cha-words",
            ".cha-content",
            "div.cha-content p"
        )
        val chapterParagraph = ".cha-paragraph p"
        val chapterComments = ".para-comment"
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
     * Fix cover URL (add https: prefix if needed)
     */
    private fun fixCoverUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http") -> url
            url.startsWith("/") -> "$mainUrl$url"
            else -> url
        }
    }

    /**
     * Map status string to standardized status
     */
    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null

        return when {
            statusText.contains("Ongoing", ignoreCase = true) -> "Ongoing"
            statusText.contains("Completed", ignoreCase = true) -> "Completed"
            statusText.contains("Hiatus", ignoreCase = true) -> "On Hiatus"
            else -> statusText.trim()
        }
    }

    /**
     * Extract book ID from URL
     * URL format: /book/shadow-slave_22196546206090805
     * Book ID is the number after the underscore
     */
    private fun extractBookId(url: String): String? {
        // Try to get from cache first
        bookIdCache[url]?.let { return it }

        // Pattern: anything_<bookId>
        val regex = Regex("_?(\\d{10,})") // Book IDs are typically 17+ digits
        val match = regex.find(url)
        val bookId = match?.groupValues?.getOrNull(1)

        // Cache it
        if (bookId != null) {
            bookIdCache[url] = bookId
        }

        return bookId
    }

    /**
     * Extract or generate CSRF token
     */
    private fun extractCsrfToken(document: Document): String? {
        // Try to find in meta tags
        document.selectFirstOrNull("meta[name=csrf-token]")?.attrOrNull("content")?.let {
            csrfToken = it
            return it
        }

        // Try to find in scripts
        val scripts = document.select("script").map { it.html() }
        for (script in scripts) {
            // Look for csrfToken assignment
            val tokenMatch = Regex("_csrfToken[\"']?\\s*[:=]\\s*[\"']([a-f0-9-]+)[\"']").find(script)
            tokenMatch?.groupValues?.getOrNull(1)?.let {
                csrfToken = it
                return it
            }
        }

        // Generate a UUID-like token if we can't find one
        // Webnovel sometimes accepts any valid UUID format
        if (csrfToken == null) {
            csrfToken = java.util.UUID.randomUUID().toString()
        }

        return csrfToken
    }

    /**
     * Clean chapter content HTML
     */
    private fun cleanChapterHtml(html: String): String {
        var cleaned = html

        cleaned = cleaned.replace(Regex("<pirate>.*?</pirate>", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(
            Regex("Find authorized novels in Webnovel.*?for visiting\\.",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            ""
        )

        cleaned = HtmlUtils.cleanChapterContent(cleaned, "webnovel")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")

        return cleaned.trim()
    }

    /**
     * Convert Unix timestamp (milliseconds) to readable date
     */
    private fun formatTimestamp(timestamp: Long?): String? {
        if (timestamp == null || timestamp <= 0) return null

        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            null
        }
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseCategoryNovels(document: Document): List<Novel> {
        return document.select(Selectors.categoryContainer).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(Selectors.categoryThumb) ?: return@mapNotNull null

            val name = thumb.attrOrNull("title")
            if (name.isNullOrBlank()) return@mapNotNull null

            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            val imgElement = element.selectFirstOrNull("${Selectors.categoryCover}")
            val rawCover = imgElement?.attrOrNull("data-original")
                ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)

            Novel(
                name = name,
                url = novelUrl,
                posterUrl = posterUrl,
                apiName = this.name
            )
        }
    }

    private fun parseSearchNovels(document: Document): List<Novel> {
        return document.select(Selectors.searchContainer).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(Selectors.searchThumb) ?: return@mapNotNull null

            val name = thumb.attrOrNull("title")
            if (name.isNullOrBlank()) return@mapNotNull null

            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            val imgElement = element.selectFirstOrNull("${Selectors.searchCover}")
            val rawCover = imgElement?.attrOrNull("src")
                ?: imgElement?.attrOrNull("data-original")
            val posterUrl = fixCoverUrl(rawCover)

            Novel(
                name = name,
                url = novelUrl,
                posterUrl = posterUrl,
                apiName = this.name
            )
        }
    }

    // ================================================================
    // RELATED NOVELS
    // ================================================================

    /**
     * Parse related novels from "You May Also Like" section
     * These are in the HTML under ul.j_books_you_also_like
     */
    private fun parseRelatedNovels(document: Document): List<Novel> {
        val relatedItems = document.select(Selectors.relatedContainer)

        return relatedItems.mapNotNull { item ->
            // Get the main link element
            val linkElement = item.selectFirstOrNull("a.m-book-title")
                ?: item.selectFirstOrNull(Selectors.relatedThumb)
                ?: return@mapNotNull null

            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            // Get title from link title attribute or h3 element
            val title = linkElement.attrOrNull("title")
                ?: item.selectFirstOrNull(Selectors.relatedTitle)?.textOrNull()?.trim()
                ?: item.selectFirstOrNull("h3")?.textOrNull()?.trim()
                ?: return@mapNotNull null

            // Get cover image
            val imgElement = item.selectFirstOrNull(Selectors.relatedCover)
            val rawCover = imgElement?.attrOrNull("data-original")
                ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)

            // Get rating if available
            val ratingText = item.selectFirstOrNull(Selectors.relatedRating)?.textOrNull()
            val rating = ratingText?.toFloatOrNull()?.let {
                RatingUtils.from5Stars(it)
            }

            Novel(
                name = title,
                url = novelUrl,
                posterUrl = posterUrl,
                rating = rating,
                apiName = this.name
            )
        }
    }

    // ================================================================
    // REVIEWS
    // ================================================================

    /**
     * Load user reviews using Webnovel's API
     *
     * API: GET /go/pcm/bookReview/get-reviews
     * Parameters:
     * - _csrfToken: CSRF token from cookies
     * - bookId: Novel's book ID (number from URL)
     * - pageIndex: Page number (1-based)
     * - pageSize: Number of reviews per page (default 20)
     * - orderBy: Sort order (1=popular, 2=newest)
     * - novelType: 0 for novels
     * - needSummary: 1 to include summary
     * - _: Timestamp for cache busting
     */
    override suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean
    ): List<UserReview> {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        // Extract book ID from URL
        val bookId = extractBookId(fullUrl)
        if (bookId.isNullOrBlank()) {
            return emptyList()
        }

        // Ensure we have a CSRF token
        if (csrfToken.isNullOrBlank()) {
            // Fetch the novel page to get/generate CSRF token
            try {
                val response = get(fullUrl, customHeaders)
                extractCsrfToken(response.document)
            } catch (e: Exception) {
                // Generate a fallback token
                csrfToken = java.util.UUID.randomUUID().toString()
            }
        }

        val token = csrfToken ?: java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val reviewUrl = buildString {
            append("$mainUrl/go/pcm/bookReview/get-reviews?")
            append("_csrfToken=$token")
            append("&bookId=$bookId")
            append("&pageIndex=$page")
            append("&pageSize=20")
            append("&orderBy=1") // 1 = popular, 2 = newest
            append("&novelType=0")
            append("&needSummary=1")
            append("&_=$timestamp")
        }

        return try {
            val response = get(reviewUrl, getAjaxHeaders())
            val json = JSONObject(response.text)

            parseReviewsFromJson(json, showSpoilers)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parse reviews from API JSON response
     *
     * Response structure:
     * {
     *   "code": 0,
     *   "data": {
     *     "reviews": [
     *       {
     *         "reviewId": "...",
     *         "userName": "...",
     *         "userAvatar": "...",
     *         "content": "...",
     *         "totalScore": 5.0,
     *         "createTime": 1234567890000,
     *         "likeCount": 123,
     *         "chapterRead": 100,
     *         "detailScore": {
     *           "writingScore": 5,
     *           "storyScore": 5,
     *           "characterScore": 5,
     *           "worldScore": 5,
     *           "updateScore": 5
     *         },
     *         ...
     *       }
     *     ],
     *     "pageInfo": { ... }
     *   }
     * }
     */
    private fun parseReviewsFromJson(json: JSONObject, showSpoilers: Boolean): List<UserReview> {
        val reviews = mutableListOf<UserReview>()

        val code = json.optInt("code", -1)
        if (code != 0) {
            return emptyList()
        }

        val data = json.optJSONObject("data") ?: return emptyList()
        val reviewsArray = data.optJSONArray("reviews") ?: return emptyList()

        for (i in 0 until reviewsArray.length()) {
            val reviewObj = reviewsArray.optJSONObject(i) ?: continue

            // Basic info
            val username = reviewObj.optString("userName", null)?.takeIf { it.isNotBlank() }
            val avatarUrl = reviewObj.optString("userAvatar", null)?.let { fixCoverUrl(it) }
            val content = reviewObj.optString("content", null)?.takeIf { it.isNotBlank() }
                ?: continue

            // Title (Webnovel reviews don't have titles, use chapter read info)
            val chapterRead = reviewObj.optInt("chapterRead", 0)
            val title = if (chapterRead > 0) "Read $chapterRead chapters" else null

            // Time
            val createTime = reviewObj.optLong("createTime", 0)
            val time = formatTimestamp(createTime)

            // Overall score (0-5 scale, convert to 0-1000)
            val totalScore = reviewObj.optDouble("totalScore", 0.0)
            val overallScore = if (totalScore > 0) {
                (totalScore * 200).toInt() // 5 * 200 = 1000
            } else null

            // Advanced scores (detailed breakdown)
            val advancedScores = mutableListOf<ReviewScore>()
            val detailScore = reviewObj.optJSONObject("detailScore")
            if (detailScore != null) {
                // Writing Quality
                val writingScore = detailScore.optInt("writingScore", 0)
                if (writingScore > 0) {
                    advancedScores.add(ReviewScore("Writing Quality", writingScore * 200))
                }

                // Story Development
                val storyScore = detailScore.optInt("storyScore", 0)
                if (storyScore > 0) {
                    advancedScores.add(ReviewScore("Story Development", storyScore * 200))
                }

                // Character Design
                val characterScore = detailScore.optInt("characterScore", 0)
                if (characterScore > 0) {
                    advancedScores.add(ReviewScore("Character Design", characterScore * 200))
                }

                // World Background
                val worldScore = detailScore.optInt("worldScore", 0)
                if (worldScore > 0) {
                    advancedScores.add(ReviewScore("World Background", worldScore * 200))
                }

                // Update Stability
                val updateScore = detailScore.optInt("updateScore", 0)
                if (updateScore > 0) {
                    advancedScores.add(ReviewScore("Update Stability", updateScore * 200))
                }
            }

            // Like count (could be shown in UI if supported)
            val likeCount = reviewObj.optInt("likeCount", 0)

            // Add like count to username if significant
            val displayUsername = if (likeCount > 0 && username != null) {
                "$username (👍 $likeCount)"
            } else {
                username
            }

            reviews.add(
                UserReview(
                    content = content,
                    title = title,
                    username = displayUsername,
                    time = time,
                    avatarUrl = avatarUrl,
                    overallScore = overallScore,
                    advancedScores = advancedScores.toList()  // Fixed: convert MutableList to List
                )
            )
        }

        return reviews
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?
    ): MainPageResult {
        val params = mutableListOf<String>()

        val basePath = if (!tag.isNullOrEmpty() && !tag.contains("?")) {
            "/stories/$tag"
        } else if (!tag.isNullOrEmpty()) {
            "/stories/$tag"
        } else {
            "/stories/novel"
        }

        val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "1"
        params.add("orderBy=$sort")
        params.add("bookStatus=0")
        params.add("pageIndex=$page")

        val queryString = params.joinToString("&")
        val url = if (basePath.contains("?")) {
            "$mainUrl$basePath&$queryString"
        } else {
            "$mainUrl$basePath?$queryString"
        }

        val response = get(url, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please use the WebView browser to solve the challenge.")
        }

        // Try to extract CSRF token for later use
        extractCsrfToken(document)

        val novels = parseCategoryNovels(document)

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            .replace("+", "%20")
        val url = "$mainUrl/search?keywords=$encodedQuery&pageIndex=1"

        val response = get(url, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please use the WebView browser to solve the challenge.")
        }

        // Try to extract CSRF token for later use
        extractCsrfToken(document)

        return parseSearchNovels(document)
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please use the WebView browser to solve the challenge.")
        }

        // Extract CSRF token for reviews API
        extractCsrfToken(document)

        // Extract and cache book ID
        val bookId = extractBookId(fullUrl)
        if (bookId != null) {
            bookIdCache[fullUrl] = bookId
        }

        // Get novel title
        val name = document.selectFirstOrNull("${Selectors.detailCover}")?.attrOrNull("alt")
            ?: document.selectFirst(Selectors.detailTitle)?.textOrNull()?.trim()
            ?: "Unknown"

        // Load chapters from catalog page
        val catalogUrl = fullUrl.trimEnd('/') + "/catalog"
        val chapters = loadChaptersFromCatalog(catalogUrl)

        // Extract all metadata
        val metadata = extractMetadata(document)

        // Parse related novels from page
        val relatedNovels = parseRelatedNovels(document)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating,
            peopleVoted = metadata.peopleVoted,
            status = metadata.status,
            relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val rating: Int? = null,
        val peopleVoted: Int? = null,
        val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val coverElement = document.selectFirstOrNull(Selectors.detailCover)
        val rawCover = coverElement?.attrOrNull("src")
            ?: coverElement?.attrOrNull("data-original")
        val posterUrl = fixCoverUrl(rawCover)

        val synopsis = document.selectFirstOrNull(Selectors.detailSynopsis)?.let { element ->
            element.select("br").append("\\n")
            element.text()
                .replace("\\n", "\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        } ?: "No Summary Found"

        val genresText = document.selectFirstOrNull(Selectors.detailGenres)?.attrOrNull("title")
        val tags = genresText?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

        var author: String? = null
        document.select(Selectors.detailAuthorLabel).forEach { element ->
            if (element.text().trim() == "Author:") {
                author = element.nextElementSibling()?.textOrNull()?.trim()
                return@forEach
            }
        }

        if (author.isNullOrBlank()) {
            author = document.selectFirstOrNull("p.ell a.c_primary")?.textOrNull()?.trim()
        }

        var statusText: String? = null
        document.select("${Selectors.detailStatus}").forEach { element ->
            if (element.attrOrNull("title") == "Status") {
                statusText = element.nextElementSibling()?.textOrNull()?.trim()
                return@forEach
            }
        }

        if (statusText.isNullOrBlank()) {
            statusText = document.selectFirstOrNull(".det-hd-detail svg")
                ?.takeIf { it.attrOrNull("title") == "Status" }
                ?.nextElementSibling()
                ?.textOrNull()
                ?.trim()
        }

        val status = parseStatus(statusText)

        val ratingText = document.selectFirstOrNull(".g_star_num small")?.textOrNull()
            ?: document.selectFirstOrNull("[class*='score']")?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let {
            RatingUtils.from5Stars(it)
        }

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = null,
            status = status
        )
    }

    private suspend fun loadChaptersFromCatalog(catalogUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            val response = get(catalogUrl, customHeaders)
            val document = response.document

            document.select(Selectors.volumeContainer).forEach { volumeElement ->
                val volumeText = volumeElement.selectFirstOrNull("h4")?.textOrNull()?.trim()
                    ?: volumeElement.ownText().trim()

                val volumeMatch = Regex("Volume\\s*(\\d+)", RegexOption.IGNORE_CASE).find(volumeText)
                val volumeName = volumeMatch?.let { "Vol.${it.groupValues[1]}" } ?: ""

                volumeElement.select(Selectors.chapterItem).forEach { chapterElement ->
                    val link = chapterElement.selectFirstOrNull(Selectors.chapterLink)
                        ?: return@forEach

                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.textOrNull()?.trim()
                        ?: return@forEach

                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach

                    val isLocked = chapterElement.select(Selectors.chapterLocked).isNotEmpty()

                    val chapterName = buildString {
                        if (volumeName.isNotBlank()) {
                            append("$volumeName: ")
                        }
                        append(chapterTitle)
                        if (isLocked) {
                            append(" 🔒")
                        }
                    }

                    chapters.add(
                        Chapter(
                            name = chapterName,
                            url = chapterUrl
                        )
                    )
                }
            }

            if (chapters.isEmpty()) {
                document.select(".j_catalog_list ${Selectors.chapterLink}").forEach { link ->
                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.textOrNull()?.trim()
                        ?: return@forEach

                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach

                    val isLocked = link.parent()?.select(Selectors.chapterLocked)?.isNotEmpty() == true

                    val chapterName = if (isLocked) "$chapterTitle 🔒" else chapterTitle

                    chapters.add(
                        Chapter(
                            name = chapterName,
                            url = chapterUrl
                        )
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load chapter list: ${e.message}")
        }

        return chapters
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected. Please use the WebView browser to solve the challenge.")
        }

        document.select(Selectors.chapterComments).remove()

        val titleHtml = document.selectFirstOrNull(Selectors.chapterTitle)?.html() ?: ""

        var contentHtml = ""

        val contentElement = document.selectAny(Selectors.chapterContent).firstOrNull()

        if (contentElement != null) {
            contentHtml = contentElement.html()
        } else {
            val paragraphs = document.select(Selectors.chapterParagraph)
            if (paragraphs.isNotEmpty()) {
                contentHtml = paragraphs.map { it.outerHtml() }.joinToString("\n")
            }
        }

        if (contentHtml.isBlank()) {
            return null
        }

        val fullHtml = if (titleHtml.isNotBlank()) {
            "$titleHtml\n$contentHtml"
        } else {
            contentHtml
        }

        return cleanChapterHtml(fullHtml)
    }
}