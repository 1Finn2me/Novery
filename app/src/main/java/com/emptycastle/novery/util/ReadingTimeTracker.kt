// com/emptycastle/novery/util/ReadingTimeTracker.kt

package com.emptycastle.novery.util

import com.emptycastle.novery.data.repository.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks reading time and periodically saves to database.
 * Should be created per reading session.
 */
class ReadingTimeTracker(
    private val statsRepository: StatsRepository,
    private val scope: CoroutineScope
) {
    private var trackingJob: Job? = null
    private var currentNovelUrl: String? = null
    private var currentNovelName: String? = null

    private var sessionStartTime: Long = 0
    private var totalSessionSeconds: Long = 0
    private var lastSaveTime: Long = 0

    private val saveIntervalMs = 60_000L // Save every 60 seconds

    /**
     * Start tracking reading time for a novel
     */
    fun startTracking(novelUrl: String, novelName: String) {
        // Stop any existing tracking
        stopTracking()

        currentNovelUrl = novelUrl
        currentNovelName = novelName
        sessionStartTime = System.currentTimeMillis()
        lastSaveTime = sessionStartTime
        totalSessionSeconds = 0

        trackingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(saveIntervalMs)
                saveProgress()
            }
        }
    }

    /**
     * Pause tracking (e.g., when app goes to background)
     */
    fun pauseTracking() {
        trackingJob?.cancel()
        trackingJob = null

        // Save current progress
        saveProgressSync()
    }

    /**
     * Resume tracking after pause
     */
    fun resumeTracking() {
        if (currentNovelUrl != null && trackingJob == null) {
            lastSaveTime = System.currentTimeMillis()

            trackingJob = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(saveIntervalMs)
                    saveProgress()
                }
            }
        }
    }

    /**
     * Stop tracking and save final progress
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null

        // Save final progress
        saveProgressSync()

        currentNovelUrl = null
        currentNovelName = null
        sessionStartTime = 0
        totalSessionSeconds = 0
    }

    /**
     * Get current session duration in seconds
     */
    fun getCurrentSessionSeconds(): Long {
        if (sessionStartTime == 0L) return 0
        val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
        return totalSessionSeconds + elapsed
    }

    private suspend fun saveProgress() {
        val novelUrl = currentNovelUrl ?: return
        val novelName = currentNovelName ?: return

        val now = System.currentTimeMillis()
        val elapsedSinceLastSave = (now - lastSaveTime) / 1000

        if (elapsedSinceLastSave > 0) {
            statsRepository.recordReadingTime(
                novelUrl = novelUrl,
                novelName = novelName,
                durationSeconds = elapsedSinceLastSave
            )

            totalSessionSeconds += elapsedSinceLastSave
            lastSaveTime = now
        }
    }

    private fun saveProgressSync() {
        val novelUrl = currentNovelUrl ?: return
        val novelName = currentNovelName ?: return

        val now = System.currentTimeMillis()
        val elapsedSinceLastSave = (now - lastSaveTime) / 1000

        if (elapsedSinceLastSave > 0) {
            scope.launch(Dispatchers.IO) {
                statsRepository.recordReadingTime(
                    novelUrl = novelUrl,
                    novelName = novelName,
                    durationSeconds = elapsedSinceLastSave
                )
            }

            totalSessionSeconds += elapsedSinceLastSave
            lastSaveTime = now
        }
    }
}