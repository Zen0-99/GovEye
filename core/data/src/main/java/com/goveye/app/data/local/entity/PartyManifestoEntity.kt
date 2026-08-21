package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "party_manifestos")
data class PartyManifestoEntity(
    @PrimaryKey val partyId: Int,
    val manifestoText: String,
    val manifestoYear: Int,
    val wordCount: Int,
    val source: String? = null
)
