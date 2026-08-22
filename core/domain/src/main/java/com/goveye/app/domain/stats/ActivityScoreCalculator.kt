package com.goveye.app.domain.stats

/**
 * Peer averages for a house (Commons or Lords).
 * Used for normalizing activity score components.
 */
data class PeerAverages(
    val averageQuestions: Float,
    val averageSpeeches: Float,
    val averageCommittees: Float,
    val averageParticipation: Float = 0f,
    val averageRebellion: Float = 0f,
    val mpCount: Int = 0
)

/**
 * Breakdown of the activity score by component.
 * Each component is 0.0–max (e.g. votes 0.0–4.0, questions 0.0–2.0).
 */
data class ScoreBreakdown(
    // 0.0-4.0
    val voteParticipationContribution: Float,
    // 0.0-2.0
    val questionsContribution: Float,
    // 0.0-2.0
    val speechesContribution: Float,
    // 0.0-2.0
    val committeesContribution: Float
)

/**
 * Mechanical parliamentary activity score (0.0-10.0).
 *
 * Weights:
 * - Vote participation: 4.0
 * - Questions: 2.0
 * - Speeches: 2.0
 * - Committees: 2.0
 *
 * Each component is normalized relative to the house average.
 * A count of 2× the average = full marks for that component.
 *
 * This is a transparency tool, not an editorial judgment.
 * A higher score means more recorded activity, not better performance.
 */
data class ActivityScore(val score: Float, val breakdown: ScoreBreakdown)

/**
 * Computes the parliamentary activity score.
 * Pure function — no DI, no side effects.
 */
object ActivityScoreCalculator {
    private const val VOTE_WEIGHT = 4.0f
    private const val QUESTIONS_WEIGHT = 2.0f
    private const val SPEECHES_WEIGHT = 2.0f
    private const val COMMITTEES_WEIGHT = 2.0f

    fun compute(
        voteParticipationRate: Float,
        questionCount: Int,
        speechCount: Int,
        committeeCount: Int,
        peerAverages: PeerAverages
    ): ActivityScore {
        val voteContribution = (voteParticipationRate * VOTE_WEIGHT).coerceIn(0f, VOTE_WEIGHT)
        val questionsContribution = normalize(questionCount, peerAverages.averageQuestions, QUESTIONS_WEIGHT)
        val speechesContribution = normalize(speechCount, peerAverages.averageSpeeches, SPEECHES_WEIGHT)
        val committeesContribution = normalize(committeeCount, peerAverages.averageCommittees, COMMITTEES_WEIGHT)

        val total = voteContribution + questionsContribution + speechesContribution + committeesContribution

        return ActivityScore(
            score = total.coerceIn(0f, 10f),
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
    private fun normalize(count: Int, average: Float, weight: Float): Float {
        if (average <= 0f) return if (count > 0) weight else 0f
        val ratio = count.toFloat() / (average * 2f)
        return (ratio * weight).coerceIn(0f, weight)
    }
}
