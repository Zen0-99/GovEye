package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Sky color scheme — muted blue-gray accents with blue background (dark) /
 * white background (light).
 *
 * The accent colors have a faint blue hue (muted Monet style) — not pure
 * gray, but not vibrant either. A 70/30 blend of neutral gray and soft blue
 * gives the UI a subtle cool tone that feels modern without being colorful.
 *
 * Background and surface containers retain the original blue (dark) / white
 * (light) — only the accent slots (primary, secondary, tertiary, containers,
 * outlines) carry the blue tint.
 *
 * Party colors are applied per-profile via a CompositionLocal override —
 * see [com.goveye.app.ui.theme.LocalPartyAccent].
 *
 * Dark mode: deep navy background (#0D1622), blue-gray accents.
 * Light mode: off-white background (#FAFAFA), blue-gray accents.
 */
internal object SkyColorScheme : BaseColorScheme() {
    // Muted blue accent — 70% gray + 30% soft blue (#5B7FA8)
    // This gives a faint blue hue without being vibrant
    private val mutedBlueDark = Color(0xFFA8B5C7) // light blue-gray for dark mode accents
    private val mutedBlueDarkContainer = Color(0xFF2A3548) // darker blue-gray container
    private val mutedBlueLight = Color(0xFF4A6080) // medium blue-gray for light mode accents
    private val mutedBlueLightContainer = Color(0xFFD8E0EC) // light blue-gray container

    override val darkScheme: ColorScheme =
        darkColorScheme(
            primary = mutedBlueDark,
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = mutedBlueDarkContainer,
            onPrimaryContainer = Color(0xFFE0E8F0),
            inversePrimary = Color(0xFF5B7FA8),
            secondary = mutedBlueDark,
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = mutedBlueDarkContainer,
            onSecondaryContainer = Color(0xFFE0E8F0),
            tertiary = mutedBlueDark,
            onTertiary = Color(0xFF1A1A1A),
            tertiaryContainer = mutedBlueDarkContainer,
            onTertiaryContainer = Color(0xFFE0E8F0),
            background = Color(0xFF0D1622),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF0D1622),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF1E2530),
            onSurfaceVariant = Color(0xFFA0AAB8),
            surfaceTint = mutedBlueDark,
            inverseSurface = Color(0xFFE0E0E0),
            inverseOnSurface = Color(0xFF0D1622),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF6070A0),
            outlineVariant = Color(0xFF404858),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF08111C),
            surfaceContainerLow = Color(0xFF101824),
            surfaceContainer = Color(0xFF141D2A),
            surfaceContainerHigh = Color(0xFF1A2330),
            surfaceContainerHighest = Color(0xFF222B38),
            surfaceBright = Color(0xFF2A3340),
            surfaceDim = Color(0xFF0A1220)
        )

    override val lightScheme: ColorScheme =
        lightColorScheme(
            primary = mutedBlueLight,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = mutedBlueLightContainer,
            onPrimaryContainer = Color(0xFF1A2535),
            inversePrimary = Color(0xFFA8B5C7),
            secondary = mutedBlueLight,
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = mutedBlueLightContainer,
            onSecondaryContainer = Color(0xFF1A2535),
            tertiary = mutedBlueLight,
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = mutedBlueLightContainer,
            onTertiaryContainer = Color(0xFF1A2535),
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFAFAFA),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF0F2F5),
            onSurfaceVariant = Color(0xFF5A6878),
            surfaceTint = mutedBlueLight,
            inverseSurface = Color(0xFF2A2A2A),
            inverseOnSurface = Color(0xFFF0F0F0),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF8090A8),
            outlineVariant = Color(0xFFD0D8E0),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF0F0F0),
            surfaceContainer = Color(0xFFE8E8E8),
            surfaceContainerHigh = Color(0xFFE2E2E2),
            surfaceContainerHighest = Color(0xFFDCDCDC),
            surfaceBright = Color(0xFFFAFAFA),
            surfaceDim = Color(0xFFE0E0E0)
        )
}
