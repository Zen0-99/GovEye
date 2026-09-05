package com.goveye.app.ui.utils

import com.goveye.app.R

/**
 * Maps MNIS party IDs to drawable resource IDs for party logos.
 * Returns null if no logo exists — callers should fall back to abbreviation text.
 *
 * Party IDs are from the UK Parliament MNIS API:
 * https://github.com/ukparliament/ontologies/blob/master/meta/relational/mnis/general-election/backups/parties/mnis.csv
 */
fun partyLogoResId(partyId: Int): Int? = when (partyId) {
    // Alliance Party of Northern Ireland
    1 -> R.drawable.party_logo_1

    // Conservative Party
    4 -> R.drawable.party_logo_4

    // Democratic Unionist Party (DUP)
    7 -> R.drawable.party_logo_7

    // Independent
    8 -> R.drawable.party_logo_8

    // Labour Party
    15 -> R.drawable.party_logo_15

    // Liberal Democrats
    17 -> R.drawable.party_logo_17

    // Plaid Cymru
    22 -> R.drawable.party_logo_22

    // Scottish National Party (SNP)
    29 -> R.drawable.party_logo_29

    // Sinn Féin
    30 -> R.drawable.party_logo_30

    // Social Democratic & Labour Party (SDLP)
    31 -> R.drawable.party_logo_31

    // Ulster Unionist Party (UUP)
    38 -> R.drawable.party_logo_38

    // Green Party
    44 -> R.drawable.party_logo_44

    // Traditional Unionist Voice (TUV)
    158 -> R.drawable.party_logo_158

    // Reform UK
    1036 -> R.drawable.party_logo_1036

    // Restore Britain
    1117 -> R.drawable.party_logo_1117

    // No logo available — fall back to abbreviation text
    else -> null
}
