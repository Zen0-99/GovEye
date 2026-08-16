package com.goveye.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = MpEntity::class)
@Entity(tableName = "mps_fts")
data class MpFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    @ColumnInfo(name = "nameListAs")
    val nameListAs: String,
    @ColumnInfo(name = "nameDisplayAs")
    val nameDisplayAs: String,
    @ColumnInfo(name = "constituencyName")
    val constituencyName: String,
    @ColumnInfo(name = "partyName")
    val partyName: String,
)
