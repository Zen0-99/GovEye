package com.goveye.app.domain.stats

import com.goveye.app.domain.model.VoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivisionWeightCalculatorTest {

    @Test
    fun `MP votes with party (no rebellion, landslide) - low weight`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.AYE,
            isRebellion = false,
            divisionCloseness = 0.0,
        )
        assertEquals(3.0, weight.score, 0.001)
    }

    @Test
    fun `MP votes with party (no rebellion, close vote) - medium weight`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.AYE,
            isRebellion = false,
            divisionCloseness = 1.0,
        )
        assertEquals(6.0, weight.score, 0.001)
    }

    @Test
    fun `MP rebels (close vote) - high weight clamped to 10`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.NO,
            isRebellion = true,
            divisionCloseness = 1.0,
        )
        assertEquals(10.0, weight.score, 0.001)
    }

    @Test
    fun `MP rebels (landslide) - high weight`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.NO,
            isRebellion = true,
            divisionCloseness = 0.0,
        )
        assertEquals(7.0, weight.score, 0.001)
    }

    @Test
    fun `MP did not vote - zero weight`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.NO_VOTE_RECORDED,
            isRebellion = false,
            divisionCloseness = 0.5,
        )
        assertEquals(0.0, weight.score, 0.001)
    }

    @Test
    fun `Score never exceeds 10`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.AYE,
            isRebellion = true,
            divisionCloseness = 1.0,
        )
        assertTrue(weight.score <= 10.0)
    }

    @Test
    fun `Rebellion weight is always higher than non-rebellion for same closeness`() {
        val rebellionWeight = DivisionWeightCalculator.compute(
            mpVote = VoteType.NO,
            isRebellion = true,
            divisionCloseness = 0.5,
        )
        val nonRebellionWeight = DivisionWeightCalculator.compute(
            mpVote = VoteType.AYE,
            isRebellion = false,
            divisionCloseness = 0.5,
        )
        assertTrue(rebellionWeight.score > nonRebellionWeight.score)
    }

    @Test
    fun `DivisionWeight data class fields are populated correctly`() {
        val weight = DivisionWeightCalculator.compute(
            mpVote = VoteType.NO,
            isRebellion = true,
            divisionCloseness = 0.5,
        )
        assertTrue(weight.isRebellion)
        assertEquals(VoteType.NO, weight.voteType)
    }
}
