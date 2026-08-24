package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Written question from the Parliament Written Questions API.
 *
 * Produced by `build_written_questions.py` at build time from
 * `questions-statements-api.parliament.uk/api/writtenquestions/questions`.
 * Stored in the bundled DB (read-only, build-time data).
 *
 * Fields mirror the API response shape:
 * - id: Parliament question ID
 * - memberId: the MP/peer who asked the question (askingMemberId)
 * - uin: Unique Identification Number
 * - dateTabled: ISO date string
 * - answeringBodyId / answeringBodyName: the government department
 * - questionText: the full question text (255-char truncation fallback applied)
 * - house: 1 (Commons) or 2 (Lords)
 */
@Entity(tableName = "written_questions")
data class WrittenQuestionEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val uin: String,
    val dateTabled: String,
    val answeringBodyId: Int,
    val answeringBodyName: String,
    val questionText: String,
    val house: Int,
    val lastUpdated: Long
)
