package com.goveye.app.data.mapper

import com.goveye.app.data.dto.bills.BillDto
import com.goveye.app.data.dto.bills.BillStageDto
import com.goveye.app.data.dto.bills.StageSittingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillMapperTest {
    @Test
    fun `maps bill DTO to domain with current stage`() {
        val dto =
            BillDto(
                billId = 3973,
                shortTitle = "Test Bill",
                currentHouse = "Commons",
                originatingHouse = "Commons",
                lastUpdate = "2025-09-16",
                currentStage =
                    BillStageDto(
                        id = 1,
                        stageId = 7,
                        description = "2nd reading",
                        abbreviation = "2R",
                        house = "Commons",
                        sortOrder = 2
                    )
            )

        val bill = BillMapper.toDomain(dto)

        assertEquals(3973, bill.id)
        assertEquals("Test Bill", bill.shortTitle)
        assertEquals("2nd reading", bill.currentStage?.description)
        assertEquals("2R", bill.currentStage?.abbreviation)
    }

    @Test
    fun `maps bill DTO with null current stage`() {
        val dto =
            BillDto(
                billId = 3973,
                shortTitle = "Test Bill",
                currentHouse = "Commons",
                originatingHouse = "Commons",
                lastUpdate = "2025-09-16",
                currentStage = null
            )

        val bill = BillMapper.toDomain(dto)

        assertNull(bill.currentStage)
    }

    @Test
    fun `maps stage DTO to domain with sitting dates`() {
        val dto =
            BillStageDto(
                id = 1,
                stageId = 7,
                description = "2nd reading",
                abbreviation = "2R",
                house = "Commons",
                sortOrder = 2,
                stageSittings =
                    listOf(
                        StageSittingDto(date = "2025-07-11"),
                        StageSittingDto(date = null)
                    )
            )

        val stage = BillMapper.toDomain(dto)

        assertEquals(listOf("2025-07-11"), stage.sittingDates)
    }

    @Test
    fun `maps stages list sorted by sortOrder`() {
        val dtos =
            listOf(
                BillStageDto(id = 2, stageId = 7, description = "2nd reading", house = "Commons", sortOrder = 2),
                BillStageDto(id = 1, stageId = 6, description = "1st reading", house = "Commons", sortOrder = 1)
            )

        val stages = BillMapper.toDomainStages(dtos)

        assertEquals(2, stages.size)
        assertEquals(1, stages[0].sortOrder)
        assertEquals(2, stages[1].sortOrder)
    }

    @Test
    fun `maps bill list`() {
        val dtos =
            listOf(
                BillDto(
                    billId = 1,
                    shortTitle = "A",
                    currentHouse = "Commons",
                    originatingHouse = "Commons",
                    lastUpdate = "2025-01-01"
                ),
                BillDto(
                    billId = 2,
                    shortTitle = "B",
                    currentHouse = "Commons",
                    originatingHouse = "Commons",
                    lastUpdate = "2025-01-02"
                )
            )

        val bills = BillMapper.toDomainList(dtos)

        assertEquals(2, bills.size)
        assertEquals(1, bills[0].id)
        assertEquals(2, bills[1].id)
    }
}
