package com.goveye.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * Tag attached to a government publication, derived from pattern matching
 * on title + summary + body text. Produced by `build_tags.py` at build time.
 *
 * Composite key: (publicationId, tag).
 */
@Serializable
@Entity(
    tableName = "publication_tags",
    primaryKeys = ["publicationId", "tag"]
)
data class PublicationTagEntity(val publicationId: Int, val tag: String, val hitCount: Int)
