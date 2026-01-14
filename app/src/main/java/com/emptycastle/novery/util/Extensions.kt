package com.emptycastle.novery.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Fix relative URLs to absolute URLs
 */
fun String?.fixUrl(baseUrl: String): String? {
    if (this == null) return null
    if (this.isBlank()) return null

    return when {
        this.startsWith("http") -> this
        this.startsWith("//") -> "https:$this"
        this.startsWith("/") -> "$baseUrl$this"
        else -> "$baseUrl/$this"
    }
}

/**
 * Remove http/https prefix from URL
 */
fun String.removeHttpPrefix(): String {
    return this.removePrefix("https://").removePrefix("http://")
}

/**
 * Format timestamp to readable date
 */
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format timestamp to relative time (e.g., "2 hours ago")
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
        else -> "Just now"
    }
}

/**
 * Truncate string with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) this
    else this.take(maxLength - 3) + "..."
}