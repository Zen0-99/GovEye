package com.goveye.app.domain.stats

/**
 * Precomputed statistics for a single MP.
 *
 * Read from the `mp_stats` table (populated at build time by build_precompute.py).
 * When the table is empty (old DB), StatsRepository falls back to runtime computation.
 */
data class MpStats(
    val memberId: Int,
    val house: Int,
    val questionCount: Int,
    val speechCount: Int,
    val committeeCount: Int,
    val voteParticipationRate: Float,
    val rebellionRate: Float,
    val rebellionCount: Int,
    val totalDivisionsVoted: Int,
    val activityScore: Int,
    val rebellionPercentile: Int,
    val participationPercentile: Int,
    val questionsPercentile: Int,
    val speechesPercentile: Int,
    val committeesPercentile: Int
)
