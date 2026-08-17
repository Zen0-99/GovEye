package com.goveye.app.domain.stats

import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.VoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RebellionCalculatorTest {
    private fun vote(
        divisionId: Int,
        memberId: Int,
        voteType: VoteType,
        partyName: String = "Labour",
    ) = DivisionVote(
        divisionId = divisionId,
        memberId = memberId,
        vote = voteType,
        memberName = "MP$memberId",
        partyName = partyName,
        partyColour = null,
        constituencyName = null,
        isTeller = false,
    )

    @Test
    fun `MP votes with party majority - no rebellion`() {
        val memberVotes = listOf(vote(1, 100, VoteType.AYE))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.AYE),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.AYE),
                vote(1, 103, VoteType.NO),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(0, stats.rebellionCount)
        assertEquals(1, stats.totalDivisionsVoted)
        assertEquals(0f, stats.rebellionRate)
    }

    @Test
    fun `MP votes against party majority - rebellion`() {
        val memberVotes = listOf(vote(1, 100, VoteType.NO))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.NO),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.AYE),
                vote(1, 103, VoteType.AYE),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(1, stats.rebellionCount)
        assertEquals(1, stats.totalDivisionsVoted)
        assertEquals(1f, stats.rebellionRate)
    }

    @Test
    fun `Party tie - no rebellion counted`() {
        val memberVotes = listOf(vote(1, 100, VoteType.AYE))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.AYE),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.NO),
                vote(1, 103, VoteType.NO),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(0, stats.rebellionCount)
        assertEquals(1, stats.totalDivisionsVoted)
    }

    @Test
    fun `MP didn't vote - not counted`() {
        val memberVotes = listOf(vote(1, 100, VoteType.NO_VOTE_RECORDED))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.NO_VOTE_RECORDED),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.AYE),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(0, stats.rebellionCount)
        assertEquals(0, stats.totalDivisionsVoted)
    }

    @Test
    fun `Empty votes - zero rebellion rate`() {
        val stats = RebellionCalculator.compute(emptyList(), emptyMap(), "Labour")
        assertEquals(0, stats.rebellionCount)
        assertEquals(0, stats.totalDivisionsVoted)
        assertEquals(0f, stats.rebellionRate)
    }

    @Test
    fun `All rebellions - 100 percent rebellion rate`() {
        val memberVotes = listOf(
            vote(1, 100, VoteType.NO),
            vote(2, 100, VoteType.NO),
        )
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.NO),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.AYE),
            ),
            2 to listOf(
                vote(2, 100, VoteType.NO),
                vote(2, 101, VoteType.AYE),
                vote(2, 102, VoteType.AYE),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(2, stats.rebellionCount)
        assertEquals(2, stats.totalDivisionsVoted)
        assertEquals(1f, stats.rebellionRate)
    }

    @Test
    fun `Mixed rebellions and party-line votes`() {
        val memberVotes = listOf(
            vote(1, 100, VoteType.AYE),  // with party
            vote(2, 100, VoteType.NO),   // against party
            vote(3, 100, VoteType.AYE),  // with party
        )
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.AYE),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.NO),
            ),
            2 to listOf(
                vote(2, 100, VoteType.NO),
                vote(2, 101, VoteType.AYE),
                vote(2, 102, VoteType.AYE),
            ),
            3 to listOf(
                vote(3, 100, VoteType.AYE),
                vote(3, 101, VoteType.AYE),
                vote(3, 102, VoteType.AYE),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(1, stats.rebellionCount)
        assertEquals(3, stats.totalDivisionsVoted)
        assertEquals(1f / 3f, stats.rebellionRate, 0.001f)
    }

    @Test
    fun `Rebellion instance contains correct details`() {
        val memberVotes = listOf(vote(1, 100, VoteType.NO))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.NO),
                vote(1, 101, VoteType.AYE),
                vote(1, 102, VoteType.AYE),
                vote(1, 103, VoteType.AYE),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        assertEquals(1, stats.rebellionInstances.size)
        val instance = stats.rebellionInstances[0]
        assertEquals(1, instance.divisionId)
        assertEquals(VoteType.NO, instance.mpVote)
        assertEquals(VoteType.AYE, instance.partyMajorityVote)
        assertEquals(3, instance.partyAyeCount)
        assertEquals(1, instance.partyNoCount)
    }

    @Test
    fun `Different party members are not counted`() {
        val memberVotes = listOf(vote(1, 100, VoteType.NO))
        val allVotes = mapOf(
            1 to listOf(
                vote(1, 100, VoteType.NO, "Labour"),
                vote(1, 101, VoteType.AYE, "Conservative"),
                vote(1, 102, VoteType.AYE, "Conservative"),
                vote(1, 103, VoteType.AYE, "Conservative"),
            ),
        )
        val stats = RebellionCalculator.compute(memberVotes, allVotes, "Labour")
        // Only 1 Labour vote (the MP themselves), so no party majority can be determined
        assertEquals(0, stats.rebellionCount)
        assertEquals(1, stats.totalDivisionsVoted)
    }
}
