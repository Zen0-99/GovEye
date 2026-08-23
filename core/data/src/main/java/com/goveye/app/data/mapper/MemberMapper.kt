package com.goveye.app.data.mapper

import com.goveye.app.data.dto.members.BiographyExperienceDto
import com.goveye.app.data.dto.members.BiographyItemDto
import com.goveye.app.data.dto.members.ContactDto
import com.goveye.app.data.dto.members.MemberDto
import com.goveye.app.data.dto.members.MemberItem
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.BiographyItem
import com.goveye.app.domain.model.Constituency
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.Party

object MemberMapper {
    fun toDomain(dto: MemberDto): Mp = Mp(
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
                    // The API returns "Lab" for both "Labour" and "Labour (Co-op)"
                    // since they share party ID 15. Append " Co-op" to distinguish
                    // the Co-operative variant in the directory list.
                    abbreviation = if (it.name.contains("(Co-op)", ignoreCase = true)) {
                        "${it.abbreviation} Co-op"
                    } else {
                        it.abbreviation
                    },
                    backgroundColour = it.backgroundColour,
                    foregroundColour = it.foregroundColour
                )
            },
        constituency =
            dto.latestHouseMembership?.let {
                Constituency(
                    id = it.membershipFromId,
                    name = it.membershipFrom
                )
            },
        house = dto.latestHouseMembership?.house ?: 1,
        membershipStartDate = dto.latestHouseMembership?.membershipStartDate,
        isActive = dto.latestHouseMembership?.membershipStatus?.statusIsActive ?: false,
        thumbnailUrl = dto.thumbnailUrl
    )

    fun toDomain(items: List<MemberItem>): List<Mp> = items.map { toDomain(it.value) }

    fun toDomainMp(entity: MpEntity): Mp = Mp(
        id = entity.id,
        nameListAs = entity.nameListAs,
        nameDisplayAs = entity.nameDisplayAs,
        nameFullTitle = entity.nameFullTitle,
        gender = entity.gender,
        party = Party(
            id = entity.partyId,
            name = entity.partyName,
            abbreviation = entity.partyAbbreviation,
            backgroundColour = entity.partyBackgroundColour,
            foregroundColour = entity.partyForegroundColour
        ),
        constituency = Constituency(
            id = entity.constituencyId,
            name = entity.constituencyName
        ),
        house = entity.house,
        membershipStartDate = entity.membershipStartDate,
        isActive = entity.isActive,
        thumbnailUrl = entity.thumbnailUrl
    )

    fun toExperienceDomain(dto: BiographyExperienceDto): BiographyExperience = BiographyExperience(
        id = dto.id,
        type = dto.type,
        title = dto.title,
        organisation = dto.organisation,
        startMonth = dto.startMonth,
        startYear = dto.startYear,
        endMonth = dto.endMonth,
        endYear = dto.endYear
    )

    fun toBiographyDomain(dto: BiographyItemDto): BiographyItem = BiographyItem(
        house = dto.house?.name,
        name = dto.name,
        startDate = dto.startDate,
        endDate = dto.endDate,
        isCurrent = dto.endDate == null
    )

    fun toContactDomain(dto: ContactDto): Contact = Contact(
        type = dto.type,
        isPreferred = dto.isPreferred,
        isWebAddress = dto.isWebAddress,
        line1 = dto.line1,
        line2 = dto.line2,
        line3 = dto.line3,
        line4 = dto.line4,
        line5 = dto.line5,
        postcode = dto.postcode,
        phone = dto.phone,
        email = dto.email,
        website = dto.website,
        openingHours = dto.openingHours
    )

    fun toEntity(mp: Mp, timestamp: Long): MpEntity = MpEntity(
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
        lastUpdated = timestamp
    )
}
