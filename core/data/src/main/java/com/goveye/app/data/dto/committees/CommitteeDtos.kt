package com.goveye.app.data.dto.committees

import kotlinx.serialization.Serializable

@Serializable
data class CommitteeSearchResponse(
    val items: List<CommitteeItem> = emptyList(),
)

@Serializable
data class CommitteeItem(
    val id: Int,
    val name: String,
    val house: String? = null,
    val category: CommitteeCategory? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)

@Serializable
data class CommitteeCategory(
    val id: Int,
    val name: String? = null,
)
