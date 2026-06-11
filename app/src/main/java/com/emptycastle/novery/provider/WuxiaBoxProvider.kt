package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterGroup
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import org.jsoup.nodes.Element

/**
 * Provider for WuxiaBox.com
 * Ported from QuickNovel implementation.
 * Marking as open to allow inheritance for similar providers (e.g. FanMTL).
 */
open class WuxiaBoxProvider : MainProvider() {

    override val name = "WuxiaBox"
    override val mainUrl = "https://www.wuxiabox.com"
    override val hasMainPage = true
    override val iconRes: Int = R.drawable.ic_provider_wuxiabox

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", "all"),
        FilterOption("Fan-Fic", "fan-fiction"),
        FilterOption("Faloo", "faloo"),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("CRomance", "contemporary-romance"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern Fantasy", "eastern-fantasy"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Fantasy Romance", "fantasy-romance"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Josei", "josei"),
        FilterOption("Lolicon", "lolicon"),
        FilterOption("Magical Realism", "magical-realism"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Shounen Ai", "shounen-ai"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Video Games", "video-games"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Two-D", "two-dimensional"),
        FilterOption("Erciyuan", "erciyuan"),
        FilterOption("Game", "game"),
        FilterOption("Military", "military"),
        FilterOption("Urban Life", "urban-life"),
        FilterOption("Yuri", "yuri"),
        FilterOption("Chinese", "chinese"),
        FilterOption("Japanese", "japanese"),
        FilterOption("Hentai", "hentai"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Magic", "magic"),
        FilterOption("Shoujo Ai", "shoujo-ai"),
        FilterOption("Urban", "urban"),
        FilterOption("VR", "virtual-reality"),
        FilterOption("Wuxia Xianxia", "wuxia_xianxia"),
        FilterOption("Official", "official_circles"),
        FilterOption("Sci-fi", "science_fiction"),
        FilterOption("Thriller", "suspense_thriller"),
        FilterOption("Travel Through Time", "travel_through_time")
    )

    override val orderBys = listOf(
        FilterOption("New", "newstime"),
        FilterOption("Popular", "onclick"),
        FilterOption("Updates", "lastdotime")
    )

    override val extraFilterGroups = listOf(
        FilterGroup(
            label = "Category",
            key = "category",
            options = listOf(
                FilterOption("All", "all"),
                FilterOption("Completed", "Completed"),
                FilterOption("Ongoing", "Ongoing")
            )
        )
    )

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?,
        extraFilters: Map<String, String>
    ): MainPageResult {
        val category = extraFilters["category"] ?: "all"
        val sort = orderBy ?: "newstime"
        val genre = tag ?: "all"

        val url = "$mainUrl/list/${genre}/${category}-${sort}-${page}.html"
        val document = get(url).document

        val novels = document.select("li.novel-item").mapNotNull { element ->
            val a = element.selectFirstOrNull("a[title]") ?: return@mapNotNull null
            val title = a.attr("title").takeIf { it.isNotBlank() }
                ?: element.selectFirstOrNull("h4.novel-title")?.text() ?: return@mapNotNull null
            
            val href = a.attr("href")
            val poster = element.selectFirstOrNull("img")?.let {
                it.attr("data-src").ifBlank { it.attr("src") }
            }

            Novel(
                name = title,
                url = fixUrl(href) ?: return@mapNotNull null,
                posterUrl = fixUrl(poster),
                apiName = this.name
            )
        }

        return MainPageResult(url, novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val searchResponse = post(
            "$mainUrl/e/search/index.php",
            data = mapOf(
                "show" to "title",
                "tempid" to "1",
                "tbname" to "news",
                "keyboard" to encodedQuery
            )
        )

        // Extract searchId from the redirect URL
        val redirectUrl = searchResponse.effectiveUrl
        val searchId = Regex("searchid=(\\d+)").find(redirectUrl)?.groupValues?.get(1) 
            ?: return emptyList()

        val results = mutableListOf<Novel>()
        var currentPage = 0
        
        while (true) {
            val url = "$mainUrl/e/search/result/index.php?page=$currentPage&searchid=$searchId"
            val document = get(url).document
            val pageItems = document.select("li.novel-item").mapNotNull { element ->
                val a = element.selectFirstOrNull("a[title]") ?: return@mapNotNull null
                val href = a.attr("href")
                val title = a.attr("title").ifBlank { element.selectFirstOrNull("h4.novel-title")?.text() } ?: return@mapNotNull null
                
                val poster = element.selectFirstOrNull("img")?.let {
                    it.attr("data-src").ifBlank { it.attr("src") }
                }

                Novel(
                    name = title,
                    url = fixUrl(href) ?: return@mapNotNull null,
                    posterUrl = fixUrl(poster),
                    apiName = this.name
                )
            }

            if (pageItems.isEmpty()) break
            results.addAll(pageItems)
            currentPage++
            
            // Limit search results for performance if many pages
            if (currentPage >= 3) break
        }

        return results
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url).document
        val title = document.selectFirstOrNull("h1.novel-title")?.text() ?: return null
        val author = document.selectFirstOrNull("div.author [itemprop=author]")?.text()
        val synopsis = document.selectFirstOrNull("meta[itemprop=description]")?.attr("content")
            ?: document.selectFirstOrNull("div.summary, div.desc, #intro")?.text() ?: ""
        
        val poster = document.selectFirstOrNull("div.fixed-img img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }

        val statusText = document.selectFirstOrNull("div.header-stats strong:matches(Ongoing|Completed)")?.text()
        
        // Extract bookId for AJAX chapter loading
        val bookId = url.substringAfterLast("/").substringBefore(".html")
        val chapters = mutableListOf<Chapter>()
        var currentPage = 0

        while (true) {
            val ajaxUrl = "$mainUrl/e/extend/fy.php?page=$currentPage&wjm=$bookId&X-Requested-With=XMLHttpRequest&_=${System.currentTimeMillis()}"
            val ajaxDoc = get(ajaxUrl).document
            val chapterElements = ajaxDoc.select("ul.chapter-list li")
            
            if (chapterElements.isEmpty()) break
            
            val pageChapters = chapterElements.mapNotNull { li ->
                val link = li.selectFirstOrNull("a") ?: return@mapNotNull null
                val cUrl = fixUrl(link.attr("href")) ?: return@mapNotNull null
                val cTitle = link.selectFirstOrNull("strong.chapter-title")?.text()?.trim() ?: "Chapter"
                Chapter(name = cTitle, url = cUrl)
            }
            
            chapters.addAll(pageChapters)
            currentPage++
            
            // Safety break for extremely large lists
            if (currentPage > 50) break
        }

        return NovelDetails(
            url = url,
            name = title,
            chapters = chapters,
            author = author,
            posterUrl = fixUrl(poster),
            synopsis = synopsis,
            status = statusText
        )
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url).document
        val content = document.selectFirstOrNull("div.chapter-content") ?: return null

        // Cleanup placeholders and images
        content.select("img[src*=disable-blocker.jpg], script, div[align=center]").remove()

        return HtmlUtils.cleanChapterContent(content.html(), "wuxiabox")
    }
}
