package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Pre-fetched MP career experience entry from the Members API.
 * One row per experience item per MP (employment history, political posts, etc.).
 */
@Serializable
@Entity(tableName = "mp_experience")
data class MpExperienceEntity(
    @PrimaryKey val id: Int, // Experience item ID from the API (unique globally)
    val mpId: Int,
    val type: String?,
    val typeId: Int?,
    val title: String?,
    val organisation: String?,
    val startMonth: Int?,
    val startYear: Int?,
    val endMonth: Int?,
    val endYear: Int?,
    val lastUpdated: Long
)
