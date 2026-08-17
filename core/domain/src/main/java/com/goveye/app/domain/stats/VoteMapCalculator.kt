package com.goveye.app.domain.stats

import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType

/**
 * Result of a single vote in the vote map.
 */
enum class VoteMapResult {
    WITH_PARTY,
    REBEL,
    NO_VOTE,
}

/**
 * A single tile in the vote map grid.
 */
data class VoteMapTile(
    val divisionId: Int,
    val divisionTitle: String,
    val divisionDate: String,
    val mpVote: VoteType,
    val partyLine: VoteType,
    val voteResult: VoteMapResult,
    val topic: String,
)

/**
 * Computes vote map tiles from an MP's voting record.
 * Uses rebellion instances to determine party line.
 */
object VoteMapCalculator {
    /**
     * @param memberVotes MP's votes with division context
     * @param rebellionInstances Known rebellion instances (from RebellionCalculator)
     * @param partyName MP's party name
     */
    fun compute(
        memberVotes: List<MemberVoteWithDivision>,
        rebellionInstances: List<RebellionInstance>,
    ): List<VoteMapTile> {
        val rebellionDivisionIds = rebellionInstances.map { it.divisionId }.toSet()

        return memberVotes.map { vote ->
            val isRebel = vote.divisionId in rebellionDivisionIds
            val result = when {
                vote.vote == VoteType.NO_VOTE_RECORDED -> VoteMapResult.NO_VOTE
                isRebel -> VoteMapResult.REBEL
                else -> VoteMapResult.WITH_PARTY
            }
            VoteMapTile(
                divisionId = vote.divisionId,
                divisionTitle = vote.divisionTitle,
                divisionDate = vote.divisionDate,
                mpVote = vote.vote,
                partyLine = if (isRebel) {
                    // If rebel, party line is opposite of MP's vote
                    if (vote.vote == VoteType.AYE) VoteType.NO else VoteType.AYE
                } else {
                    // If not rebel, party line is same as MP's vote
                    vote.vote
                },
                voteResult = result,
                topic = extractTopic(vote.divisionTitle),
            )
        }.sortedByDescending { it.divisionDate }
    }

    /**
     * Extract a topic keyword from a division title.
     * Simple heuristic — not NLP.
     */
    fun extractTopic(title: String): String {
        // "Bill: " → bill name before the colon
        val colonIndex = title.indexOf(":")
        if (colonIndex > 0) {
            return title.substring(0, colonIndex).trim().take(30)
        }
        // "Opposition Day: " → the topic after the colon
        // Fallback: first 3 words
        return title.split(" ").take(3).joinToString(" ").take(30)
    }
}
