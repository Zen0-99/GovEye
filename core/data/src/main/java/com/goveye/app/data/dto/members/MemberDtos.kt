package com.goveye.app.data.dto.members

import kotlinx.serialization.Serializable

@Serializable
data class MemberSearchResponse(val items: List<MemberItem> = emptyList())

@Serializable
data class MemberItem(val value: MemberDto, val links: List<LinkDto> = emptyList())

@Serializable
data class MemberDto(
    val id: Int,
    val nameListAs: String,
    val nameDisplayAs: String,
    val nameFullTitle: String? = null,
    val nameAddressAs: String? = null,
    val latestParty: PartyDto? = null,
    val gender: String? = null,
    val latestHouseMembership: HouseMembershipDto? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class PartyDto(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val backgroundColour: String,
    val foregroundColour: String
)

@Serializable
data class HouseMembershipDto(
    val membershipFrom: String,
    val membershipFromId: Int,
    val house: Int,
    val membershipStartDate: String? = null,
    val membershipEndDate: String? = null,
    val membershipStatus: MembershipStatusDto? = null
)

@Serializable
data class MembershipStatusDto(
    val statusIsActive: Boolean,
    val statusDescription: String? = null,
    val startDate: String? = null
)

@Serializable
data class LinkDto(val rel: String? = null, val href: String? = null, val method: String? = null)

@Serializable
data class MemberResponse(val value: MemberDto, val links: List<LinkDto> = emptyList())

@Serializable
data class SynopsisResponse(val value: String? = null, val links: List<LinkDto> = emptyList())

@Serializable
data class ContactResponse(val value: List<ContactDto> = emptyList(), val links: List<LinkDto> = emptyList())

@Serializable
data class ContactDto(
    val type: String? = null,
    val typeId: Int? = null,
    val isPreferred: Boolean? = null,
    val isWebAddress: Boolean? = null,
    val line1: String? = null,
    val line2: String? = null,
    val line3: String? = null,
    val line4: String? = null,
    val line5: String? = null,
    val postcode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val openingHours: String? = null
)

@Serializable
data class ExperienceResponse(
    val value: List<BiographyExperienceDto> = emptyList(),
    val links: List<LinkDto> = emptyList()
)

@Serializable
data class BiographyExperienceDto(
    val id: Int,
    val type: String? = null,
    val typeId: Int? = null,
    val title: String? = null,
    val organisation: String? = null,
    val startMonth: Int? = null,
    val startYear: Int? = null,
    val endMonth: Int? = null,
    val endYear: Int? = null
)

@Serializable
data class BiographyResponse(val value: List<BiographyItemDto> = emptyList(), val links: List<LinkDto> = emptyList())

@Serializable
data class BiographyItemDto(
    val house: HouseDto? = null,
    val name: String? = null,
    val id: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val additionalInfo: String? = null,
    val additionalInfoLink: String? = null
)

@Serializable
data class HouseDto(val id: Int, val name: String? = null)
