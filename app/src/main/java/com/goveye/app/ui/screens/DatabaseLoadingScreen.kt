package com.goveye.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.data.update.DatabaseUpdateState

/**
 * Loading screen shown during first-launch DB download and patch application
 * (D-04, D-05, D-10a).
 *
 * Material 3 design: centered column with app name, progress indicator, status
 * text, and a data size hint. For metered connections (Pitfall 4), shows a
 * Wi-Fi warning dialog before proceeding with the per-API DB downloads.
 *
 * - [DatabaseUpdateState.NeedsPatches] — indeterminate progress (patches are
 *   tiny, applied in <2s, no progress bar needed)
 * - [DatabaseUpdateState.NeedsFullDownload] — determinate download progress
 *   for first-launch per-API DB downloads
 *
 * @param state The current [DatabaseUpdateState] driving the UI.
 * @param onDownloadNow Called when the user confirms download on metered connection.
 * @param onWaitForWifi Called when the user chooses to wait for Wi-Fi.
 */
@Composable
fun DatabaseLoadingScreen(
    state: DatabaseUpdateState,
    onDownloadNow: () -> Unit = {},
    onWaitForWifi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWifiDialog by remember { mutableStateOf(state is DatabaseUpdateState.NeedsWifi) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // App name as placeholder logo
            Text(
                text = "GovEye",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            // Progress indicator
            when (state) {
                is DatabaseUpdateState.Downloading -> {
                    if (state.isFullDb && state.progress > 0f) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                is DatabaseUpdateState.NeedsPatches -> {
                    // Patches are tiny (5-50KB each, up to 5 = max 250KB) — indeterminate
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is DatabaseUpdateState.Applying -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is DatabaseUpdateState.NeedsFullDownload,
                is DatabaseUpdateState.NeedsWifi -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                else -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status text
            val statusText = when (state) {
                is DatabaseUpdateState.Downloading -> {
                    if (state.isFullDb) {
                        val percent = (state.progress * 100).toInt()
                        "Downloading parliamentary data... $percent%"
                    } else {
                        "Downloading updates..."
                    }
                }

                is DatabaseUpdateState.NeedsPatches -> {
                    if (state.patches.isNotEmpty()) {
                        "Applying updates..."
                    } else {
                        "Checking for updates..."
                    }
                }

                is DatabaseUpdateState.Applying -> "Applying updates..."

                is DatabaseUpdateState.NeedsFullDownload -> "Preparing download..."

                is DatabaseUpdateState.NeedsWifi -> "Waiting for Wi-Fi..."

                is DatabaseUpdateState.Failed -> "Download failed: ${state.message}"

                else -> "Almost ready..."
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Data size hint for full DB downloads
            if (state is DatabaseUpdateState.NeedsFullDownload ||
                (state is DatabaseUpdateState.Downloading && state.isFullDb)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloading parliamentary data (7 databases)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Wi-Fi warning dialog for metered connections (Pitfall 4)
    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = {
                showWifiDialog = false
                onWaitForWifi()
            },
            title = { Text("Wi-Fi recommended") },
            text = {
                Text("GovEye needs to download parliamentary data (7 databases). Connect to Wi-Fi?")
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
