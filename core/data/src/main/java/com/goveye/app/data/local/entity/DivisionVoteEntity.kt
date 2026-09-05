package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "division_votes",
    primaryKeys = ["divisionId", "memberId"],
    indices = [Index("memberId")]
)
data class DivisionVoteEntity(
    val divisionId: Int,
    val memberId: Int,
    val vote: String,
    val memberName: String,
    val partyName: String,
    val partyColour: String,
    val constituencyName: String,
    val isTeller: Boolean = false,
    val proxyName: String? = null
)
