package com.goveye.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = PartyManifestoEntity::class)
@Entity(tableName = "party_manifestos_fts4")
data class PartyManifestoFts4Entity(
    val manifestoText: String,
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0
)
