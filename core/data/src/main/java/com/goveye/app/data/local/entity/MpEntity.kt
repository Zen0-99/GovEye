package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mps")
data class MpEntity(
    @PrimaryKey val id: Int,
    val nameListAs: String,
    val nameDisplayAs: String,
    val nameFullTitle: String? = null,
    val nameAddressAs: String? = null,
    val gender: String? = null,
    val partyId: Int,
    val partyName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val partyForegroundColour: String,
    val constituencyId: Int,
    val constituencyName: String,
    val house: Int,
    val membershipStartDate: String? = null,
    val membershipEndDate: String? = null,
    val isActive: Boolean,
    val thumbnailUrl: String? = null,
    val lastUpdated: Long
)
