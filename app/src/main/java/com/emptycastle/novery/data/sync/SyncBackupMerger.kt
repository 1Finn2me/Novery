package com.emptycastle.novery.data.sync

import com.emptycastle.novery.data.backup.AppSettingsBackup
import com.emptycastle.novery.data.backup.BackupData
import com.emptycastle.novery.data.backup.BookmarkBackup
import com.emptycastle.novery.data.backup.HistoryBackup
import com.emptycastle.novery.data.backup.LibraryBackup
import com.emptycastle.novery.data.backup.ReadChapterBackup
import com.emptycastle.novery.data.backup.ReaderSettingsBackup
import com.emptycastle.novery.data.backup.ReadingStatsBackup
import com.emptycastle.novery.data.backup.ReadingStreakBackup

/**
 * Merges local and remote backup snapshots into a single sync-safe payload.
 */
object SyncBackupMerger {

    fun merge(local: BackupData, remote: BackupData): BackupData {
        return BackupData(
            version = maxOf(local.version, remote.version),
            createdAt = maxOf(local.createdAt, remote.createdAt),
            appVersion = local.appVersion.ifBlank { remote.appVersion },
            deviceInfo = local.deviceInfo.ifBlank { remote.deviceInfo },
            library = mergeLibrary(local.library, remote.library),
            bookmarks = mergeBookmarks(local.bookmarks, remote.bookmarks),
            history = mergeHistory(local.history, remote.history),
            readChapters = mergeReadChapters(local.readChapters, remote.readChapters),
            readingStats = mergeReadingStats(local.readingStats, remote.readingStats),
            readingStreak = mergeReadingStreak(local.readingStreak, remote.readingStreak),
            appSettings = mergeAppSettings(local.appSettings, remote.appSettings),
            readerSettings = mergeReaderSettings(local.readerSettings, remote.readerSettings)
        )
    }

    private fun mergeLibrary(
        local: List<LibraryBackup>,
        remote: List<LibraryBackup>
    ): List<LibraryBackup> {
        return (local + remote)
            .groupBy { it.url }
            .map { (_, entries) ->
                val newestRead = entries.maxByOrNull { it.lastReadAt ?: 0L } ?: entries.first()
                val newestUpdate = entries.maxByOrNull { it.lastUpdatedAt }
                val newestMeta = entries.maxByOrNull { libraryTimestamp(it) } ?: entries.first()

                newestMeta.copy(
                    name = newestMeta.name.ifBlank { newestRead.name },
                    posterUrl = newestMeta.posterUrl ?: newestRead.posterUrl,
                    latestChapter = newestUpdate?.latestChapter ?: newestMeta.latestChapter,
                    lastChapterUrl = newestRead.lastChapterUrl,
                    lastChapterName = newestRead.lastChapterName,
                    lastReadAt = newestRead.lastReadAt,
                    lastScrollIndex = newestRead.lastScrollIndex,
                    lastScrollOffset = newestRead.lastScrollOffset,
                    totalChapterCount = entries.maxOf { it.totalChapterCount },
                    acknowledgedChapterCount = entries.maxOf { it.acknowledgedChapterCount },
                    lastCheckedAt = entries.maxOf { it.lastCheckedAt },
                    lastUpdatedAt = entries.maxOf { it.lastUpdatedAt },
                    lastReadChapterIndex = entries.maxOf { it.lastReadChapterIndex },
                    unreadChapterCount = newestRead.unreadChapterCount.coerceAtLeast(0)
                )
            }
            .sortedByDescending { libraryTimestamp(it) }
    }

    private fun mergeBookmarks(
        local: List<BookmarkBackup>,
        remote: List<BookmarkBackup>
    ): List<BookmarkBackup> {
        return (local + remote)
            .groupBy { bookmarkKey(it) }
            .map { (_, entries) ->
                entries.maxByOrNull { it.updatedAt } ?: entries.first()
            }
            .sortedByDescending { it.updatedAt }
    }

