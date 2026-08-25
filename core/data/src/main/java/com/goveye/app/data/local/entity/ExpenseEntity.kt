package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mpId: Int,
    val category: String,
    val bucket: String,
    val amountPence: Long,
    val claimDate: String? = null,
    val status: String? = null,
    val lastUpdated: Long,
    // Phase 13: IPSA descriptive fields (nullable — additive migration v18→v19)
    val shortDescription: String? = null,
    val details: String? = null,
    val claimNumber: String? = null,
    val journeyType: String? = null,
    val journeyFrom: String? = null,
    val journeyTo: String? = null,
    val travel: String? = null,
    val nights: String? = null,
    val mileage: String? = null,
    val amountPaidPence: Long? = null,
    val amountNotPaidPence: Long? = null,
    val amountRepaidPence: Long? = null,
    val reasonIfNotPaid: String? = null,
    val supplyMonth: String? = null,
    val supplyPeriod: String? = null
)
