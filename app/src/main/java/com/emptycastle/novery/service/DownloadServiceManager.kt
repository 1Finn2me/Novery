package com.emptycastle.novery.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton manager for interacting with the DownloadService.
 * Provides easy access to download state and control methods.
 */
object DownloadServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var service: DownloadService? = null
    private var isBound = false
    private var bindingContext: Context? = null

    // Pending download request (if service not yet bound)
    private var pendingRequest: DownloadRequest? = null

    // Observable state
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val downloadBinder = binder as DownloadService.LocalBinder
            service = downloadBinder.getService()
            isBound = true
            _isConnected.value = true

            // Observe service state
            scope.launch {
                service?.downloadState?.collect { state ->
                    _downloadState.value = state
                }
            }

            // Execute pending request if any
            pendingRequest?.let { request ->
                service?.startDownload(request)
                pendingRequest = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isConnected.value = false
        }
    }

    /**
     * Bind to the download service
     */
    fun bind(context: Context) {
        if (isBound) return

        bindingContext = context.applicationContext
        val intent = Intent(context, DownloadService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbind from the download service
     */
    fun unbind() {
        if (!isBound) return

        try {
            bindingContext?.unbindService(connection)
        } catch (e: Exception) {
            // Not bound
        }

        service = null
        isBound = false
        bindingContext = null
        _isConnected.value = false
    }

    /**
     * Start downloading chapters
     */
    fun startDownload(
        context: Context,
        provider: MainProvider,
        novel: Novel,
        chapters: List<Chapter>
    ) {
        if (chapters.isEmpty()) return

        val request = DownloadRequest(
            novelUrl = novel.url,
            novelName = novel.name,
            novelCoverUrl = novel.posterUrl,
            providerName = provider.name,
            chapterUrls = chapters.map { it.url },
            chapterNames = chapters.map { it.name }
        )

        // Start service first
        DownloadService.start(context)

        // Bind if not already bound
        if (!isBound) {
            pendingRequest = request
            bind(context)
        } else {
            service?.startDownload(request)
        }
    }

    /**
     * Pause the current download
     */
    fun pauseDownload() {
        service?.pauseDownload()
    }

    /**
     * Resume a paused download
     */
    fun resumeDownload() {
        service?.resumeDownload()
    }

    /**
     * Cancel the current download
     */
    fun cancelDownload() {
        service?.cancelDownload()
    }

    /**
     * Check if a download is currently active
     */
    fun isDownloading(): Boolean {
        return _downloadState.value.isActive
    }

    /**
     * Check if currently downloading a specific novel
     */
    fun isDownloadingNovel(novelUrl: String): Boolean {
        val state = _downloadState.value
        return state.isActive && state.novelUrl == novelUrl
    }

    /**
     * Get the current download progress (0-100)
     */
    fun getProgressPercent(): Int {
        return (_downloadState.value.progressPercent * 100).toInt()
    }
}