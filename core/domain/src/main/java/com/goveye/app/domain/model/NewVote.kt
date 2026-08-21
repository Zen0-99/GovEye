package com.goveye.app.domain.model

/**
 * A newly detected vote by a followed MP — used by the VotePollingWorker
 * to dispatch notifications (D-02).
 */
data class NewVote(
    val memberId: Int,
    val memberName: String,
    val thumbnailUrl: String?,
    val partyName: String,
    val divisionId: Int,
    val house: Int,
    val divisionTitle: String,
    val voteType: VoteType,
    val isRebel: Boolean
)
