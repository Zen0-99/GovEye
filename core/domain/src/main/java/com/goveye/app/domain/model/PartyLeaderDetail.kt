package com.goveye.app.domain.model

/**
 * Rich party leader info — resolved by joining party_leaders → mps → bio_data.
 *
 * @param memberId The MP's ID (mps.id / party_leaders.memberId)
 * @param name Display name from the mps table
 * @param constituency Constituency name from the mps table
 * @param thumbnailUrl Photo URL from the mps table (may be null)
 * @param partyBackgroundColour Party colour hex (for the avatar border)
 * @param title Leader title from party_leaders (e.g. "Prime Minister", "Leader of the Opposition")
 * @param age Age computed from bio_data.dateOfBirth, or null if DOB is unknown
 * @param leaderSinceLabel Human-readable tenure label (e.g. "Leader for 2 years", "Leader for 3 months"), or null if unknown
 */
data class PartyLeaderDetail(
    val memberId: Int,
    val name: String,
    val constituency: String,
    val thumbnailUrl: String?,
    val partyBackgroundColour: String,
    val title: String,
    val age: Int? = null,
    val leaderSinceLabel: String? = null
)
