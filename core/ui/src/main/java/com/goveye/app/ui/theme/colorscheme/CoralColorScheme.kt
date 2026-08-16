package com.goveye.app.ui.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Coral color scheme — muted purple (D-06, D-07).
 *
 * Seed color: ~#8B6FA3 (hue ~280, low chroma).
 * This is the DEFAULT scheme (D-07).
 * Full M3 palette generated via Material Theme Builder tonal system.
 */
internal object CoralColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFD2BCF0),
            onPrimary = Color(0xFF3A2956),
            primaryContainer = Color(0xFF514070),
            onPrimaryContainer = Color(0xFFEBDDFF),
            inversePrimary = Color(0xFF6B5A80),
            secondary = Color(0xFFCDC2D8),
            onSecondary = Color(0xFF342D3F),
            secondaryContainer = Color(0xFF4B444F),
            onSecondaryContainer = Color(0xFFE9DEF0),
            tertiary = Color(0xFFF0B2C5),
            onTertiary = Color(0xFF4A1F2D),
            tertiaryContainer = Color(0xFF643544),
            onTertiaryContainer = Color(0xFFFFD9E2),
            background = Color(0xFF141218),
            onBackground = Color(0xFFE5E0E8),
            surface = Color(0xFF141218),
            onSurface = Color(0xFFE5E0E8),
            surfaceVariant = Color(0xFF4A454E),
            onSurfaceVariant = Color(0xFFC9C4CE),
            surfaceTint = Color(0xFFD2BCF0),
            inverseSurface = Color(0xFFE5E0E8),
            inverseOnSurface = Color(0xFF141218),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF948F98),
            outlineVariant = Color(0xFF4A454E),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF0E0D12),
            surfaceContainerLow = Color(0xFF1C1A20),
            surfaceContainer = Color(0xFF201E24),
            surfaceContainerHigh = Color(0xFF2A282E),
            surfaceContainerHighest = Color(0xFF353339),
            surfaceBright = Color(0xFF3B383F),
            surfaceDim = Color(0xFF141218)
        )

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF6B5A80),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEBDDFF),
            onPrimaryContainer = Color(0xFF251443),
            inversePrimary = Color(0xFFD2BCF0),
            secondary = Color(0xFF635B70),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE9DEF0),
            onSecondaryContainer = Color(0xFF1F1929),
            tertiary = Color(0xFF7E5260),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD9E2),
            onTertiaryContainer = Color(0xFF31101D),
            background = Color(0xFFFEF7FF),
            onBackground = Color(0xFF1D1A20),
            surface = Color(0xFFFEF7FF),
            onSurface = Color(0xFF1D1A20),
            surfaceVariant = Color(0xFFE7E0EB),
            onSurfaceVariant = Color(0xFF4A454E),
            surfaceTint = Color(0xFF6B5A80),
            inverseSurface = Color(0xFF322F35),
            inverseOnSurface = Color(0xFFF5EFF7),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF7C757E),
            outlineVariant = Color(0xFFC9C4CE),
            scrim = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF8F1FA),
            surfaceContainer = Color(0xFFF2EBF4),
            surfaceContainerHigh = Color(0xFFFCF5FE),
            surfaceContainerHighest = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFFEF7FF),
            surfaceDim = Color(0xFFDED8E0)
        )
}
