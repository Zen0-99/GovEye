package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Party leader identified from Parliament Members API current posts +
 * MNIS bio_data (D-07). Produced by `build_party_leaders.py` at build time.
 *
 * Fields:
 * - partyId: Parliament party ID
 * - memberId: the MP who leads this party
 * - title: leader-type title (e.g. "Prime Minister", "Leader of the Opposition")
 */
@Serializable
@Entity(tableName = "party_leaders")
data class PartyLeaderEntity(@PrimaryKey val partyId: Int, val memberId: Int, val title: String)
