package com.emptycastle.novery.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.util.NotificationImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "DownloadService"
private const val EXTRA_ACTION = "action"
private const val ACTION_START = "start"
private const val IDLE_TIMEOUT_MS = 10000L // 10 seconds to wait for download request

class DownloadService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var notificationJob: Job? = null
    private var idleTimeoutJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val stateMutex = Mutex()

    private val offlineRepository by lazy { RepositoryProvider.getOfflineRepository() }
    private val novelRepository by lazy { RepositoryProvider.getNovelRepository() }

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Use Deque for priority insertion
    private val downloadQueue = ConcurrentLinkedDeque<DownloadRequest>()

    private var currentRequest: DownloadRequest? = null
    private var downloadedChapterUrls: Set<String> = emptySet()
    private var pausedAtIndex: Int = 0
    private var failedChapters = mutableListOf<Pair<Int, String>>()

    private var cachedCoverBitmap: Bitmap? = null

    // Track if foreground was started
    private val isForegroundStarted = AtomicBoolean(false)
    private val hasReceivedWork = AtomicBoolean(false)

    // Current notification ID - unique per novel
    private var currentNotificationId: Int = NotificationHelper.NOTIFICATION_ID_PREPARING

    // Speed calculation
    private var lastSpeedCalculationTime = 0L
    private var lastBytesDownloaded = 0L
    private val speedHistory = mutableListOf<Long>()
    private val maxSpeedHistorySize = 5

    // Notification throttling
    private var lastNotificationUpdateTime = 0L
    private val minNotificationUpdateInterval = 500L

    private val actionReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NotificationHelper.ACTION_DOWNLOAD_PAUSE -> pauseDownload()
                NotificationHelper.ACTION_DOWNLOAD_RESUME -> resumeDownload()
                NotificationHelper.ACTION_DOWNLOAD_CANCEL -> cancelDownload()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        registerActionReceiver()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called, isForegroundStarted=${isForegroundStarted.get()}")

        // CRITICAL: Must start foreground immediately when started via startForegroundService
        if (!isForegroundStarted.getAndSet(true)) {
            startInitialForeground()
        }

        // Start idle timeout - if no work arrives, stop the service
        startIdleTimeout()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        unregisterActionReceiver()
        releaseWakeLock()
        downloadJob?.cancel()
        notificationJob?.cancel()
        idleTimeoutJob?.cancel()
        serviceScope.cancel()
        cachedCoverBitmap = null
    }

    /**
     * Start foreground with a "preparing" notification immediately
     * This is required when started via startForegroundService()
     */
    private fun startInitialForeground() {
        Log.d(TAG, "Starting initial foreground notification")

        val notification = NotificationHelper.buildPreparingNotification(this)
        currentNotificationId = NotificationHelper.NOTIFICATION_ID_PREPARING

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    currentNotificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(currentNotificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground", e)
        }
    }

    /**
     * Start a timeout - if no work arrives within the timeout, stop the service
     */
    private fun startIdleTimeout() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = serviceScope.launch {
            delay(IDLE_TIMEOUT_MS)

            if (!hasReceivedWork.get() && !_downloadState.value.isActive && downloadQueue.isEmpty()) {
                Log.d(TAG, "Idle timeout reached, stopping service")
                withContext(Dispatchers.Main) {
                    safeStopSelf()
                }
            }
        }
    }

    private fun cancelIdleTimeout() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
    }

    private fun safeStopSelf() {
        try {
            // Cancel the current notification
            NotificationHelper.cancelNotification(this, currentNotificationId)
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground", e)
        }
        stopSelf()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerActionReceiver() {
        val filter = IntentFilter().apply {
            addAction(NotificationHelper.ACTION_DOWNLOAD_PAUSE)
            addAction(NotificationHelper.ACTION_DOWNLOAD_RESUME)
            addAction(NotificationHelper.ACTION_DOWNLOAD_CANCEL)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(actionReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
    }

    private fun unregisterActionReceiver() {
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver not registered")
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Novery:DownloadWakeLock"
            ).apply {
                acquire(2 * 60 * 60 * 1000L) // 2 hours max
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
        wakeLock = null
    }

    // ================================================================
    // QUEUE MANAGEMENT
    // ================================================================

    /**
     * Start or queue a download request
     */
    fun startDownload(request: DownloadRequest) {
        Log.d(TAG, "startDownload: ${request.novelName} (${request.totalChapters} chapters)")

        // Mark that we've received work
        hasReceivedWork.set(true)
        cancelIdleTimeout()

        serviceScope.launch {
            stateMutex.withLock {
                if (_downloadState.value.isActive) {
                    // Add to queue based on priority
                    addToQueue(request)
                    updateQueueState()
                    Log.d(TAG, "Added to queue. Queue size: ${downloadQueue.size}")
                } else {
                    beginDownload(request)
                }
            }
        }
    }

    /**
     * Add to queue with priority support
     */
    private fun addToQueue(request: DownloadRequest) {
        when (request.priority) {
            DownloadPriority.HIGH -> downloadQueue.addFirst(request)
            DownloadPriority.NORMAL -> downloadQueue.addLast(request)
            DownloadPriority.LOW -> downloadQueue.addLast(request)
        }
    }

    /**
     * Queue multiple downloads at once
     */
    fun queueDownloads(requests: List<DownloadRequest>) {
        if (requests.isEmpty()) return

        Log.d(TAG, "queueDownloads: ${requests.size} requests")

        // Mark that we've received work
        hasReceivedWork.set(true)
        cancelIdleTimeout()

        serviceScope.launch {
            stateMutex.withLock {
                // Sort by priority
                val sorted = requests.sortedByDescending { it.priority }

                if (!_downloadState.value.isActive) {
                    // Start first one immediately
                    val first = sorted.firstOrNull()
                    if (first != null) {
                        // Add rest to queue
                        sorted.drop(1).forEach { addToQueue(it) }
                        beginDownload(first)
                    }
                } else {
                    // All go to queue
                    sorted.forEach { addToQueue(it) }
                    updateQueueState()
                }
            }
        }
    }

    /**
     * Remove a specific item from the queue
     */
    fun removeFromQueue(novelUrl: String) {
        downloadQueue.removeIf { it.novelUrl == novelUrl }
        updateQueueState()
    }

    /**
     * Clear the entire queue
     */
    fun clearQueue() {
        downloadQueue.clear()
        updateQueueState()
    }

    /**
     * Get current queue as list
     */
    fun getQueue(): List<QueuedDownload> {
        return downloadQueue.map { it.toQueuedDownload() }
    }

    fun getQueueSize(): Int = downloadQueue.size

    /**
     * Update state with current queue info
     */
    private fun updateQueueState() {
        val queuedDownloads = downloadQueue.map { it.toQueuedDownload() }
        val totalQueuedChapters = downloadQueue.sumOf { it.totalChapters }

        _downloadState.update {
            it.copy(
                queuedDownloads = queuedDownloads,
                totalQueuedChapters = totalQueuedChapters
            )
        }
    }

    // ================================================================
    // DOWNLOAD EXECUTION
    // ================================================================

    private fun beginDownload(request: DownloadRequest) {
        Log.d(TAG, "beginDownload: ${request.novelName}")

        // Generate unique notification ID for this novel
        val newNotificationId = NotificationHelper.getProgressNotificationId(request.novelUrl)
        val previousNotificationId = currentNotificationId

        currentRequest = request
        currentNotificationId = newNotificationId
        pausedAtIndex = 0
        failedChapters.clear()
        speedHistory.clear()
        lastSpeedCalculationTime = System.currentTimeMillis()
        lastBytesDownloaded = 0
        lastNotificationUpdateTime = 0
        cachedCoverBitmap = null

        // Set initial state
        _downloadState.value = DownloadState(
            isActive = true,
            isPaused = false,
            novelName = request.novelName,
            novelUrl = request.novelUrl,
            novelCoverUrl = request.novelCoverUrl,
            currentChapterName = "Starting...",
            currentProgress = 0,
            totalChapters = request.totalChapters,
            startTimeMillis = System.currentTimeMillis(),
            queuedDownloads = downloadQueue.map { it.toQueuedDownload() },
            totalQueuedChapters = downloadQueue.sumOf { it.totalChapters }
        )

        acquireWakeLock()
        startPeriodicNotificationUpdates()

        serviceScope.launch {
            // Preload cover
            preloadCoverImage(request.novelCoverUrl)

            // Update notification with actual download info
            withContext(Dispatchers.Main) {
                // Cancel previous notification if different
                if (previousNotificationId != newNotificationId) {
                    NotificationHelper.cancelNotification(this@DownloadService, previousNotificationId)
                }
                updateForegroundNotification()
            }

            // Execute download
            executeDownload(0)
        }
    }

    /**
     * Update the foreground notification with the current novel's unique ID
     */
    private fun updateForegroundNotification() {
        val state = _downloadState.value
        val notification = NotificationHelper.buildDownloadProgressNotification(
            context = this,
            state = state,
            coverBitmap = cachedCoverBitmap
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    currentNotificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(currentNotificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating foreground notification", e)
        }
    }

    private fun startPeriodicNotificationUpdates() {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                val state = _downloadState.value
                if (state.isActive && !state.isPaused) {
                    updateNotificationIfNeeded(force = false)
                }
                delay(1000)
            }
        }
    }

    private fun stopPeriodicNotificationUpdates() {
        notificationJob?.cancel()
        notificationJob = null
    }

    private suspend fun preloadCoverImage(coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) return

        try {
            val bitmap = NotificationImageLoader.loadImage(
                context = this@DownloadService,
                imageUrl = coverUrl,
                rounded = true
            )
            cachedCoverBitmap = bitmap
            _downloadState.update { it.copy(novelCoverBitmap = bitmap) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cover image", e)
        }
    }

    private fun processNextInQueue() {
        Log.d(TAG, "processNextInQueue, queue size: ${downloadQueue.size}")

        stopPeriodicNotificationUpdates()

        val nextRequest = downloadQueue.poll()
        if (nextRequest != null) {
            beginDownload(nextRequest)
        } else {
            // All done
            Log.d(TAG, "Download queue empty, stopping service")
            cachedCoverBitmap = null
            releaseWakeLock()

            // Reset state
            _downloadState.value = DownloadState()
            currentRequest = null

            safeStopSelf()
        }
    }

    // ================================================================
    // PAUSE / RESUME / CANCEL
    // ================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun pauseDownload() {
        if (!_downloadState.value.isActive || _downloadState.value.isPaused) return

        Log.d(TAG, "pauseDownload")
        downloadJob?.cancel()
        _downloadState.update { it.copy(isPaused = true) }

        serviceScope.launch(Dispatchers.Main) {
            updateNotificationIfNeeded(force = true)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun resumeDownload() {
        if (!_downloadState.value.isPaused) return

        Log.d(TAG, "resumeDownload from index $pausedAtIndex")

        _downloadState.update {
            it.copy(
                isPaused = false,
                startTimeMillis = System.currentTimeMillis()
            )
        }

        serviceScope.launch(Dispatchers.Main) {
            updateNotificationIfNeeded(force = true)
        }

        executeDownload(pausedAtIndex)
    }

    fun cancelDownload() {
        Log.d(TAG, "cancelDownload")

        downloadJob?.cancel()
        stopPeriodicNotificationUpdates()
        downloadQueue.clear()

        _downloadState.value = DownloadState()
        currentRequest = null
        cachedCoverBitmap = null

        try {
            // Cancel the current progress notification
            NotificationHelper.cancelNotification(this, currentNotificationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification", e)
        }

        releaseWakeLock()
        safeStopSelf()
    }

    fun skipCurrentChapter() {
        pausedAtIndex++
        if (_downloadState.value.isPaused) {
            resumeDownload()
        }
    }

    // ================================================================
    // DOWNLOAD LOGIC
    // ================================================================

    private fun executeDownload(startIndex: Int) {
        val request = currentRequest ?: return

        downloadJob = serviceScope.launch {
            try {
                val provider = novelRepository.getProvider(request.providerName)
                if (provider == null) {
                    onDownloadError("Provider not found: ${request.providerName}")
                    return@launch
                }

                // Get already downloaded chapters
                downloadedChapterUrls = try {
                    offlineRepository.getDownloadedChapterUrls(request.novelUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting downloaded URLs", e)
                    emptySet()
                }

                // Save novel metadata
                val novel = Novel(
                    name = request.novelName,
                    url = request.novelUrl,
                    posterUrl = request.novelCoverUrl,
                    apiName = request.providerName
                )

                try {
                    offlineRepository.saveNovelMetadata(novel)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving novel metadata", e)
                }

                var successCount = _downloadState.value.successCount
                var failedCount = _downloadState.value.failedCount
                var skippedCount = _downloadState.value.skippedCount
                var totalBytes = _downloadState.value.bytesDownloaded

                for (index in startIndex until request.chapterUrls.size) {
                    // Check if coroutine is still active
                    if (!isActive) {
                        pausedAtIndex = index
                        return@launch
                    }

                    // Check if paused
                    if (_downloadState.value.isPaused) {
                        pausedAtIndex = index
                        return@launch
                    }

                    val chapterUrl = request.chapterUrls.getOrNull(index) ?: continue
                    val chapterName = request.chapterNames.getOrNull(index) ?: "Chapter ${index + 1}"

                    // Update state - processing
                    updateProgress(
                        chapterName = chapterName,
                        progress = index,
                        successCount = successCount,
                        failedCount = failedCount,
                        skippedCount = skippedCount,
                        bytesDownloaded = totalBytes,
                        isProcessing = true
                    )

                    // Skip if already downloaded
                    if (downloadedChapterUrls.contains(chapterUrl)) {
                        skippedCount++
                        updateProgress(
                            chapterName = chapterName,
                            progress = index + 1,
                            successCount = successCount,
                            failedCount = failedCount,
                            skippedCount = skippedCount,
                            bytesDownloaded = totalBytes,
                            isProcessing = false
                        )
                        continue
                    }

                    // Download with retry
                    val result = downloadChapterWithRetry(
                        chapterUrl = chapterUrl,
                        chapterName = chapterName,
                        novelUrl = request.novelUrl,
                        provider = provider,
                        maxRetries = if (request.retryOnFailure) request.maxRetries else 0
                    )

                    when (result) {
                        is ChapterDownloadResult.Success -> {
                            successCount++
                            totalBytes += result.bytesDownloaded
                        }
                        is ChapterDownloadResult.Failed -> {
                            failedCount++
                            failedChapters.add(index to chapterUrl)
                        }
                        is ChapterDownloadResult.Skipped -> {
                            skippedCount++
                        }
                    }

                    // Update state - completed
                    updateProgress(
                        chapterName = chapterName,
                        progress = index + 1,
                        successCount = successCount,
                        failedCount = failedCount,
                        skippedCount = skippedCount,
                        bytesDownloaded = totalBytes,
                        isProcessing = false
                    )

                    // Adaptive delay
                    delay(getAdaptiveDelay(failedCount))
                }

                // Download complete
                onDownloadComplete(
                    DownloadResult(
                        novelUrl = request.novelUrl,
                        novelName = request.novelName,
                        novelCoverUrl = request.novelCoverUrl,
                        successCount = successCount,
                        failedCount = failedCount,
                        skippedCount = skippedCount,
                        totalChapters = request.totalChapters,
                        elapsedTimeMs = System.currentTimeMillis() - _downloadState.value.startTimeMillis,
                        bytesDownloaded = totalBytes
                    )
                )

            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                if (isActive) {
                    onDownloadError(e.message ?: "Download failed unexpectedly")
                }
            }
        }
    }

    private suspend fun downloadChapterWithRetry(
        chapterUrl: String,
        chapterName: String,
        novelUrl: String,
        provider: com.emptycastle.novery.provider.MainProvider,
        maxRetries: Int
    ): ChapterDownloadResult {
        var lastError: String? = null
        var retryCount = 0

        while (retryCount <= maxRetries) {
            try {
                val content = withContext(Dispatchers.IO) {
                    provider.loadChapterContent(chapterUrl)
                }

                if (!content.isNullOrBlank()) {
                    offlineRepository.saveChapter(
                        chapterUrl = chapterUrl,
                        novelUrl = novelUrl,
                        title = chapterName,
                        content = content
                    )
                    return ChapterDownloadResult.Success(
                        chapterUrl = chapterUrl,
                        bytesDownloaded = content.length.toLong() * 2 // UTF-16
                    )
                } else {
                    lastError = "Empty content received"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"

                if (!isRetryableError(e)) {
                    return ChapterDownloadResult.Failed(
                        chapterUrl = chapterUrl,
                        error = lastError,
                        retryable = false
                    )
                }
            }

            if (retryCount < maxRetries) {
                val delayMs = (1000L * (1 shl retryCount)).coerceAtMost(30000L)
                _downloadState.update { it.copy(retryCount = retryCount + 1) }
                delay(delayMs)
            }

            retryCount++
        }

        return ChapterDownloadResult.Failed(
            chapterUrl = chapterUrl,
            error = lastError ?: "Max retries exceeded",
            retryable = true
        )
    }

    private fun isRetryableError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return when {
            e is java.net.SocketTimeoutException -> true
            e is java.net.UnknownHostException -> true
            e is java.io.IOException -> true
            message.contains("timeout") -> true
            message.contains("connection") -> true
            message.contains("reset") -> true
            message.contains("429") -> true
            message.contains("503") -> true
            else -> false
        }
    }

    private fun getAdaptiveDelay(failedCount: Int): Long {
        return when {
            failedCount > 10 -> 1000L
            failedCount > 5 -> 500L
            failedCount > 2 -> 350L
            else -> 200L
        }
    }

    // ================================================================
    // STATE UPDATES
    // ================================================================

    private suspend fun updateProgress(
        chapterName: String,
        progress: Int,
        successCount: Int,
        failedCount: Int,
        skippedCount: Int,
        bytesDownloaded: Long,
        isProcessing: Boolean = false
    ) {
        stateMutex.withLock {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastSpeedCalculationTime

            val currentSpeed = if (timeDiff > 500) {
                val bytesDiff = bytesDownloaded - lastBytesDownloaded
                val speed = (bytesDiff * 1000 / timeDiff.coerceAtLeast(1))

                lastSpeedCalculationTime = now
                lastBytesDownloaded = bytesDownloaded

                speedHistory.add(speed)
                if (speedHistory.size > maxSpeedHistorySize) {
                    speedHistory.removeAt(0)
                }

                speedHistory.average().toLong()
            } else {
                _downloadState.value.downloadSpeed
            }

            _downloadState.update {
                it.copy(
                    currentChapterName = if (isProcessing) "Downloading: $chapterName" else chapterName,
                    currentProgress = progress,
                    successCount = successCount,
                    failedCount = failedCount,
                    skippedCount = skippedCount,
                    bytesDownloaded = bytesDownloaded,
                    downloadSpeed = currentSpeed,
                    retryCount = 0,
                    novelCoverBitmap = cachedCoverBitmap
                )
            }
        }

        // Update notification
        if (shouldUpdateNotification(progress)) {
            withContext(Dispatchers.Main) {
                updateNotificationIfNeeded(force = true)
            }
        }
    }

    private fun shouldUpdateNotification(progress: Int): Boolean {
        val total = _downloadState.value.totalChapters
        if (total == 0) return false

        // Always update on first, last, and milestones
        if (progress == 0 || progress == 1 || progress == total) return true

        val percentComplete = (progress * 100) / total
        val previousPercent = ((progress - 1) * 100) / total

        return when {
            total <= 20 -> true
            total <= 50 -> progress % 2 == 0
            total <= 100 -> progress % 5 == 0 || percentComplete / 5 > previousPercent / 5
            else -> progress % 10 == 0 || percentComplete / 5 > previousPercent / 5
        }
    }

    // ================================================================
    // COMPLETION HANDLERS
    // ================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun onDownloadComplete(result: DownloadResult) {
        Log.d(TAG, "onDownloadComplete: ${result.novelName}, success=${result.successCount}")

        stopPeriodicNotificationUpdates()

        // Get the progress notification ID for this novel to cancel it
        val progressNotificationId = NotificationHelper.getProgressNotificationId(result.novelUrl)

        serviceScope.launch {
            try {
                // Cancel the progress notification first
                NotificationHelper.cancelNotification(this@DownloadService, progressNotificationId)

                val notification = NotificationHelper.buildDownloadCompleteNotification(
                    context = this@DownloadService,
                    novelName = result.novelName,
                    novelCoverUrl = result.novelCoverUrl,
                    chaptersDownloaded = result.successCount,
                    totalChapters = result.totalChapters,
                    failedCount = result.failedCount,
                    elapsedTimeMs = result.elapsedTimeMs,
                    queueRemaining = downloadQueue.size
                )

                // Use a unique notification ID for completion based on novel URL
                val completeNotificationId = NotificationHelper.getCompleteNotificationId(result.novelUrl)

                NotificationHelper.getNotificationManager(this@DownloadService).notify(
                    completeNotificationId,
                    notification
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error showing completion notification", e)
            }

            // Reset current state
            _downloadState.value = DownloadState(
                queuedDownloads = downloadQueue.map { it.toQueuedDownload() },
                totalQueuedChapters = downloadQueue.sumOf { it.totalChapters }
            )
            currentRequest = null
            cachedCoverBitmap = null

            // Process next in queue
            processNextInQueue()
        }
    }

    private fun onDownloadError(message: String) {
        Log.e(TAG, "onDownloadError: $message")

        stopPeriodicNotificationUpdates()
        val request = currentRequest

        _downloadState.update {
            it.copy(
                isActive = false,
                error = message
            )
        }

        if (request != null) {
            // Cancel the progress notification
            val progressNotificationId = NotificationHelper.getProgressNotificationId(request.novelUrl)
            NotificationHelper.cancelNotification(this, progressNotificationId)

            serviceScope.launch {
                try {
                    val notification = NotificationHelper.buildDownloadErrorNotification(
                        context = this@DownloadService,
                        novelName = request.novelName,
                        novelCoverUrl = request.novelCoverUrl,
                        errorMessage = message,
                        chaptersCompleted = _downloadState.value.successCount,
                        totalChapters = request.totalChapters
                    )

                    // Use unique error notification ID
                    val errorNotificationId = NotificationHelper.getErrorNotificationId(request.novelUrl)

                    NotificationHelper.getNotificationManager(this@DownloadService).notify(
                        errorNotificationId,
                        notification
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing error notification", e)
                }
            }
        }

        currentRequest = null
        cachedCoverBitmap = null
        processNextInQueue()
    }

    // ================================================================
    // NOTIFICATION
    // ================================================================

    private fun updateDownloadNotification() {
        val state = _downloadState.value

        val notification = NotificationHelper.buildDownloadProgressNotification(
            context = this,
            state = state,
            coverBitmap = cachedCoverBitmap
        )

        try {
            NotificationManagerCompat.from(this).notify(
                currentNotificationId,
                notification
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotificationIfNeeded(force: Boolean) {
        val now = System.currentTimeMillis()

        if (!force && (now - lastNotificationUpdateTime) < minNotificationUpdateInterval) {
            return
        }

        lastNotificationUpdateTime = now

        val state = _downloadState.value
        if (!state.isActive && !state.isPaused) return

        val notification = NotificationHelper.buildDownloadProgressNotification(
            context = this,
            state = state,
            coverBitmap = cachedCoverBitmap
        )

        try {
            NotificationManagerCompat.from(this).notify(
                currentNotificationId,
                notification
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted")
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service", e)
            }
        }
    }
}