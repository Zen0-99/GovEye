package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Tag attached to a division, derived from pattern matching on debate
 * speeches + division title. Produced by build_tags.py at build time.
 *
 * Tags allow users to filter/follow topics (e.g. "Universal Credit",
 * "Immigration", "Climate") across all divisions.
 */
@Entity(
    tableName = "division_tags",
    primaryKeys = ["divisionId", "tag"]
)
data class DivisionTagEntity(val divisionId: Int, val tag: String, val hitCount: Int)
