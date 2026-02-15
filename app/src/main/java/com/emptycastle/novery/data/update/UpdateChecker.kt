package com.emptycastle.novery.data.update

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases for app updates.
 */
class UpdateChecker(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val GITHUB_API =
            "https://api.github.com/repos/1Finn2me/Novery/releases/latest"
        private const val GITHUB_REPO = "https://github.com/1Finn2me/Novery"
    }

    /**
     * Response from GitHub Releases API
     */
    data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("name") val name: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("body") val body: String?,
        @SerializedName("published_at") val publishedAt: String?,
        @SerializedName("assets") val assets: List<GitHubAsset>?
    )

    data class GitHubAsset(
        @SerializedName("browser_download_url") val downloadUrl: String,
        @SerializedName("name") val name: String,
        @SerializedName("size") val size: Long
    )

    data class UpdateResult(
        val updateAvailable: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String?,
        val releaseUrl: String?,
        val releaseNotes: String?,
        val apkSizeBytes: Long?
    ) {
        fun formattedApkSize(): String? {
            val bytes = apkSizeBytes ?: return null
            return when {
                bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
                bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
                else -> "$bytes B"
            }
        }
    }

    /**
     * Check GitHub for the latest release and compare with current version.
     */
    suspend fun checkForUpdate(): Result<UpdateResult> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("GitHub API returned ${response.code}")
                )
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val release = gson.fromJson(body, GitHubRelease::class.java)
            val currentVersion = getCurrentVersion()
            val latestVersion = release.tagName.removePrefix("v")

            // Find the APK asset
            val apkAsset = release.assets?.firstOrNull {
                it.name.endsWith(".apk")
            }

            val updateAvailable = isNewerVersion(currentVersion, latestVersion)

            Result.success(
                UpdateResult(
                    updateAvailable = updateAvailable,
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    downloadUrl = apkAsset?.downloadUrl,
                    releaseUrl = release.htmlUrl,
                    releaseNotes = release.body,
                    apkSizeBytes = apkAsset?.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current app version from BuildConfig.
     */
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Compare two semantic version strings.
     * Returns true if latestVersion is newer than currentVersion.
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

            // Pad to same length
            val maxLength = maxOf(currentParts.size, latestParts.size)
            val currentPadded = currentParts + List(maxLength - currentParts.size) { 0 }
            val latestPadded = latestParts + List(maxLength - latestParts.size) { 0 }

            for (i in 0 until maxLength) {
                if (latestPadded[i] > currentPadded[i]) return true
                if (latestPadded[i] < currentPadded[i]) return false
            }
            return false // Same version
        } catch (e: Exception) {
            return false
        }
    }
}