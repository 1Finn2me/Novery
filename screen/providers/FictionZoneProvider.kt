package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.HeadMainPageResponse
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.SearchResponse
import com.lagradost.quicknovel.newChapterData
import com.lagradost.quicknovel.newSearchResponse
import com.lagradost.quicknovel.newStreamResponse
import com.lagradost.quicknovel.setStatus
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant

class FictionZoneProvider : MainAPI() {

    override val name = "Fiction Zone"
    override val mainUrl = "https://fictionzone.net"
    override val hasMainPage = true
    override val rateLimitTime: Long = 100
    override val iconId = R.drawable.icon_fictionzone
    override val iconBackgroundId = R.color.primaryGrayBackground

    override val orderBys = listOf(
        "Most Popular" to "bookmark_count",
        "Newest"       to "created_at",
    )

    // ── API helpers ────────────────────────────────────────────────────────────

    private val proxyUrl = "$mainUrl/api/__api_party/fictionzone"
    private val cdnBase  = "https://cdn.fictionzone.net/insecure/rs:fill:165:250/"

    /** POST to the FictionZone API proxy and return the parsed JSON object. */
    private suspend fun getData(path: String): JSONObject {
        val body = JSONObject().apply {
            put("path", path)
            put("headers", JSONArray().apply {
                put(JSONArray(listOf("content-type", "application/json")))
                put(JSONArray(listOf("x-request-time", Instant.now().toString())))
            })
            put("method", "GET")
        }
        val text = app.post(
            proxyUrl,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept"       to "application/json",
            ),
            json = body,
        ).text
        return JSONObject(text)
    }

    private fun coverUrl(image: String?): String? =
        if (image.isNullOrEmpty()) null else "$cdnBase$image.webp"

    private fun JSONObject.toSearchResponse(): SearchResponse {
        val slug = optString("slug")
        return this@FictionZoneProvider.newSearchResponse(
            name = optString("title"),
            url  = "$mainUrl/novel/$slug",
        ) {
            posterUrl = coverUrl(optString("image").takeIf { it.isNotEmpty() })
        }
    }

    // ── Main Page ──────────────────────────────────────────────────────────────

    override suspend fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?,
    ): HeadMainPageResponse {
        val sortBy = orderBy ?: "bookmark_count"
        val path   = "/platform/browse?page=$page&page_size=20&sort_by=$sortBy&sort_order=desc&include_genres=true"
        val data   = getData(path)
        val novels = data.optJSONObject("data")?.optJSONArray("novels")
            ?: return HeadMainPageResponse(path, emptyList())
        val results = (0 until novels.length()).map { novels.getJSONObject(it).toSearchResponse() }
        return HeadMainPageResponse(path, results)
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val path    = "/platform/browse?search=$encoded&page=1&page_size=20&search_in_synopsis=true&sort_by=bookmark_count&sort_order=desc&include_genres=true"
        val data    = getData(path)
        val novels  = data.optJSONObject("data")?.optJSONArray("novels") ?: return emptyList()
        return (0 until novels.length()).map { novels.getJSONObject(it).toSearchResponse() }
    }

    // ── Novel Detail ───────────────────────────────────────────────────────────

    override suspend fun load(url: String): LoadResponse? {
        // url is "$mainUrl/novel/{slug}"
        val slug    = url.substringAfterLast("/novel/")
        val data    = getData("/platform/novel-details?slug=$slug")
        val novel   = data.optJSONObject("data") ?: return null
        val title   = novel.optString("title").takeIf { it.isNotEmpty() } ?: return null
        val novelId = novel.optString("id")

        // Cover & metadata
        val cover    = coverUrl(novel.optString("image").takeIf { it.isNotEmpty() })
        val synopsis = novel.optString("synopsis").takeIf { it.isNotEmpty() }
        val status   = novel.optInt("status", -1)

        // Author (first contributor with role "author")
        val contributors = novel.optJSONArray("contributors")
        var author: String? = null
        if (contributors != null) {
            for (i in 0 until contributors.length()) {
                val c = contributors.getJSONObject(i)
                if (c.optString("role") == "author") {
                    author = c.optString("display_name").takeIf { it.isNotEmpty() }
                    break
                }
            }
        }

        // Genres + Tags merged
        val tags = mutableListOf<String>()
        novel.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) tags += arr.getJSONObject(i).optString("name")
        }
        novel.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) tags += arr.getJSONObject(i).optString("name")
        }

        // Chapter list
        val chapData  = getData("/platform/chapter-lists?novel_id=$novelId")
        val chapArray = chapData.optJSONObject("data")?.optJSONArray("chapters")
        val chapters: List<ChapterData> = if (chapArray != null) {
            (0 until chapArray.length()).map { i ->
                val c         = chapArray.getJSONObject(i)
                val chapterId = c.optString("chapter_id")
                // Pipe-separated: display URL | API path (mirrors the TS plugin)
                val chapterUrl = "$url/$chapterId|/platform/chapter-content?novel_id=$novelId&chapter_id=$chapterId"
                newChapterData(name = c.optString("title"), url = chapterUrl, fix = false) {
                    dateOfRelease = c.optString("published_date").takeIf { it.isNotEmpty() }
                }
            }
        } else emptyList()

        return newStreamResponse(name = title, url = url, data = chapters) {
            this.author    = author
            this.posterUrl = cover
            this.synopsis  = synopsis
            this.tags      = tags.takeIf { it.isNotEmpty() }
            when (status) {
                1    -> setStatus("ongoing")
                0    -> setStatus("completed")
                else -> setStatus(null)
            }
        }
    }

    // ── Chapter Content ────────────────────────────────────────────────────────

    override suspend fun loadHtml(url: String): String? {
        // url = ".../{chapterId}|/platform/chapter-content?..."
        val apiPath = url.substringAfter("|")
        val data    = getData(apiPath)
        val content = data.optJSONObject("data")?.optString("content")
            ?.takeIf { it.isNotEmpty() } ?: return null
        return "<p>" + content.replace("\n", "</p><p>") + "</p>"
    }
}
