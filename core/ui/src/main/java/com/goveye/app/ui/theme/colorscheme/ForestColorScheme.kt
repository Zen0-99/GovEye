package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Forest color scheme — muted forest green (D-06).
 *
 * Seed color: ~#4A7C59 (hue ~135, low chroma).
 * Full M3 palette generated via Material Theme Builder tonal system.
 */
internal object ForestColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFF9DD4A8),
            onPrimary = Color(0xFF00391A),
            primaryContainer = Color(0xFF1B5E2C),
            onPrimaryContainer = Color(0xFFBAF0C3),
            inversePrimary = Color(0xFF386B46),
            secondary = Color(0xFFB6CCBA),
            onSecondary = Color(0xFF213528),
            secondaryContainer = Color(0xFF374B3D),
            onSecondaryContainer = Color(0xFFD0E8D5),
            tertiary = Color(0xFFA7CCDF),
            onTertiary = Color(0xFF0A3548),
            tertiaryContainer = Color(0xFF234B5F),
            onTertiaryContainer = Color(0xFFC2E8FB),
            background = Color(0xFF101510),
            onBackground = Color(0xFFE0E4DD),
            surface = Color(0xFF101510),
            onSurface = Color(0xFFE0E4DD),
            surfaceVariant = Color(0xFF404940),
            onSurfaceVariant = Color(0xFFC0C9BF),
            surfaceTint = Color(0xFF9DD4A8),
            inverseSurface = Color(0xFFE0E4DD),
            inverseOnSurface = Color(0xFF101510),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF8A938A),
            outlineVariant = Color(0xFF404940),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF0B0F0A),
            surfaceContainerLow = Color(0xFF181D17),
            surfaceContainer = Color(0xFF1C211B),
            surfaceContainerHigh = Color(0xFF262B25),
            surfaceContainerHighest = Color(0xFF31362F),
            surfaceBright = Color(0xFF363B34),
            surfaceDim = Color(0xFF101510)
        )

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF386B46),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFBAF0C3),
            onPrimaryContainer = Color(0xFF00210B),
            inversePrimary = Color(0xFF9DD4A8),
            secondary = Color(0xFF4E6354),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD0E8D5),
            onSecondaryContainer = Color(0xFF0D1F13),
            tertiary = Color(0xFF3F6374),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFC2E8FB),
            onTertiaryContainer = Color(0xFF001E2E),
            background = Color(0xFFF7FBF4),
            onBackground = Color(0xFF181D17),
            surface = Color(0xFFF7FBF4),
            onSurface = Color(0xFF181D17),
            surfaceVariant = Color(0xFFDCE5DB),
            onSurfaceVariant = Color(0xFF404940),
            surfaceTint = Color(0xFF386B46),
            inverseSurface = Color(0xFF2D322B),
            inverseOnSurface = Color(0xFFEEF2EB),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF707971),
            outlineVariant = Color(0xFFC0C9BF),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF1F5EE),
            surfaceContainer = Color(0xFFEBEFE9),
            surfaceContainerHigh = Color(0xFFF5F9F3),
            surfaceContainerHighest = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFF7FBF4),
            surfaceDim = Color(0xFFD7DBD4)
        )
}
