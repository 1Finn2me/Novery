package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.RatingUtils
import kotlinx.coroutines.delay
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.atomic.AtomicLong

/**
 * Provider for NovelBin.com
 * Updated logic: Cloned exact chapter content loading from QuickNovel's AllNovelProvider.
 */
class NovelBinProvider : MainProvider() {

    override val name = "NovelBin"
    override val mainUrl = "https://novelbin.com"
    override val hasMainPage = true
    override val iconRes: Int = R.drawable.ic_provider_novelbin

    private val searchInterval = 3400L
    private val lastSearchTime = AtomicLong(0)
    private val fullPosterRegex = Regex("/novel_[0-9]*_[0-9]*/")

    override val tags = listOf(
        FilterOption("All", "All"),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Anime & Comics", "anime-&-comics"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Fanfiction", "fanfiction"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Game", "game"),
        FilterOption("Gender Bender", "gender-bender"),
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
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Urban Life", "urban-life"),
        FilterOption("Video Games", "video-games"),
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
        FilterOption("Most Popular", "sort/top-view-novel"),
        FilterOption("Store", "store")
    )

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?,
        extraFilters: Map<String, String>
    ): MainPageResult {
        val url = if (orderBy == "" && tag != null && tag != "All") {
            "$mainUrl/genre/$tag?page=$page"
        } else {
            val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "sort/top-hot-novel"
            "$mainUrl/$sort?page=$page"
        }

        val document = get(url).document
        val novels = document.select("div.list>div.row").mapNotNull { element ->
            val a = element.selectFirst("div > div > h3.novel-title > a") ?: return@mapNotNull null
            Novel(
                name = a.text(),
                url = fixUrl(a.attr("href")) ?: return@mapNotNull null,
                posterUrl = fixPosterUrl(element.selectFirst("img")),
                apiName = this.name
            )
        }

        return MainPageResult(url, novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val now = System.currentTimeMillis()
        if (now - lastSearchTime.get() < searchInterval) delay(searchInterval - (now - lastSearchTime.get()))
        lastSearchTime.set(System.currentTimeMillis())

        val url = "$mainUrl/search?keyword=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val document = get(url).document
        
        return document.select("div.list>div.row").mapNotNull { element ->
            val a = element.selectFirst("div > div > h3.novel-title > a") ?: return@mapNotNull null
            Novel(
                name = a.text(),
                url = fixUrl(a.attr("href")) ?: return@mapNotNull null,
                posterUrl = fixPosterUrl(element.selectFirst("img")),
                apiName = this.name
            )
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url).document
        val name = document.selectFirst("h3.title")?.text() ?: return null

        val dataNovelId = document.select("#rating").attr("data-novel-id")
        val ajaxUrl = "$mainUrl/ajax/chapter-archive?novelId=$dataNovelId"
        val chapterData = get(ajaxUrl).document

        var parsed = chapterData.select("select > option")
        if (parsed.isEmpty()) {
            parsed = chapterData.select("li[data-chapter-item] > a")
        }

        val chapters = parsed.mapNotNull { c ->
            val cUrl = c.attr("value").takeIf { it.isNotBlank() } ?: c.attr("href")
            if (cUrl.isNullOrBlank() || cUrl == "#") return@mapNotNull null
            
            val cName = c.text().ifEmpty { "Chapter" }
            Chapter(name = cName, url = fixUrl(cUrl) ?: "")
        }

        val infoDivs = document.select("div.info > div").takeIf { it.isNotEmpty() } ?: document.select("ul.info > li")
        
        val author = infoDivs.find { it.text().contains("Author:") }?.selectFirst("a")?.text()
        val tags = infoDivs.find { it.text().contains("Genre") }?.select("a")?.mapNotNull { it.text().takeIf { t -> t.trim().isNotBlank() } }
        val status = infoDivs.find { it.text().contains("Status:") }?.selectFirst("a")?.text()

        val imgElement = document.selectFirst("div.book img")
        val posterUrl = fixUrl(
            imgElement?.attr("src")?.takeIf { it.isNotBlank() } ?: imgElement?.attr("data-src")
        )?.replace(fullPosterRegex, "/novel/")

        val synopsis = document.selectFirst("div.desc-text")?.text()

        val rating = document.selectFirst("div.small > em > strong:nth-child(1) > span")?.text()?.toFloatOrNull()?.let {
            RatingUtils.from10Points(it)
        }
        val peopleVoted = document.selectFirst("div.small > em > strong:nth-child(3) > span")?.text()?.toIntOrNull()

        return NovelDetails(
            url = url,
            name = name,
            chapters = chapters,
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
     * Cloned exact logic from QuickNovel's AllNovelProvider.loadHtml()
     */
    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url).document
        val content = (document.selectFirst("#chapter-content")
            ?: document.selectFirst("#chr-content"))
        
        if (content == null) return null

        return content.html()
            .replace(
                Regex("<iframe .* src=\"//ad.{0,2}-ads.com/.*\" style=\".*\"></iframe>"),
                " "
            ).replace(
                " If you find any errors ( broken links, non-standard content, etc.. ), Please let us know < report chapter > so we can fix it as soon as possible.",
                " "
            ).replace(
                "If you find any errors ( Ads popup, ads redirect, broken links, non-standard content, etc.. ), Please let us know < report chapter > so we can fix it as soon as possible.",
                " "
            ).replace("[Updated from F r e e w e b n o v e l. c o m]", "")
    }

    private fun fixPosterUrl(img: Element?): String? {
        val src = img?.attrOrNull("data-src") ?: img?.attrOrNull("src") ?: return null
        return fixUrl(src.replace(fullPosterRegex, "/novel/"))
    }
}
