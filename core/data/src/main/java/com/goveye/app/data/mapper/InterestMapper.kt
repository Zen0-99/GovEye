package com.goveye.app.data.mapper

import com.goveye.app.data.dto.interests.InterestDto
import com.goveye.app.data.dto.interests.InterestFieldDto
import com.goveye.app.domain.model.Interest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object InterestMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toDomain(dto: InterestDto, memberId: Int): Interest =
        Interest(
            id = dto.id,
            memberId = dto.member?.id ?: memberId,
            summary = dto.summary,
            categoryName = dto.category.name,
            categoryNumber = dto.category.number,
            registrationDate = dto.registrationDate,
            publishedDate = dto.publishedDate,
            fieldsJson = json.encodeToString(ListSerializer(InterestFieldDto.serializer()), dto.fields),
        )

    fun toDomainList(dtos: List<InterestDto>, memberId: Int): List<Interest> = dtos.map { toDomain(it, memberId) }
}
