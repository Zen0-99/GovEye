package com.goveye.app.data.local.entity

/**
 * Result of joining [FollowEntity] with [MpEntity].
 *
 * Used by the Following screen roster and the vote polling worker to get
 * followed MP display info (name, thumbnail, party) in a single query.
 */
data class FollowedMpWithDetail(
    val memberId: Int,
    val followedAt: Long,
    val isMuted: Boolean,
    val nameDisplayAs: String,
    val nameListAs: String,
    val thumbnailUrl: String?,
    val partyName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val partyForegroundColour: String,
    val constituencyName: String,
    val house: Int
)
