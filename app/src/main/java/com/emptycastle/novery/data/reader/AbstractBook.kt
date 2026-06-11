package com.emptycastle.novery.data.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread

abstract class AbstractBook {
    open fun resolveUrl(url: String): String {
        return url
    }

    abstract val canReload: Boolean

    abstract fun size(): Int
    abstract fun title(): String
    abstract fun getChapterTitle(index: Int): String
    abstract fun getLoadingStatus(index: Int): String?

    @Throws
    open fun loadImage(image: String): ByteArray? {
        return null
    }

    fun loadImageBitmap(image: String): Bitmap? {
        return try {
            val data = this.loadImage(image) ?: return null
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (t: Throwable) {
            Log.e("AbstractBook", "Error loading image bitmap", t)
            null
        }
    }

    @WorkerThread
    @Throws
    abstract suspend fun getChapterData(index: Int, reload: Boolean): String

    abstract fun expand(last: String): Boolean

    @WorkerThread
    @Throws
    protected abstract suspend fun posterBytes(): ByteArray?

    private var poster: Bitmap? = null

    /**
     * Optional author of the book.
     */
    abstract fun author(): String?

    /**
     * Get the poster bitmap.
     */
    fun getPoster(): Bitmap? {
        return poster
    }

    /**
     * Call this to initialize the poster bitmap asynchronously.
     */
    suspend fun initializePoster() {
        if (poster == null) {
            poster = try {
                posterBytes()?.let { byteArray ->
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                }
            } catch (t: Throwable) {
                Log.e("AbstractBook", "Error initializing poster", t)
                null
            }
        }
    }
}
