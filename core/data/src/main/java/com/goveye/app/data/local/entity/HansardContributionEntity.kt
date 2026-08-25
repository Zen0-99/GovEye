package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "hansard_contributions")
data class HansardContributionEntity(
    @PrimaryKey val itemId: Long,
    val memberId: Int,
    val memberName: String,
    val contributionText: String,
    val sittingDate: String,
    val house: String,
    val debateSection: String,
    val debateSectionId: Long,
    val lastUpdated: Long
)
