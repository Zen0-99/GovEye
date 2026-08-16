package com.goveye.app.data.mapper

import com.goveye.app.data.dto.bills.BillDto
import com.goveye.app.data.dto.bills.BillStageDto
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.BillStage

object BillMapper {
    fun toDomain(dto: BillDto): Bill =
        Bill(
            id = dto.billId,
            shortTitle = dto.shortTitle,
            longTitle = dto.longTitle,
            summary = dto.summary,
            currentHouse = dto.currentHouse,
            originatingHouse = dto.originatingHouse,
            isAct = dto.isAct,
            currentStage = dto.currentStage?.let { toDomain(it) },
        )

    fun toDomain(dto: BillStageDto): BillStage =
        BillStage(
            stageId = dto.stageId,
            description = dto.description,
            abbreviation = dto.abbreviation ?: "",
            house = dto.house,
            sortOrder = dto.sortOrder,
            sittingDates = dto.stageSittings.mapNotNull { it.date },
        )

    fun toDomainList(dtos: List<BillDto>): List<Bill> = dtos.map { toDomain(it) }

    fun toDomainStages(dtos: List<BillStageDto>): List<BillStage> = dtos.map { toDomain(it) }.sortedBy { it.sortOrder }
}
