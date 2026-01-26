package com.emptycastle.novery.ui.screens.details.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "CoverZoomDialog"

@Composable
fun CoverZoomDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    // Image state
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Permission state
    var hasSavePermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSavePermission = isGranted
        Log.d(TAG, "Permission granted: $isGranted")
        if (isGranted) {
            // Trigger save after permission granted
            loadedBitmap?.let { bitmap ->
                scope.launch {
                    isSaving = true
                    val result = saveImageToGallery(context, bitmap, title)
                    isSaving = false
                    snackbarHostState.showSnackbar(
                        if (result) "Image saved to Pictures/Novery" else "Failed to save image"
                    )
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Storage permission is required to save images")
            }
        }
    }

    // Load image
    LaunchedEffect(imageUrl) {
        isLoading = true
        hasError = false
        loadedBitmap = null

        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            val drawable = result.drawable

            loadedBitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                null -> null
                else -> {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, width, height)
                        drawable.draw(canvas)
                    }
                }
            }

            hasError = loadedBitmap == null
            Log.d(TAG, "Image loaded: ${loadedBitmap != null}, size: ${loadedBitmap?.width}x${loadedBitmap?.height}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image", e)
            hasError = true
        } finally {
            isLoading = false
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "zoom_scale"
    )

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun saveImage() {
        val bitmap = loadedBitmap
        if (bitmap == null) {
            scope.launch { snackbarHostState.showSnackbar("Image not loaded yet") }
            return
        }

        // Check if we need permission (Android 9 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Android ${Build.VERSION.SDK_INT}, hasPermission: $hasPermission")

            if (!hasPermission) {
                Log.d(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission")
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }

        // Permission granted or not needed, proceed with save
        scope.launch {
            isSaving = true
            Log.d(TAG, "Starting save, bitmap: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")
            val result = saveImageToGallery(context, bitmap, title)
            isSaving = false
            snackbarHostState.showSnackbar(
                if (result) "Image saved to Pictures/Novery" else "Failed to save image"
            )
        }
    }

    fun shareImage() {
        val bitmap = loadedBitmap
        if (bitmap == null) {
            scope.launch { snackbarHostState.showSnackbar("Image not loaded yet") }
            return
        }

        scope.launch {
            isSharing = true
            val success = shareImageIntent(context, bitmap, title)
            isSharing = false
            if (!success) {
                snackbarHostState.showSnackbar("Failed to share image")
            }
        }
    }

    fun copyUrl() {
        clipboardManager.setText(AnnotatedString(imageUrl))
        scope.launch {
            snackbarHostState.showSnackbar("Image URL copied to clipboard")
        }
    }

    fun openInBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))
            context.startActivity(intent)
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Couldn't open URL")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { _ ->
                            if (scale > 1f) {
                                resetZoom()
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = { showControls = !showControls }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        if (scale > 1f) {
                            val maxOffset = (scale - 1) * 500f
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main Image
            loadedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }

            // Loading indicator
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Error state
            AnimatedVisibility(
                visible = hasError && !isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = "Error",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Failed to load image",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Surface(
                        onClick = { openInBrowser() },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Open in browser",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Top bar
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                TopControlBar(
                    zoomLevel = animatedScale,
                    showResetButton = scale != 1f,
                    onResetZoom = { resetZoom() },
                    onDismiss = onDismiss
                )
            }

            // Bottom action bar
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                BottomActionBar(
                    title = title,
                    isSaving = isSaving,
                    isSharing = isSharing,
                    isImageLoaded = loadedBitmap != null,
                    canZoomIn = scale < 5f,
                    canZoomOut = scale > 0.5f,
                    onSave = { saveImage() },
                    onShare = { shareImage() },
                    onCopyUrl = { copyUrl() },
                    onZoomIn = { scale = (scale * 1.5f).coerceAtMost(5f) },
                    onZoomOut = {
                        scale = (scale / 1.5f).coerceAtLeast(0.5f)
                        if (scale <= 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                )
            }

            // Zoom hint
            AnimatedVisibility(
                visible = showControls && scale == 1f && !isLoading && !hasError,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 280.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Double-tap to zoom • Pinch to adjust",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Saving/Sharing overlay
            AnimatedVisibility(
                visible = isSaving || isSharing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (isSaving) "Saving image..." else "Preparing to share...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 180.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.DarkGray,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TopControlBar(
    zoomLevel: Float,
    showResetButton: Boolean,
    onResetZoom: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(
                visible = showResetButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ControlButton(
                    icon = Icons.Default.CenterFocusStrong,
                    contentDescription = "Reset zoom",
                    onClick = onResetZoom
                )
            }

            ControlButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun BottomActionBar(
    title: String,
    isSaving: Boolean,
    isSharing: Boolean,
    isImageLoaded: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ActionButton(
                    icon = Icons.Outlined.Download,
                    label = "Save",
                    isLoading = isSaving,
                    enabled = isImageLoaded,
                    onClick = onSave
                )

                ActionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    isLoading = isSharing,
                    enabled = isImageLoaded,
                    onClick = onShare
                )

                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy URL",
                    onClick = onCopyUrl
                )

                ActionButton(
                    icon = Icons.Outlined.ZoomIn,
                    label = "Zoom +",
                    enabled = canZoomIn,
                    onClick = onZoomIn
                )

                ActionButton(
                    icon = Icons.Outlined.ZoomOut,
                    label = "Zoom -",
                    enabled = canZoomOut,
                    onClick = onZoomOut
                )
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .widthIn(max = 320.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val isEnabled = enabled && !isLoading

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isEnabled) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

// ============= Image Utility Functions =============

private suspend fun saveImageToGallery(
    context: Context,
    bitmap: Bitmap,
    title: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val saveBitmap = ensureSoftwareBitmap(bitmap)
        if (saveBitmap == null) {
            Log.e(TAG, "Failed to create software bitmap")
            return@withContext false
        }

        Log.d(TAG, "Saving bitmap: ${saveBitmap.width}x${saveBitmap.height}, config: ${saveBitmap.config}")

        val filename = "${sanitizeFilename(title)}_${System.currentTimeMillis()}.jpg"

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageModern(context, saveBitmap, filename)
        } else {
            saveImageLegacy(context, saveBitmap, filename)
        }

        Log.d(TAG, "Save result: $result")
        result
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save image", e)
        false
    }
}

private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            Log.d(TAG, "Converting hardware bitmap to software bitmap")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to convert bitmap", e)
        null
    }
}

