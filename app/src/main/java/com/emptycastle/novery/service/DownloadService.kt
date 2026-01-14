package com.emptycastle.novery.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service for downloading chapters in the background.
 * Shows progress notification with pause/resume/cancel controls.
 */
class DownloadService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Repositories
    private val offlineRepository by lazy { RepositoryProvider.getOfflineRepository() }
    private val novelRepository by lazy { RepositoryProvider.getNovelRepository() }

    // State
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Current download info
    private var currentRequest: DownloadRequest? = null
    private var downloadedChapterUrls: Set<String> = emptySet()
    private var pausedAtIndex: Int = 0

    // Broadcast receiver for notification actions
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
        registerActionReceiver()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If service is restarted and no active download, stop
        if (!_downloadState.value.isActive && currentRequest == null) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterActionReceiver()
        releaseWakeLock()
        downloadJob?.cancel()
        serviceScope.cancel()
    }

    // ================================================================
    // BROADCAST RECEIVER
    // ================================================================

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerActionReceiver() {
        val filter = IntentFilter().apply {
            addAction(NotificationHelper.ACTION_DOWNLOAD_PAUSE)
            addAction(NotificationHelper.ACTION_DOWNLOAD_RESUME)
            addAction(NotificationHelper.ACTION_DOWNLOAD_CANCEL)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    private fun unregisterActionReceiver() {
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    // ================================================================
    // WAKE LOCK
    // ================================================================

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Novery:DownloadWakeLock"
        ).apply {
            acquire(60 * 60 * 1000L) // 1 hour max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    // ================================================================
    // DOWNLOAD CONTROL
    // ================================================================

    /**
     * Start a new download
     */
    fun startDownload(request: DownloadRequest) {
        if (_downloadState.value.isActive) {
            // Already downloading - queue or reject
            return
        }

        currentRequest = request
        pausedAtIndex = 0

        _downloadState.value = DownloadState(
            isActive = true,
            isPaused = false,
            novelName = request.novelName,
            novelUrl = request.novelUrl,
            currentChapterName = "Starting...",
            currentProgress = 0,
            totalChapters = request.totalChapters
        )

        startForegroundNotification()
        executeDownload(0)
    }

    /**
     * Pause the current download
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun pauseDownload() {
        if (!_downloadState.value.isActive) return

        downloadJob?.cancel()
        _downloadState.value = _downloadState.value.copy(isPaused = true)
        updateNotification()
    }

    /**
     * Resume a paused download
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun resumeDownload() {
        if (!_downloadState.value.isPaused) return

        _downloadState.value = _downloadState.value.copy(isPaused = false)
        updateNotification()
        executeDownload(pausedAtIndex)
    }

    /**
     * Cancel the current download
     */
    fun cancelDownload() {
        downloadJob?.cancel()

        _downloadState.value = DownloadState()
        currentRequest = null

        NotificationHelper.cancelNotification(this, NotificationHelper.NOTIFICATION_ID_DOWNLOAD)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ================================================================
    // DOWNLOAD EXECUTION
    // ================================================================

    private fun executeDownload(startIndex: Int) {
        val request = currentRequest ?: return

        downloadJob = serviceScope.launch @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) {
            try {
                // Get provider
                val provider = novelRepository.getProvider(request.providerName)
                if (provider == null) {
                    onDownloadError("Provider not found: ${request.providerName}")
                    return@launch
                }

                // Get already downloaded chapters
                downloadedChapterUrls = offlineRepository.getDownloadedChapterUrls(request.novelUrl)

                // Save novel metadata
                val novel = Novel(
                    name = request.novelName,
                    url = request.novelUrl,
                    posterUrl = request.novelCoverUrl,
                    apiName = request.providerName
                )
                offlineRepository.saveNovelMetadata(novel)

                var successCount = _downloadState.value.successCount
                var failedCount = _downloadState.value.failedCount

                for (index in startIndex until request.chapterUrls.size) {
                    // Check if cancelled or paused
                    if (!isActive) {
                        pausedAtIndex = index
                        return@launch
                    }

                    if (_downloadState.value.isPaused) {
                        pausedAtIndex = index
                        return@launch
                    }

                    val chapterUrl = request.chapterUrls[index]
                    val chapterName = request.chapterNames[index]

                    // Update state
                    _downloadState.value = _downloadState.value.copy(
                        currentChapterName = chapterName,
                        currentProgress = index + 1
                    )
                    updateNotification()

                    // Skip if already downloaded
                    if (downloadedChapterUrls.contains(chapterUrl)) {
                        successCount++
                        _downloadState.value = _downloadState.value.copy(successCount = successCount)
                        continue
                    }

                    // Download chapter
                    try {
                        val content = provider.loadChapterContent(chapterUrl)

                        if (content != null) {
                            offlineRepository.saveChapter(
                                chapterUrl = chapterUrl,
                                novelUrl = request.novelUrl,
                                title = chapterName,
                                content = content
                            )
                            successCount++
                        } else {
                            failedCount++
                        }
                    } catch (e: Exception) {
                        failedCount++
                        e.printStackTrace()
                    }

                    _downloadState.value = _downloadState.value.copy(
                        successCount = successCount,
                        failedCount = failedCount
                    )

                    // Small delay to avoid rate limiting
                    delay(250)
                }

                // Download complete
                onDownloadComplete(successCount, failedCount)

            } catch (e: Exception) {
                if (isActive) {
                    onDownloadError(e.message ?: "Download failed")
                }
            }
        }
    }

    private fun onDownloadComplete(successCount: Int, failedCount: Int) {
        val request = currentRequest ?: return

        // Show completion notification
        val notification = NotificationHelper.buildDownloadCompleteNotification(
            context = this,
            novelName = request.novelName,
            chaptersDownloaded = successCount,
            totalChapters = request.totalChapters
        )

        NotificationHelper.getNotificationManager(this).notify(
            NotificationHelper.NOTIFICATION_ID_DOWNLOAD_COMPLETE,
            notification
        )

        // Reset state
        _downloadState.value = DownloadState()
        currentRequest = null

        // Stop service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onDownloadError(message: String) {
        val request = currentRequest

        _downloadState.value = _downloadState.value.copy(
            isActive = false,
            error = message
        )

        // Show error notification
        if (request != null) {
            val notification = NotificationHelper.buildDownloadErrorNotification(
                context = this,
                novelName = request.novelName,
                errorMessage = message
            )

            NotificationHelper.getNotificationManager(this).notify(
                NotificationHelper.NOTIFICATION_ID_DOWNLOAD_COMPLETE,
                notification
            )
        }

        currentRequest = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ================================================================
    // NOTIFICATIONS
    // ================================================================

    private fun startForegroundNotification() {
        val state = _downloadState.value

        val notification = NotificationHelper.buildDownloadProgressNotification(
            context = this,
            novelName = state.novelName,
            chapterName = state.currentChapterName,
            progress = state.currentProgress,
            total = state.totalChapters,
            isPaused = state.isPaused
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_DOWNLOAD,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_DOWNLOAD, notification)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification() {
        val state = _downloadState.value

        val notification = NotificationHelper.buildDownloadProgressNotification(
            context = this,
            novelName = state.novelName,
            chapterName = state.currentChapterName,
            progress = state.currentProgress,
            total = state.totalChapters,
            isPaused = state.isPaused
        )

        NotificationManagerCompat.from(this).notify(
            NotificationHelper.NOTIFICATION_ID_DOWNLOAD,
            notification
        )
    }

    // ================================================================
    // COMPANION
    // ================================================================

    companion object {
        /**
         * Start the download service
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}