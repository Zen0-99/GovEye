package com.goveye.app.domain.model

/**
 * Party-level breakdown of votes in a division.
 * Used for the party breakdown chart on division detail.
 */
data class PartyBreakdown(
    val partyName: String,
    val partyColour: String,
    val ayeCount: Int,
    val noCount: Int,
    val totalMembers: Int
)

/**
 * Division with full voter lists (Ayes and Noes).
 * Used on the division detail screen.
 */
data class DivisionWithVotes(val division: Division, val ayes: List<DivisionVote>, val noes: List<DivisionVote>)

/**
 * A member's vote with the associated division context.
 * Used in the MP profile voting record list.
 */
data class MemberVoteWithDivision(
    val divisionId: Int,
    val divisionTitle: String,
    val divisionDate: String,
    val house: Int,
    val ayeCount: Int,
    val noCount: Int,
    val vote: VoteType,
    val isTeller: Boolean
)
