package com.goveye.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware vote colors.
 *
 * Aye (teal): brighter in dark mode for visibility on dark surfaces.
 * No (orange): darker in light mode for visibility on light surfaces.
 */
object VoteColors {
    /** Aye/Content — teal. Brighter in dark mode. */
    val aye: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF4DB6AC) else Color(0xFF00796B)

    /** No/Not Content — orange/red. Brighter in dark mode. */
    val no: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF7043) else Color(0xFFD84315)

    /** No vote recorded — gray. Same in both modes. */
    val noVote: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFBDBDBD) else Color(0xFF9E9E9E)
}
