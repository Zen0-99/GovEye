package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Ember color scheme — muted terracotta (D-06).
 *
 * Seed color: ~#B8654A (hue ~20, low chroma).
 * Full M3 palette generated via Material Theme Builder tonal system.
 */
internal object EmberColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFFFB5A0),
            onPrimary = Color(0xFF561F14),
            primaryContainer = Color(0xFF723525),
            onPrimaryContainer = Color(0xFFFFDAD2),
            inversePrimary = Color(0xFF8C4B3A),
            secondary = Color(0xFFE7BDB0),
            onSecondary = Color(0xFF442A21),
            secondaryContainer = Color(0xFF5D4037),
            onSecondaryContainer = Color(0xFFFFDAD2),
            tertiary = Color(0xFFDBBE6D),
            onTertiary = Color(0xFF392E00),
            tertiaryContainer = Color(0xFF524411),
            onTertiaryContainer = Color(0xFFF8D785),
            background = Color(0xFF1A1311),
            onBackground = Color(0xFFF0DEDA),
            surface = Color(0xFF1A1311),
            onSurface = Color(0xFFF0DEDA),
            surfaceVariant = Color(0xFF53433F),
            onSurfaceVariant = Color(0xFFD8C2BC),
            surfaceTint = Color(0xFFFFB5A0),
            inverseSurface = Color(0xFFF0DEDA),
            inverseOnSurface = Color(0xFF1A1311),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF9E8C87),
            outlineVariant = Color(0xFF53433F),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF140E0C),
            surfaceContainerLow = Color(0xFF221B19),
            surfaceContainer = Color(0xFF261F1D),
            surfaceContainerHigh = Color(0xFF312927),
            surfaceContainerHighest = Color(0xFF3C3431),
            surfaceBright = Color(0xFF423936),
            surfaceDim = Color(0xFF1A1311)
        )

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF8C4B3A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDAD2),
            onPrimaryContainer = Color(0xFF2F0A04),
            inversePrimary = Color(0xFFFFB5A0),
            secondary = Color(0xFF77574D),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDAD2),
            onSecondaryContainer = Color(0xFF2C1510),
            tertiary = Color(0xFF6E5627),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF8D785),
            onTertiaryContainer = Color(0xFF221A00),
            background = Color(0xFFFFFBFF),
            onBackground = Color(0xFF201A18),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF201A18),
            surfaceVariant = Color(0xFFF5DED8),
            onSurfaceVariant = Color(0xFF53433F),
            surfaceTint = Color(0xFF8C4B3A),
            inverseSurface = Color(0xFF362F2C),
            inverseOnSurface = Color(0xFFFBEEEA),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF85736E),
            outlineVariant = Color(0xFFD8C2BC),
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
