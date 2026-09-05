package com.goveye.app.domain.stats

/**
 * Peer averages for a house (Commons or Lords).
 * Used for trait bar peer comparison (not for activity score computation).
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
 * Each component is 0.0–max.
 */
data class ScoreBreakdown(
    // 0.0-2.5 — participation rate * 2.5
    val voteParticipationContribution: Float,
    // 0.0-2.0 — questions per month, scaled
    val questionsContribution: Float,
    // 0.0-2.0 — speeches per month, scaled
    val speechesContribution: Float,
    // 0.0-1.5 — committee tenure days, scaled
    val committeesContribution: Float,
    // 0.0-2.0 — finance declarations per month, scaled
    val financeContribution: Float = 0f
)

/**
 * Mechanical parliamentary activity score (0.0-10.0).
 *
 * Self-performance scoring — no peer normalization. The score reflects
 * the MP's own activity. Peer comparison happens implicitly: MP A with
 * 7.0 vs MP B with 8.0 IS the "vs peers" comparison.
 *
 * Weights (total 10.0):
 * - Vote participation: 2.5 (25%) — participation rate, tenure-aware
 * - Questions: 2.0 (20%) — per-month rate (count / months since tenure)
 * - Speeches: 2.0 (20%) — per-month rate (count / months since tenure)
 * - Committees: 1.5 (15%) — tenure-weighted (total committee days)
 * - Finance: 2.0 (20%) — declarations per month (interests + expenses)
 *
 * Scaling for per-month rates:
 * - Questions/speeches: 2 per month = full marks. Linear up to that.
 * - Committees: 1000 total committee days = full marks. Linear up to that.
 * - Finance: 15 declarations per month = full marks. Linear up to that.
 */
data class ActivityScore(val score: Float, val breakdown: ScoreBreakdown)

/**
 * Computes the parliamentary activity score.
 * Pure function — no DI, no side effects.
 */
object ActivityScoreCalculator {
    private const val VOTE_WEIGHT = 2.5f
    private const val QUESTIONS_WEIGHT = 2.0f
    private const val SPEECHES_WEIGHT = 2.0f
    private const val COMMITTEES_WEIGHT = 1.5f
    private const val FINANCE_WEIGHT = 2.0f

    // Per-month rate that earns full marks for questions/speeches.
    // 2 questions per month over a tenure = very active.
    private const val FULL_MARKS_QUESTIONS_PER_MONTH = 2.0f
    private const val FULL_MARKS_SPEECHES_PER_MONTH = 2.0f

    // Total committee days that earn full marks.
    // 1000 days ≈ ~3 years on committees = solid engagement.
    private const val FULL_MARKS_COMMITTEE_DAYS = 1000.0f

    // Finance uses rate-vs-average scoring (same as the trait radar).
    // The financeTraitScore (0-100) is passed in — 100 = at peer average.
    // No absolute threshold; the activity score contribution is directly
    // proportional to the trait score, keeping the two displays consistent.

    /**
     * Compute the activity score from self-performance metrics.
     *
     * @param voteParticipationRate 0.0-1.0, tenure-aware
     * @param questionCount Total questions tabled since tenure
     * @param speechCount Total speeches made since tenure
     * @param monthsSinceTenureStart Number of months from tenure start to now
     * @param committeeTenureDays Total days across all committee memberships
     * @param financeTraitScore Finance trait score (0-100, rate-vs-average). 100 = at peer average.
     */
    fun compute(
        voteParticipationRate: Float,
        questionCount: Int,
        speechCount: Int,
        monthsSinceTenureStart: Int,
        committeeTenureDays: Int,
        financeTraitScore: Int = 0
    ): ActivityScore {
        val voteContribution = (voteParticipationRate * VOTE_WEIGHT).coerceIn(0f, VOTE_WEIGHT)

        val questionsPerMonth = if (monthsSinceTenureStart > 0) {
            questionCount.toFloat() / monthsSinceTenureStart
        } else {
            questionCount.toFloat()
        }
        val questionsContribution = scaleRate(questionsPerMonth, FULL_MARKS_QUESTIONS_PER_MONTH, QUESTIONS_WEIGHT)

        val speechesPerMonth = if (monthsSinceTenureStart > 0) {
            speechCount.toFloat() / monthsSinceTenureStart
        } else {
            speechCount.toFloat()
        }
        val speechesContribution = scaleRate(speechesPerMonth, FULL_MARKS_SPEECHES_PER_MONTH, SPEECHES_WEIGHT)

        val committeesContribution = scaleRate(
            committeeTenureDays.toFloat(),
            FULL_MARKS_COMMITTEE_DAYS,
            COMMITTEES_WEIGHT
        )

        // Finance: directly proportional to the trait score (0-100).
        // 100% trait score = full weight. 50% = half weight.
        val financeContribution = (financeTraitScore.toFloat() / 100f * FINANCE_WEIGHT)
            .coerceIn(0f, FINANCE_WEIGHT)

        val total = voteContribution + questionsContribution + speechesContribution +
            committeesContribution + financeContribution

        return ActivityScore(
            score = total.coerceIn(0f, 10f),
            breakdown = ScoreBreakdown(
                voteParticipationContribution = voteContribution,
                questionsContribution = questionsContribution,
                speechesContribution = speechesContribution,
                committeesContribution = committeesContribution,
                financeContribution = financeContribution
            )
        )
    }

    /**
     * Scale a rate linearly: 0 → 0% of weight, fullMarks → 100% of weight.
     * Capped at 100%.
     */
    private fun scaleRate(rate: Float, fullMarks: Float, weight: Float): Float {
        if (fullMarks <= 0f) return 0f
        val ratio = rate / fullMarks
        return (ratio * weight).coerceIn(0f, weight)
    }
}
