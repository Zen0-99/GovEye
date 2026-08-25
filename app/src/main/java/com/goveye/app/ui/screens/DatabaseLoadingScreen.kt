package com.goveye.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goveye.app.data.update.DatabaseUpdateState

/**
 * Loading screen shown during first-launch DB download and patch application.
 *
 * Layout:
 * - Top: "GovEye" title + subtitle
 * - Center: Large circular progress indicator with percentage inside
 * - Bottom: "You can minimise the app" hint (during download) or
 *   Restart button (after download completes)
 *
 * When the download completes, the circular progress ring fills to 100%
 * and the percentage text fades into a checkmark icon inside the same
 * circle. A Restart button appears at the bottom — same design as the
 * onboarding Continue button.
 *
 * @param state The current [DatabaseUpdateState] driving the UI.
 * @param onDownloadNow Called when the user confirms download on metered connection.
 * @param onWaitForWifi Called when the user chooses to wait for Wi-Fi.
 * @param onRetry Called when the user taps Retry after a download failure.
 * @param onRestart Called when the user taps Restart after download completes.
 * @param onCancel Called when the user confirms cancellation of the download.
 * @param onRedownload Called when the user taps Download after a cancellation.
 */
@Composable
fun DatabaseLoadingScreen(
    state: DatabaseUpdateState,
    onDownloadNow: () -> Unit = {},
    onWaitForWifi: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRestart: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRedownload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWifiDialog by remember { mutableStateOf(state is DatabaseUpdateState.NeedsWifi) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val isProgressState = state is DatabaseUpdateState.Downloading ||
        state is DatabaseUpdateState.Checking ||
        state is DatabaseUpdateState.NeedsPatches ||
        state is DatabaseUpdateState.Applying ||
        state is DatabaseUpdateState.NeedsFullDownload

    val isRestartState = state is DatabaseUpdateState.NeedsRestart
    val isCanceledState = state is DatabaseUpdateState.Canceled

    val subtitle = when (state) {
        is DatabaseUpdateState.Downloading -> "Downloading parliamentary data"
        is DatabaseUpdateState.Checking -> "Checking for updates"
        is DatabaseUpdateState.NeedsPatches -> "Applying updates"
        is DatabaseUpdateState.Applying -> "Applying updates"
        is DatabaseUpdateState.NeedsFullDownload -> "Preparing download"
        is DatabaseUpdateState.NeedsWifi -> "Waiting for Wi-Fi"
        is DatabaseUpdateState.Failed -> "Download failed"
        is DatabaseUpdateState.NeedsRestart -> "Download complete"
        is DatabaseUpdateState.Canceled -> "Download Canceled"
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
        // Shown during download AND after completion — the circle stays
        // in place; only the content inside changes (percentage → checkmark).
        AnimatedVisibility(
            visible = isProgressState || isRestartState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressWithPercentage(state)
        }

        // Canceled state — unfilled circle with X icon
        AnimatedVisibility(
            visible = isCanceledState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CanceledCircle()
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

        // ── Bottom: hint + cancel button (during download) ───────────
        AnimatedVisibility(
            visible = isProgressState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "You can minimise the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel")
                }
            }
        }

        // Restart button — same design as onboarding Continue button.
        // Bottom-aligned, full width, 48dp height. No back button.
        AnimatedVisibility(
            visible = isRestartState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Restart GovEye")
            }
        }

        // Download button — shown after cancellation, same design as Restart.
        AnimatedVisibility(
            visible = isCanceledState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Button(
                onClick = onRedownload,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Download")
            }
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

    // Cancel download confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel download?") },
            text = {
                Text(
                    "The download will stop and any progress so far will be discarded. You can restart it at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancel()
                }) {
                    Text("Yes, cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep downloading")
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
 *
 * For the NeedsRestart state (download complete), the ring fills to 100% and
 * the percentage text fades out while a checkmark icon fades in — all inside
 * the same circle, so there's no layout jump.
 */
@Composable
private fun CircularProgressWithPercentage(state: DatabaseUpdateState) {
    val isDownloading = state is DatabaseUpdateState.Downloading &&
        (state as DatabaseUpdateState.Downloading).isFullDb

    val isRestartState = state is DatabaseUpdateState.NeedsRestart

    if (isDownloading || isRestartState) {
        val actualProgress = if (isRestartState) {
            1f
        } else {
            (state as DatabaseUpdateState.Downloading).progress
        }

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

        // Animate the alpha of percentage text vs checkmark — when download
        // completes, percentage fades out and checkmark fades in.
        val contentAlpha = remember { androidx.compose.animation.core.Animatable(1f) }

        LaunchedEffect(isRestartState) {
            if (isRestartState) {
                // Snap progress to 100% first
                displayedProgress.snapTo(1f)
                // Fade percentage out, checkmark in
                contentAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = androidx.compose.animation.core.LinearEasing)
                )
            }
        }

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
            // Center content — cross-fade between percentage and checkmark.
            // percentageAlpha goes from 1 → 0 when download completes.
            // checkmarkAlpha is the inverse (0 → 1).
            val percentageAlpha = contentAlpha.value
            val checkmarkAlpha = 1f - contentAlpha.value

            // Percentage text — fades out on completion
            if (percentageAlpha > 0.01f) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 64.sp,
                    modifier = Modifier.graphicsLayer { alpha = percentageAlpha }
                )
            }
            // Checkmark icon — fades in on completion
            if (checkmarkAlpha > 0.01f) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { alpha = checkmarkAlpha }
                )
            }
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
 * Canceled state — unfilled progress circle (track only, no progress ring)
 * with an X icon in the center. Same 280dp size as the download circle so
 * the layout doesn't jump.
 */
@Composable
private fun CanceledCircle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp)
    ) {
        // Background track only — no progress ring (unfilled)
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 12.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        // X icon in center
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
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
