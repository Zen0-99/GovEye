package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.MemberVoteWithDivisionRow
import com.goveye.app.data.local.dao.PartyBreakdownRow
import com.goveye.app.data.local.dao.SpeechWithDivision
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
     * One-shot: get an MP's votes as domain objects (no Flow wrapper).
     * Used by rebellion rate calculation where we need the data once.
     */
    suspend fun getMemberVotes(memberId: Int): List<DivisionVote> =
        divisionDao.getVotesForMember(memberId).map { it.toDomain() }

    /**
     * Compute party-level breakdown of Ayes and Noes for a division.
     * Uses a single SQL GROUP BY query — replaces loading 650 vote entities
     * and grouping in Kotlin.
     */
    suspend fun getPartyBreakdown(divisionId: Int): List<PartyBreakdown> =
        divisionDao.getPartyBreakdownForDivision(divisionId).map { it.toDomain() }

    private fun PartyBreakdownRow.toDomain() = PartyBreakdown(
        partyName = partyName,
        partyColour = partyColour,
        ayeCount = ayeCount,
        noCount = noCount,
        totalMembers = totalMembers
    )

    /**
     * Get a member's voting record with division context.
     * Uses a single SQL JOIN — replaces N+1 queries (1 + N where N is the
     * number of divisions the MP voted in, ~200 for active MPs).
     * Returns a list of [MemberVoteWithDivision] sorted by date descending.
     */
    suspend fun getMemberVotingWithDivisions(memberId: Int): List<MemberVoteWithDivision> =
        divisionDao.getAllMemberVoting(memberId).mapNotNull { it.toDomain() }

    /**
     * Get a page of a member's voting record with division context.
     * Uses a single SQL JOIN with LIMIT/OFFSET — no N+1 queries.
     */
    suspend fun getPagedMemberVoting(memberId: Int, limit: Int, offset: Int): List<MemberVoteWithDivision> =
        divisionDao.getPagedMemberVoting(memberId, limit, offset)
            .mapNotNull { it.toDomain() }

    /**
     * Search a member's voting record by division title.
     * Returns a page of results with LIMIT/OFFSET.
     */
    suspend fun searchPagedMemberVoting(
        memberId: Int,
        query: String,
        limit: Int,
        offset: Int
    ): List<MemberVoteWithDivision> = divisionDao.searchPagedMemberVoting(memberId, query, limit, offset)
        .mapNotNull { it.toDomain() }

    suspend fun countVotesForMember(memberId: Int): Int = divisionDao.countVotesForMember(memberId)

    suspend fun countSearchVotesForMember(memberId: Int, query: String): Int =
        divisionDao.countSearchVotesForMember(memberId, query)

    private fun MemberVoteWithDivisionRow.toDomain(): MemberVoteWithDivision? {
        val voteType = runCatching { VoteType.valueOf(vote) }.getOrNull() ?: return null
        return MemberVoteWithDivision(
            divisionId = divisionId,
            divisionTitle = divisionTitle,
            divisionDate = divisionDate,
            house = house,
            ayeCount = ayeCount,
            noCount = noCount,
            vote = voteType,
            isTeller = isTeller
        )
    }

    /**
     * Get all votes for a set of divisions (used for rebellion computation).
     * Returns a map of divisionId → list of all votes in that division.
     *
     * WARNING: This does N+1 queries and loads 650 entities per division.
     * For rebellion rate, use [getPartyVoteCounts] instead — it does a single
     * SQL GROUP BY and returns ~200 rows instead of 130k+ entities.
     */
    suspend fun getAllVotesForDivisions(divisionIds: List<Int>): Map<Int, List<DivisionVote>> =
        divisionIds.associateWith { id ->
            divisionDao.getVotesForDivision(id).map { it.toDomain() }
        }

    /**
     * Get pre-aggregated party vote counts for a set of divisions.
     * Single SQL query with GROUP BY — returns one row per division with
     * the party's aye/no counts. Used by RebellionCalculator.computeAggregated.
     *
     * This replaces getAllVotesForDivisions for rebellion rate computation:
     * - Before: N+1 queries, 650 entities per division, 130k+ objects in memory
     * - After: 1 query, ~200 rows, no entity loading
     */
    suspend fun getPartyVoteCounts(
        divisionIds: List<Int>,
        partyName: String
    ): Map<Int, com.goveye.app.domain.stats.PartyVoteSummary> = divisionDao.getPartyVoteCounts(divisionIds, partyName)
        .associate { it.divisionId to it.toDomain() }

    private fun com.goveye.app.data.local.dao.PartyVoteCount.toDomain() = com.goveye.app.domain.stats.PartyVoteSummary(
        divisionId = divisionId,
        partyAyes = partyAyes,
        partyNoes = partyNoes
    )

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

    suspend fun getSpeechesByMember(memberId: Int, limit: Int = 50): List<SpeechWithDivision> =
        debateSpeechDao.getSpeechesByMember(memberId, limit)

    private fun DebateSpeechEntity.toDomain(): DebateSpeech = DebateSpeech(
        debateGid = debateGid,
        speechGid = speechGid,
        divisionId = divisionId,
        speakerName = speakerName,
        memberId = memberId,
        twfyPersonId = twfyPersonId,
        speakerPosition = speakerPosition,
        speechText = speechText,
        speechOrder = speechOrder,
        isIntervention = isIntervention
    )
}
