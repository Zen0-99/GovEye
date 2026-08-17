package com.goveye.app.data.dto.ayesnoes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AyesMpResponse(
    val mp: AyesMpDetail? = null,
)

@Serializable
data class AyesMpDetail(
    @SerialName("parliament_member_id") val parliamentMemberId: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("votes_recorded") val votesRecorded: Int? = null,
    val ayes: Int? = null,
    val noes: Int? = null,
    val rebellions: Int? = null,
)
