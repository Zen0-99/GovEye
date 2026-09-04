package com.goveye.app.domain.stats

/**
 * A single trait bar for the FotMob-style trait display.
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
 */
object TraitBarCalculator {
    /**
     * @param rebellionRate MP's rebellion rate (0-1)
     * @param participationRate MP's vote participation rate (0-1)
     * @param questionCount MP's question count
     * @param speechCount MP's speech count
     * @param committeeCount MP's committee count
     * @param peerRebellionRates All peers' rebellion rates
     * @param peerParticipationRates All peers' participation rates
     * @param peerQuestionCounts All peers' question counts
     * @param peerSpeechCounts All peers' speech counts
     * @param peerCommitteeCounts All peers' committee counts
     * @param peerAverages Peer averages for display
     */
    fun compute(
        rebellionRate: Float,
        participationRate: Float,
        questionCount: Int,
        speechCount: Int,
        committeeCount: Int,
        peerRebellionRates: List<Float>,
        peerParticipationRates: List<Float>,
        peerQuestionCounts: List<Int>,
        peerSpeechCounts: List<Int>,
        peerCommitteeCounts: List<Int>,
        peerAverages: PeerAverages
    ): List<TraitBar> = listOf(
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
            percentile = PercentileCalculator.computePercentile(questionCount, peerQuestionCounts),
            mpValue = questionCount.toFloat(),
            peerAverage = peerAverages.averageQuestions
        ),
        TraitBar(
            label = "Speeches",
            percentile = PercentileCalculator.computePercentile(speechCount, peerSpeechCounts),
            mpValue = speechCount.toFloat(),
            peerAverage = peerAverages.averageSpeeches
        ),
        TraitBar(
            label = "Committees",
            percentile = PercentileCalculator.computePercentile(committeeCount, peerCommitteeCounts),
            mpValue = committeeCount.toFloat(),
            peerAverage = peerAverages.averageCommittees
        )
    )

    private fun List<Float>.averageOrNull(): Float? = if (isEmpty()) null else average().toFloat()
}
