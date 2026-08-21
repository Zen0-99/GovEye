package com.goveye.app.data.mapper

import com.goveye.app.data.dto.hansard.HansardContributionDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HansardMapperTest {
    @Test
    fun `maps contribution DTO to domain`() {
        val dto =
            HansardContributionDto(
                memberName = "Test MP",
                memberId = 172,
                contributionText = "Hello",
                sittingDate = "2026-07-23",
                house = "Commons",
                debateSection = "Test Debate",
                itemId = 12345
            )

        val domain = HansardMapper.toDomain(dto)

        assertEquals("Test MP", domain.memberName)
        assertEquals(172, domain.memberId)
        assertEquals(12345, domain.itemId)
        assertEquals("Commons", domain.house)
    }

    @Test
    fun `maps contribution list`() {
        val dtos =
            listOf(
                HansardContributionDto(
                    memberName = "A",
                    memberId = 1,
                    contributionText = "x",
                    sittingDate = "2026-01-01",
                    house = "Commons",
                    debateSection = "D"
                ),
                HansardContributionDto(
                    memberName = "B",
                    memberId = 2,
                    contributionText = "y",
                    sittingDate = "2026-01-02",
                    house = "Lords",
                    debateSection = "E"
                )
            )

        val result = HansardMapper.toDomainList(dtos)

        assertEquals(2, result.size)
        assertEquals("A", result[0].memberName)
        assertEquals("B", result[1].memberName)
    }
}
