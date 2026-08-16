package com.goveye.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table that shadows [MpEntity] for full-text search (D-28).
 *
 * The [contentEntity] link lets Room manage the FTS index automatically —
 * inserts/updates/deletes on [MpEntity] propagate to this table.
 */
@Fts4(contentEntity = MpEntity::class)
@Entity(tableName = "mps_fts")
data class MpFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "constituency")
    val constituency: String,
    @ColumnInfo(name = "party")
    val party: String
)
