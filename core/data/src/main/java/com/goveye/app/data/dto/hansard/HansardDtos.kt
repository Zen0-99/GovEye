package com.goveye.app.data.dto.hansard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HansardSearchResponse(
    @SerialName("TotalMembers") val totalMembers: Int = 0,
    @SerialName("TotalContributions") val totalContributions: Int = 0,
    @SerialName("TotalWrittenStatements") val totalWrittenStatements: Int = 0,
    @SerialName("TotalWrittenAnswers") val totalWrittenAnswers: Int = 0,
    @SerialName("TotalDebates") val totalDebates: Int = 0,
    @SerialName("TotalDivisions") val totalDivisions: Int = 0,
    @SerialName("SearchTerms") val searchTerms: List<String> = emptyList(),
    @SerialName("Contributions") val contributions: List<HansardContributionDto> = emptyList(),
)

@Serializable
data class HansardContributionDto(
    @SerialName("MemberName") val memberName: String,
    @SerialName("MemberId") val memberId: Int,
    @SerialName("ContributionText") val contributionText: String,
    @SerialName("ContributionTextFull") val contributionTextFull: String? = null,
    @SerialName("SittingDate") val sittingDate: String,
    @SerialName("House") val house: String,
    @SerialName("DebateSection") val debateSection: String,
    @SerialName("ItemId") val itemId: Long = 0,
    @SerialName("DebateSectionId") val debateSectionId: Long = 0,
)
