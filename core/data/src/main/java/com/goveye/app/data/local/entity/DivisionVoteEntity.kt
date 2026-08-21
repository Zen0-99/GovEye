package com.goveye.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "division_votes", primaryKeys = ["divisionId", "memberId"])
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
