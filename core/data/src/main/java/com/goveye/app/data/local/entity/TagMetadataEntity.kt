package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Precomputed metadata for each tag: description + count of divisions
 * and bills that carry this tag. Built by build_tags.py at seed build time
 * so the app doesn't need to run COUNT queries at runtime.
 */
@Entity(
    tableName = "tag_metadata",
    primaryKeys = ["tag"]
)
data class TagMetadataEntity(val tag: String, val description: String, val divisionCount: Int, val billCount: Int)
