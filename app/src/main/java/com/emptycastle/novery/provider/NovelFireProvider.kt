// com/emptycastle/novery/provider/NovelFireProvider.kt
package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.ConcurrentHashMap

class NovelFireProvider : MainProvider() {

    override val name = "NovelFire"
    override val mainUrl = "https://novelfire.net"
    override val hasMainPage = true
    override val hasReviews = true
    override val iconRes: Int = R.drawable.ic_provider_novelfire

    // Thread-safe state
    private val commentCursors = ConcurrentHashMap<String, String?>()
    private val postIdCache = ConcurrentHashMap<String, String>()

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "3"),
        FilterOption("Adult", "28"),
        FilterOption("Adventure", "4"),
        FilterOption("Anime", "46"),
        FilterOption("Arts", "47"),
        FilterOption("Comedy", "5"),
        FilterOption("Drama", "24"),
        FilterOption("Eastern", "44"),
        FilterOption("Ecchi", "26"),
        FilterOption("Fan-fiction", "48"),
        FilterOption("Fantasy", "6"),
        FilterOption("Game", "19"),
        FilterOption("Gender Bender", "25"),
        FilterOption("Harem", "7"),
        FilterOption("Historical", "12"),
        FilterOption("Horror", "37"),
        FilterOption("Isekai", "49"),
        FilterOption("Josei", "2"),
        FilterOption("Lgbt+", "45"),
        FilterOption("Magic", "50"),
        FilterOption("Magical Realism", "51"),
        FilterOption("Manhua", "52"),
        FilterOption("Martial Arts", "15"),
        FilterOption("Mature", "8"),
        FilterOption("Mecha", "34"),
        FilterOption("Military", "53"),
        FilterOption("Modern Life", "54"),
        FilterOption("Movies", "55"),
        FilterOption("Mystery", "16"),
        FilterOption("Other", "64"),
        FilterOption("Psychological", "9"),
        FilterOption("Realistic Fiction", "56"),
        FilterOption("Reincarnation", "43"),
        FilterOption("Romance", "1"),
        FilterOption("School Life", "21"),
        FilterOption("Sci-fi", "20"),
        FilterOption("Seinen", "10"),
        FilterOption("Shoujo", "38"),
        FilterOption("Shoujo Ai", "57"),
        FilterOption("Shounen", "17"),
        FilterOption("Shounen Ai", "39"),
        FilterOption("Slice of Life", "13"),
        FilterOption("Smut", "29"),
        FilterOption("Sports", "42"),
        FilterOption("Supernatural", "18"),
        FilterOption("System", "58"),
        FilterOption("Tragedy", "32"),
        FilterOption("Urban", "63"),
        FilterOption("Urban Life", "59"),
        FilterOption("Video Games", "60"),
        FilterOption("War", "61"),
        FilterOption("Wuxia", "31"),
        FilterOption("Xianxia", "23"),
        FilterOption("Xuanhuan", "22"),
        FilterOption("Yaoi", "14"),
        FilterOption("Yuri", "62")
    )

    override val orderBys = listOf(
        FilterOption("Rank (Top)", "rank-top"),
        FilterOption("Rating Score (Top)", "rating-score-top"),
        FilterOption("Review Count (Most)", "review"),
        FilterOption("Comment Count (Most)", "comment"),
        FilterOption("Bookmark Count (Most)", "bookmark"),
        FilterOption("Today Views (Most)", "today-view"),
        FilterOption("Monthly Views (Most)", "monthly-view"),
        FilterOption("Total Views (Most)", "total-view"),
        FilterOption("Title (A>Z)", "abc"),
        FilterOption("Title (Z>A)", "cba"),
        FilterOption("Last Updated (Newest)", "date"),
        FilterOption("Chapter Count (Most)", "chapter-count-most")
    )

    // ================================================================
    // SELECTOR CONFIGURATIONS
    // ================================================================

    private object Selectors {
        val novelContainers = listOf(".novel-item", ".novel-list .novel-item")
        val novelTitle = listOf(".novel-title > a", "a[title]")
        val novelDetailTitle = listOf(".novel-title", ".cover > img[alt]")
        val chapterContent = listOf("#content", ".chapter-content")
        val synopsis = listOf(".summary .content", ".description")
        val poster = listOf(".cover > img", ".novel-cover > img")
        val author = listOf(".author .property-item > span", ".author span")
        val genres = listOf(".categories .property-item", ".genres a")
        val status = listOf(".header-stats .ongoing", ".header-stats .completed")
        val rating = listOf(".nub", ".rating-value")
        val postId = listOf("#novel-report[report-post_id]")
        val views = listOf(".header-stats span:has(i.icon-eye) strong")
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

    private fun deSlash(url: String): String {
        return if (url.startsWith("/")) url.substring(1) else url
    }

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("data-src")
            ?: imgElement.attrOrNull("src")
            ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image/gif")) return null
        val cleanedSrc = deSlash(rawSrc)
        return if (cleanedSrc.startsWith("http")) cleanedSrc else "$mainUrl/$cleanedSrc"
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

    private fun parseViewCount(text: String?): String? {
        if (text.isNullOrBlank()) return null
        // Clean up the text, keeping numbers, decimals, and K/M/B suffixes
        val cleaned = text.replace(Regex("[^0-9.KMBkmb]"), "").trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    /**
     * Parse view count to integer value
     * Converts: "217.5K" -> 217500, "1.4M" -> 1400000, etc.
     */
    private fun parseViewCountToInt(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.replace(Regex("[^0-9.KMBkmb]"), "").trim().uppercase()
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
            else -> cleaned.replace(".", "").toIntOrNull()
        }
    }

    private fun extractPostId(document: Document): String? {
        for (selector in Selectors.postId) {
            val element = document.selectFirstOrNull(selector)
            val id = element?.attrOrNull("report-post_id")
            if (!id.isNullOrBlank()) return id
        }
        return null
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "novelfire")
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun extractNovelSlug(url: String): String {
        return url
            .replace(mainUrl, "")
            .replace("$mainUrl/", "")
            .removePrefix("/")
            .removePrefix("book/")
            .removeSuffix("/")
            .split("/")
            .firstOrNull() ?: url
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.selectAny(Selectors.novelContainers)
        return elements.mapNotNull { element -> parseNovelElement(element) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst(Selectors.novelTitle) ?: return null
        val name = titleElement.attrOrNull("title") ?: titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null
        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(deSlash(href.replace(mainUrl, "").replace("$mainUrl/", ""))) ?: return null
        val imgElement = element.selectFirstOrNull(".novel-cover > img") ?: element.selectFirstOrNull("img")
        val posterUrl = fixPosterUrl(imgElement)
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    private fun parseSearchNovels(document: Document): List<Novel> {
        val elements = document.select(".novel-list.chapters .novel-item")
        return elements.mapNotNull { element ->
            val linkElement = element.selectFirstOrNull("a") ?: return@mapNotNull null
            val name = linkElement.attrOrNull("title") ?: linkElement.textOrNull()?.trim()
            if (name.isNullOrBlank()) return@mapNotNull null
            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(deSlash(href.replace(mainUrl, "").replace("$mainUrl/", ""))) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull(".novel-cover > img") ?: element.selectFirstOrNull("img")
            val posterUrl = fixPosterUrl(imgElement)
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    // ================================================================
    // RELATED NOVELS
    // ================================================================

    private suspend fun loadRelatedNovels(postId: String?): List<Novel> {
        if (postId.isNullOrBlank()) return emptyList()
        return try {
            val url = "$mainUrl/ajax/novelYouMayLike?post_id=$postId"
            val response = get(url, mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest"
            ))
            val json = JSONObject(response.text)
            val html = json.optString("html", "")
            if (html.isBlank()) return emptyList()
            val document = Jsoup.parse(html)
            val novelItems = document.select("li.novel-item")
            novelItems.mapNotNull { item ->
                val linkElement = item.selectFirstOrNull("a") ?: return@mapNotNull null
                val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
                val title = item.selectFirstOrNull("h5.novel-title")?.textOrNull()?.trim() ?: return@mapNotNull null
                val imgElement = item.selectFirstOrNull("figure.novel-cover img")
                val posterUrl = fixPosterUrl(imgElement)
                val novelUrl = fixUrl(deSlash(href.removePrefix(mainUrl).removePrefix("/"))) ?: return@mapNotNull null
                Novel(name = title, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ================================================================
    // COMMENTS / REVIEWS
    // ================================================================

    override suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean
    ): List<UserReview> {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val novelSlug = extractNovelSlug(url)

        // Get or fetch post_id
        val postId = if (page == 1) {
            val response = get(fullUrl)
            val id = extractPostId(response.document)
            if (id != null) {
                postIdCache[novelSlug] = id
            }
            id
        } else {
            postIdCache[novelSlug]
        }

        if (postId.isNullOrBlank()) return emptyList()

        // Build comment API URL
        val cursor = if (page == 1) {
            commentCursors.remove(postId)
            ""
        } else {
            commentCursors[postId] ?: return emptyList()
        }

        val commentUrl = buildString {
            append("$mainUrl/comment/show?")
            append("post_id=$postId")
            append("&chapter_id=")
            append("&order_by=newest")
            if (cursor.isNotEmpty()) {
                append("&cursor=$cursor")
            }
        }

        return try {
            val response = get(commentUrl, mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest"
            ))
            val json = JSONObject(response.text)
            parseCommentsFromJson(json, postId, showSpoilers)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseCommentsFromJson(
        json: JSONObject,
        postId: String,
        showSpoilers: Boolean
    ): List<UserReview> {
        val reviews = mutableListOf<UserReview>()

        val html = json.optString("html", "")
        if (html.isBlank()) return emptyList()

        // Store next cursor
        val nextCursor = json.optString("next_cursor", null)
        if (!nextCursor.isNullOrBlank()) {
            commentCursors[postId] = nextCursor
        } else {
            commentCursors.remove(postId)
        }

        val document = Jsoup.parse(html)

        // Parse top-level comments
        document.select("li").forEach { listItem ->
            val commentEl = listItem.selectFirstOrNull("> div.comment-item")
            if (commentEl != null) {
                val review = parseCommentElement(commentEl, showSpoilers)
                if (review != null) {
                    // Check for nested replies
                    val repliesContainer = listItem.selectFirstOrNull(".reply-comments ul")
                    val nestedReplies = mutableListOf<UserReview>()
                    var hasMoreReplies = false

                    repliesContainer?.select("> li")?.forEach { replyLi ->
                        // Check for "View More Replies" button
                        if (replyLi.hasClass("show_replies")) {
                            hasMoreReplies = true
                            return@forEach
                        }

                        val replyComment = replyLi.selectFirstOrNull("div.comment-item")
                        if (replyComment != null) {
                            val reply = parseCommentElement(replyComment, showSpoilers)
                            if (reply != null) {
                                nestedReplies.add(reply)
                            }
                        }
                    }

                    reviews.add(review.copy(
                        replies = nestedReplies.toList(),
                        replyCount = nestedReplies.size + if (hasMoreReplies) 1 else 0,
                        hasMoreReplies = hasMoreReplies,
                        providerData = mapOf("postId" to postId)
                    ))
                }
            }
        }

        return reviews
    }

    private fun parseCommentElement(
        element: Element,
        showSpoilers: Boolean
    ): UserReview? {
        val commentId = element.attrOrNull("data-comid") ?: return null

        val header = element.selectFirstOrNull("div.comment-header") ?: return null
        val body = element.selectFirstOrNull("div.comment-body") ?: return null

        // Username
        val username = header.selectFirstOrNull("span.username")?.textOrNull()?.trim()

        // User ID from link
        val userLink = header.selectFirstOrNull("a[href*='/user/']")
        val userId = userLink?.attrOrNull("href")?.split("/")?.lastOrNull()

        // Avatar URL
        val avatarElement = header.selectFirstOrNull("img.avatar")
        var avatarUrl = avatarElement?.attrOrNull("src")
        if (avatarUrl != null) {
            if (avatarUrl.contains("default-avatar") || avatarUrl.contains("data:image")) {
                avatarUrl = null
            } else if (!avatarUrl.startsWith("http")) {
                avatarUrl = "https://images.novelfire.net$avatarUrl"
            }
        }

        // Time
        val time = header.selectFirstOrNull("span.post-date")?.textOrNull()?.trim()

        // User tier
        val tier = header.selectFirstOrNull("span.tier")?.textOrNull()?.trim()

        // Spoiler check
        val commentTextElement = body.selectFirstOrNull("div.comment-text")
        val isSpoiler = commentTextElement?.attrOrNull("data-spoiler") == "1"

        // Get content
        val content = commentTextElement?.html() ?: return null
        if (content.isBlank()) return null

        // Edit status
        val isEdited = body.selectFirstOrNull(".edited") != null

        // Like/Dislike counts
        val parsedLikeCount = body.selectFirstOrNull(".like-group span")?.textOrNull()?.toIntOrNull() ?: 0
        val parsedDislikeCount = body.selectFirstOrNull(".dislike-group span")?.textOrNull()?.toIntOrNull() ?: 0

        // Parent reply info
        val parentLink = header.selectFirstOrNull("div.parent-link a")
        val parentUsername = parentLink?.textOrNull()?.trim()
        val parentCommentId = parentLink?.attrOrNull("href")?.removePrefix("#lnw-comment-")

        return UserReview(
            id = commentId,
            content = content,
            username = username,
            userId = userId,
            avatarUrl = avatarUrl,
            userTier = tier?.takeIf { it != "Reader" },
            time = time,
            likeCount = parsedLikeCount,
            dislikeCount = parsedDislikeCount,
            isSpoiler = isSpoiler,
            isEdited = isEdited,
            parentReviewId = parentCommentId,
            parentUsername = parentUsername
        )
    }

    /**
     * Load more replies for a specific comment
     */
    suspend fun loadCommentReplies(
        postId: String,
        parentCommentId: String
    ): List<UserReview> {
        val url = "$mainUrl/comment/replies?post_id=$postId&parent_id=$parentCommentId"

        return try {
            val response = get(url, mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest"
            ))

            val json = JSONObject(response.text)
            val html = json.optString("html", "")
            if (html.isBlank()) return emptyList()

            val document = Jsoup.parse(html)
            document.select("div.comment-item").mapNotNull { el ->
                parseCommentElement(el, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(page: Int, orderBy: String?, tag: String?): MainPageResult {
        val params = mutableListOf<String>()
        if (!tag.isNullOrEmpty()) {
            params.add("categories[]=$tag")
        }
        params.add("ctgcon=and")
        params.add("totalchapter=0")
        params.add("ratcon=min")
        params.add("rating=0")
        params.add("status=-1")
        val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "rank-top"
        params.add("sort=$sort")
        params.add("page=$page")
        val queryString = params.joinToString("&")
        val url = "$mainUrl/search-adv?$queryString"
        val response = get(url)
        val document = response.document
        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search?keyword=$encodedQuery&page=1"
        val response = get(url)
        val document = response.document
        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }
        return parseSearchNovels(document)
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val novelPath = deSlash(url.replace(mainUrl, "").replace("$mainUrl/", ""))
        val novelSlug = extractNovelSlug(novelPath)
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$novelPath"
        val response = get(fullUrl)
        val document = response.document
        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }
        val name = document.selectFirst(Selectors.novelDetailTitle)?.textOrNull()?.trim()
            ?: document.selectFirstOrNull(".cover > img")?.attrOrNull("alt")
            ?: "Unknown"
        val postId = extractPostId(document)
        if (postId != null) {
            postIdCache[novelSlug] = postId
        }
        val chapters = loadChaptersWithDates(novelSlug, postId)
        val metadata = extractMetadata(document)
        val relatedNovels = loadRelatedNovels(postId)
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
            views = metadata.views,
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
        val status: String? = null,
        val views: Int? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        // Author
        val author = document.selectFirst(Selectors.author)?.textOrNull()?.trim()

        // Poster URL
        val posterUrl = document.selectFirst(Selectors.poster)?.let { imgElement ->
            val src = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src")
            if (!src.isNullOrBlank() && !src.contains("data:image")) {
                if (src.startsWith("http")) src else "$mainUrl/${deSlash(src)}"
            } else null
        }

        // Synopsis
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            element.text().replace("Show More", "").trim().takeIf { it.isNotBlank() }
        } ?: "No Summary Found"

        // Tags/Genres
        val tags = document.selectAny(Selectors.genres)
            .mapNotNull { it.textOrNull()?.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // Status
        val statusText = document.selectFirstOrNull(".header-stats .ongoing")?.textOrNull()
            ?: document.selectFirstOrNull(".header-stats .completed")?.textOrNull()
        val status = parseStatus(statusText)

        // Rating
        val ratingText = document.selectFirst(Selectors.rating)?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let { RatingUtils.from5Stars(it) }

        // Views - use parseViewCountToInt instead of parseViewCount
        val viewsText = document.selectFirst(Selectors.views)?.textOrNull()?.trim()
        val views = parseViewCountToInt(viewsText)

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            status = status,
            views = views
        )
    }

    // ================================================================
    // CHAPTERS
    // ================================================================

    private suspend fun loadChaptersWithDates(novelSlug: String, postId: String?): List<Chapter> {
        if (postId.isNullOrBlank()) {
            return loadChaptersFromHtml(novelSlug)
        }
        val chapters = mutableListOf<Chapter>()
        try {
            val ajaxUrl = "$mainUrl/listChapterDataAjax?post_id=$postId"
            val response = get(ajaxUrl)
            val responseText = response.text
            if (responseText.contains("You are being rate limited")) {
                throw Exception("NovelFire is rate limiting requests.")
            }
            if (responseText.contains("Page Not Found 404")) {
                return loadChaptersFromHtml(novelSlug)
            }
            val json = JSONObject(responseText)
            val dataArray = json.optJSONArray("data") ?: return loadChaptersFromHtml(novelSlug)
            for (i in 0 until dataArray.length()) {
                val chapterObj = dataArray.getJSONObject(i)
                val nSort = chapterObj.optInt("n_sort", i + 1)
                val title = chapterObj.optString("title", "")
                val slug = chapterObj.optString("slug", "")
                val createdAt = chapterObj.optString("created_at", null)
                val chapterName = if (title.isNotBlank()) {
                    Jsoup.parse(title).text()
                } else if (slug.isNotBlank()) {
                    slug.replace("-", " ").replaceFirstChar { it.uppercase() }
                } else {
                    "Chapter $nSort"
                }
                val chapterPath = "book/$novelSlug/chapter-$nSort"
                val chapterUrl = fixUrl(chapterPath) ?: continue
                chapters.add(Chapter(name = chapterName, url = chapterUrl, dateOfRelease = createdAt))
            }
            chapters.sortBy { chapter ->
                Regex("chapter-(\\d+)").find(chapter.url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return loadChaptersFromHtml(novelSlug)
        }
        return chapters
    }

    private suspend fun loadChaptersFromHtml(novelSlug: String): List<Chapter> {
        val chaptersUrl = "$mainUrl/book/$novelSlug/chapters"
        return try {
            val response = get(chaptersUrl)
            val document = response.document
            val chapters = mutableListOf<Chapter>()
            val chapterItems = document.select("ul li:has(a[href*='/chapter-'])")
            for (item in chapterItems) {
                val linkElement = item.selectFirstOrNull("a") ?: continue
                val href = linkElement.attrOrNull("href") ?: continue
                val titleAttr = linkElement.attrOrNull("title")
                val titleElement = item.selectFirstOrNull("strong.chapter-title")
                val chapterNoElement = item.selectFirstOrNull("span.chapter-no")
                val chapterTitle = titleAttr ?: titleElement?.textOrNull()?.trim()
                ?: linkElement.textOrNull()?.trim() ?: continue
                val timeElement = item.selectFirstOrNull("time.chapter-update")
                val dateOfRelease = timeElement?.attrOrNull("datetime") ?: timeElement?.textOrNull()?.trim()
                val chapterNo = chapterNoElement?.textOrNull()?.trim()
                val chapterName = if (!chapterNo.isNullOrBlank() &&
                    !chapterTitle.startsWith("Chapter $chapterNo", ignoreCase = true)) {
                    "Chapter $chapterNo – $chapterTitle"
                } else {
                    chapterTitle
                }
                val chapterUrl = fixUrl(deSlash(href.removePrefix(mainUrl).removePrefix("/"))) ?: continue
                chapters.add(Chapter(name = chapterName, url = chapterUrl, dateOfRelease = dateOfRelease))
            }
            chapters.sortBy { chapter ->
                Regex("chapter-(\\d+)").find(chapter.url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }
            chapters
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val response = get(fullUrl)
        val document = response.document
        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }
        val contentElement = document.selectFirst(Selectors.chapterContent) ?: return null
        val oddsElements = contentElement.select(":not(p, h1, h2, h3, h4, h5, h6, span, i, b, u, em, strong, img, a, div, br, hr)")
        oddsElements.forEach { ele ->
            val tagName = ele.tagName()
            if (tagName.length > 5) {
                ele.remove()
            }
        }
        contentElement.select(
            ".ads, .adsbygoogle, script, style, .ads-holder, .ads-middle, [id*='ads'], [class*='ads'], .hidden, [style*='display:none'], [style*='display: none']"
        ).remove()
        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}