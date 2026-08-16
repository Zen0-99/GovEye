package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Sky color scheme — muted sky blue (D-06).
 *
 * Seed color: ~#5B8DB8 (hue ~210, low chroma).
 * Full M3 palette generated via Material Theme Builder tonal system.
 */
internal object SkyColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFF9ACAF6),
            onPrimary = Color(0xFF003253),
            primaryContainer = Color(0xFF28536F),
            onPrimaryContainer = Color(0xFFD1E4FF),
            inversePrimary = Color(0xFF446B8C),
            secondary = Color(0xFFBCC8DA),
            onSecondary = Color(0xFF263041),
            secondaryContainer = Color(0xFF3C4758),
            onSecondaryContainer = Color(0xFFD8E4F0),
            tertiary = Color(0xFFC0C4DD),
            onTertiary = Color(0xFF2A3043),
            tertiaryContainer = Color(0xFF40465B),
            onTertiaryContainer = Color(0xFFDDE0F9),
            background = Color(0xFF111316),
            onBackground = Color(0xFFE2E2E5),
            surface = Color(0xFF111316),
            onSurface = Color(0xFFE2E2E5),
            surfaceVariant = Color(0xFF43474E),
            onSurfaceVariant = Color(0xFFC3C7CF),
            surfaceTint = Color(0xFF9ACAF6),
            inverseSurface = Color(0xFFE2E2E5),
            inverseOnSurface = Color(0xFF111316),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF8D9199),
            outlineVariant = Color(0xFF43474E),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF0C0E10),
            surfaceContainerLow = Color(0xFF191B1E),
            surfaceContainer = Color(0xFF1D1F22),
            surfaceContainerHigh = Color(0xFF272A2D),
            surfaceContainerHighest = Color(0xFF323538),
            surfaceBright = Color(0xFF373A3D),
            surfaceDim = Color(0xFF111316)
        )

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF446B8C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1E4FF),
            onPrimaryContainer = Color(0xFF001D34),
            inversePrimary = Color(0xFF9ACAF6),
            secondary = Color(0xFF54606F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD8E4F0),
            onSecondaryContainer = Color(0xFF111D2A),
            tertiary = Color(0xFF5B6074),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFDDE0F9),
            onTertiaryContainer = Color(0xFF181B2C),
            background = Color(0xFFF9F9FC),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFF9F9FC),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFDFE3EB),
            onSurfaceVariant = Color(0xFF43474E),
            surfaceTint = Color(0xFF446B8C),
            inverseSurface = Color(0xFF2F3033),
            inverseOnSurface = Color(0xFFF0F0F3),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF74777F),
            outlineVariant = Color(0xFFC3C7CF),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF3F3F7),
            surfaceContainer = Color(0xFFEDEDF1),
            surfaceContainerHigh = Color(0xFFF7F7FB),
            surfaceContainerHighest = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFF9F9FC),
            surfaceDim = Color(0xFFD9D9DD)
        )
}
