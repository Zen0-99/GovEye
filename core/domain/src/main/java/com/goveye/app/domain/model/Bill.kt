package com.goveye.app.domain.model

data class Bill(
    val id: Int,
    val shortTitle: String,
    val longTitle: String?,
    val summary: String?,
    val currentHouse: String,
    val originatingHouse: String,
    val isAct: Boolean,
    val currentStage: BillStage?,
)

data class BillStage(
    val stageId: Int,
    val description: String,
    val abbreviation: String,
    val house: String,
    val sortOrder: Int,
    val sittingDates: List<String>,
)
