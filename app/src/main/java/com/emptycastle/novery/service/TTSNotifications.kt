package com.emptycastle.novery.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.media.session.MediaButtonReceiver
import androidx.palette.graphics.Palette
import com.emptycastle.novery.R
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Modern TTS notification manager with rich media controls.
 */
object TTSNotifications {

    //region Constants

    private const val TTS_CHANNEL_ID = "NoveryTTS"
    private const val MEDIA_SESSION_TAG = "NoveryTTS"

    const val TTS_NOTIFICATION_ID = 133742

    const val ACTION_PLAY = "com.emptycastle.novery.TTS_PLAY"
    const val ACTION_PAUSE = "com.emptycastle.novery.TTS_PAUSE"
    const val ACTION_STOP = "com.emptycastle.novery.TTS_STOP"
    const val ACTION_NEXT = "com.emptycastle.novery.TTS_NEXT"
    const val ACTION_PREVIOUS = "com.emptycastle.novery.TTS_PREVIOUS"

    private const val COMPACT_ACTION_PREVIOUS = 0
    private const val COMPACT_ACTION_PLAY_PAUSE = 1
    private const val COMPACT_ACTION_NEXT = 2

    @ColorInt private const val DEFAULT_PRIMARY_COLOR = 0xFF6200EE.toInt()
    @ColorInt private const val DEFAULT_ON_PRIMARY_COLOR = 0xFFFFFFFF.toInt()

    //endregion

    //region State

    private val hasCreatedNotificationChannel = AtomicBoolean(false)

    @Volatile
    private var _mediaSession: MediaSessionCompat? = null
    val mediaSession: MediaSessionCompat?
        get() = _mediaSession

    private var serviceRef: WeakReference<TTSService>? = null

    // Cached palette colors
    private var cachedPrimaryColor: Int = DEFAULT_PRIMARY_COLOR
    private var cachedOnPrimaryColor: Int = DEFAULT_ON_PRIMARY_COLOR
    private var lastBitmapHashCode: Int = 0

    // State for display
    private var currentSpeechRate: Float = 1.0f
    private var sleepTimerRemaining: Int? = null

    //endregion

    //region Public API

    fun updateSpeechRate(rate: Float) {
        currentSpeechRate = rate
    }

    fun updateSleepTimer(remainingMinutes: Int?) {
        sleepTimerRemaining = remainingMinutes
    }

    /**
     * Reset all cached state
     */
    fun resetState() {
        cachedPrimaryColor = DEFAULT_PRIMARY_COLOR
        cachedOnPrimaryColor = DEFAULT_ON_PRIMARY_COLOR
        lastBitmapHashCode = 0
        currentSpeechRate = 1.0f
        sleepTimerRemaining = null
    }

    //endregion