    private fun mergeHistory(
        local: List<HistoryBackup>,
        remote: List<HistoryBackup>
    ): List<HistoryBackup> {
        return (local + remote)
            .groupBy { it.novelUrl }
            .map { (_, entries) ->
                entries.maxByOrNull { it.timestamp } ?: entries.first()
            }
            .sortedByDescending { it.timestamp }
    }

    private fun mergeReadChapters(
        local: List<ReadChapterBackup>,
        remote: List<ReadChapterBackup>
    ): List<ReadChapterBackup> {
        return (local + remote)
            .groupBy { it.chapterUrl }
            .map { (_, entries) ->
                entries.maxByOrNull { it.readAt } ?: entries.first()
            }
            .sortedByDescending { it.readAt }
    }

    private fun mergeReadingStats(
        local: List<ReadingStatsBackup>,
        remote: List<ReadingStatsBackup>
    ): List<ReadingStatsBackup> {
        return (local + remote)
            .groupBy { "${it.novelUrl}|${it.date}" }
            .map { (_, entries) ->
                val newest = entries.maxByOrNull { it.updatedAt } ?: entries.first()
                newest.copy(
                    readingTimeSeconds = entries.maxOf { it.readingTimeSeconds },
                    chaptersRead = entries.maxOf { it.chaptersRead },
                    wordsRead = entries.maxOf { it.wordsRead },
                    sessionsCount = entries.maxOf { it.sessionsCount },
                    longestSessionSeconds = entries.maxOf { it.longestSessionSeconds },
                    createdAt = entries.minOf { it.createdAt },
                    updatedAt = entries.maxOf { it.updatedAt }
                )
            }
            .sortedByDescending { it.updatedAt }
    }

    private fun mergeReadingStreak(
        local: ReadingStreakBackup?,
        remote: ReadingStreakBackup?
    ): ReadingStreakBackup? {
        return when {
            local == null -> remote
            remote == null -> local
            else -> {
                val newest = if (local.updatedAt >= remote.updatedAt) local else remote
                newest.copy(
                    currentStreak = maxOf(local.currentStreak, remote.currentStreak),
                    longestStreak = maxOf(local.longestStreak, remote.longestStreak),
                    lastReadDate = maxOf(local.lastReadDate, remote.lastReadDate),
                    totalDaysRead = maxOf(local.totalDaysRead, remote.totalDaysRead),
                    totalReadingTimeSeconds = maxOf(
                        local.totalReadingTimeSeconds,
                        remote.totalReadingTimeSeconds
                    ),
                    updatedAt = maxOf(local.updatedAt, remote.updatedAt)
                )
            }
        }
    }

    private fun mergeAppSettings(
        local: AppSettingsBackup?,
        remote: AppSettingsBackup?
    ): AppSettingsBackup? {
        return pickNewest(local, remote) { it.updatedAt }
    }

    private fun mergeReaderSettings(
        local: ReaderSettingsBackup?,
        remote: ReaderSettingsBackup?
    ): ReaderSettingsBackup? {
        return pickNewest(local, remote) { it.updatedAt }
    }

    private fun bookmarkKey(bookmark: BookmarkBackup): String {
        val segment = bookmark.segmentId ?: bookmark.textSnippet.orEmpty()
        return buildString {
            append(bookmark.novelUrl)
            append('|')
            append(bookmark.chapterUrl)
            append('|')
            append(segment)
            append('|')
            append(bookmark.segmentIndex)
            append('|')
            append(bookmark.category)
        }
    }

    private fun libraryTimestamp(entry: LibraryBackup): Long {
        return maxOf(
            entry.addedAt,
            entry.lastReadAt ?: 0L,
            entry.lastCheckedAt,
            entry.lastUpdatedAt
        )
    }

    private fun <T> pickNewest(
        local: T?,
        remote: T?,
        timestamp: (T) -> Long
    ): T? {
        return when {
            local == null -> remote
            remote == null -> local
            timestamp(local) >= timestamp(remote) -> local
            else -> remote
        }
    }
}
