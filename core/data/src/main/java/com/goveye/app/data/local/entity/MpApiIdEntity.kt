package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Cross-API ID mapping table.
 *
 * The Members API `id` is the canonical key (matches `mps.id`).
 * Other APIs use different ID fields — this table maps them all
 * to the Members API ID so the app can query with a single ID
 * regardless of which API sourced the data.
 *
 * Build scripts resolve IDs at build time and populate this table.
 * The app never needs to know which API a given ID came from.
 */
@Serializable
@Entity(tableName = "mp_api_ids")
data class MpApiIdEntity(
    /** Members API ID — canonical key, matches mps.id */
    @PrimaryKey val memberId: Int,
    /** Committees API mnisId (same as Members API id for Commons MPs) */
    val mnisId: Int? = null,
    /** TheyWorkForYou person ID (used by Hansard API) */
    val twfyPersonId: Int? = null,
    /** IPSA member ID (expenses) */
    val ipsaMemberId: String? = null,
    /** PublicWhip member ID */
    val publicWhipId: String? = null,
    val lastUpdated: Long
)
