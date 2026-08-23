package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Tag attached to a legislation item, derived from pattern matching on
 * title. Produced by `build_tags.py` at build time.
 *
 * Composite key: (legislationId, tag).
 */
@Entity(
    tableName = "legislation_tags",
    primaryKeys = ["legislationId", "tag"]
)
data class LegislationTagEntity(val legislationId: Int, val tag: String, val hitCount: Int)
