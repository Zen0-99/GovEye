package com.goveye.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.goveye.app.domain.AppTheme
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.theme.colorscheme.BaseColorScheme
import com.goveye.app.ui.theme.colorscheme.CoralColorScheme
import com.goveye.app.ui.theme.colorscheme.EmberColorScheme
import com.goveye.app.ui.theme.colorscheme.ForestColorScheme
import com.goveye.app.ui.theme.colorscheme.SkyColorScheme

/**
 * GovEye M3 theme composable (D-05, DESIGN-01, DESIGN-02).
 *
 * Ported from Miko's [eu.kanade.presentation.theme.TachiyomiTheme], simplified:
 * - No ContentMode parameter (D-08)
 * - No Moko Resources
 * - No Injekt — theme state passed as parameters (collected from
 *   [com.goveye.app.data.preference.ThemePreferences] by the caller in :app)
 * - M3 default typography (D-09 — no custom Typography.kt)
 * - No material-color-utilities for Sky (dynamic) — system dynamic color API + Coral fallback
 *
 * Colors animate via a single synchronized [Animatable] progress value that
 * drives [lerp] across every color slot — all colors reach their target
 * simultaneously over ~400ms (D-05).
 *
 * @param appTheme the selected color scheme (Forest/Sky/Ember/Coral)
 * @param themeMode light/dark/system preference
 * @param isAmoled AMOLED toggle — sets surfaces to pure black in dark mode
 * @param content the composable content to theme
 */
@Composable
fun GovEyeTheme(appTheme: AppTheme, themeMode: ThemeMode, isAmoled: Boolean, content: @Composable () -> Unit) {
    val isDark =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    BaseGovEyeTheme(
        appTheme = appTheme,
        isAmoled = isAmoled,
        isDark = isDark,
        content = content
    )
}

/**
 * Preview-friendly overload with explicit dark/light control.
 */
@Composable
fun GovEyeTheme(
    appTheme: AppTheme = AppTheme.CORAL,
    isAmoled: Boolean = false,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    BaseGovEyeTheme(
        appTheme = appTheme,
        isAmoled = isAmoled,
        isDark = isDark,
        content = content
    )
}

@Composable
private fun BaseGovEyeTheme(appTheme: AppTheme, isAmoled: Boolean, isDark: Boolean, content: @Composable () -> Unit) {
    val targetScheme = getThemeColorScheme(appTheme, isAmoled, isDark)
    val colorScheme = animatedColorScheme(targetScheme)
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private const val THEME_ANIMATION_DURATION_MS = 400

/**
 * Synchronized theme color transition using a single [Animatable] progress value.
 *
 * Instead of animating each color slot independently (28+ separate
 * [androidx.compose.animation.animateColorAsState] instances), a single
 * progress float (0→1) drives [lerp] across every color in the [ColorScheme].
 *
 * The animation fires whenever the [target] ColorScheme changes — detected by
 * comparing the primary + background color values, which are unique per
 * theme+mode+amoled combination.
 */
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val fromScheme = remember { mutableStateOf(target) }
    val toScheme = remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    val targetKey = target.primary.value to target.background.value
    val lastKey = remember { mutableStateOf(targetKey) }

    LaunchedEffect(targetKey) {
        if (lastKey.value != targetKey) {
            val currentProgress = progress.value
            fromScheme.value = lerpScheme(fromScheme.value, toScheme.value, currentProgress)
            toScheme.value = target
            lastKey.value = targetKey
            progress.snapTo(0f)
            progress.animateTo(1f, tween(THEME_ANIMATION_DURATION_MS))
        }
    }

    val p = progress.value
    return lerpScheme(fromScheme.value, toScheme.value, p)
}

/**
 * Linearly interpolate every color slot between [from] and [to] by [progress].
 * Uses [Color.lerp] (RGB space) for each color.
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

@Composable
@ReadOnlyComposable
private fun getThemeColorScheme(appTheme: AppTheme, isAmoled: Boolean, isDark: Boolean): ColorScheme {
    val colorScheme: BaseColorScheme =
        if (appTheme == AppTheme.SKY) {
            SkyColorScheme(LocalContext.current)
        } else {
            colorSchemes.getValue(appTheme)
        }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> =
    mapOf(
        AppTheme.FOREST to ForestColorScheme,
        AppTheme.EMBER to EmberColorScheme,
        AppTheme.CORAL to CoralColorScheme
    )
