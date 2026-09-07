package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Unified career timeline event from the Parliament API Biography endpoint
 * and Wikipedia/Wikidata.
 *
 * One row per career event per MP. Categories include:
 * - government_post   — Secretary of State, Minister, Whip, etc.
 * - opposition_post   — Shadow Secretary, Shadow Minister, etc.
 * - other_post        — Party leader, NEC member, etc.
 * - committee         — Committee memberships
 * - representation    — Constituency election history ("Elected N times")
 * - party_affiliation — Party changes over time
 * - house_membership  — When they entered/left each house
 * - education         — From Wikipedia/Wikidata (P69)
 * - occupation        — From Wikipedia/Wikidata (P106)
 *
 * Source is 'parliament' for Biography API data, 'wikipedia' for Wikidata.
 */
@Serializable
@Entity(tableName = "mp_career_events", indices = [Index("mpId")])
data class MpCareerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mpId: Int,
    val category: String,
    val name: String?,
    val house: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val additionalInfo: String? = null,
    val additionalInfoLink: String? = null,
    val constituencyName: String? = null,
    val constituencyId: Int? = null,
    val source: String = "parliament",
    val lastUpdated: Long
)
