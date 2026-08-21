package com.goveye.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goveye.app.data.update.DatabaseUpdateState

/**
 * Loading screen shown during first-launch DB download and patch application.
 *
 * Layout:
 * - Top: "GovEye" title + "Downloading parliamentary data" subtitle
 * - Center: Large circular progress indicator with percentage inside
 * - Bottom: "This only happens once" hint
 *
 * Background matches the app's theme (MaterialTheme.colorScheme.background),
 * same as Scaffold's default containerColor used throughout the app.
 *
 * @param state The current [DatabaseUpdateState] driving the UI.
 * @param onDownloadNow Called when the user confirms download on metered connection.
 * @param onWaitForWifi Called when the user chooses to wait for Wi-Fi.
 * @param onRetry Called when the user taps Retry after a download failure.
 */
@Composable
fun DatabaseLoadingScreen(
    state: DatabaseUpdateState,
    onDownloadNow: () -> Unit = {},
    onWaitForWifi: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWifiDialog by remember { mutableStateOf(state is DatabaseUpdateState.NeedsWifi) }

    val isProgressState = state is DatabaseUpdateState.Downloading ||
        state is DatabaseUpdateState.Checking ||
        state is DatabaseUpdateState.NeedsPatches ||
        state is DatabaseUpdateState.Applying ||
        state is DatabaseUpdateState.NeedsFullDownload

    val subtitle = when (state) {
        is DatabaseUpdateState.Downloading -> "Downloading parliamentary data"
        is DatabaseUpdateState.Checking -> "Checking for updates"
        is DatabaseUpdateState.NeedsPatches -> "Applying updates"
        is DatabaseUpdateState.Applying -> "Applying updates"
        is DatabaseUpdateState.NeedsFullDownload -> "Preparing download"
        is DatabaseUpdateState.NeedsWifi -> "Waiting for Wi-Fi"
        is DatabaseUpdateState.Failed -> "Download failed"
        else -> "Loading"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Top: title + subtitle ─────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Text(
                text = "GovEye",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Center: circular progress ─────────────────────────────────
        AnimatedVisibility(
            visible = isProgressState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressWithPercentage(state)
        }

        AnimatedVisibility(
            visible = state is DatabaseUpdateState.Failed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ErrorSection(
                message = (state as? DatabaseUpdateState.Failed)?.message ?: "",
                onRetry = onRetry
            )
        }

        AnimatedVisibility(
            visible = state is DatabaseUpdateState.NeedsWifi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            WifiNeededSection(onDownloadAnyway = { showWifiDialog = true })
        }

        // ── Bottom: hint ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = isProgressState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = "This only happens once",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    // Wi-Fi warning dialog for metered connections
    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = {
                showWifiDialog = false
                onWaitForWifi()
            },
            title = { Text("Wi-Fi recommended") },
            text = {
                Text(
                    "GovEye needs to download parliamentary data (7 databases). Connect to Wi-Fi to avoid mobile data charges."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWifiDialog = false
                    onDownloadNow()
                }) {
                    Text("Download now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWifiDialog = false
                    onWaitForWifi()
                }) {
                    Text("Wait for Wi-Fi")
                }
            }
        )
    }
}

/**
 * Large circular progress indicator with the download percentage displayed inside.
 *
 * 280dp diameter, 12dp stroke width, large percentage text in the center.
 *
 * For the download state, uses a "fake" smooth animation that climbs toward the
 * next milestone (actual progress + 1/7) so the user sees continuous movement
 * instead of jumps. When the actual progress catches up, the animation snaps
 * to it and continues toward the next milestone.
 *
 * For indeterminate states (checking, applying, preparing), shows a spinner.
 */
@Composable
private fun CircularProgressWithPercentage(state: DatabaseUpdateState) {
    val isDownloading = state is DatabaseUpdateState.Downloading &&
        (state as DatabaseUpdateState.Downloading).isFullDb

    if (isDownloading) {
        val actualProgress = (state as DatabaseUpdateState.Downloading).progress

        // Fake smooth progress: animates toward the next milestone so the user
        // sees continuous movement. When actual progress jumps ahead, snaps to it.
        val displayedProgress = remember { androidx.compose.animation.core.Animatable(0f) }

        LaunchedEffect(actualProgress) {
            // If the actual progress has jumped ahead of our animation, snap to it
            if (displayedProgress.value < actualProgress) {
                displayedProgress.snapTo(actualProgress)
            }
            // Animate toward the next milestone (actual + 1/7 ≈ 14%)
            // but cap at 1.0 (100%) so we don't overshoot
            val nextMilestone = (actualProgress + 1f / 7f).coerceAtMost(1f)
            displayedProgress.animateTo(
                targetValue = nextMilestone,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 10000,
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
        }

        val progress = displayedProgress.value
        val percent = (progress * 100).toInt()

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            // Background track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            // Progress ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            // Percentage text in center — large
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 64.sp
            )
        }
    } else {
        // Indeterminate spinner — same 280dp size as the determinate circle
        // so the layout doesn't jump when progress data starts arriving.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            // Background track (static ring, same as determinate mode)
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            // Indeterminate spinner on top
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Error section — error icon, message in a card, retry button.
 */
@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Wi-Fi needed section — explanation + download-anyway button.
 */
@Composable
private fun WifiNeededSection(onDownloadAnyway: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Wifi,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "GovEye needs to download parliamentary data. Connect to Wi-Fi " +
                "to continue, or download now using mobile data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onDownloadAnyway,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Wifi,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("Download now anyway")
        }
    }
}
