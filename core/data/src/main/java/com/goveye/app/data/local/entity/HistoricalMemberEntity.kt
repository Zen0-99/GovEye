package com.goveye.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "historical_members")
data class HistoricalMemberEntity(
    @PrimaryKey val twfyPersonId: Int,
    val parliamentMemberId: Int? = null,
    val displayName: String,
    val alternateNames: String? = null,
    val party: String? = null,
    val house: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val constituency: String? = null,
    val isCurrent: Int = 0,
    val lastUpdated: Long
)

@Fts4(contentEntity = HistoricalMemberEntity::class)
@Entity(tableName = "historical_members_fts4")
data class HistoricalMemberFts4Entity(
    val displayName: String,
    val alternateNames: String? = null,
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0
)
