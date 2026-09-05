package com.goveye.app.domain.stats

/**
 * A single trait bar for the FotMob-style trait display.
 *
 * For Loyalty and Participation: `percentile` stores the percentile rank,
 * `mpValue` stores the actual rate (0-100). Display uses `mpValue`.
 *
 * For Questions, Speeches, Committees: `percentile` stores the normalized
 * rate-based score (0-100, clamped). `mpValue` stores the raw count.
 * Display uses `percentile` (the normalized score).
 */
data class TraitBar(
    val label: String,
    val percentile: Int, // 0-100
    val mpValue: Float,
    val peerAverage: Float
)

/**
 * Computes trait bars comparing an MP to their house peers.
 * Pure function.
 *
 * Questions/Speeches/Finance use rate-based scoring:
 *   score = (mpCount / mpYearsServed) / (avgCount / avgYearsServed) * 100
 *   An MP at the average rate scores 100%.
 *
 * Committees use count-based scoring:
 *   score = (mpCount / ceiling) * 100, ceiling = 10
 *   5 committees = 50%, 10+ = 100%.
 */
object TraitBarCalculator {
    /**
     * @param rebellionRate MP's rebellion rate (0-1)
     * @param participationRate MP's vote participation rate (0-1)
     * @param questionCount MP's question count
     * @param speechCount MP's speech count
     * @param committeeCount MP's committee count
     * @param mpYearsServed MP's years served in Parliament
     * @param avgYearsServed Average years served across peers
     * @param peerRebellionRates All peers' rebellion rates
     * @param peerParticipationRates All peers' participation rates
     * @param peerQuestionCounts All peers' question counts
     * @param peerSpeechCounts All peers' speech counts
     * @param peerCommitteeCounts All peers' committee counts
     * @param peerAverages Peer averages for display
     * @param financeCount MP's total financial declarations (interests + expenses)
     * @param avgFinanceCount Average total declarations across peers
     */
    fun compute(
        rebellionRate: Float,
        participationRate: Float,
        questionCount: Int,
        speechCount: Int,
        committeeCount: Int,
        mpYearsServed: Float,
        avgYearsServed: Float,
        peerRebellionRates: List<Float>,
        peerParticipationRates: List<Float>,
        peerQuestionCounts: List<Int>,
        peerSpeechCounts: List<Int>,
        peerCommitteeCounts: List<Int>,
        peerAverages: PeerAverages,
        financeCount: Int = 0,
        avgFinanceCount: Float = 0f
    ): List<TraitBar> {
        // Rate-based scores for Questions, Speeches, and Finance.
        // score = (mpRate / avgRate) * 100, where rate = count / yearsServed.
        val mpYears = mpYearsServed.coerceAtLeast(0.5f) // avoid div-by-zero
        val avgYears = avgYearsServed.coerceAtLeast(0.5f)
        val avgQRate = peerAverages.averageQuestions / avgYears
        val avgSRate = peerAverages.averageSpeeches / avgYears
        val avgFRate = avgFinanceCount / avgYears
        val questionsScore = if (avgQRate > 0f) {
            ((questionCount / mpYears) / avgQRate * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val speechesScore = if (avgSRate > 0f) {
            ((speechCount / mpYears) / avgSRate * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val financeScore = if (avgFRate > 0f) {
            ((financeCount / mpYears) / avgFRate * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Count-based score for Committees.
        // score = (count / ceiling) * 100, ceiling = 10
        // (5 committees = 50%, 10+ = 100%)
        val committeesScore = (committeeCount / 10f * 100f).toInt().coerceIn(0, 100)

        return listOf(
            // Loyalty = 100% - rebellionRate. Percentile inverted:
            // 0% rebellion = 100th percentile loyalty (most loyal).
            TraitBar(
                label = "Loyalty",
                percentile = 100 - PercentileCalculator.computePercentile(rebellionRate, peerRebellionRates),
                mpValue = (1f - rebellionRate) * 100,
                peerAverage = (1f - (peerRebellionRates.averageOrNull() ?: 0f)) * 100
            ),
            TraitBar(
                label = "Participation",
                percentile = PercentileCalculator.computePercentile(participationRate, peerParticipationRates),
                mpValue = participationRate * 100,
                peerAverage = (peerParticipationRates.averageOrNull() ?: 0f) * 100
            ),
            TraitBar(
                label = "Questions",
                percentile = questionsScore,
                mpValue = questionCount.toFloat(),
                peerAverage = peerAverages.averageQuestions
            ),
            TraitBar(
                label = "Speeches",
                percentile = speechesScore,
                mpValue = speechCount.toFloat(),
                peerAverage = peerAverages.averageSpeeches
            ),
            TraitBar(
                label = "Committees",
                percentile = committeesScore,
                mpValue = committeeCount.toFloat(),
                peerAverage = peerAverages.averageCommittees
            ),
            TraitBar(
                label = "Finance",
                percentile = financeScore,
                mpValue = financeCount.toFloat(),
                peerAverage = avgFinanceCount
            )
        )
    }

    private fun List<Float>.averageOrNull(): Float? = if (isEmpty()) null else average().toFloat()
}
