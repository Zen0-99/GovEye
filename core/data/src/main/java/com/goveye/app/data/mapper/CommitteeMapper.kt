package com.goveye.app.data.mapper

import com.goveye.app.data.dto.committees.CommitteeItem
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.domain.model.Committee

object CommitteeMapper {
    fun toEntity(dto: CommitteeItem, timestamp: Long): CommitteeEntity = CommitteeEntity(
        id = dto.id,
        name = dto.name,
        house = dto.house,
        categoryName = dto.category?.name,
        startDate = dto.startDate,
        endDate = dto.endDate,
        isActive = dto.endDate == null,
        lastUpdated = timestamp
    )

    fun toDomain(entity: CommitteeEntity): Committee = Committee(
        id = entity.id,
        name = entity.name,
        house = entity.house,
        categoryName = entity.categoryName,
        startDate = entity.startDate,
        endDate = entity.endDate,
        isActive = entity.isActive
    )

    fun toCrossRef(memberId: Int, committeeId: Int, timestamp: Long): MpCommitteeCrossRef =
        MpCommitteeCrossRef(memberId = memberId, committeeId = committeeId, lastUpdated = timestamp)
}
