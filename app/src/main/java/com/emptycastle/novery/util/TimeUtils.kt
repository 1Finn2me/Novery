package com.emptycastle.novery.util

import java.util.concurrent.TimeUnit

/**
 * Format a timestamp to a relative time string (e.g., "2 hours ago")
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days == 1L) "Yesterday" else "$days days ago"
        }
        diff < TimeUnit.DAYS.toMillis(30) -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
        diff < TimeUnit.DAYS.toMillis(365) -> {
            val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
            if (months == 1L) "1 month ago" else "$months months ago"
        }
        else -> {
            val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
            if (years == 1L) "1 year ago" else "$years years ago"
        }
    }
}