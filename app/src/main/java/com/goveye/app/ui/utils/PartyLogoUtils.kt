package com.goveye.app.ui.utils

import com.goveye.app.R

/**
 * Maps party IDs to drawable resource IDs for party logos.
 * Returns null if no logo exists — callers should fall back to abbreviation text.
 */
fun partyLogoResId(partyId: Int): Int? = when (partyId) {
    4 -> R.drawable.party_logo_4

    // Conservative
    15 -> R.drawable.party_logo_15

    // Labour
    17 -> R.drawable.party_logo_17

    // Liberal Democrats
    44 -> R.drawable.party_logo_44

    // Green
    22 -> R.drawable.party_logo_22

    // Plaid Cymru
    29 -> R.drawable.party_logo_29

    // SNP
    1036 -> R.drawable.party_logo_1036

    // Reform UK
    else -> null
}
