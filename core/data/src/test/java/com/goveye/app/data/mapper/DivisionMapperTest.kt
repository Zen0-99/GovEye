package com.goveye.app.data.mapper

import com.goveye.app.data.dto.votes.DivisionDto
import com.goveye.app.data.dto.votes.MemberVoteDto
import com.goveye.app.data.dto.votes.PublishedDivisionDto
import com.goveye.app.data.dto.votes.VoterDto
import com.goveye.app.domain.model.VoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DivisionMapperTest {
    @Test
    fun `maps division DTO to domain`() {
        val dto =
            DivisionDto(
                divisionId = 2411,
                date = "2026-07-15",
                title = "Test Division",
                ayeCount = 330,
                noCount = 109,
                isDeferred = true
            )

        val division = DivisionMapper.toDomain(dto)

        assertEquals(2411, division.id)
        assertEquals("Test Division", division.title)
        assertEquals(330, division.ayeCount)
        assertEquals(109, division.noCount)
        assertEquals(1, division.house)
    }

    @Test
    fun `maps voter DTO to division vote`() {
        val dto =
            VoterDto(
                memberId = 39,
                name = "John Whittingdale",
                party = "Conservative",
                partyColour = "0063ba",
                memberFrom = "Maldon"
            )

        val vote = DivisionMapper.toDomain(dto, divisionId = 2409, vote = VoteType.AYE)

        assertEquals(39, vote.memberId)
        assertEquals(VoteType.AYE, vote.vote)
        assertEquals("John Whittingdale", vote.memberName)
        assertEquals(2409, vote.divisionId)
    }

    @Test
    fun `maps member vote DTO with no vote recorded`() {
        val dto =
            MemberVoteDto(
                memberId = 172,
                memberVotedAye = false,
                memberVotedNo = false,
                publishedDivision =
                    PublishedDivisionDto(
                        divisionId = 2406,
                        date = "2026-07-13",
                        title = "Test"
                    )
            )

        val vote = DivisionMapper.toDomain(dto)

        assertEquals(VoteType.NO_VOTE_RECORDED, vote?.vote)
        assertEquals(2406, vote?.divisionId)
    }

    @Test
    fun `maps member vote DTO with null published division returns null`() {
        val dto =
            MemberVoteDto(
                memberId = 172,
                publishedDivision = null
            )

        val vote = DivisionMapper.toDomain(dto)

        assertNull(vote)
    }

    @Test
    fun `maps division list`() {
        val dtos =
            listOf(
                DivisionDto(divisionId = 1, date = "2026-01-01", title = "A"),
                DivisionDto(divisionId = 2, date = "2026-01-02", title = "B")
            )

        val divisions = DivisionMapper.toDomainList(dtos)

        assertEquals(2, divisions.size)
        assertEquals(1, divisions[0].id)
        assertEquals(2, divisions[1].id)
    }
}
