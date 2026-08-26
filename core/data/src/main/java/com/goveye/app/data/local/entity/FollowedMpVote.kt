package com.goveye.app.data.local.entity

/**
 * A vote by a followed MP on a specific division.
 *
 * Used by the feed to show how the user's followed MPs voted (Aye/No)
 * on division cards.
 */
data class FollowedMpVote(
    val divisionId: Int,
    val memberId: Int,
    val vote: String,
    val memberName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val divisionTitle: String,
    val divisionDate: String,
    val divisionHouse: Int,
    val ayeCount: Int,
    val noCount: Int
)
