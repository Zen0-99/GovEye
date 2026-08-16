package com.goveye.app.data.mapper

import com.goveye.app.data.dto.votes.DivisionDto
import com.goveye.app.data.dto.votes.MemberVoteDto
import com.goveye.app.data.dto.votes.VoterDto
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.VoteType

object DivisionMapper {
    fun toDomain(dto: DivisionDto): Division =
        Division(
            id = dto.divisionId,
            title = dto.title,
            date = dto.date,
            number = dto.number,
            ayeCount = dto.ayeCount,
            noCount = dto.noCount,
            isDeferred = dto.isDeferred,
            house = 1,
        )

    fun toDomain(dto: VoterDto, divisionId: Int, vote: VoteType): DivisionVote =
        DivisionVote(
            divisionId = divisionId,
            memberId = dto.memberId,
            vote = vote,
            memberName = dto.name,
            partyName = dto.party,
            partyColour = dto.partyColour,
            constituencyName = dto.memberFrom,
            isTeller = false,
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
            isTeller = dto.memberWasTeller,
        )
    }

    fun toDomainList(dtos: List<DivisionDto>): List<Division> = dtos.map { toDomain(it) }
}
