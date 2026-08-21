package com.goveye.app.ui.utils

import com.goveye.app.R

/**
 * Maps party IDs to drawable resource IDs for party logos.
 * Returns null if no logo exists — callers should fall back to abbreviation text.
 */
fun partyLogoResId(partyId: Int): Int? = when (partyId) {
    1 -> R.drawable.party_logo_1

    // Alliance
    4 -> R.drawable.party_logo_4

    // Conservative
    7 -> R.drawable.party_logo_7

    // DUP
    8 -> R.drawable.party_logo_8

    // Independent
    15 -> R.drawable.party_logo_15

    // Labour
    17 -> R.drawable.party_logo_17

    // Liberal Democrats
    22 -> R.drawable.party_logo_22

    // Plaid Cymru
    29 -> R.drawable.party_logo_29

    // SNP
    30 -> R.drawable.party_logo_30

    // Sinn Féin
    31 -> R.drawable.party_logo_31

    // SDLP
    38 -> R.drawable.party_logo_38

    // UUP
    44 -> R.drawable.party_logo_44

    // Green
    47 -> R.drawable.party_logo_47

    // Speaker
    158 -> R.drawable.party_logo_158

    // TUV
    1036 -> R.drawable.party_logo_1036

    // Reform UK
    1115 -> R.drawable.party_logo_1115

    // Your Party
    1117 -> R.drawable.party_logo_1117

    // Restore Britain
    else -> null
}
