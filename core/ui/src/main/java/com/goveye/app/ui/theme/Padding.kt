package com.goveye.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

/**
 * Standardised spacing tokens (D-05, RESEARCH.md §2.1).
 *
 * Ported from Miko's [tachiyomi.presentation.core.components.material.Padding].
 * Accessed via [MaterialTheme.padding] extension so spacing is theme-aware
 * and consistent across all composables.
 */
class Padding {
    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val mediumSmall = 12.dp

    val small = 8.dp

    val extraSmall = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()
