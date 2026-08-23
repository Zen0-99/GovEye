package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Legislation item from the legislation.gov.uk API.
 *
 * Produced by `build_legislation.py` at build time from the
 * `/new/data.feed` Atom feed (genuinely new legislation only).
 *
 * Fields:
 * - id: sequential integer ID assigned by the build script
 * - title: legislation title
 * - type: e.g. "ukpga" (Act), "uksi" (Statutory Instrument)
 * - year / number: legislation year and number within that year
 * - date: enactment date (ISO string)
 * - url: legislation.gov.uk URL
 */
@Entity(tableName = "legislation")
data class LegislationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val type: String,
    val year: Int,
    val number: Int,
    val date: String,
    val url: String,
    val lastUpdated: Long
)
