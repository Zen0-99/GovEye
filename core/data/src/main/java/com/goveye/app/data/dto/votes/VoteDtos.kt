package com.goveye.app.data.dto.votes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivisionDto(
    @SerialName("DivisionId") val divisionId: Int,
    @SerialName("Date") val date: String,
    @SerialName("PublicationUpdated") val publicationUpdated: String? = null,
    @SerialName("Number") val number: Int? = null,
    @SerialName("IsDeferred") val isDeferred: Boolean = false,
    @SerialName("Title") val title: String,
    @SerialName("AyeCount") val ayeCount: Int = 0,
    @SerialName("NoCount") val noCount: Int = 0,
    @SerialName("AyeTellers") val ayeTellers: List<TellerDto>? = null,
    @SerialName("NoTellers") val noTellers: List<TellerDto>? = null,
    @SerialName("Ayes") val ayes: List<VoterDto> = emptyList(),
    @SerialName("Noes") val noes: List<VoterDto> = emptyList(),
    @SerialName("NoVoteRecorded") val noVoteRecorded: List<VoterDto> = emptyList(),
)

@Serializable
data class VoterDto(
    @SerialName("MemberId") val memberId: Int,
    @SerialName("Name") val name: String,
    @SerialName("Party") val party: String? = null,
    @SerialName("SubParty") val subParty: String? = null,
    @SerialName("PartyColour") val partyColour: String? = null,
    @SerialName("PartyAbbreviation") val partyAbbreviation: String? = null,
    @SerialName("MemberFrom") val memberFrom: String? = null,
    @SerialName("ListAs") val listAs: String? = null,
    @SerialName("ProxyName") val proxyName: String? = null,
)

@Serializable
data class TellerDto(
    @SerialName("MemberId") val memberId: Int,
    @SerialName("Name") val name: String,
    @SerialName("Party") val party: String? = null,
    @SerialName("SubParty") val subParty: String? = null,
    @SerialName("PartyColour") val partyColour: String? = null,
    @SerialName("PartyAbbreviation") val partyAbbreviation: String? = null,
    @SerialName("MemberFrom") val memberFrom: String? = null,
    @SerialName("ListAs") val listAs: String? = null,
    @SerialName("ProxyName") val proxyName: String? = null,
)

@Serializable
data class MemberVoteDto(
    @SerialName("MemberId") val memberId: Int,
    @SerialName("MemberVotedAye") val memberVotedAye: Boolean = false,
    @SerialName("MemberVotedNo") val memberVotedNo: Boolean = false,
    @SerialName("MemberWasTeller") val memberWasTeller: Boolean = false,
    @SerialName("PublishedDivision") val publishedDivision: PublishedDivisionDto? = null,
)

@Serializable
data class PublishedDivisionDto(
    @SerialName("DivisionId") val divisionId: Int,
    @SerialName("Date") val date: String,
    @SerialName("Title") val title: String,
    @SerialName("AyeCount") val ayeCount: Int = 0,
    @SerialName("NoCount") val noCount: Int = 0,
)
