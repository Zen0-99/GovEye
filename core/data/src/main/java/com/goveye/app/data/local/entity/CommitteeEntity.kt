package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "committees")
data class CommitteeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val house: String? = null,
    val categoryName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isActive: Boolean,
    val lastUpdated: Long,
)

@Entity(tableName = "mp_committee_cross_ref", primaryKeys = ["memberId", "committeeId"])
data class MpCommitteeCrossRef(
    val memberId: Int,
    val committeeId: Int,
    val lastUpdated: Long,
)
