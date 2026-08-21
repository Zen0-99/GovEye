package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: Int,
    val shortTitle: String,
    val longTitle: String? = null,
    val summary: String? = null,
    val currentHouse: String,
    val originatingHouse: String,
    val lastUpdate: String,
    val billWithdrawn: String? = null,
    val isDefeated: Boolean,
    val isAct: Boolean,
    val billTypeId: Int? = null,
    val currentStageDescription: String? = null,
    val currentStageAbbreviation: String? = null,
    val lastUpdated: Long
)
