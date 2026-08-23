package com.goveye.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * Color transitions are animated using the same synchronized [Animatable]
 * progress pattern as [GovEyeTheme] — a single 0→1 float drives [lerp]
 * across every overridden color slot over ~400ms, so all party accent
 * colors reach their target simultaneously instead of snapping.
 *
 * Overridden slots:
 * - primary / onPrimary / primaryContainer / onPrimaryContainer
 * - secondary / onSecondary / secondaryContainer / onSecondaryContainer
 * - tertiary / onTertiary / tertiaryContainer / onTertiaryContainer
 * - surfaceTint
 *
 * Surface containers (surfaceContainer, surfaceContainerHigh, etc.) are
 * NOT overridden — they stay neutral (same as the nav bar) so rounded
 * section boxes don't pick up party colors.
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

private const val PARTY_ACCENT_ANIMATION_MS = 400

/**
 * Builds a [ColorScheme] with party-color accent overrides applied to the
 * current [MaterialTheme.colorScheme], with a synchronized animated
 * transition (same pattern as [GovEyeTheme]'s theme switching).
 *
 * Returns the unmodified scheme when [accent] is null (but still animates
 * back to the base scheme if transitioning from a party-accented one).
 *
 * @param accent the party color, or null
 * @param isDark whether the current theme is dark (affects on-colors)
 */
@Composable
fun partyAccentColorScheme(accent: Color?, isDark: Boolean): ColorScheme {
    val base = MaterialTheme.colorScheme
    val targetScheme = computePartyScheme(base, accent, isDark)

    // Synchronized animation — same pattern as GovEyeTheme.animatedColorScheme
    val fromScheme = remember { mutableStateOf(targetScheme) }
    val toScheme = remember { mutableStateOf(targetScheme) }
    val progress = remember { Animatable(1f) }
    val targetKey = targetScheme.primary.value to targetScheme.surfaceTint.value
    val lastKey = remember { mutableStateOf(targetKey) }

    LaunchedEffect(targetKey) {
        if (lastKey.value != targetKey) {
            val currentProgress = progress.value
            fromScheme.value = lerpScheme(fromScheme.value, toScheme.value, currentProgress)
            toScheme.value = targetScheme
            lastKey.value = targetKey
            progress.snapTo(0f)
            progress.animateTo(1f, tween(PARTY_ACCENT_ANIMATION_MS))
        }
    }

    val p = progress.value
    return lerpScheme(fromScheme.value, toScheme.value, p)
}

/**
 * Computes the party-accented ColorScheme from the base scheme.
 * Pure function — no animation, no side effects.
 */
private fun computePartyScheme(base: ColorScheme, accent: Color?, isDark: Boolean): ColorScheme {
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
        alpha = 1f
    )

    // Derive readable on-colors for the softened accent
    val luminance = softenedAccent.red * 0.299f + softenedAccent.green * 0.587f + softenedAccent.blue * 0.114f
    val onAccent = if (luminance > 0.5f) Color(0xFF1A1A1A) else Color.White
    val accentContainer = if (isDark) {
        softenedAccent.copy(
            alpha = 0.25f
        ).compositeOver(Color(0xFF1A1A1A))
    } else {
        softenedAccent.copy(alpha = 0.15f).compositeOver(Color.White)
    }
    val onAccentContainer = if (isDark) softenedAccent.copy(alpha = 0.9f) else softenedAccent.copy(alpha = 0.8f)

    // Surface containers are NOT overridden — they stay neutral so rounded
    // section boxes match the nav bar color regardless of party.

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
        surfaceTint = softenedAccent
    )
}

/**
 * Linearly interpolate every color slot between [from] and [to] by [progress].
 * Mirrors the lerpScheme in GovEyeTheme.kt — kept here to avoid coupling.
 */
private fun lerpScheme(from: ColorScheme, to: ColorScheme, progress: Float): ColorScheme {
    if (progress >= 1f) return to
    if (progress <= 0f) return from
    return to.copy(
        primary = lerp(from.primary, to.primary, progress),
        onPrimary = lerp(from.onPrimary, to.onPrimary, progress),
        primaryContainer = lerp(from.primaryContainer, to.primaryContainer, progress),
        onPrimaryContainer = lerp(from.onPrimaryContainer, to.onPrimaryContainer, progress),
        inversePrimary = lerp(from.inversePrimary, to.inversePrimary, progress),
        secondary = lerp(from.secondary, to.secondary, progress),
        onSecondary = lerp(from.onSecondary, to.onSecondary, progress),
        secondaryContainer = lerp(from.secondaryContainer, to.secondaryContainer, progress),
        onSecondaryContainer = lerp(from.onSecondaryContainer, to.onSecondaryContainer, progress),
        tertiary = lerp(from.tertiary, to.tertiary, progress),
        onTertiary = lerp(from.onTertiary, to.onTertiary, progress),
        tertiaryContainer = lerp(from.tertiaryContainer, to.tertiaryContainer, progress),
        onTertiaryContainer = lerp(from.onTertiaryContainer, to.onTertiaryContainer, progress),
        background = lerp(from.background, to.background, progress),
        onBackground = lerp(from.onBackground, to.onBackground, progress),
        surface = lerp(from.surface, to.surface, progress),
        onSurface = lerp(from.onSurface, to.onSurface, progress),
        surfaceVariant = lerp(from.surfaceVariant, to.surfaceVariant, progress),
        onSurfaceVariant = lerp(from.onSurfaceVariant, to.onSurfaceVariant, progress),
        surfaceTint = lerp(from.surfaceTint, to.surfaceTint, progress),
        inverseSurface = lerp(from.inverseSurface, to.inverseSurface, progress),
        inverseOnSurface = lerp(from.inverseOnSurface, to.inverseOnSurface, progress),
        error = lerp(from.error, to.error, progress),
        onError = lerp(from.onError, to.onError, progress),
        errorContainer = lerp(from.errorContainer, to.errorContainer, progress),
        onErrorContainer = lerp(from.onErrorContainer, to.onErrorContainer, progress),
        outline = lerp(from.outline, to.outline, progress),
        outlineVariant = lerp(from.outlineVariant, to.outlineVariant, progress),
        scrim = lerp(from.scrim, to.scrim, progress),
        surfaceContainerLowest = lerp(from.surfaceContainerLowest, to.surfaceContainerLowest, progress),
        surfaceContainerLow = lerp(from.surfaceContainerLow, to.surfaceContainerLow, progress),
        surfaceContainer = lerp(from.surfaceContainer, to.surfaceContainer, progress),
        surfaceContainerHigh = lerp(from.surfaceContainerHigh, to.surfaceContainerHigh, progress),
        surfaceContainerHighest = lerp(from.surfaceContainerHighest, to.surfaceContainerHighest, progress),
        surfaceBright = lerp(from.surfaceBright, to.surfaceBright, progress),
        surfaceDim = lerp(from.surfaceDim, to.surfaceDim, progress)
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
