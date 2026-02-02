// com/emptycastle/novery/provider/WebnovelProvider.kt
package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.RepliesResult
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class WebnovelProvider : MainProvider() {

    override val name = "Webnovel"
    override val mainUrl = "https://www.webnovel.com"
    override val hasMainPage = true
    override val hasReviews = true
    override val iconRes: Int = R.drawable.ic_provider_webnovel
    override val ratingScale: RatingScale = RatingScale.FIVE_STAR

    // Thread-safe state
    private val csrfToken = AtomicReference<String?>(null)
    private val bookIdCache = ConcurrentHashMap<String, String>()

    companion object {
        private const val SCORE_MULTIPLIER = 200

        private object Endpoints {
            const val REVIEW_DETAIL = "/go/pcm/bookReview/detail"
        }

        private object Selectors {
            const val REVIEW_CONTAINER = ".j_pageReviewList"
            const val AVATAR = "a.g_avatar img"
            const val USERNAME = ".m-comment-hd-mn a.c_l"
            const val USER_LEVEL = ".g_lv"
            const val USER_BADGE = ".m-comment-hd-mn img[alt=Badge]"
            const val STAR_CONTAINER = ".g_star"
            const val CONTENT = ".j_book_review_content"
            const val SPOILER_CONTAINER = ".m-comment-spoiler"
            const val TIME = ".m-comment-ft strong.fl"
            const val LIKE_COUNT = ".j_like_num"
            const val REPLY_BUTTON = ".m-comment-reply-btn"
            const val REVIEW_IMAGE = ".m-comment-img img"
        }
    }

    private val customHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to mainUrl
    )

    private fun getAjaxHeaders(): Map<String, String> = customHeaders + mapOf(
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "X-Requested-With" to "XMLHttpRequest"
    )

    // ================================================================
    // FILTER OPTIONS (keep existing)
    // ================================================================

    override val tags = listOf(
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
    // SELECTOR CONFIGURATIONS (keep existing)
    // ================================================================

    private object DetailSelectors {
        val categoryContainer = ".j_category_wrapper li"
        val categoryThumb = ".g_thumb"
        val categoryCover = ".g_thumb > img"
        val searchContainer = ".j_list_container li"
        val searchThumb = ".g_thumb"
        val searchCover = ".g_thumb > img"
        val detailCover = ".g_thumb > img"
        val detailTitle = listOf(".g_thumb > img", ".det-hd-detail h2", "div.g_col h2")
        val detailGenres = ".det-hd-detail > .det-hd-tag"
        val detailSynopsis = ".j_synopsis > p"
        val detailAuthorLabel = ".det-info .c_s"
        val detailStatus = ".det-hd-detail svg[title=Status]"
        val relatedContainer = "ul.j_books_you_also_like li"
        val relatedThumb = ".g_thumb"
        val relatedCover = ".g_thumb img"
        val relatedTitle = "a.m-book-title h3"
        val relatedRating = ".g_star_num small"
        val volumeContainer = ".volume-item"
        val chapterItem = "li"
        val chapterLink = "a"
        val chapterLocked = "svg"
        val chapterTitle = ".cha-tit"
        val chapterContent = listOf(".cha-words", ".cha-content", "div.cha-content p")
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

    private fun fixCoverUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http") -> url
            url.startsWith("/") -> "$mainUrl$url"
            else -> url
        }
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when {
            statusText.contains("Ongoing", ignoreCase = true) -> "Ongoing"
            statusText.contains("Completed", ignoreCase = true) -> "Completed"
            statusText.contains("Hiatus", ignoreCase = true) -> "On Hiatus"
            else -> statusText.trim()
        }
    }

    private fun extractBookId(url: String): String? {
        bookIdCache[url]?.let { return it }
        val regex = Regex("_?(\\d{10,})")
        val match = regex.find(url)
        val bookId = match?.groupValues?.getOrNull(1)
        if (bookId != null) {
            bookIdCache[url] = bookId
        }
        return bookId
    }

    private fun extractCsrfToken(document: Document): String? {
        document.selectFirstOrNull("meta[name=csrf-token]")?.attrOrNull("content")?.let {
            csrfToken.set(it)
            return it
        }

        val scripts = document.select("script").map { it.html() }
        for (script in scripts) {
            val tokenMatch = Regex("_csrfToken[\"']?\\s*[:=]\\s*[\"']([a-f0-9-]+)[\"']").find(script)
            tokenMatch?.groupValues?.getOrNull(1)?.let {
                csrfToken.set(it)
                return it
            }
        }

        if (csrfToken.get() == null) {
            csrfToken.set(java.util.UUID.randomUUID().toString())
        }
        return csrfToken.get()
    }

    private fun generateCsrfToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        csrfToken.set(token)
        return token
    }

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
    // ENHANCED REVIEWS
    // ================================================================

    override suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean
    ): List<UserReview> {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        return try {
            val response = get(fullUrl, customHeaders)
            val document = response.document

            extractCsrfToken(document)
            val bookId = extractBookId(fullUrl)

            parseReviewsFromHtml(document, bookId, showSpoilers)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseReviewsFromHtml(
        document: Document,
        bookId: String?,
        showSpoilers: Boolean
    ): List<UserReview> {
        val reviews = mutableListOf<UserReview>()

        document.select(Selectors.REVIEW_CONTAINER).forEach { reviewEl ->
            val review = parseReviewElement(reviewEl, bookId, showSpoilers)
            if (review != null) {
                reviews.add(review)
            }
        }

        return reviews
    }

    private fun parseReviewElement(
        reviewEl: Element,
        bookId: String?,
        showSpoilers: Boolean
    ): UserReview? {
        // Extract review ID
        val reviewId = extractReviewId(reviewEl) ?: return null

        // Check for spoiler
        val spoilerContainer = reviewEl.selectFirst(Selectors.SPOILER_CONTAINER)
        val isSpoiler = spoilerContainer != null

        // Get content element
        val contentElement = if (isSpoiler) {
            spoilerContainer?.selectFirst(Selectors.CONTENT)
        } else {
            reviewEl.selectFirst(Selectors.CONTENT)
        }

        val content = contentElement?.text()?.trim() ?: return null
        if (content.isBlank()) return null

        // Username
        val usernameEl = reviewEl.selectFirst(Selectors.USERNAME)
        val username = usernameEl?.attrOrNull("title") ?: usernameEl?.text()?.trim()
        val userId = usernameEl?.attrOrNull("href")?.extractUserId()

        // Avatar
        val avatarEl = reviewEl.selectFirst(Selectors.AVATAR)
        val avatarUrl = fixCoverUrl(
            avatarEl?.attrOrNull("data-original") ?: avatarEl?.attrOrNull("src")
        )?.takeIf { !it.contains("data:image/gif") }

        // User level
        val levelEl = reviewEl.selectFirst(Selectors.USER_LEVEL)
        val userLevel = levelEl?.attrOrNull("title")
            ?.replace("Level", "", ignoreCase = true)
            ?.trim()
            ?.toIntOrNull()
            ?: levelEl?.text()
                ?.replace("LV", "", ignoreCase = true)
                ?.trim()
                ?.toIntOrNull()

        // User badge
        val badgeEl = reviewEl.selectFirst(Selectors.USER_BADGE)
        val userBadgeUrl = fixCoverUrl(
            badgeEl?.attrOrNull("data-original") ?: badgeEl?.attrOrNull("src")
        )?.takeIf { !it.contains("data:image/gif") }

        // Rating
        val starContainer = reviewEl.selectFirst(Selectors.STAR_CONTAINER)
        val fullStars = starContainer?.select("svg._on")?.size ?: 0
        val halfStars = starContainer?.select("svg._half")?.size ?: 0
        val ratingValue = fullStars + (halfStars * 0.5f)
        val overallScore = if (ratingValue > 0) (ratingValue * SCORE_MULTIPLIER).toInt() else null

        // Time
        val time = reviewEl.selectFirst(Selectors.TIME)?.text()?.trim()

        // Like count
        val parsedLikeCount = reviewEl.selectFirst(Selectors.LIKE_COUNT)?.text()?.toIntOrNull() ?: 0

        // Reply count
        val replyButton = reviewEl.selectFirst(Selectors.REPLY_BUTTON)
        val parsedReplyCount = replyButton?.attrOrNull("data-rc")?.toIntOrNull() ?: 0
        val hasReplies = parsedReplyCount > 0

        // Images
        val images = reviewEl.select(Selectors.REVIEW_IMAGE).mapNotNull { img ->
            fixCoverUrl(img.attrOrNull("data-original") ?: img.attrOrNull("src"))
                ?.takeIf { !it.contains("data:image/gif") }
        }

        // Pinned status
        val isPinned = reviewEl.attrOrNull("data-pinned") == "1"

        return UserReview(
            id = reviewId,
            content = content,
            username = username,
            userId = userId,
            avatarUrl = avatarUrl,
            userLevel = userLevel,
            userBadgeUrl = userBadgeUrl,
            overallScore = overallScore,
            time = time,
            likeCount = parsedLikeCount,
            replyCount = parsedReplyCount,
            hasMoreReplies = hasReplies,
            isSpoiler = isSpoiler,
            isPinned = isPinned,
            images = images,
            providerData = buildMap {
                bookId?.let { put("bookId", it) }
                put("reviewId", reviewId)
            }
        )
    }

    /**
     * Load replies for a specific review using the API
     */
    suspend fun loadReviewReplies(
        review: UserReview,
        page: Int = 1
    ): RepliesResult {
        val bookId = review.providerData["bookId"] ?: return RepliesResult(emptyList(), false)
        val reviewId = review.id
        val token = csrfToken.get() ?: generateCsrfToken()

        val url = buildString {
            append("$mainUrl${Endpoints.REVIEW_DETAIL}?")
            append("_csrfToken=$token")
            append("&reviewId=$reviewId")
            append("&pageIndex=$page")
            append("&bookId=$bookId")
            append("&_=${System.currentTimeMillis()}")
        }

        return try {
            val response = get(url, getAjaxHeaders())
            val json = JSONObject(response.text)
            parseRepliesFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            RepliesResult(emptyList(), false)
        }
    }

    private fun parseRepliesFromJson(json: JSONObject): RepliesResult {
        val code = json.optInt("code", -1)
        if (code != 0) return RepliesResult(emptyList(), false)

        val data = json.optJSONObject("data") ?: return RepliesResult(emptyList(), false)
        val replyItems = data.optJSONArray("replyItems") ?: return RepliesResult(emptyList(), false)
        val isLast = data.optBoolean("isLast", true)

        val replies = mutableListOf<UserReview>()

        for (i in 0 until replyItems.length()) {
            val replyObj = replyItems.optJSONObject(i) ?: continue
            val reply = parseReplyFromJson(replyObj)
            if (reply != null) {
                replies.add(reply)
            }
        }

        return RepliesResult(
            replies = replies,
            hasMore = !isLast
        )
    }

    private fun parseReplyFromJson(obj: JSONObject): UserReview? {
        val reviewId = obj.optString("reviewId", null)?.takeIf { it.isNotBlank() } ?: return null
        val content = obj.optString("content", null)?.takeIf { it.isNotBlank() } ?: return null

        val username = obj.optString("userName", null)?.takeIf { it.isNotBlank() }
        val userId = obj.optLong("userId", 0).takeIf { it > 0 }?.toString()
        val userLevel = obj.optInt("userLevel", 0).takeIf { it > 0 }

        // Avatar URL construction
        val headImageId = obj.optLong("headImageId", 0)
        val avatarUrl = if (headImageId > 0 && userId != null) {
            "https://user-pic.webnovel.com/userheadimg/$userId-10/100.jpg?uut=$headImageId"
        } else null

        // Badge URL
        val badgeUrl = obj.optString("holdBadgeCoverURL", null)?.takeIf { it.isNotBlank() }
        val badgeCoverId = obj.optLong("holdBadgeCoverId", 0)
        val userBadgeUrl = if (badgeUrl != null && badgeCoverId > 0) {
            "${badgeUrl}40.png?mt=$badgeCoverId"
        } else null

        // Time
        val time = obj.optString("createTimeFormat", null)

        // Engagement
        val parsedLikeCount = obj.optInt("likeAmount", 0)
        val isLikedByAuthor = obj.optInt("isLikedByAuthor", 0) == 1
        val isModerator = obj.optInt("isViceModerator", 0) == 1

        // Parent reply info
        val parentReviewId = obj.optLong("pReviewId", 0).takeIf { it > 0 }?.toString()
        val parentUsername = obj.optString("pUserName", null)?.takeIf { it.isNotBlank() }
        val parentContent = obj.optString("pContent", null)?.takeIf { it.isNotBlank() }

        // Images
        val imageItems = obj.optJSONArray("imageItems")
        val images = if (imageItems != null) {
            (0 until imageItems.length()).mapNotNull { i ->
                imageItems.optString(i)?.takeIf { it.isNotBlank() }
            }
        } else emptyList()

        return UserReview(
            id = reviewId,
            content = content,
            username = username,
            userId = userId,
            avatarUrl = avatarUrl,
            userLevel = userLevel,
            userBadgeUrl = userBadgeUrl,
            time = time,
            likeCount = parsedLikeCount,
            isLikedByAuthor = isLikedByAuthor,
            isModerator = isModerator,
            parentReviewId = parentReviewId,
            parentUsername = parentUsername,
            parentContentPreview = parentContent?.take(100),
            images = images
        )
    }

    private fun extractReviewId(element: Element): String? {
        // Try data-ejs attribute first
        val ejsData = element.attrOrNull("data-ejs")
        if (!ejsData.isNullOrBlank()) {
            try {
                val json = JSONObject(ejsData)
                json.optString("reviewId")?.takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: Exception) {}
        }

        // Try class name pattern
        val className = element.className()
        val match = Regex("j_review_del_(\\d+)").find(className)
        return match?.groupValues?.getOrNull(1)
    }

    private fun String.extractUserId(): String? {
        return this.split("/").lastOrNull()?.takeIf { it.isNotBlank() }
    }

    // ================================================================
    // NOVEL PARSING (keep existing methods)
    // ================================================================

    private fun parseCategoryNovels(document: Document): List<Novel> {
        return document.select(DetailSelectors.categoryContainer).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(DetailSelectors.categoryThumb) ?: return@mapNotNull null
            val name = thumb.attrOrNull("title")
            if (name.isNullOrBlank()) return@mapNotNull null
            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull(DetailSelectors.categoryCover)
            val rawCover = imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    private fun parseSearchNovels(document: Document): List<Novel> {
        return document.select(DetailSelectors.searchContainer).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(DetailSelectors.searchThumb) ?: return@mapNotNull null
            val name = thumb.attrOrNull("title")
            if (name.isNullOrBlank()) return@mapNotNull null
            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull(DetailSelectors.searchCover)
            val rawCover = imgElement?.attrOrNull("src") ?: imgElement?.attrOrNull("data-original")
            val posterUrl = fixCoverUrl(rawCover)
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    private fun parseRelatedNovels(document: Document): List<Novel> {
        val relatedItems = document.select(DetailSelectors.relatedContainer)
        return relatedItems.mapNotNull { item ->
            val linkElement = item.selectFirstOrNull("a.m-book-title")
                ?: item.selectFirstOrNull(DetailSelectors.relatedThumb)
                ?: return@mapNotNull null
            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val title = linkElement.attrOrNull("title")
                ?: item.selectFirstOrNull(DetailSelectors.relatedTitle)?.textOrNull()?.trim()
                ?: item.selectFirstOrNull("h3")?.textOrNull()?.trim()
                ?: return@mapNotNull null
            val imgElement = item.selectFirstOrNull(DetailSelectors.relatedCover)
            val rawCover = imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)
            val ratingText = item.selectFirstOrNull(DetailSelectors.relatedRating)?.textOrNull()
            val rating = ratingText?.toFloatOrNull()?.let { RatingUtils.from5Stars(it) }
            Novel(name = title, url = novelUrl, posterUrl = posterUrl, rating = rating, apiName = this.name)
        }
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(page: Int, orderBy: String?, tag: String?): MainPageResult {
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
            throw Exception("Cloudflare protection detected.")
        }
        extractCsrfToken(document)
        val novels = parseCategoryNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val url = "$mainUrl/search?keywords=$encodedQuery&pageIndex=1"
        val response = get(url, customHeaders)
        val document = response.document
        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected.")
        }
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
            throw Exception("Cloudflare protection detected.")
        }
        extractCsrfToken(document)
        val bookId = extractBookId(fullUrl)
        if (bookId != null) {
            bookIdCache[fullUrl] = bookId
        }
        val name = document.selectFirstOrNull(DetailSelectors.detailCover)?.attrOrNull("alt")
            ?: document.selectFirst(DetailSelectors.detailTitle)?.textOrNull()?.trim()
            ?: "Unknown"
        val catalogUrl = fullUrl.trimEnd('/') + "/catalog"
        val chapters = loadChaptersFromCatalog(catalogUrl)
        val metadata = extractMetadata(document)
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
        val coverElement = document.selectFirstOrNull(DetailSelectors.detailCover)
        val rawCover = coverElement?.attrOrNull("src") ?: coverElement?.attrOrNull("data-original")
        val posterUrl = fixCoverUrl(rawCover)
        val synopsis = document.selectFirstOrNull(DetailSelectors.detailSynopsis)?.let { element ->
            element.select("br").append("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        } ?: "No Summary Found"
        val genresText = document.selectFirstOrNull(DetailSelectors.detailGenres)?.attrOrNull("title")
        val tags = genresText?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
        var author: String? = null
        document.select(DetailSelectors.detailAuthorLabel).forEach { element ->
            if (element.text().trim() == "Author:") {
                author = element.nextElementSibling()?.textOrNull()?.trim()
                return@forEach
            }
        }
        if (author.isNullOrBlank()) {
            author = document.selectFirstOrNull("p.ell a.c_primary")?.textOrNull()?.trim()
        }
        var statusText: String? = null
        document.select(DetailSelectors.detailStatus).forEach { element ->
            if (element.attrOrNull("title") == "Status") {
                statusText = element.nextElementSibling()?.textOrNull()?.trim()
                return@forEach
            }
        }
        val status = parseStatus(statusText)
        val ratingText = document.selectFirstOrNull(".g_star_num small")?.textOrNull()
            ?: document.selectFirstOrNull("[class*='score']")?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let { RatingUtils.from5Stars(it) }
        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            status = status
        )
    }

    private suspend fun loadChaptersFromCatalog(catalogUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        try {
            val response = get(catalogUrl, customHeaders)
            val document = response.document
            document.select(DetailSelectors.volumeContainer).forEach { volumeElement ->
                val volumeText = volumeElement.selectFirstOrNull("h4")?.textOrNull()?.trim()
                    ?: volumeElement.ownText().trim()
                val volumeMatch = Regex("Volume\\s*(\\d+)", RegexOption.IGNORE_CASE).find(volumeText)
                val volumeName = volumeMatch?.let { "Vol.${it.groupValues[1]}" } ?: ""
                volumeElement.select(DetailSelectors.chapterItem).forEach { chapterElement ->
                    val link = chapterElement.selectFirstOrNull(DetailSelectors.chapterLink) ?: return@forEach
                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.textOrNull()?.trim()
                        ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach
                    val isLocked = chapterElement.select(DetailSelectors.chapterLocked).isNotEmpty()
                    val chapterName = buildString {
                        if (volumeName.isNotBlank()) append("$volumeName: ")
                        append(chapterTitle)
                        if (isLocked) append(" 🔒")
                    }
                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }
            }
            if (chapters.isEmpty()) {
                document.select(".j_catalog_list ${DetailSelectors.chapterLink}").forEach { link ->
                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.textOrNull()?.trim()
                        ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach
                    val isLocked = link.parent()?.select(DetailSelectors.chapterLocked)?.isNotEmpty() == true
                    val chapterName = if (isLocked) "$chapterTitle 🔒" else chapterTitle
                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
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
            throw Exception("Cloudflare protection detected.")
        }
        document.select(DetailSelectors.chapterComments).remove()
        val titleHtml = document.selectFirstOrNull(DetailSelectors.chapterTitle)?.html() ?: ""
        var contentHtml = ""
        val contentElement = document.selectAny(DetailSelectors.chapterContent).firstOrNull()
        if (contentElement != null) {
            contentHtml = contentElement.html()
        } else {
            val paragraphs = document.select(DetailSelectors.chapterParagraph)
            if (paragraphs.isNotEmpty()) {
                contentHtml = paragraphs.map { it.outerHtml() }.joinToString("\n")
            }
        }
        if (contentHtml.isBlank()) return null
        val fullHtml = if (titleHtml.isNotBlank()) "$titleHtml\n$contentHtml" else contentHtml
        return cleanChapterHtml(fullHtml)
    }
}