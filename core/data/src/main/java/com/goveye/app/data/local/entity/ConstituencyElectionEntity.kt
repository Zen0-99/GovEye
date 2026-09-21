package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * An election result for a constituency (general elections and by-elections,
 * latest plus history) from the members-api
 * `Location/Constituency/{id}/ElectionResult` endpoints.
 *
 * One row per (constituencyId, electionId). `isNotional` marks results
 * recomputed for new boundaries (D-04). `turnout` is votes cast (int),
 * NOT a percentage. Party colours are hex strings without '#'.
 */
@Serializable
@Entity(
    tableName = "constituency_elections",
    primaryKeys = ["constituencyId", "electionId"],
    indices = [Index("constituencyId")]
)
data class ConstituencyElectionEntity(
    val constituencyId: Int,
    val electionId: Int,
    val result: String?, // e.g. "Lab Hold", "RUK Gain", "Con Gain from Lab"
    val isNotional: Boolean, // true = notional recomputed result after boundary change (D-04)
    val electorate: Int?,
    val turnout: Int?, // votes cast (int, verified — not a percentage)
    val majority: Int?,
    val winningPartyId: Int?,
    val winningPartyName: String?,
    val winningPartyColour: String?, // winningParty.backgroundColour hex, no '#'
    val electionTitle: String?, // "2024 General Election", "2025-05-01 Runcorn and Helsby By-election"
    val electionDate: String?, // ISO datetime
    val isGeneralElection: Boolean,
    val constituencyName: String?,
    val lastUpdated: Long
)
