package com.goveye.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a hex color string (e.g. "d50000") and returns a muted variant.
 * Muting: blend 30% toward neutral gray to desaturate (D-13).
 */
fun parseMutedPartyColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF757575)
    return try {
        val raw = Color(android.graphics.Color.parseColor("#$hex"))
        val neutral = Color(0xFF757575)
        Color(
            red = raw.red * 0.7f + neutral.red * 0.3f,
            green = raw.green * 0.7f + neutral.green * 0.3f,
            blue = raw.blue * 0.7f + neutral.blue * 0.3f,
            alpha = 1f
        )
    } catch (e: Exception) {
        Color(0xFF757575)
    }
}

/**
 * Parses a hex color string (e.g. "d50000") and returns the raw color.
 * Used for borders and subtle backgrounds where muting is not desired.
 */
fun parsePartyColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF757575)
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: Exception) {
        Color(0xFF757575)
    }
}

/** Derives initials from a display name (max 2 chars). E.g. "Diane Abbott" -> "DA". */
fun deriveInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}
