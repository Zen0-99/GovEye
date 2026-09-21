package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * A candidate row for one constituency election — one row per
 * (constituencyId, electionId, rankOrder); rankOrder 1 is the winner.
 *
 * `memberId` links to mps.id when the candidate is/was an MP (null
 * otherwise) — it backs the "elections this MP contested" join.
 * `resultChange` is a free-form string from the API ("RUK Gain", "0.1%",
 * ""), not a numeric. Party colour is a hex string without '#' (may be
 * null).
 */
@Serializable
@Entity(
    tableName = "constituency_election_candidates",
    primaryKeys = ["constituencyId", "electionId", "rankOrder"],
    indices = [Index("memberId")]
)
data class ConstituencyElectionCandidateEntity(
    val constituencyId: Int,
    val electionId: Int,
    val rankOrder: Int, // 1 = winner
    val memberId: Int?, // links to mps.id when the candidate is/was an MP — else null
    val name: String?,
    val partyId: Int?,
    val partyName: String?,
    val partyAbbreviation: String?,
    val partyColour: String?, // party.backgroundColour hex, no '#' (may be null)
    val resultChange: String?, // e.g. "RUK Gain", "0.1%", "" — string, not numeric
    val votes: Int?,
    val lastUpdated: Long
)
