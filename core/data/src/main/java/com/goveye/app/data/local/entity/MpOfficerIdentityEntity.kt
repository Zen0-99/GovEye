package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Companies House officer identity for an MP, matched via officer search +
 * appointment cross-matching (optionally adjudicated by Jev).
 * One row per MP — an MP can have several CH officer records; this holds the
 * best-matched identity. dobMonth/dobYear are month/year only (CH redacts day).
 */
@Serializable
@Entity(tableName = "mp_officer_identity")
data class MpOfficerIdentityEntity(
    @PrimaryKey val mpId: Int,
    val officerId: String,
    val officerName: String,
    val dobMonth: Int? = null,
    val dobYear: Int? = null,
    val nationality: String? = null,
    val countryOfResidence: String? = null,
    // 'interest-match' | 'heuristic' | 'jev'
    val matchMethod: String? = null,
    // Jev choice probability (0-1) or heuristic score share; null if unmatched
    val matchConfidence: Float? = null,
    val isDisqualified: Boolean = false,
    // JSON array of disqualification detail objects when isDisqualified
    val disqualificationJson: String? = null,
    val lastUpdated: Long
)
