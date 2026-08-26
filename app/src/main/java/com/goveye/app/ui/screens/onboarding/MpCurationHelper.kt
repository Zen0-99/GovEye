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

    /**
     * Groups recommended MPs by tag, returning up to [maxPerTag] MPs per tag.
     *
     * Each tag that the user selected becomes a group. Within each group, MPs
     * are sorted by their score for that specific tag (descending). An MP can
     * appear in multiple groups if they match multiple tags — this signals to
     * the user that the MP is a strong match across their interests.
     *
     * Party leaders are included in each tag group they have a match in, plus
     * a special "Party Leaders" group if they have no tag matches.
     *
     * @return list of [TagGroupedMps], one per selected tag (sorted by tag name)
     */
    fun getTagGroupedMps(
        selectedTags: Set<String>,
        mpTags: List<MpTag>,
        partyLeaders: List<PartyLeader>,
        maxPerTag: Int = 5
    ): List<TagGroupedMps> {
        val leaderMemberIds = partyLeaders.map { it.memberId }.toSet()
        val leaderByMemberId = partyLeaders.associateBy { it.memberId }

        // Build per-tag score maps: tag → (memberId → score)
        val scoresByTag = mutableMapOf<String, MutableMap<Int, Int>>()
        if (selectedTags.isNotEmpty()) {
            for (mpTag in mpTags) {
                if (mpTag.tag in selectedTags) {
                    scoresByTag
                        .getOrPut(mpTag.tag) { mutableMapOf() }
                        .merge(mpTag.memberId, mpTag.hitCount) { a, b -> a + b }
                }
            }
        }

        // Build groups
        val groups = mutableListOf<TagGroupedMps>()
        for (tag in selectedTags.sorted()) {
            val tagScores = scoresByTag[tag] ?: emptyMap()
            val mps = tagScores.entries
                .map { (memberId, score) ->
                    RecommendedMp(
                        memberId = memberId,
                        matchedTags = listOf(tag),
                        totalScore = score,
                        isPartyLeader = memberId in leaderMemberIds,
                        leaderTitle = leaderByMemberId[memberId]?.title
                    )
                }
                .sortedByDescending { it.totalScore }
                .take(maxPerTag)
            if (mps.isNotEmpty()) {
                groups.add(TagGroupedMps(tag = tag, mps = mps))
            }
        }

        // Add a "Party Leaders" group for leaders with no tag matches
        val leadersWithTags = scoresByTag.values.flatMap { it.keys }.toSet()
        val leadersWithoutTags = partyLeaders.filter { it.memberId !in leadersWithTags }
        if (leadersWithoutTags.isNotEmpty()) {
            groups.add(
                TagGroupedMps(
                    tag = "Party Leaders",
                    mps = leadersWithoutTags.map { leader ->
                        RecommendedMp(
                            memberId = leader.memberId,
                            matchedTags = emptyList(),
                            totalScore = 0,
                            isPartyLeader = true,
                            leaderTitle = leader.title
                        )
                    }
                )
            )
        }

        return groups
    }
}
