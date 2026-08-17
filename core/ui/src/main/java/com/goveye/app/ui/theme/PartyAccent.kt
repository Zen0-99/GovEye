package com.goveye.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Per-profile party accent color system.
 *
 * The app theme is grayscale (Sky). When viewing an MP profile, the party
 * color overrides specific accent slots so the profile feels branded by
 * the MP's party (e.g., Labour red, Conservative blue, Lib Dem yellow).
 *
 * [LocalPartyAccent] holds the raw party color (or null when not in a
 * profile context). [partyAccentColorScheme] returns a modified
 * [ColorScheme] with the accent slots overridden — used by a nested
 * [MaterialTheme] wrapper in the profile screen.
 *
 * Overridden slots:
 * - primary / onPrimary / primaryContainer / onPrimaryContainer
 * - secondary / onSecondary / secondaryContainer / onSecondaryContainer
 * - tertiary / onTertiary / tertiaryContainer / onTertiaryContainer
 * - surfaceTint
 *
 * Surface containers get a subtle party tint (8% blend) so rounded boxes
 * feel party-themed without overwhelming readability.
 */
val LocalPartyAccent = compositionLocalOf<Color?> { null }

/**
 * Returns the party accent color from [LocalPartyAccent], or null if not
 * in a profile context.
 */
val partyAccent: Color?
    @Composable
    @ReadOnlyComposable
    get() = LocalPartyAccent.current

/**
 * Builds a [ColorScheme] with party-color accent overrides applied to the
 * current [MaterialTheme.colorScheme]. Returns the unmodified scheme when
 * [accent] is null.
 *
 * @param accent the party color, or null
 * @param isDark whether the current theme is dark (affects on-colors)
 */
@Composable
fun partyAccentColorScheme(
    accent: Color?,
    isDark: Boolean,
): ColorScheme {
    val base = MaterialTheme.colorScheme
    if (accent == null) return base

    // Desaturate the party color 45% toward neutral gray — still clearly
    // party-colored but not overwhelming. Raw party colors (e.g. Labour
    // red #DC241f) are too saturated for UI accents; pure muted (70/30)
    // is too washed out. 55/45 is the middle ground.
    val neutral = Color(0xFF808080)
    val softenedAccent = Color(
        red = accent.red * 0.55f + neutral.red * 0.45f,
        green = accent.green * 0.55f + neutral.green * 0.45f,
        blue = accent.blue * 0.55f + neutral.blue * 0.45f,
        alpha = 1f,
    )

    // Derive readable on-colors for the softened accent
    val luminance = softenedAccent.red * 0.299f + softenedAccent.green * 0.587f + softenedAccent.blue * 0.114f
    val onAccent = if (luminance > 0.5f) Color(0xFF1A1A1A) else Color.White
    val accentContainer = if (isDark) softenedAccent.copy(alpha = 0.25f).compositeOver(Color(0xFF1A1A1A)) else softenedAccent.copy(alpha = 0.15f).compositeOver(Color.White)
    val onAccentContainer = if (isDark) softenedAccent.copy(alpha = 0.9f) else softenedAccent.copy(alpha = 0.8f)

    // Subtle party tint on surface containers (5% blend — less than before)
    val tintedSurfaceContainer = lerp(base.surfaceContainer, softenedAccent, 0.05f)
    val tintedSurfaceContainerHigh = lerp(base.surfaceContainerHigh, softenedAccent, 0.05f)
    val tintedSurfaceContainerHighest = lerp(base.surfaceContainerHighest, softenedAccent, 0.05f)
    val tintedSurfaceContainerLow = lerp(base.surfaceContainerLow, softenedAccent, 0.04f)
    val tintedSurfaceVariant = lerp(base.surfaceVariant, softenedAccent, 0.04f)

    return base.copy(
        primary = softenedAccent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        secondary = softenedAccent,
        onSecondary = onAccent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        tertiary = softenedAccent,
        onTertiary = onAccent,
        tertiaryContainer = accentContainer,
        onTertiaryContainer = onAccentContainer,
        surfaceTint = softenedAccent,
        surfaceVariant = tintedSurfaceVariant,
        surfaceContainerLow = tintedSurfaceContainerLow,
        surfaceContainer = tintedSurfaceContainer,
        surfaceContainerHigh = tintedSurfaceContainerHigh,
        surfaceContainerHighest = tintedSurfaceContainerHighest,
    )
}

/** Composites [this] color over [background] (alpha blending). */
private fun Color.compositeOver(background: Color): Color {
    if (alpha >= 1f) return this
    val a = alpha + background.alpha * (1f - alpha)
    if (a <= 0f) return Color.Transparent
    val r = (red * alpha + background.red * background.alpha * (1f - alpha)) / a
    val g = (green * alpha + background.green * background.alpha * (1f - alpha)) / a
    val b = (blue * alpha + background.blue * background.alpha * (1f - alpha)) / a
    return Color(r, g, b, a)
}