    //region Notification Channel

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            TTS_CHANNEL_ID,
            context.getString(R.string.tts_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.tts_channel_description)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }

        context.notificationManager.createNotificationChannel(channel)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (hasCreatedNotificationChannel.compareAndSet(false, true)) {
            createNotificationChannel(context)
        }
    }

    //endregion

    //region Color Extraction

    /**
     * Synchronously extracts colors from bitmap using Palette.
     * Call this on a background thread if bitmap is large.
     */
    private fun extractColorsSync(bitmap: Bitmap?) {
        if (bitmap == null) {
            cachedPrimaryColor = DEFAULT_PRIMARY_COLOR
            cachedOnPrimaryColor = DEFAULT_ON_PRIMARY_COLOR
            lastBitmapHashCode = 0
            return
        }

        val bitmapHash = bitmap.hashCode()
        if (bitmapHash == lastBitmapHashCode && lastBitmapHashCode != 0) {
            return // Already extracted for this bitmap
        }

        lastBitmapHashCode = bitmapHash

        try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            // Priority: Vibrant > Muted > DarkVibrant > DarkMuted
            cachedPrimaryColor = palette.getVibrantColor(
                palette.getMutedColor(
                    palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(DEFAULT_PRIMARY_COLOR)
                    )
                )
            )

            cachedOnPrimaryColor = calculateOnColor(cachedPrimaryColor)
        } catch (e: Exception) {
            e.printStackTrace()
            cachedPrimaryColor = DEFAULT_PRIMARY_COLOR
            cachedOnPrimaryColor = DEFAULT_ON_PRIMARY_COLOR
        }
    }

    @ColorInt
    private fun calculateOnColor(@ColorInt backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    //endregion

    //region Media Session

    @MainThread
    fun initializeMediaSession(
        service: TTSService,
        content: TTSContent,
        context: Context
    ) {
        releaseMediaSession()

        serviceRef = WeakReference(service)

        val mediaButtonIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_PLAY_PAUSE
        )

        _mediaSession = MediaSessionCompat(
            context,
            MEDIA_SESSION_TAG,
            ComponentName(context, MediaButtonReceiver::class.java),
            mediaButtonIntent
        ).apply {
            setCallback(createMediaSessionCallback())
            setMetadata(buildMediaMetadata(content))
            setPlaybackState(buildPlaybackState(TTSStatus.STOPPED, 0, content.totalSegments))
            isActive = true
        }
    }

    private fun createMediaSessionCallback(): MediaSessionCompat.Callback {
        return object : MediaSessionCompat.Callback() {

            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                val keyEvent = mediaButtonEvent.getKeyEventCompat()
                    ?: return super.onMediaButtonEvent(mediaButtonEvent)

                if (keyEvent.action != KeyEvent.ACTION_DOWN) {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                return handleMediaKeyEvent(keyEvent.keyCode)
                        || super.onMediaButtonEvent(mediaButtonEvent)
            }

            override fun onPlay() { serviceRef?.get()?.resume() }
            override fun onPause() { serviceRef?.get()?.pause() }
            override fun onStop() { serviceRef?.get()?.stop() }
            override fun onFastForward() { serviceRef?.get()?.next() }
            override fun onRewind() { serviceRef?.get()?.previous() }
            override fun onSkipToNext() { serviceRef?.get()?.next() }
            override fun onSkipToPrevious() { serviceRef?.get()?.previous() }
            override fun onSeekTo(pos: Long) { serviceRef?.get()?.seekToSegment(pos.toInt()) }
        }
    }

    private fun handleMediaKeyEvent(keyCode: Int): Boolean {
        val service = serviceRef?.get() ?: return false

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { service.togglePlayPause(); true }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { service.pause(); true }
            KeyEvent.KEYCODE_MEDIA_PLAY -> { service.resume(); true }
            KeyEvent.KEYCODE_MEDIA_STOP -> { service.stop(); true }
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> { service.next(); true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> { service.previous(); true }
            else -> false
        }
    }

    private fun buildMediaMetadata(content: TTSContent, coverBitmap: Bitmap? = null): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.chapterName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, content.novelName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, content.novelName)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, content.chapterName)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, content.novelName)
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, content.totalSegments.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, content.totalSegments.toLong() * 1000)
            .apply {
                coverBitmap?.let {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
                }
            }
            .build()
    }

    private fun buildPlaybackState(
        status: TTSStatus,
        currentSegment: Int,
        totalSegments: Int
    ): PlaybackStateCompat {
        val state = when (status) {
            TTSStatus.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            TTSStatus.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            TTSStatus.STOPPED -> PlaybackStateCompat.STATE_STOPPED
        }

        val position = (currentSegment * 1000).toLong()
        val playbackSpeed = if (status == TTSStatus.PLAYING) currentSpeechRate else 0f

        return PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_PLAYBACK_ACTIONS)
            .setState(state, position, playbackSpeed)
            .build()
    }

    @MainThread
    fun updatePlaybackState(status: TTSStatus, currentSegment: Int, totalSegments: Int) {
        _mediaSession?.setPlaybackState(buildPlaybackState(status, currentSegment, totalSegments))
    }

    @MainThread
    fun updateMetadata(content: TTSContent, coverBitmap: Bitmap? = null) {
        extractColorsSync(coverBitmap)
        _mediaSession?.setMetadata(buildMediaMetadata(content, coverBitmap))
    }

    @MainThread
    fun releaseMediaSession() {
        _mediaSession?.run {
            isActive = false
            setCallback(null)
            release()
        }
        _mediaSession = null
        serviceRef?.clear()
        serviceRef = null
        resetState()
    }

    //endregion

    //region Notification Creation

    private enum class MediaAction(
        @DrawableRes val iconRes: Int,
        val title: String,
        val action: String
    ) {
        PLAY(R.drawable.ic_play, "Play", ACTION_PLAY),
        PAUSE(R.drawable.ic_pause, "Pause", ACTION_PAUSE),
        STOP(R.drawable.ic_stop, "Stop", ACTION_STOP),
        NEXT(R.drawable.ic_skip_next, "Next", ACTION_NEXT),
        PREVIOUS(R.drawable.ic_skip_previous, "Previous", ACTION_PREVIOUS);

        fun toNotificationAction(context: Context): NotificationCompat.Action {
            val intent = Intent(action).apply {
                setPackage(context.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(iconRes, title, pendingIntent).build()
        }
    }

    /**
     * Creates a TTS notification with rich metadata display.
     */
    fun createNotification(
        context: Context,
        content: TTSContent,
        status: TTSStatus,
        currentSegment: Int,
        coverBitmap: Bitmap? = null
    ): Notification? {
        if (status == TTSStatus.STOPPED) {
            cancelNotification(context)
            return null
        }

        ensureNotificationChannel(context)

        // Extract colors synchronously (should be quick for notification-sized bitmaps)
        extractColorsSync(coverBitmap)

        val progressPercent = calculateProgress(currentSegment, content.totalSegments)
        val contentText = buildContentText(content.novelName, currentSegment, content.totalSegments)
        val subText = buildSubText()

        val builder = NotificationCompat.Builder(context, TTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tts)
            .setContentTitle(content.chapterName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(status == TTSStatus.PLAYING)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setColorized(true)
            .setColor(cachedPrimaryColor)

        // Add subtext with speed/timer indicators
        if (subText.isNotBlank()) {
            builder.setSubText(subText)
        }

        // Set cover art
        coverBitmap?.let { builder.setLargeIcon(it) }

        // Content intent
        context.packageManager
            .getLaunchIntentForPackage(context.packageName)?.let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
            }

        // Delete intent
        val deleteIntent = createActionPendingIntent(context, ACTION_STOP)
        builder.setDeleteIntent(deleteIntent)

        // Add actions
        addMediaActions(builder, status, context)

        // Configure media style
        configureMediaStyle(builder, context)

        return builder.build()
    }

    /**
     * Builds content text with segment progress.
     */
    private fun buildContentText(novelName: String, currentSegment: Int, totalSegments: Int): String {
        val progress = "${currentSegment + 1}/$totalSegments"
        return "$novelName • $progress"
    }

    /**
     * Builds subtext with optional speed and timer indicators.
     */
    private fun buildSubText(): String {
        val parts = mutableListOf<String>()

        // Speech rate (if not default)
        if (currentSpeechRate != 1.0f) {
            val rateStr = if (currentSpeechRate == currentSpeechRate.toInt().toFloat()) {
                "${currentSpeechRate.toInt()}x"
            } else {
                "${currentSpeechRate}x"
            }
            parts.add("⚡ $rateStr")
        }

        // Sleep timer
        sleepTimerRemaining?.let { minutes ->
            if (minutes > 0) {
                parts.add("⏰ ${minutes}m")
            }
        }

        return parts.joinToString(" • ")
    }

    private fun calculateProgress(currentSegment: Int, totalSegments: Int): Int {
        if (totalSegments <= 1) return 0
        return ((currentSegment.toFloat() / (totalSegments - 1)) * 100).toInt().coerceIn(0, 100)
    }

    private fun addMediaActions(
        builder: NotificationCompat.Builder,
        status: TTSStatus,
        context: Context
    ) {
        builder.addAction(MediaAction.PREVIOUS.toNotificationAction(context))

        when (status) {
            TTSStatus.PLAYING -> builder.addAction(MediaAction.PAUSE.toNotificationAction(context))
            TTSStatus.PAUSED -> builder.addAction(MediaAction.PLAY.toNotificationAction(context))
            TTSStatus.STOPPED -> { /* No action */ }
        }

        builder.addAction(MediaAction.NEXT.toNotificationAction(context))
        builder.addAction(MediaAction.STOP.toNotificationAction(context))
    }

    private fun configureMediaStyle(
        builder: NotificationCompat.Builder,
        context: Context
    ) {
        val cancelIntent = createActionPendingIntent(context, ACTION_STOP)

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowCancelButton(true)
            .setCancelButtonIntent(cancelIntent)
            .setShowActionsInCompactView(
                COMPACT_ACTION_PREVIOUS,
                COMPACT_ACTION_PLAY_PAUSE,
                COMPACT_ACTION_NEXT
            )

        _mediaSession?.sessionToken?.let { token ->
            style.setMediaSession(token)
        }

        builder.setStyle(style)
    }

    private fun createActionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    //endregion

    //region Notification Management

    fun cancelNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(TTS_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notify(
        context: Context,
        content: TTSContent,
        status: TTSStatus,
        currentSegment: Int,
        coverBitmap: Bitmap? = null
    ) {
        updatePlaybackState(status, currentSegment, content.totalSegments)

        val notification = createNotification(context, content, status, currentSegment, coverBitmap)
            ?: return

        if (!hasNotificationPermission(context)) return

        try {
            NotificationManagerCompat.from(context).notify(TTS_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    //endregion

    //region Utilities

    private const val SUPPORTED_PLAYBACK_ACTIONS: Long =
        PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED

    private val Context.notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun Intent.getKeyEventCompat(): KeyEvent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }
    }

    //endregion
}

/**
 * TTS Status enum
 */
enum class TTSStatus {
    PLAYING,
    PAUSED,
    STOPPED
}