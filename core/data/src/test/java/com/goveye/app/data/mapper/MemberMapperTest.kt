package com.goveye.app.data.mapper

import com.goveye.app.data.dto.members.HouseMembershipDto
import com.goveye.app.data.dto.members.MemberDto
import com.goveye.app.data.dto.members.MemberItem
import com.goveye.app.data.dto.members.MembershipStatusDto
import com.goveye.app.data.dto.members.PartyDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberMapperTest {
    @Test
    fun `maps DTO to domain correctly`() {
        val dto =
            MemberDto(
                id = 172,
                nameListAs = "Abbott, Ms Diane",
                nameDisplayAs = "Diane Abbott",
                nameFullTitle = "Ms Diane Abbott",
                nameAddressAs = "Ms Abbott",
                latestParty =
                    PartyDto(
                        id = 15,
                        name = "Labour",
                        abbreviation = "Lab",
                        backgroundColour = "d50000",
                        foregroundColour = "ffffff",
                    ),
                gender = "F",
                latestHouseMembership =
                    HouseMembershipDto(
                        membershipFrom = "Hackney North and Stoke Newington",
                        membershipFromId = 4074,
                        house = 1,
                        membershipStartDate = "1987-06-11T00:00:00",
                        membershipStatus = MembershipStatusDto(statusIsActive = true),
                    ),
                thumbnailUrl = "https://example.com/thumb.jpg",
            )

        val mp = MemberMapper.toDomain(dto)

        assertEquals(172, mp.id)
        assertEquals("Abbott, Ms Diane", mp.nameListAs)
        assertEquals("Labour", mp.party?.name)
        assertEquals("Lab", mp.party?.abbreviation)
        assertEquals("d50000", mp.party?.backgroundColour)
        assertEquals("Hackney North and Stoke Newington", mp.constituency?.name)
        assertEquals(4074, mp.constituency?.id)
        assertTrue(mp.isActive)
        assertEquals(1, mp.house)
        assertEquals("1987-06-11T00:00:00", mp.membershipStartDate)
    }

    @Test
    fun `handles null party`() {
        val dto =
            MemberDto(
                id = 172,
                nameListAs = "Abbott, Ms Diane",
                nameDisplayAs = "Diane Abbott",
                latestParty = null,
                latestHouseMembership = null,
            )

        val mp = MemberMapper.toDomain(dto)

        assertNull(mp.party)
    }

    @Test
    fun `handles null house membership`() {
        val dto =
            MemberDto(
                id = 172,
                nameListAs = "Abbott, Ms Diane",
                nameDisplayAs = "Diane Abbott",
                latestParty = null,
                latestHouseMembership = null,
            )

        val mp = MemberMapper.toDomain(dto)

        assertNull(mp.constituency)
        assertEquals(1, mp.house)
        assertFalse(mp.isActive)
        assertNull(mp.membershipStartDate)
    }

    @Test
    fun `maps list of items`() {
        val items =
            listOf(
                MemberItem(
                    value =
                        MemberDto(
                            id = 172,
                            nameListAs = "Abbott, Ms Diane",
                            nameDisplayAs = "Diane Abbott",
                        ),
                ),
                MemberItem(
                    value =
                        MemberDto(
                            id = 39,
                            nameListAs = "Whittingdale, Sir John",
                            nameDisplayAs = "John Whittingdale",
                        ),
                ),
            )

        val mps = MemberMapper.toDomain(items)

        assertEquals(2, mps.size)
        assertEquals(172, mps[0].id)
        assertEquals(39, mps[1].id)
    }
}