private fun saveImageModern(context: Context, bitmap: Bitmap, filename: String): Boolean {
    Log.d(TAG, "Using modern save (API 29+)")

    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Novery")
    }

    var uri: Uri? = null
    return try {
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            Log.e(TAG, "Failed to create MediaStore entry")
            return false
        }

        Log.d(TAG, "Created URI: $uri")

        resolver.openOutputStream(uri)?.use { outputStream ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            Log.d(TAG, "Compress result: $compressed")
            if (!compressed) {
                throw Exception("Failed to compress bitmap")
            }
        } ?: throw Exception("Failed to open output stream")

        Log.d(TAG, "Image saved successfully")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Exception while saving", e)
        uri?.let {
            try {
                resolver.delete(it, null, null)
            } catch (_: Exception) {
            }
        }
        false
    }
}

@Suppress("DEPRECATION")
private fun saveImageLegacy(context: Context, bitmap: Bitmap, filename: String): Boolean {
    Log.d(TAG, "Using legacy save (API < 29)")

    try {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        Log.d(TAG, "Pictures directory: ${imagesDir.absolutePath}, exists: ${imagesDir.exists()}, canWrite: ${imagesDir.canWrite()}")

        val appDir = File(imagesDir, "Novery")
        Log.d(TAG, "App directory: ${appDir.absolutePath}, exists: ${appDir.exists()}")

        if (!appDir.exists()) {
            val created = appDir.mkdirs()
            Log.d(TAG, "Created app directory: $created")
            if (!created) {
                // Try alternative: save directly to Pictures folder
                Log.d(TAG, "Falling back to Pictures folder directly")
                return saveToFile(File(imagesDir, filename), bitmap, context)
            }
        }

        return saveToFile(File(appDir, filename), bitmap, context)
    } catch (e: Exception) {
        Log.e(TAG, "Legacy save failed", e)
        return false
    }
}

@Suppress("DEPRECATION")
private fun saveToFile(file: File, bitmap: Bitmap, context: Context): Boolean {
    return try {
        FileOutputStream(file).use { stream ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            Log.d(TAG, "Compress to file result: $compressed")
            if (!compressed) {
                throw Exception("Failed to compress bitmap")
            }
        }

        // Notify gallery
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.fromFile(file)
        })

        Log.d(TAG, "Image saved to: ${file.absolutePath}")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save to file", e)
        file.delete()
        false
    }
}

private suspend fun shareImageIntent(
    context: Context,
    bitmap: Bitmap,
    title: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val saveBitmap = ensureSoftwareBitmap(bitmap) ?: return@withContext false

        val cachePath = File(context.cacheDir, "shared_images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cachePath, "share_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { stream ->
            saveBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        withContext(Dispatchers.Main) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share cover image"))
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to share image", e)
        false
    }
}

private fun sanitizeFilename(name: String): String {
    return name
        .replace(Regex("[^a-zA-Z0-9._\\-\\s]"), "")
        .replace(Regex("\\s+"), "_")
        .take(50)
        .trimEnd('_')
        .ifEmpty { "cover" }
}