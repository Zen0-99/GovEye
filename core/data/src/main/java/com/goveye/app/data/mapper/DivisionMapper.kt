package com.goveye.app.data.mapper

import com.goveye.app.data.dto.votes.DivisionDto
import com.goveye.app.data.dto.votes.LordsDivisionDto
import com.goveye.app.data.dto.votes.LordsMemberVoteDto
import com.goveye.app.data.dto.votes.LordsVoterDto
import com.goveye.app.data.dto.votes.MemberVoteDto
import com.goveye.app.data.dto.votes.VoterDto
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.VoteType

object DivisionMapper {
    // --- Commons ---

    fun toDomain(dto: DivisionDto): Division = Division(
        id = dto.divisionId,
        title = dto.title,
        date = dto.date,
        number = dto.number,
        ayeCount = dto.ayeCount,
        noCount = dto.noCount,
        isDeferred = dto.isDeferred,
        house = 1
    )

    fun toDomain(dto: VoterDto, divisionId: Int, vote: VoteType): DivisionVote = DivisionVote(
        divisionId = divisionId,
        memberId = dto.memberId,
        vote = vote,
        memberName = dto.name,
        partyName = dto.party,
        partyColour = dto.partyColour,
        constituencyName = dto.memberFrom,
        isTeller = false
    )

    fun toDomain(dto: MemberVoteDto): DivisionVote? {
        val publishedDivision = dto.publishedDivision ?: return null
        val voteType =
            when {
                dto.memberVotedAye -> VoteType.AYE
                dto.memberVotedNo -> VoteType.NO
                else -> VoteType.NO_VOTE_RECORDED
            }
        return DivisionVote(
            divisionId = publishedDivision.divisionId,
            memberId = dto.memberId,
            vote = voteType,
            memberName = "",
            partyName = null,
            partyColour = null,
            constituencyName = null,
            isTeller = dto.memberWasTeller
        )
    }

    fun toDomainList(dtos: List<DivisionDto>): List<Division> = dtos.map { toDomain(it) }

    // --- Lords ---
    // Lords use Content / Not Content instead of Aye / No.
    // We map Content → AYE, Not Content → NO for unified handling.

    fun toDomain(dto: LordsDivisionDto): Division = Division(
        id = dto.divisionId,
        title = dto.title,
        date = dto.date,
        number = dto.number,
        ayeCount = dto.memberContentCount,
        noCount = dto.memberNotContentCount,
        isDeferred = false,
        house = 2
    )

    fun toDomain(dto: LordsVoterDto, divisionId: Int, vote: VoteType): DivisionVote = DivisionVote(
        divisionId = divisionId,
        memberId = dto.memberId,
        vote = vote,
        memberName = dto.name,
        partyName = dto.party,
        partyColour = dto.partyColour,
        constituencyName = dto.memberFrom,
        isTeller = false
    )

    fun toDomain(dto: LordsMemberVoteDto): DivisionVote? {
        val publishedDivision = dto.publishedDivision ?: return null
        val voteType =
            when {
                dto.memberVotedContent -> VoteType.AYE
                dto.memberVotedNotContent -> VoteType.NO
                else -> VoteType.NO_VOTE_RECORDED
            }
        return DivisionVote(
            divisionId = publishedDivision.divisionId,
            memberId = dto.memberId,
            vote = voteType,
            memberName = "",
            partyName = null,
            partyColour = null,
            constituencyName = null,
            isTeller = dto.memberWasTeller
        )
    }

    fun toLordsDomainList(dtos: List<LordsDivisionDto>): List<Division> = dtos.map { toDomain(it) }
}
