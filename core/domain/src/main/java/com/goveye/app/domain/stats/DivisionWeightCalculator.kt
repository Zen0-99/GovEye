package com.goveye.app.domain.stats

import com.goveye.app.domain.model.VoteType

/**
 * Per-division significance weight for an MP's activity timeline.
 *
 * Unlike [ActivityScoreCalculator] which computes an aggregate 0-100 score
 * across all parliamentary activity, this calculator produces a per-division
 * significance score on a 0-10 scale — the FotMob "match rating" equivalent.
 *
 * Scoring formula:
 * - Base score for voting (not NO_VOTE_RECORDED): 3.0
 * - Rebellion bonus: +4.0 (rebellions are more significant)
 * - Closeness bonus: divisionCloseness * 3.0 (close votes are more significant)
 * - NO_VOTE_RECORDED: 0.0 (no participation = no weight)
 * - Final score clamped to 0.0..10.0
 *
 * `divisionCloseness` is computed by the caller as:
 * `1.0 - abs(ayeCount - noCount) / (ayeCount + noCount)` (when total > 0, else 0.0).
 */
data class DivisionWeight(
    val score: Double,
    val isRebellion: Boolean,
    val voteType: VoteType,
)

/**
 * Computes per-division significance on a 0-10 scale.
 * Pure function — no DI, no side effects, no Android dependencies.
 */
object DivisionWeightCalculator {

    private const val BASE_SCORE = 3.0
    private const val REBELLION_BONUS = 4.0
    private const val CLOSENESS_WEIGHT = 3.0

    fun compute(
        mpVote: VoteType,
        isRebellion: Boolean,
        divisionCloseness: Double,
    ): DivisionWeight {
        val score = if (mpVote == VoteType.NO_VOTE_RECORDED) {
            0.0
        } else {
            val closenessBonus = divisionCloseness * CLOSENESS_WEIGHT
            val rebellionBonus = if (isRebellion) REBELLION_BONUS else 0.0
            (BASE_SCORE + rebellionBonus + closenessBonus).coerceIn(0.0, 10.0)
        }

        return DivisionWeight(
            score = score,
            isRebellion = isRebellion,
            voteType = mpVote,
        )
    }
}
