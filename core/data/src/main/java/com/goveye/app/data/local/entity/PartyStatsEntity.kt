package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "party_stats")
data class PartyStatsEntity(
    @PrimaryKey val partyId: Int,
    val description: String? = null,
    val foundedYear: String? = null,
    val leaderName: String? = null,
    val lastElectionVoteShare: Float? = null,
    val lastElectionSeats: Int? = null,
    val lastElectionYear: Int? = null,
    val lastUpdated: Long
)
