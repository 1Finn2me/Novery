package com.emptycastle.novery.domain.model

/**
 * Display options for imported EPUB books in library
 */
enum class ImportedBooksDisplay(val id: String) {
    /**
     * Option A: EPUBs appear alongside online novels in the same list
     * Uses "Local" provider name with a badge
     */
    MIXED("mixed"),

    /**
     * Option B: Separate filter - adds "IMPORTED" to filter chips
     * EPUBs only visible when this filter is active
     */
    FILTER("filter"),

    /**
     * Option C: Separate collapsible section at top of library
     */
    SECTION("section");

    fun displayName(): String = when (this) {
        MIXED -> "Mixed with Online"
        FILTER -> "Separate Filter"
        SECTION -> "Separate Section"
    }

    fun description(): String = when (this) {
        MIXED -> "Imported books appear alongside online novels with a 'Local' badge"
        FILTER -> "Imported books have their own filter tab in the library"
        SECTION -> "Imported books appear in a collapsible section at the top"
    }

    companion object {
        fun fromId(id: String): ImportedBooksDisplay {
            return entries.find { it.id == id } ?: MIXED
        }
    }
}