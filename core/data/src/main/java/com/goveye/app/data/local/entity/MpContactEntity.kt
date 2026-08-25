package com.goveye.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * Pre-fetched MP contact entry from the Members API.
 * One row per contact per MP (an MP can have multiple contacts:
 * constituency office, parliamentary office, web/social, etc.).
 *
 * Composite primary key (mpId, typeId) — an MP can have at most one
 * contact per typeId (e.g. one "Constituency Address", one "Website").
 */
@Serializable
@Entity(
    tableName = "mp_contacts",
    primaryKeys = ["mpId", "typeId"]
)
data class MpContactEntity(
    val mpId: Int,
    val typeId: Int,
    val type: String?,
    val isPreferred: Boolean?,
    val isWebAddress: Boolean?,
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val line4: String?,
    val line5: String?,
    val postcode: String?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val openingHours: String?,
    val lastUpdated: Long
)
