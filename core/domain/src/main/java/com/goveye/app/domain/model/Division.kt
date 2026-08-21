package com.goveye.app.domain.model

data class Division(
    val id: Int,
    val title: String,
    val date: String,
    val number: Int?,
    val ayeCount: Int,
    val noCount: Int,
    val isDeferred: Boolean,
    val house: Int,
    val twfyDebateUrl: String? = null
)

data class DivisionVote(
    val divisionId: Int,
    val memberId: Int,
    val vote: VoteType,
    val memberName: String,
    val partyName: String?,
    val partyColour: String?,
    val constituencyName: String?,
    val isTeller: Boolean
)

enum class VoteType {
    AYE,
    NO,
    NO_VOTE_RECORDED
}
