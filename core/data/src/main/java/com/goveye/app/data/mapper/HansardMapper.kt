package com.goveye.app.data.mapper

import com.goveye.app.data.dto.hansard.HansardContributionDto
import com.goveye.app.domain.model.HansardContribution

object HansardMapper {
    fun toDomain(dto: HansardContributionDto): HansardContribution = HansardContribution(
        itemId = dto.itemId,
        memberId = dto.memberId,
        memberName = dto.memberName,
        contributionText = dto.contributionText,
        sittingDate = dto.sittingDate,
        house = dto.house,
        debateSection = dto.debateSection
    )

    fun toDomainList(dtos: List<HansardContributionDto>): List<HansardContribution> = dtos.map { toDomain(it) }
}
