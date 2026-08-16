package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Ember color scheme — muted red-ember (D-06).
 *
 * Seed color: ~#A04030 (hue ~10, medium chroma).
 * Shifted toward red from original terracotta to avoid peach/brown appearance.
 * Full M3 palette generated via Material Theme Builder tonal system.
 */
internal object EmberColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFFF9888),
            onPrimary = Color(0xFF5C1A12),
            primaryContainer = Color(0xFF8C2A1A),
            onPrimaryContainer = Color(0xFFFFD0CC),
            inversePrimary = Color(0xFFA04030),
            secondary = Color(0xFFE8A89A),
            onSecondary = Color(0xFF4A231C),
            secondaryContainer = Color(0xFF6B332B),
            onSecondaryContainer = Color(0xFFFFD0CC),
            tertiary = Color(0xFFDBBE6D),
            onTertiary = Color(0xFF392E00),
            tertiaryContainer = Color(0xFF524411),
            onTertiaryContainer = Color(0xFFF8D785),
            background = Color(0xFF1A1212),
            onBackground = Color(0xFFF0D5D0),
            surface = Color(0xFF1A1212),
            onSurface = Color(0xFFF0D5D0),
            surfaceVariant = Color(0xFF533838),
            onSurfaceVariant = Color(0xFFD8B5B0),
            surfaceTint = Color(0xFFFF9888),
            inverseSurface = Color(0xFFF0D5D0),
            inverseOnSurface = Color(0xFF1A1212),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF9E7B78),
            outlineVariant = Color(0xFF533838),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF140D0D),
            surfaceContainerLow = Color(0xFF221818),
            surfaceContainer = Color(0xFF261C1C),
            surfaceContainerHigh = Color(0xFF312626),
            surfaceContainerHighest = Color(0xFF3C3030),
            surfaceBright = Color(0xFF423535),
            surfaceDim = Color(0xFF1A1212)
        )

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFFA04030),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD0CC),
            onPrimaryContainer = Color(0xFF2F0A04),
            inversePrimary = Color(0xFFFF9888),
            secondary = Color(0xFF7D5249),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFD0CC),
            onSecondaryContainer = Color(0xFF2C1510),
            tertiary = Color(0xFF6E5627),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF8D785),
            onTertiaryContainer = Color(0xFF221A00),
            background = Color(0xFFFFFBFF),
            onBackground = Color(0xFF201A18),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF201A18),
            surfaceVariant = Color(0xFFF5D8D2),
            onSurfaceVariant = Color(0xFF533838),
            surfaceTint = Color(0xFFA04030),
            inverseSurface = Color(0xFF362F2C),
            inverseOnSurface = Color(0xFFFBEEEA),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF856561),
            outlineVariant = Color(0xFFD8B5B0),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFCF1ED),
            surfaceContainer = Color(0xFFF6EBE7),
            surfaceContainerHigh = Color(0xFFFFF5F1),
            surfaceContainerHighest = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFFFFBFF),
            surfaceDim = Color(0xFFE0D4D0)
        )
}
