package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors

/**
 * Displays an image from chapter content
 */
@Composable
fun ChapterImageItem(
    item: ReaderDisplayItem.Image,
    colors: ReaderColors,
    horizontalPadding: Dp,
    baseUrl: String? = null,
    onImageClick: ((String) -> Unit)? = null
) {
    val image = item.image
    val resolvedUrl = resolveImageUrl(image.url, baseUrl)

    val refererUrl = remember(resolvedUrl) {
        try {
            val uri = android.net.Uri.parse(resolvedUrl)
            "${uri.scheme}://${uri.host}/"
        } catch (e: Exception) {
            baseUrl?.let {
                val uri = android.net.Uri.parse(it)
                "${uri.scheme}://${uri.host}/"
            } ?: ""
        }
    }

    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    // Track if we've determined the actual image size
    var hasLoadedSize by remember { mutableStateOf(false) }
    var imageAspectRatio by remember { mutableStateOf(16f / 9f) } // Default aspect ratio

    // Use a STABLE minimum height to prevent layout shifts
    // Only update size AFTER image loads to avoid jarring shifts
    val stableHeight = remember { 200.dp } // Fixed stable height during loading

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.text.copy(alpha = 0.05f))
            .then(
                if (!hasLoadedSize) {
                    // Use stable height during loading to prevent layout shifts
                    Modifier.height(stableHeight)
                } else {
                    // After loading, use natural height with constraints
                    Modifier.heightIn(min = 100.dp, max = 400.dp)
                }
            )
            .clickable(enabled = onImageClick != null) {
                onImageClick?.invoke(resolvedUrl)
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(resolvedUrl)
                .crossfade(false) // Disable crossfade to reduce layout changes
                .addHeader("Referer", refererUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build(),
            contentDescription = image.altText ?: "Chapter image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onState = { state ->
                imageState = state
                // Mark as loaded when we have the image
                if (state is AsyncImagePainter.State.Success) {
                    hasLoadedSize = true
                }
            }
        )

        // Loading indicator
        if (imageState is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = colors.accent,
                strokeWidth = 2.dp
            )
        }

        // Error state
        if (imageState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Failed to load image",
                    modifier = Modifier.size(48.dp),
                    tint = colors.text.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * Resolve potentially relative image URLs
 */
private fun resolveImageUrl(url: String, baseUrl: String?): String {
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") && baseUrl != null -> {
            val base = baseUrl.trimEnd('/')
            "$base$url"
        }
        baseUrl != null -> {
            val base = baseUrl.substringBeforeLast("/")
            "$base/$url"
        }
        else -> url
    }
}

/**
 * Full-screen image viewer dialog
 */
@Composable
fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full size image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}