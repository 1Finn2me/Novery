package com.emptycastle.novery.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

private const val TAG = "DownloadServiceManager"

/**
 * Enhanced singleton manager for interacting with the DownloadService.
 * Features:
 * - Thread-safe operations
 * - Automatic reconnection
 * - Download queue management
 * - Comprehensive state observation
 * - Multiple download support
 */
object DownloadServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val connectionMutex = Mutex()

    private var service: DownloadService? = null
    private var isBound = false
    private var isBinding = false
    private var bindingContextRef: WeakReference<Context>? = null

    // Pending operations when service not connected
    private val pendingRequests = mutableListOf<DownloadRequest>()
    private var pendingAction: PendingAction? = null

    // Observable state
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private sealed class PendingAction {
        object Pause : PendingAction()
        object Resume : PendingAction()
        object Cancel : PendingAction()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")

            val downloadBinder = binder as? DownloadService.LocalBinder
            if (downloadBinder == null) {
                Log.e(TAG, "Invalid binder received")
                return
            }

            service = downloadBinder.getService()
            isBound = true
            isBinding = false
            _isConnected.value = true

            // Observe service state
            scope.launch {
                service?.downloadState?.collect { state ->
                    _downloadState.value = state
                }
            }

            // Execute pending operations
            scope.launch {
                connectionMutex.withLock {
                    // Process pending requests
                    if (pendingRequests.isNotEmpty()) {
                        Log.d(TAG, "Processing ${pendingRequests.size} pending requests")
                        service?.queueDownloads(pendingRequests.toList())
                        pendingRequests.clear()
                    }

                    // Execute pending action
                    when (pendingAction) {
                        is PendingAction.Pause -> service?.pauseDownload()
                        is PendingAction.Resume -> service?.resumeDownload()
                        is PendingAction.Cancel -> service?.cancelDownload()
                        null -> {}
                    }
                    pendingAction = null
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            service = null
            isBound = false
            _isConnected.value = false
        }
    }

    /**
     * Bind to the download service
     */
    fun bind(context: Context) {
        scope.launch {
            connectionMutex.withLock {
                if (isBound || isBinding) return@launch

                Log.d(TAG, "Binding to service")
                isBinding = true
                bindingContextRef = WeakReference(context.applicationContext)

                try {
                    val intent = Intent(context, DownloadService::class.java)
                    context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                } catch (e: Exception) {
                    Log.e(TAG, "Error binding to service", e)
                    isBinding = false
                }
            }
        }
    }

    /**
     * Unbind from the download service
     */
    fun unbind() {
        scope.launch {
            connectionMutex.withLock {
                if (!isBound) return@launch

                Log.d(TAG, "Unbinding from service")

                try {
                    bindingContextRef?.get()?.unbindService(connection)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unbinding", e)
                }

                service = null
                isBound = false
                isBinding = false
                bindingContextRef = null
                _isConnected.value = false
            }
        }
    }

    /**
     * Ensure connected to service, starting it if needed
     */
    private suspend fun ensureConnected(context: Context): Boolean {
        if (isBound && service != null) return true

        if (!isBinding) {
            DownloadService.start(context)
            bind(context)
        }

        // Wait for connection with timeout
        var attempts = 0
        while (!isBound && attempts < 50) { // 5 second timeout
            delay(100)
            attempts++
        }

        return isBound && service != null
    }

    /**
     * Start downloading chapters for a novel
     */
    fun startDownload(
        context: Context,
        provider: MainProvider,
        novel: Novel,
        chapters: List<Chapter>,
        priority: DownloadPriority = DownloadPriority.NORMAL
    ) {
        if (chapters.isEmpty()) {
            Log.d(TAG, "No chapters to download")
            return
        }

        val request = DownloadRequest(
            novelUrl = novel.url,
            novelName = novel.name,
            novelCoverUrl = novel.posterUrl,
            providerName = provider.name,
            chapterUrls = chapters.map { it.url },
            chapterNames = chapters.map { it.name },
            priority = priority
        )

        startDownload(context, request)
    }

    /**
     * Start downloading with a pre-built request
     */
    fun startDownload(context: Context, request: DownloadRequest) {
        Log.d(TAG, "startDownload: ${request.novelName} (${request.totalChapters} chapters)")

        scope.launch {
            connectionMutex.withLock {
                val connected = ensureConnected(context)

                if (connected && service != null) {
                    service?.startDownload(request)
                } else {
                    Log.d(TAG, "Service not connected, adding to pending")
                    pendingRequests.add(request)
                }
            }
        }
    }

    /**
     * Queue multiple downloads at once
     */
    fun queueDownloads(
        context: Context,
        provider: MainProvider,
        downloads: List<Pair<Novel, List<Chapter>>>
    ) {
        val requests = downloads.mapNotNull { (novel, chapters) ->
            if (chapters.isEmpty()) null
            else DownloadRequest(
                novelUrl = novel.url,
                novelName = novel.name,
                novelCoverUrl = novel.posterUrl,
                providerName = provider.name,
                chapterUrls = chapters.map { it.url },
                chapterNames = chapters.map { it.name }
            )
        }

        if (requests.isEmpty()) return

        scope.launch {
            connectionMutex.withLock {
                val connected = ensureConnected(context)

                if (connected && service != null) {
                    service?.queueDownloads(requests)
                } else {
                    pendingRequests.addAll(requests)
                }
            }
        }
    }

    /**
     * Queue multiple download requests
     */
    fun queueDownloadRequests(context: Context, requests: List<DownloadRequest>) {
        if (requests.isEmpty()) return

        scope.launch {
            connectionMutex.withLock {
                val connected = ensureConnected(context)

                if (connected && service != null) {
                    service?.queueDownloads(requests)
                } else {
                    pendingRequests.addAll(requests)
                }
            }
        }
    }

    /**
     * Remove a novel from the download queue
     */
    fun removeFromQueue(novelUrl: String) {
        service?.removeFromQueue(novelUrl)
        pendingRequests.removeIf { it.novelUrl == novelUrl }
    }

    /**
     * Clear the download queue
     */
    fun clearQueue() {
        service?.clearQueue()
        pendingRequests.clear()
    }

    /**
     * Get the current download queue
     */
    fun getQueue(): List<QueuedDownload> = service?.getQueue() ?: emptyList()

    /**
     * Pause the current download
     */
    fun pauseDownload() {
        scope.launch {
            connectionMutex.withLock {
                if (service != null) {
                    service?.pauseDownload()
                } else {
                    pendingAction = PendingAction.Pause
                }
            }
        }
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload() {
        scope.launch {
            connectionMutex.withLock {
                if (service != null) {
                    service?.resumeDownload()
                } else {
                    pendingAction = PendingAction.Resume
                }
            }
        }
    }

    /**
     * Cancel the current download and clear queue
     */
    fun cancelDownload() {
        scope.launch {
            connectionMutex.withLock {
                pendingRequests.clear()

                if (service != null) {
                    service?.cancelDownload()
                } else {
                    pendingAction = PendingAction.Cancel
                }
            }
        }
    }

    /**
     * Skip current chapter and continue
     */
    fun skipCurrentChapter() {
        service?.skipCurrentChapter()
    }

    // ================================================================
    // STATE QUERIES
    // ================================================================

    fun isDownloading(): Boolean = _downloadState.value.isActive

    fun isDownloadingNovel(novelUrl: String): Boolean {
        val state = _downloadState.value
        return state.isActive && state.novelUrl == novelUrl
    }

    fun isNovelInQueue(novelUrl: String): Boolean {
        return _downloadState.value.queuedDownloads.any { it.novelUrl == novelUrl }
    }

    fun isNovelDownloadingOrQueued(novelUrl: String): Boolean {
        return isDownloadingNovel(novelUrl) || isNovelInQueue(novelUrl)
    }

    fun getProgress(): Float = _downloadState.value.progressPercent

    fun getProgressPercent(): Int = _downloadState.value.progressPercentInt

    fun getETA(): String = _downloadState.value.estimatedTimeRemaining

    fun getSpeed(): String = _downloadState.value.formattedSpeed

    fun isPaused(): Boolean = _downloadState.value.isPaused

    fun hasError(): Boolean = _downloadState.value.hasError

    fun getError(): String? = _downloadState.value.error

    fun getQueueSize(): Int = _downloadState.value.queueSize

    fun clearPending() {
        scope.launch {
            connectionMutex.withLock {
                pendingRequests.clear()
            }
        }
    }
}