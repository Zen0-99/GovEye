package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Written ministerial statement from the Parliament Written Statements API.
 *
 * Produced by `build_written_statements.py` at build time from
 * `questions-statements-api.parliament.uk/api/writtenstatements/statements`.
 * Stored in the bundled DB (read-only, build-time data).
 *
 * Fields mirror the API response shape:
 * - id: Parliament statement ID
 * - memberId: the MP/peer who made the statement
 * - memberRole: e.g. "Secretary of State"
 * - uin: Unique Identification Number
 * - dateMade: ISO date string
 * - answeringBodyId / answeringBodyName: the government department
 * - title / text: statement content
 * - house: 1 (Commons) or 2 (Lords)
 */
@Serializable
@Entity(tableName = "written_statements")
data class WrittenStatementEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val memberRole: String,
    val uin: String,
    val dateMade: String,
    val answeringBodyId: Int,
    val answeringBodyName: String,
    val title: String,
    val text: String,
    val house: Int,
    val lastUpdated: Long
)
