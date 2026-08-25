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
 * Unlike `HansardContributionEntity` which only stores per-MP question
 * *counts*, this entity stores the actual question text, answering body,
 * date tabled, and UIN for each individual question — enabling the MP
 * activity feed (Phase 15) to display question content.
 *
 * Fields mirror the API response shape:
 * - id: Parliament question ID
 * - memberId: the MP who asked the question (askingMemberId in API)
 * - uin: Unique Identification Number
 * - dateTabled: ISO date string (when the question was tabled)
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
