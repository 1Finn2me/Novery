package com.emptycastle.novery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Utility for loading and caching images for notifications
 */
object NotificationImageLoader {

    private const val CACHE_DIR = "notification_images"
    private const val MAX_CACHE_SIZE = 50 // Max number of cached images
    private const val IMAGE_SIZE = 256 // Notification large icon size
    private const val CORNER_RADIUS = 16f

    /**
     * Load image from URL, with caching
     */
    suspend fun loadImage(
        context: Context,
        imageUrl: String?,
        rounded: Boolean = true
    ): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = getCachedImage(context, imageUrl)
                if (cached != null) {
                    return@withContext if (rounded) roundCorners(cached) else cached
                }

                // Download image
                val bitmap = downloadImage(imageUrl) ?: return@withContext null

                // Resize for notification
                val resized = resizeBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE)

                // Cache it
                cacheImage(context, imageUrl, resized)

                // Apply rounded corners if requested
                if (rounded) roundCorners(resized) else resized

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Preload image for a novel (call when starting download)
     */
    suspend fun preloadImage(context: Context, imageUrl: String?) {
        loadImage(context, imageUrl)
    }

    /**
     * Clear old cached images
     */
    fun clearOldCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) return

            val files = cacheDir.listFiles() ?: return
            if (files.size <= MAX_CACHE_SIZE) return

            // Sort by last modified and delete oldest
            files.sortedBy { it.lastModified() }
                .take(files.size - MAX_CACHE_SIZE)
                .forEach { it.delete() }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCachedImage(context: Context, url: String): Bitmap? {
        try {
            val file = getCacheFile(context, url)
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun cacheImage(context: Context, url: String, bitmap: Bitmap) {
        try {
            val file = getCacheFile(context, url)
            file.parentFile?.mkdirs()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCacheFile(context: Context, url: String): File {
        val hash = md5(url)
        return File(context.cacheDir, "$CACHE_DIR/$hash.png")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun downloadImage(url: String): Bitmap? {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return BitmapFactory.decodeStream(connection.inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun roundCorners(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    /**
     * Create a circular bitmap (for some notification styles)
     */
    fun makeCircular(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val left = (size - bitmap.width) / 2f
        val top = (size - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, paint)

        return output
    }
}