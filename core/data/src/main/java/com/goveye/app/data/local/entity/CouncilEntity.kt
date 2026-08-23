package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UK local authority (council) entity.
 *
 * Data sourced from planning.data.gov.uk — 329 active local authorities
 * with name, website, region, and type. Contact email/phone are optional
 * (may be populated from council websites or APIs in future).
 *
 * Used by the Councils tab in the Directory and the CouncilScreen.
 */
@Entity(tableName = "councils")
data class CouncilEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reference: String,
    val name: String,
    val website: String?,
    val region: String?,
    val localAuthorityType: String?,
    val statisticalGeography: String?,
    val wikidata: String?,
    val twitter: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val lastUpdated: Long
)
