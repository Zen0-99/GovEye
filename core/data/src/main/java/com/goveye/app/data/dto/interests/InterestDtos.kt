package com.goveye.app.data.dto.interests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class InterestsResponse(
    val items: List<InterestDto> = emptyList(),
    val totalResults: Int = 0,
    val skip: Int = 0,
    val take: Int = 0,
    val links: List<LinkDto> = emptyList()
)

@Serializable
data class InterestDto(
    val id: Int,
    val summary: String,
    val parentInterestId: Int? = null,
    val registrationDate: String? = null,
    val publishedDate: String? = null,
    val updatedDates: List<String> = emptyList(),
    val category: InterestCategoryDto,
    val member: InterestMemberDto? = null,
    val fields: List<InterestFieldDto> = emptyList(),
    val rectified: Boolean = false,
    val rectifiedDetails: String? = null
)

@Serializable
data class InterestCategoryDto(
    val id: Int,
    val number: String,
    val name: String,
    val type: String,
    val parentCategoryIds: List<Int> = emptyList()
)

@Serializable
data class InterestMemberDto(
    val id: Int,
    val nameDisplayAs: String? = null,
    val nameListAs: String? = null,
    val house: String? = null,
    val memberFrom: String? = null,
    val party: String? = null
)

@Serializable
data class InterestFieldDto(
    val name: String,
    val type: String,
    val value: JsonElement? = null,
    val values: List<List<InterestFieldDto>>? = null,
    val typeInfo: InterestTypeInfoDto? = null,
    val description: String? = null
)

@Serializable
data class InterestTypeInfoDto(val currencyCode: String? = null)

@Serializable
data class LinkDto(val rel: String? = null, val href: String? = null, val method: String? = null)
