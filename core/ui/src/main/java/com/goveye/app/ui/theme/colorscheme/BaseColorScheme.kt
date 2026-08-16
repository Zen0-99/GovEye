package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Abstract base for all custom color schemes (D-05, D-06).
 *
 * Ported from Miko's [eu.kanade.presentation.theme.colorscheme.BaseColorScheme].
 * Each subclass provides a full M3 [lightScheme] and [darkScheme] with all
 * 28+ color slots. [getColorScheme] resolves the correct variant and applies
 * the AMOLED override (pure-black surfaces) when requested.
 */
internal abstract class BaseColorScheme {
    abstract val darkScheme: ColorScheme

    abstract val lightScheme: ColorScheme

    // Cannot be pure black as there's content scrolling behind it
    // https://m3.material.io/components/navigation-bar/guidelines#90615a71-607e-485e-9e09-778bfc080563
    private val amoledSurfaceContainer = Color(0xFF0C0C0C)
    private val amoledSurfaceContainerHigh = Color(0xFF131313)
    private val amoledSurfaceContainerHighest = Color(0xFF1B1B1B)

    fun getColorScheme(isDark: Boolean, isAmoled: Boolean): ColorScheme {
        if (!isDark) return lightScheme.withSingleAccent()

        if (!isAmoled) return darkScheme.withSingleAccent()

        return darkScheme.withSingleAccent().copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = amoledSurfaceContainer,
            surfaceContainerLowest = amoledSurfaceContainer,
            surfaceContainerLow = amoledSurfaceContainer,
            surfaceContainer = amoledSurfaceContainer,
            surfaceContainerHigh = amoledSurfaceContainerHigh,
            surfaceContainerHighest = amoledSurfaceContainerHighest
        )
    }

    private fun ColorScheme.withSingleAccent(): ColorScheme = copy(
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer
    )
}
