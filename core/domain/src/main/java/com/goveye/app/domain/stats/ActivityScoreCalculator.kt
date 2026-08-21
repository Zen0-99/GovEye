package com.goveye.app.domain.stats

/**
 * Peer averages for a house (Commons or Lords).
 * Used for normalizing activity score components.
 */
data class PeerAverages(val averageQuestions: Float, val averageSpeeches: Float, val averageCommittees: Float)

/**
 * Breakdown of the activity score by component.
 */
data class ScoreBreakdown(
    // 0-40
    val voteParticipationContribution: Int,
    // 0-20
    val questionsContribution: Int,
    // 0-20
    val speechesContribution: Int,
    // 0-20
    val committeesContribution: Int
)

/**
 * Mechanical parliamentary activity score (0-100).
 *
 * Weights:
 * - Vote participation: 40%
 * - Questions: 20%
 * - Speeches: 20%
 * - Committees: 20%
 *
 * Each component is normalized relative to the house average.
 * A count of 2× the average = full marks for that component.
 *
 * This is a transparency tool, not an editorial judgment.
 * A higher score means more recorded activity, not better performance.
 */
data class ActivityScore(val score: Int, val breakdown: ScoreBreakdown)

/**
 * Computes the parliamentary activity score.
 * Pure function — no DI, no side effects.
 */
object ActivityScoreCalculator {
    private const val VOTE_WEIGHT = 40
    private const val QUESTIONS_WEIGHT = 20
    private const val SPEECHES_WEIGHT = 20
    private const val COMMITTEES_WEIGHT = 20

    fun compute(
        voteParticipationRate: Float,
        questionCount: Int,
        speechCount: Int,
        committeeCount: Int,
        peerAverages: PeerAverages
    ): ActivityScore {
        val voteContribution = (voteParticipationRate * VOTE_WEIGHT).toInt().coerceIn(0, VOTE_WEIGHT)
        val questionsContribution = normalize(questionCount, peerAverages.averageQuestions, QUESTIONS_WEIGHT)
        val speechesContribution = normalize(speechCount, peerAverages.averageSpeeches, SPEECHES_WEIGHT)
        val committeesContribution = normalize(committeeCount, peerAverages.averageCommittees, COMMITTEES_WEIGHT)

        val total = voteContribution + questionsContribution + speechesContribution + committeesContribution

        return ActivityScore(
            score = total.coerceIn(0, 100),
            breakdown = ScoreBreakdown(
                voteParticipationContribution = voteContribution,
                questionsContribution = questionsContribution,
                speechesContribution = speechesContribution,
                committeesContribution = committeesContribution
            )
        )
    }

    /**
     * Normalize a count relative to peer average.
     * count = 0 → 0% of weight
     * count = average → 50% of weight
     * count = 2× average → 100% of weight
     * Linear interpolation between these points, capped at 100%.
     */
    private fun normalize(count: Int, average: Float, weight: Int): Int {
        if (average <= 0f) return if (count > 0) weight else 0
        val ratio = count.toFloat() / (average * 2f)
        return (ratio * weight).toInt().coerceIn(0, weight)
    }
}
