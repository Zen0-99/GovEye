package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stub MP entity — Phase 2 will expand the schema with more fields
 * (party affiliation, contact details, etc.).
 *
 * Used as the [contentEntity] for [MpFtsEntity] so the FTS shadow table
 * can reference real columns (D-28).
 */
@Entity(tableName = "mps")
data class MpEntity(@PrimaryKey val id: Int, val name: String, val constituency: String, val party: String)
