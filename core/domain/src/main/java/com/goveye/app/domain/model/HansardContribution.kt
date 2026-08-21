package com.goveye.app.domain.model

data class HansardContribution(
    val itemId: Long,
    val memberId: Int,
    val memberName: String,
    val contributionText: String,
    val sittingDate: String,
    val house: String,
    val debateSection: String
)
