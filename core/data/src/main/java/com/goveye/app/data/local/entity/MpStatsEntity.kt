package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Precomputed per-MP statistics — one row per MP.
 *
 * Produced by `build_precompute.py` at build time via SQL aggregation.
 * Eliminates 5,500+ runtime DAO calls per profile open (Phase 12).
 *
 * All metrics are computed at build time from the bundled DB:
 * - questionCount, speechCount, committeeCount: COUNT queries
 * - voteParticipationRate: voted divisions / total divisions in house
 * - rebellionRate, rebellionCount, totalDivisionsVoted: party-majority method
 * - activityScore: weighted score 0.0-10.0 (votes 4.0, questions 2.0, speeches 2.0, committees 2.0)
 * - percentiles: rank-based percentile across same-house peers
 */
@Serializable
@Entity(tableName = "mp_stats")
data class MpStatsEntity(
    @PrimaryKey val memberId: Int,
    val house: Int,
    val questionCount: Int,
    val speechCount: Int,
    val committeeCount: Int,
    val voteParticipationRate: Float,
    val rebellionRate: Float,
    val rebellionCount: Int,
    val totalDivisionsVoted: Int,
    val activityScore: Float,
    val rebellionPercentile: Int,
    val participationPercentile: Int,
    val questionsPercentile: Int,
    val speechesPercentile: Int,
    val committeesPercentile: Int
)
