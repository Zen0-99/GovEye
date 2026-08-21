package com.goveye.app.domain.stats

import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.VoteType

/**
 * Result of rebellion rate computation for a single MP.
 */
data class RebellionStats(
    val rebellionCount: Int,
    val totalDivisionsVoted: Int,
    val rebellionRate: Float,
    val rebellionInstances: List<RebellionInstance>
)

/**
 * A single instance where the MP voted against their party majority.
 */
data class RebellionInstance(
    val divisionId: Int,
    val mpVote: VoteType,
    val partyMajorityVote: VoteType,
    val partyAyeCount: Int,
    val partyNoCount: Int
)

/**
 * Pre-aggregated party vote counts for a single division.
 * Used by the fast [RebellionCalculator.computeAggregated] overload —
 * avoids loading all 650 individual vote entities per division.
 */
data class PartyVoteSummary(val divisionId: Int, val partyAyes: Int, val partyNoes: Int)

/**
 * Computes rebellion rate using the party-majority methodology.
 *
 * For each division where the MP voted (Aye or No, not NoVoteRecorded):
 * 1. Find all votes by the MP's party members in that division
 * 2. Determine party majority (more Ayes → party line is Aye; more Noes → party line is No)
 * 3. If MP voted opposite to party majority → count as rebellion
 * 4. If party evenly split (tie) → skip (no rebellion possible)
 *
 * This is a pure function — no DI, no side effects. Fully testable.
 *
 * Methodology: matches PublicWhip / TheyWorkForYou approach.
 * No editorial judgment is made about whether a rebellion is justified.
 */
object RebellionCalculator {
    /**
     * @param memberVotes The MP's votes (one per division they participated in)
     * @param allVotesByDivision Map of divisionId → all votes in that division
     * @param memberPartyName The MP's party name (used to filter party members' votes)
     */
    fun compute(
        memberVotes: List<DivisionVote>,
        allVotesByDivision: Map<Int, List<DivisionVote>>,
        memberPartyName: String
    ): RebellionStats {
        val rebellions = mutableListOf<RebellionInstance>()
        var divisionsVoted = 0

        for (memberVote in memberVotes) {
            // Skip if MP didn't actually vote (NoVoteRecorded or teller-only)
            if (memberVote.vote == VoteType.NO_VOTE_RECORDED) continue
            divisionsVoted++

            val divisionVotes = allVotesByDivision[memberVote.divisionId] ?: continue
            val partyVotes = divisionVotes.filter {
                it.partyName == memberPartyName && it.vote != VoteType.NO_VOTE_RECORDED
            }

            if (partyVotes.isEmpty()) continue

            val partyAyes = partyVotes.count { it.vote == VoteType.AYE }
            val partyNoes = partyVotes.count { it.vote == VoteType.NO }

            // Skip ties — no clear party line
            if (partyAyes == partyNoes) continue

            val partyMajority = if (partyAyes > partyNoes) VoteType.AYE else VoteType.NO

            // Rebellion = MP voted opposite to party majority
            if (memberVote.vote != partyMajority) {
                rebellions.add(
                    RebellionInstance(
                        divisionId = memberVote.divisionId,
                        mpVote = memberVote.vote,
                        partyMajorityVote = partyMajority,
                        partyAyeCount = partyAyes,
                        partyNoCount = partyNoes
                    )
                )
            }
        }

        val rate = if (divisionsVoted > 0) {
            rebellions.size.toFloat() / divisionsVoted
        } else {
            0f
        }

        return RebellionStats(
            rebellionCount = rebellions.size,
            totalDivisionsVoted = divisionsVoted,
            rebellionRate = rate,
            rebellionInstances = rebellions
        )
    }

    /**
     * Fast overload — uses pre-aggregated party vote counts from a SQL
     * GROUP BY query instead of loading all individual vote entities.
     *
     * @param memberVotes The MP's votes (one per division they participated in)
     * @param partyVoteCounts Map of divisionId → party aye/no counts (already filtered by party)
     */
    fun computeAggregated(
        memberVotes: List<DivisionVote>,
        partyVoteCounts: Map<Int, PartyVoteSummary>
    ): RebellionStats {
        val rebellions = mutableListOf<RebellionInstance>()
        var divisionsVoted = 0

        for (memberVote in memberVotes) {
            if (memberVote.vote == VoteType.NO_VOTE_RECORDED) continue
            divisionsVoted++

            val counts = partyVoteCounts[memberVote.divisionId] ?: continue

            // Skip ties — no clear party line
            if (counts.partyAyes == counts.partyNoes) continue

            val partyMajority = if (counts.partyAyes > counts.partyNoes) VoteType.AYE else VoteType.NO

            if (memberVote.vote != partyMajority) {
                rebellions.add(
                    RebellionInstance(
                        divisionId = memberVote.divisionId,
                        mpVote = memberVote.vote,
                        partyMajorityVote = partyMajority,
                        partyAyeCount = counts.partyAyes,
                        partyNoCount = counts.partyNoes
                    )
                )
            }
        }

        val rate = if (divisionsVoted > 0) {
            rebellions.size.toFloat() / divisionsVoted
        } else {
            0f
        }

        return RebellionStats(
            rebellionCount = rebellions.size,
            totalDivisionsVoted = divisionsVoted,
            rebellionRate = rate,
            rebellionInstances = rebellions
        )
    }
}
