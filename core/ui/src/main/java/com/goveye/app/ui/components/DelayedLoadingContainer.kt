package com.goveye.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

/**
 * Shows a loading spinner for [minLoadingMs] before revealing the content.
 * If [isLoading] is true, shows a spinner.
 * If [isLoading] is false and [isEmpty] is true, shows [emptyMessage] — but
 * only after the minimum loading period has elapsed, so the user doesn't see
 * a flash of "no entries" before data arrives.
 *
 * Usage:
 * ```
 * DelayedLoadingContainer(
 *     isLoading = state.isLoading,
 *     isEmpty = items.isEmpty(),
 *     emptyMessage = "No entries in this category"
 * ) {
 *     LazyColumn { ... }
 * }
 * ```
 */
@Composable
fun DelayedLoadingContainer(
    isLoading: Boolean,
    isEmpty: Boolean,
    emptyMessage: String = "No entries found",
    minLoadingMs: Long = 500L,
    content: @Composable () -> Unit
) {
    // Track whether we've passed the minimum loading period
    var minLoadingElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            minLoadingElapsed = false
            delay(minLoadingMs)
            minLoadingElapsed = true
        } else {
            // Give a brief moment even when not loading — covers synchronous
            // Flow emissions that arrive in the first frame
            delay(minLoadingMs)
            minLoadingElapsed = true
        }
    }

    when {
        // Still loading or haven't passed minimum period — show spinner
        isLoading || !minLoadingElapsed -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Loading done, data is empty — show empty message
        isEmpty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading done, data available — show content
        else -> content()
    }
}
