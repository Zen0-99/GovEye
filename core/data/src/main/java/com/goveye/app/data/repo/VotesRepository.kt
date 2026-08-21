package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.DebateSpeechEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.domain.model.DebateSpeech
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.PartyBreakdown
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.model.VoteType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class VotesRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val debateSpeechDao: DebateSpeechDao
) {
    fun observeDivisions(limit: Int = 50): Flow<RepositoryResult<List<Division>>> =
        divisionDao.observeDivisions(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    fun observeDivisionsByHouse(house: Int, limit: Int = 50): Flow<RepositoryResult<List<Division>>> = if (house == 0) {
        observeDivisions(limit)
    } else {
        divisionDao.observeDivisionsByHouse(house, limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }
    }

    fun searchDivisions(query: String, house: Int = 0, limit: Int = 50): Flow<RepositoryResult<List<Division>>> =
        if (house == 0) {
            divisionDao.searchDivisions(query, limit)
        } else {
            divisionDao.searchDivisionsByHouse(query, house, limit)
        }.map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    fun observeDivision(id: Int): Flow<RepositoryResult<Division?>> = divisionDao.observeDivision(id).map { entity ->
        if (entity == null) {
            RepositoryResult(null, SyncStatus.EMPTY)
        } else {
            RepositoryResult(entity.toDomain(), SyncStatus.FRESH)
        }
    }

    fun observeVotesForDivision(divisionId: Int): Flow<RepositoryResult<List<DivisionVote>>> =
        divisionDao.observeVotesForDivision(divisionId).map { entities ->
            RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
        }

    fun observeMemberVoting(memberId: Int): Flow<RepositoryResult<List<DivisionVote>>> =
        divisionDao.observeVotesForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    /**
     * Compute party-level breakdown of Ayes and Noes for a division.
     */
    suspend fun getPartyBreakdown(divisionId: Int): List<PartyBreakdown> {
        val voteList = getVotesForDivisionSync(divisionId)
        val byParty = voteList.groupBy { it.partyName }
        return byParty.map { (party, partyVotes) ->
            PartyBreakdown(
                partyName = party,
                partyColour = partyVotes.firstOrNull()?.partyColour ?: "",
                ayeCount = partyVotes.count { it.vote == VoteType.AYE.name },
                noCount = partyVotes.count { it.vote == VoteType.NO.name },
                totalMembers = partyVotes.size
            )
        }.sortedByDescending { it.totalMembers }
    }

    /**
     * Get a member's voting record with division context.
     * Returns a list of [MemberVoteWithDivision] sorted by date descending.
     */
    suspend fun getMemberVotingWithDivisions(memberId: Int): List<MemberVoteWithDivision> {
        val votes = divisionDao.getVotesForMember(memberId)
        val divisionIds = votes.map { it.divisionId }.distinct()
        val divisions = divisionIds.associateWith { divisionDao.getDivision(it) }
        return votes.mapNotNull { voteEntity ->
            val division = divisions[voteEntity.divisionId] ?: return@mapNotNull null
            val voteType = runCatching { VoteType.valueOf(voteEntity.vote) }.getOrNull()
                ?: return@mapNotNull null
            MemberVoteWithDivision(
                divisionId = division.id,
                divisionTitle = division.title,
                divisionDate = division.date,
                house = division.house,
                ayeCount = division.ayeCount,
                noCount = division.noCount,
                vote = voteType,
                isTeller = voteEntity.isTeller
            )
        }.sortedByDescending { it.divisionDate }
    }

    /**
     * Get all votes for a set of divisions (used for rebellion computation).
     * Returns a map of divisionId → list of all votes in that division.
     */
    suspend fun getAllVotesForDivisions(divisionIds: List<Int>): Map<Int, List<DivisionVote>> =
        divisionIds.associateWith { id ->
            divisionDao.getVotesForDivision(id).map { it.toDomain() }
        }

    /**
     * Get all divisions for a given house (used for attendance calculation).
     * Returns all division dates so we can compute how many divisions
     * occurred in each period vs how many the MP voted in.
     */
    suspend fun getAllDivisionDates(house: Int): List<String> =
        divisionDao.getAllDivisionsByHouse(house).map { it.date }

    /**
     * Check if the MP's vote is a rebellion (against party majority).
     * Public so the VotePollingWorker can call it directly (detectNewVotesForMember
     * was removed; the worker now computes new votes itself from the bundled DB).
     */
    suspend fun checkIfRebel(divisionId: Int, partyName: String, mpVote: VoteType): Boolean {
        if (mpVote == VoteType.NO_VOTE_RECORDED) return false
        val votes = divisionDao.getVotesForDivision(divisionId)
        val partyVotes = votes.filter { it.partyName.equals(partyName, ignoreCase = true) }
        if (partyVotes.isEmpty()) return false
        val ayes = partyVotes.count { it.vote == "AYE" }
        val noes = partyVotes.count { it.vote == "NO" }
        if (ayes == noes) return false // tie — no rebellion
        val partyMajority = if (ayes > noes) VoteType.AYE else VoteType.NO
        return mpVote != partyMajority
    }

    /**
     * Get all votes for a single division (suspend, one-shot).
     */
    suspend fun getVotesForDivision(divisionId: Int): List<DivisionVote> =
        divisionDao.getVotesForDivision(divisionId).map { it.toDomain() }

    private suspend fun getVotesForDivisionSync(divisionId: Int): List<DivisionVoteEntity> =
        divisionDao.observeVotesForDivision(divisionId).first()

    private fun DivisionEntity.toDomain(): Division = Division(
        id = id,
        title = title,
        date = date,
        number = number,
        ayeCount = ayeCount,
        noCount = noCount,
        isDeferred = isDeferred,
        house = house,
        twfyDebateUrl = twfyDebateUrl
    )

    private fun DivisionVoteEntity.toDomain(): DivisionVote = DivisionVote(
        divisionId = divisionId,
        memberId = memberId,
        vote = runCatching { VoteType.valueOf(vote) }.getOrDefault(VoteType.NO_VOTE_RECORDED),
        memberName = memberName,
        partyName = partyName,
        partyColour = partyColour,
        constituencyName = constituencyName,
        isTeller = isTeller
    )

    // ── Debate transcripts ──────────────────────────────────────────────

    fun observeSpeechesForDivision(divisionId: Int): Flow<List<DebateSpeech>> =
        debateSpeechDao.observeSpeechesForDivision(divisionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getSpeechesForDivision(divisionId: Int): List<DebateSpeech> =
        debateSpeechDao.getSpeechesForDivision(divisionId).map { it.toDomain() }

    suspend fun countSpeechesForDivision(divisionId: Int): Int = debateSpeechDao.countSpeechesForDivision(divisionId)

    private fun DebateSpeechEntity.toDomain(): DebateSpeech = DebateSpeech(
        debateGid = debateGid,
        speechGid = speechGid,
        divisionId = divisionId,
        speakerName = speakerName,
        memberId = memberId,
        speakerPosition = speakerPosition,
        speechText = speechText,
        speechOrder = speechOrder,
        isIntervention = isIntervention
    )
}
