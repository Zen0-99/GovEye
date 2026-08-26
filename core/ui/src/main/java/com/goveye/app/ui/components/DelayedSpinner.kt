package com.goveye.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Shows a [CircularProgressIndicator] only after [delayMs] has elapsed.
 *
 * If loading finishes before the delay, the spinner never appears — the user
 * sees content directly with no flash of a spinner. This is the global pattern
 * for all loading states in the app (per user request: 0.5s delay before spinner).
 *
 * Use in place of a bare `CircularProgressIndicator` when wrapping it in an
 * `if (isLoading)` block:
 *
 * ```
 * if (state.isLoading && state.data == null) {
 *     DelayedSpinner()
 * } else { ... }
 * ```
 */
@Composable
fun DelayedSpinner(delayMs: Long = 500L, modifier: Modifier = Modifier) {
    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        showSpinner = true
    }
    if (showSpinner) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
