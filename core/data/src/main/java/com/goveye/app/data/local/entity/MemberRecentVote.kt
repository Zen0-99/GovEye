package com.goveye.app.data.local.entity

/**
 * Most recent vote by a member, joined with the division for context.
 *
 * Used by the Following screen roster to show each followed MP's latest vote.
 */
data class MemberRecentVote(
    val memberId: Int,
    val divisionId: Int,
    val house: Int,
    val title: String,
    val date: String,
    val vote: String
)
