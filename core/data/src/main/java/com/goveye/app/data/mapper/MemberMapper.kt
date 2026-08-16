package com.goveye.app.data.mapper

import com.goveye.app.data.dto.members.MemberDto
import com.goveye.app.data.dto.members.MemberItem
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.domain.model.Constituency
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.Party

object MemberMapper {
    fun toDomain(dto: MemberDto): Mp =
        Mp(
            id = dto.id,
            nameListAs = dto.nameListAs,
            nameDisplayAs = dto.nameDisplayAs,
            nameFullTitle = dto.nameFullTitle,
            gender = dto.gender,
            party =
                dto.latestParty?.let {
                    Party(
                        id = it.id,
                        name = it.name,
                        abbreviation = it.abbreviation,
                        backgroundColour = it.backgroundColour,
                        foregroundColour = it.foregroundColour,
                    )
                },
            constituency =
                dto.latestHouseMembership?.let {
                    Constituency(
                        id = it.membershipFromId,
                        name = it.membershipFrom,
                    )
                },
            house = dto.latestHouseMembership?.house ?: 1,
            membershipStartDate = dto.latestHouseMembership?.membershipStartDate,
            isActive = dto.latestHouseMembership?.membershipStatus?.statusIsActive ?: false,
            thumbnailUrl = dto.thumbnailUrl,
        )

    fun toDomain(items: List<MemberItem>): List<Mp> = items.map { toDomain(it.value) }

    fun toEntity(mp: Mp, timestamp: Long): MpEntity =
        MpEntity(
            id = mp.id,
            nameListAs = mp.nameListAs,
            nameDisplayAs = mp.nameDisplayAs,
            nameFullTitle = mp.nameFullTitle,
            nameAddressAs = null,
            gender = mp.gender,
            partyId = mp.party?.id ?: 0,
            partyName = mp.party?.name ?: "",
            partyAbbreviation = mp.party?.abbreviation ?: "",
            partyBackgroundColour = mp.party?.backgroundColour ?: "",
            partyForegroundColour = mp.party?.foregroundColour ?: "",
            constituencyId = mp.constituency?.id ?: 0,
            constituencyName = mp.constituency?.name ?: "",
            house = mp.house,
            membershipStartDate = mp.membershipStartDate,
            membershipEndDate = null,
            isActive = mp.isActive,
            thumbnailUrl = mp.thumbnailUrl,
            lastUpdated = timestamp,
        )
}
