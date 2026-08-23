package com.goveye.app.ui.screens.onboarding

import com.goveye.app.domain.model.MpTag
import com.goveye.app.domain.model.PartyLeader

/**
 * Ranks MPs by recency-weighted tag hits from the mp_tags table
 * (precomputed by 14-02 build_mp_tags.py per D-08).
 *
 * Per D-09: MPs with no matching tags are excluded from Recommended —
 * they appear in the "All MPs" list only.
 *
 * Per D-07: Party leaders are always included in Recommended even if
 * they have no tag matches.
 */
object MpCurationHelper {

    /**
     * Ranks MPs by recency-weighted tag hits.
     *
     * For each selected tag, queries mp_tags (via the allMpTags flow).
     * Aggregates scores per MP across all selected tags. Sorts by
     * totalScore descending (recency-weighted — higher score = more
     * recent/relevant per D-08).
     *
     * Marks party leaders with isPartyLeader=true (D-07). Party leaders
     * are included even if they have no tag matches.
     *
     * @return only MPs with at least 1 tag match + all party leaders,
     *   sorted by totalScore descending
     */
    fun getRecommendedMps(
        selectedTags: Set<String>,
        mpTags: List<MpTag>,
        partyLeaders: List<PartyLeader>
    ): List<RecommendedMp> {
        val leaderMemberIds = partyLeaders.map { it.memberId }.toSet()
        val leaderByMemberId = partyLeaders.associateBy { it.memberId }

        // Aggregate scores per MP across all selected tags
        val scoresByMp = mutableMapOf<Int, MutableMap<String, Int>>()
        if (selectedTags.isNotEmpty()) {
            for (mpTag in mpTags) {
                if (mpTag.tag in selectedTags) {
                    scoresByMp
                        .getOrPut(mpTag.memberId) { mutableMapOf() }
                        .merge(mpTag.tag, mpTag.hitCount) { a, b -> a + b }
                }
            }
        }

        // Build recommended MPs from tag matches
        val recommended = scoresByMp.entries.map { (memberId, tagScores) ->
            RecommendedMp(
                memberId = memberId,
                matchedTags = tagScores.keys.toList().sorted(),
                totalScore = tagScores.values.sum(),
                isPartyLeader = memberId in leaderMemberIds,
                leaderTitle = leaderByMemberId[memberId]?.title
            )
        }.toMutableList()

        // Add party leaders that have no tag matches (D-07 — always recommended)
        for (leader in partyLeaders) {
            if (leader.memberId !in scoresByMp) {
                recommended.add(
                    RecommendedMp(
                        memberId = leader.memberId,
                        matchedTags = emptyList(),
                        totalScore = 0,
                        isPartyLeader = true,
                        leaderTitle = leader.title
                    )
                )
            }
        }

        // Sort by totalScore descending (D-08 recency-weighted)
        return recommended.sortedByDescending { it.totalScore }
    }
}
