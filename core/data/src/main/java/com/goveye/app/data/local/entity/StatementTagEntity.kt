package com.goveye.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * Tag attached to a written statement, derived from pattern matching on
 * title + text. Produced by `build_tags.py` at build time.
 *
 * Composite key: (statementId, tag).
 */
@Serializable
@Entity(
    tableName = "statement_tags",
    primaryKeys = ["statementId", "tag"]
)
data class StatementTagEntity(val statementId: Int, val tag: String, val hitCount: Int)
