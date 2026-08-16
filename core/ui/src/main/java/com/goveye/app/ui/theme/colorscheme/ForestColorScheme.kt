package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Forest color scheme — muted forest green (D-06).
 *
 * Seed color: ~#4A7C59 (hue ~135, low chroma).
 * Full M3 palette generated via Material Theme Builder tonal system.
 *
 * Dark mode: darkened primary/container tones to reduce intensity.
 * Light mode: reduced brightness and saturation on container tones.
 */
internal object ForestColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFF7DBA8A),
            onPrimary = Color(0xFF00391A),
            primaryContainer = Color(0xFF154523),
            onPrimaryContainer = Color(0xFFA5D8B0),
            inversePrimary = Color(0xFF386B46),
            secondary = Color(0xFF9DB2A2),
            onSecondary = Color(0xFF213528),
            secondaryContainer = Color(0xFF374B3D),
            onSecondaryContainer = Color(0xFFB8D4BE),
            tertiary = Color(0xFF8DB5C9),
            onTertiary = Color(0xFF0A3548),
            tertiaryContainer = Color(0xFF234B5F),
            onTertiaryContainer = Color(0xFFA5D5E8),
            background = Color(0xFF101510),
            onBackground = Color(0xFFD0D4CD),
            surface = Color(0xFF101510),
            onSurface = Color(0xFFD0D4CD),
            surfaceVariant = Color(0xFF404940),
            onSurfaceVariant = Color(0xFFB0B9AF),
            surfaceTint = Color(0xFF7DBA8A),
            inverseSurface = Color(0xFFD0D4CD),
            inverseOnSurface = Color(0xFF101510),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF7A837A),
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
            primary = Color(0xFF3D6B50),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFA8D8B5),
            onPrimaryContainer = Color(0xFF00210B),
            inversePrimary = Color(0xFF7DBA8A),
            secondary = Color(0xFF526759),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFBCD4C5),
            onSecondaryContainer = Color(0xFF0D1F13),
            tertiary = Color(0xFF446776),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFAFD8E8),
            onTertiaryContainer = Color(0xFF001E2E),
            background = Color(0xFFF0F4ED),
            onBackground = Color(0xFF181D17),
            surface = Color(0xFFF0F4ED),
            onSurface = Color(0xFF181D17),
            surfaceVariant = Color(0xFFDCE5DB),
            onSurfaceVariant = Color(0xFF404940),
            surfaceTint = Color(0xFF3D6B50),
            inverseSurface = Color(0xFF2D322B),
            inverseOnSurface = Color(0xFFEEF2EB),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF707971),
            outlineVariant = Color(0xFFB0B9AF),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEAEDE6),
            surfaceContainer = Color(0xFFE4E8E2),
            surfaceContainerHigh = Color(0xFFEEF2EC),
            surfaceContainerHighest = Color(0xFFF9FCF6),
            surfaceBright = Color(0xFFF0F4ED),
            surfaceDim = Color(0xFFD0D4CE)
        )
}
