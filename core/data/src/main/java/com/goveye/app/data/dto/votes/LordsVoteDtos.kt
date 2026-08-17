package com.goveye.app.data.dto.votes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lords division DTO — different field names from Commons.
 * Lords use Content / Not Content instead of Aye / No.
 * Field names are camelCase (Commons uses PascalCase).
 */
@Serializable
data class LordsDivisionDto(
    @SerialName("divisionId") val divisionId: Int,
    @SerialName("date") val date: String,
    @SerialName("number") val number: Int? = null,
    @SerialName("title") val title: String,
    @SerialName("isWhipped") val isWhipped: Boolean = false,
    @SerialName("isGovernmentContent") val isGovernmentContent: Boolean = false,
    @SerialName("isGovernmentWin") val isGovernmentWin: Boolean = false,
    @SerialName("isHouse") val isHouse: Boolean = true,
    @SerialName("isInquorate") val isInquorate: Boolean = false,
    @SerialName("amendmentMotionNotes") val amendmentMotionNotes: String? = null,
    @SerialName("authoritativeContentCount") val authoritativeContentCount: Int = 0,
    @SerialName("authoritativeNotContentCount") val authoritativeNotContentCount: Int = 0,
    @SerialName("memberContentCount") val memberContentCount: Int = 0,
    @SerialName("memberNotContentCount") val memberNotContentCount: Int = 0,
    @SerialName("tellerContentCount") val tellerContentCount: Int = 0,
    @SerialName("tellerNotContentCount") val tellerNotContentCount: Int = 0,
    @SerialName("divisionHadTellers") val divisionHadTellers: Boolean = false,
    @SerialName("sponsoringMemberId") val sponsoringMemberId: Int? = null,
    @SerialName("contentTellers") val contentTellers: List<LordsVoterDto>? = null,
    @SerialName("notContentTellers") val notContentTellers: List<LordsVoterDto>? = null,
    @SerialName("contents") val contents: List<LordsVoterDto> = emptyList(),
    @SerialName("notContents") val notContents: List<LordsVoterDto> = emptyList(),
)

@Serializable
data class LordsVoterDto(
    @SerialName("memberId") val memberId: Int,
    @SerialName("name") val name: String,
    @SerialName("listAs") val listAs: String? = null,
    @SerialName("memberFrom") val memberFrom: String? = null,
    @SerialName("party") val party: String? = null,
    @SerialName("partyColour") val partyColour: String? = null,
    @SerialName("partyAbbreviation") val partyAbbreviation: String? = null,
    @SerialName("partyIsMainParty") val partyIsMainParty: Boolean = true,
)

@Serializable
data class LordsMemberVoteDto(
    @SerialName("memberId") val memberId: Int,
    @SerialName("memberVotedContent") val memberVotedContent: Boolean = false,
    @SerialName("memberVotedNotContent") val memberVotedNotContent: Boolean = false,
    @SerialName("memberWasTeller") val memberWasTeller: Boolean = false,
    @SerialName("publishedDivision") val publishedDivision: LordsPublishedDivisionDto? = null,
)

@Serializable
data class LordsPublishedDivisionDto(
    @SerialName("divisionId") val divisionId: Int,
    @SerialName("date") val date: String,
    @SerialName("title") val title: String,
    @SerialName("memberContentCount") val memberContentCount: Int = 0,
    @SerialName("memberNotContentCount") val memberNotContentCount: Int = 0,
)
