package com.emptycastle.novery.ui.screens.reader.components

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.screens.reader.theme.ReaderDefaults
import com.emptycastle.novery.ui.theme.Orange500

// =============================================================================
// KEEP SCREEN ON EFFECT
// =============================================================================

@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        val window = (context as? ComponentActivity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// =============================================================================
// FULL SCREEN STATES
// =============================================================================

/**
 * Loading state - supports both textColor parameter and colors parameter for backwards compatibility
 */
@Composable
fun ReaderLoadingState(
    textColor: Color? = null,
    colors: ReaderColors? = null
) {
    val displayColor = textColor ?: colors?.text ?: Color.White
    val accentColor = colors?.accent ?: Orange500

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading chapter" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading Chapter...",
                color = displayColor.copy(alpha = ReaderDefaults.LabelAlpha)
            )
        }
    }
}

/**
 * Error state - supports optional colors parameter
 */
@Composable
fun ReaderErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    colors: ReaderColors? = null
) {
    val textColor = colors?.text ?: Color.White
    val errorColor = colors?.error ?: MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = errorColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Failed to load chapter",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }

            Spacer(modifier = Modifier.height(8.dp))

            GhostButton(
                text = "Go Back",
                onClick = onBack
            )
        }
    }
}

// =============================================================================
// INLINE LIST ITEM STATES
// =============================================================================

@Composable
fun LoadingIndicatorItem(colors: ReaderColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colors.accent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading chapter...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ErrorIndicatorItem(
    error: String,
    colors: ReaderColors,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.error
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Failed to load chapter",
                style = MaterialTheme.typography.titleSmall,
                color = colors.text
            )

            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.text.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}