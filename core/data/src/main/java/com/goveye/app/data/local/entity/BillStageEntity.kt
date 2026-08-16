package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_stages", primaryKeys = ["billId", "stageId"])
data class BillStageEntity(
    val billId: Int,
    val stageId: Int,
    val description: String,
    val abbreviation: String,
    val house: String,
    val sortOrder: Int,
    val sessionId: Int? = null,
    val sittingDates: List<String>,
    val lastUpdated: Long,
)
