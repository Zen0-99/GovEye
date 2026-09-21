package com.goveye.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Card surface color adjusted to stand out more against the background.
 * Brighter in dark mode, darker in light mode. Optionally blends an accent
 * tint (party colour, vote colour, trend colour) into the adjusted base.
 *
 * This is the shared card background used across the feed and the MP profile
 * so every section reads at the same elevation. Pass [tintColor] to carry a
 * subtle accent; omit it for a neutral section card.
 */
@Composable
fun cardSurfaceColor(tintColor: Color? = null): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = MaterialTheme.colorScheme.surfaceContainer
    val adjustedBase = if (isDark) {
        lerp(baseColor, Color.White, 0.04f)
    } else {
        lerp(baseColor, Color.Black, 0.02f)
    }
    return if (tintColor == null) adjustedBase else lerp(adjustedBase, tintColor, 0.08f)
}
