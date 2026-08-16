package com.goveye.app.data.dto.bills

import kotlinx.serialization.Serializable

@Serializable
data class BillListResponse(
    val items: List<BillDto> = emptyList(),
)

@Serializable
data class BillDto(
    val billId: Int,
    val shortTitle: String,
    val longTitle: String? = null,
    val summary: String? = null,
    val currentHouse: String,
    val originatingHouse: String,
    val lastUpdate: String,
    val billWithdrawn: String? = null,
    val isDefeated: Boolean = false,
    val isAct: Boolean = false,
    val billTypeId: Int? = null,
    val introducedSessionId: Int? = null,
    val currentStage: BillStageDto? = null,
    val sponsors: List<BillSponsorDto> = emptyList(),
)

@Serializable
data class BillStageDto(
    val id: Int,
    val stageId: Int,
    val sessionId: Int? = null,
    val description: String,
    val abbreviation: String? = null,
    val house: String,
    val stageSittings: List<StageSittingDto> = emptyList(),
    val sortOrder: Int = 0,
)

@Serializable
data class StageSittingDto(
    val id: Int? = null,
    val stageId: Int? = null,
    val billStageId: Int? = null,
    val billId: Int? = null,
    val date: String? = null,
)

@Serializable
data class BillSponsorDto(
    val member: BillSponsorMemberDto? = null,
    val organisation: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class BillSponsorMemberDto(
    val memberId: Int,
    val name: String? = null,
    val party: String? = null,
    val partyColour: String? = null,
    val house: String? = null,
    val memberPhoto: String? = null,
    val memberFrom: String? = null,
)

@Serializable
data class BillStagesResponse(
    val items: List<BillStageDto> = emptyList(),
    val totalResults: Int = 0,
    val itemsPerPage: Int = 0,
)
