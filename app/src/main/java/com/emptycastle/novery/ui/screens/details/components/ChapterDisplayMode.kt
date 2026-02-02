// com/emptycastle/novery/ui/screens/details/ChapterDisplayModels.kt
package com.emptycastle.novery.ui.screens.details

enum class ChapterDisplayMode {
    SCROLL,
    PAGINATED
}

enum class ChaptersPerPage(val value: Int, val label: String) {
    TWENTY_FIVE(25, "25"),
    FIFTY(50, "50"),
    HUNDRED(100, "100"),
    TWO_HUNDRED(200, "200"),
    ALL(-1, "All");

    companion object {
        fun fromValue(value: Int): ChaptersPerPage =
            entries.find { it.value == value } ?: FIFTY
    }
}

data class PaginationState(
    val currentPage: Int = 1,
    val chaptersPerPage: ChaptersPerPage = ChaptersPerPage.FIFTY
) {
    fun getTotalPages(totalChapters: Int): Int =
        if (chaptersPerPage == ChaptersPerPage.ALL || totalChapters == 0) 1
        else ((totalChapters - 1) / chaptersPerPage.value) + 1

    fun getPageRange(totalChapters: Int): IntRange {
        if (chaptersPerPage == ChaptersPerPage.ALL) return 0 until totalChapters
        val startIndex = (currentPage - 1) * chaptersPerPage.value
        val endIndex = minOf(startIndex + chaptersPerPage.value, totalChapters)
        return startIndex until endIndex
    }

    fun canGoNext(totalChapters: Int): Boolean =
        currentPage < getTotalPages(totalChapters)

    fun canGoPrevious(): Boolean = currentPage > 1

    fun getDisplayRange(totalChapters: Int): Pair<Int, Int> {
        if (chaptersPerPage == ChaptersPerPage.ALL) return 1 to totalChapters
        val start = (currentPage - 1) * chaptersPerPage.value + 1
        val end = minOf(currentPage * chaptersPerPage.value, totalChapters)
        return start to end
    }

    fun getPageForIndex(index: Int): Int {
        if (chaptersPerPage == ChaptersPerPage.ALL) return 1
        return (index / chaptersPerPage.value) + 1
    }
}