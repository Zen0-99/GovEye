package com.goveye.app.ui.screens.onboarding

/** A stream type for a department source (D-04: source = department × stream). */
enum class StreamType(val displayName: String) {
    PUBLICATIONS("Publications"),
    STATEMENTS("Statements"),
    LEGISLATION("Legislation");

    companion object {
        val ALL = listOf(PUBLICATIONS, STATEMENTS, LEGISLATION)
    }
}

/** A stream with its checked state — used in the Sources step. */
data class StreamState(val streamType: StreamType, val isChecked: Boolean)

/** A recommended department with all 3 streams pre-checked (D-05). */
data class RecommendedDepartment(
    val organisationSlug: String,
    val organisationName: String,
    val streams: List<StreamState>
)

/** A department group for the "All sources" section — all 3 streams. */
data class DepartmentGroup(val organisationName: String, val organisationSlug: String, val streams: List<StreamState>)

/** Party info for the Parties step — from MpDao.getActiveParties(). */
data class PartyInfo(
    val partyId: Int,
    val partyName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val seatCount: Int
)

/** A recommended MP ranked by recency-weighted tag hits (D-08). */
data class RecommendedMp(
    val memberId: Int,
    val matchedTags: List<String>,
    val totalScore: Int,
    val isPartyLeader: Boolean,
    val leaderTitle: String?
)

/** Party leader info for the MPs step "Party leaders" section (D-07). */
data class PartyLeaderInfo(
    val memberId: Int,
    val name: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val title: String
)
