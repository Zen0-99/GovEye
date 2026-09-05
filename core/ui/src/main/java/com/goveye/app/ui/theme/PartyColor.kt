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

/**
 * Party color overrides by MNIS party ID.
 *
 * Some MNIS API colors are inaccurate or missing. This map provides
 * corrected colors based on the BBC's official election color palette
 * and party branding. Used by [partyColorForId] when the MNIS color
 * is wrong or empty.
 *
 * Source: BBC election color hexes + Wikipedia party colour fields.
 */
private val PARTY_COLOR_OVERRIDES = mapOf(
    1 to "d6b429", // Alliance Party — gold (BBC)
    7 to "b51c4b", // DUP — dark red (BBC)
    38 to "3b75a8", // UUP — blue (BBC)
    1117 to "1a237e" // Restore Britain — navy blue (Wikipedia)
)

/**
 * Returns the correct party color for a given MNIS party ID.
 * Falls back to the provided hex string from the MNIS API if no
 * override exists. Returns a neutral gray if both are null/blank.
 */
fun partyColorForId(partyId: Int?, mnisHex: String?): Color {
    PARTY_COLOR_OVERRIDES[partyId]?.let { return parsePartyColor(it) }
    return parsePartyColor(mnisHex)
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

/**
 * Maps a UK party name to its hex color code.
 * Used for historical members who only have a party name string, not a colour.
 * Source: Parliament API party colours (latestParty.backgroundColour).
 */
private val PARTY_NAME_TO_COLOR = mapOf(
    "Labour" to "dc241f",
    "Conservative" to "0087dc",
    "Liberal Democrat" to "faa61a",
    "Scottish National Party" to "fdf38e",
    "Plaid Cymru" to "005b54",
    "Green Party" to "6ab023",
    "Reform UK" to "12b6cf",
    "Reform" to "12b6cf",
    "Brexit Party" to "12b6cf",
    "Democratic Unionist Party" to "b51c4b",
    "DUP" to "b51c4b",
    "Sinn Féin" to "0c6b33",
    "Sinn Fein" to "0c6b33",
    "Social Democratic and Labour Party" to "0c6b33",
    "SDLP" to "0c6b33",
    "Alliance Party" to "d6b429",
    "Alliance" to "d6b429",
    "Ulster Unionist Party" to "3b75a8",
    "UUP" to "3b75a8",
    "Restore Britain" to "1a237e",
    "Independent" to "a0a0a0",
    "Speaker" to "a0a0a0",
    "Labour/Co-operative" to "dc241f",
    "Labour Co-op" to "dc241f",
    "Conservative Party" to "0087dc",
    "Liberal Democrats" to "faa61a",
    "Lib Dem" to "faa61a",
    "UK Independence Party" to "12b6cf",
    "UKIP" to "12b6cf",
    "Change UK" to "a0a0a0",
    "The Independent Group for Change" to "a0a0a0"
)

/**
 * Looks up a party colour by name (case-insensitive).
 * Returns null if the party name is not recognised.
 */
fun partyNameToColorHex(partyName: String?): String? {
    if (partyName.isNullOrBlank()) return null
    return PARTY_NAME_TO_COLOR[partyName.trim()]
        ?: PARTY_NAME_TO_COLOR.entries.firstOrNull { (name, _) ->
            name.equals(partyName.trim(), ignoreCase = true) ||
                partyName.trim().contains(name, ignoreCase = true) ||
                name.contains(partyName.trim(), ignoreCase = true)
        }?.value
}
