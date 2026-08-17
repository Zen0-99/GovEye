package com.goveye.app.data.repo

import com.goveye.app.data.api.LordsVotesApi
import com.goveye.app.data.api.VotesApi
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.mapper.DivisionMapper
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
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
    private val votesApi: VotesApi,
    private val lordsVotesApi: LordsVotesApi,
    private val mapper: DivisionMapper,
) {
    fun observeDivisions(limit: Int = 50): Flow<RepositoryResult<List<Division>>> =
        divisionDao.observeDivisions(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.DIVISIONS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeDivisionsByHouse(house: Int, limit: Int = 50): Flow<RepositoryResult<List<Division>>> =
        if (house == 0) {
            observeDivisions(limit)
        } else {
            divisionDao.observeDivisionsByHouse(house, limit).map { entities ->
                if (entities.isEmpty()) {
                    RepositoryResult(emptyList(), SyncStatus.EMPTY)
                } else {
                    val oldest = entities.minOf { it.lastUpdated }
                    val isStale = System.currentTimeMillis() - oldest > CacheTtl.DIVISIONS_MS
                    RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
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

    fun observeDivision(id: Int): Flow<RepositoryResult<Division?>> =
        divisionDao.observeDivision(id).map { entity ->
            if (entity == null) {
                RepositoryResult(null, SyncStatus.EMPTY)
            } else {
                val isStale = System.currentTimeMillis() - entity.lastUpdated > CacheTtl.DIVISIONS_MS
                RepositoryResult(entity.toDomain(), if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
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

    suspend fun refresh() {
        try {
            val divisions = votesApi.searchDivisions(itemsPerPage = 50)
            val entities = divisions.map { dto ->
                DivisionEntity(
                    id = dto.divisionId,
                    title = dto.title,
                    date = dto.date,
                    publicationUpdated = dto.publicationUpdated,
                    number = dto.number,
                    isDeferred = dto.isDeferred,
                    ayeCount = dto.ayeCount,
                    noCount = dto.noCount,
                    house = 1,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
            divisionDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    suspend fun refreshLords() {
        try {
            val divisions = lordsVotesApi.searchDivisions(itemsPerPage = 50)
            val entities = divisions.map { dto ->
                DivisionEntity(
                    id = dto.divisionId,
                    title = dto.title,
                    date = dto.date,
                    publicationUpdated = null,
                    number = dto.number,
                    isDeferred = false,
                    ayeCount = dto.memberContentCount,
                    noCount = dto.memberNotContentCount,
                    house = 2,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
            divisionDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    /**
     * Fetch full division detail with voter lists and persist votes.
     * Commons: Ayes → AYE, Noes → NO
     */
    suspend fun refreshDivisionDetail(divisionId: Int, house: Int) {
        try {
            if (house == 2) {
                val dto = lordsVotesApi.getDivision(divisionId)
                val votes = mutableListOf<DivisionVoteEntity>()
                dto.contents.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.AYE).toEntity())
                }
                dto.notContents.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.NO).toEntity())
                }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            } else {
                val dto = votesApi.getDivision(divisionId)
                val votes = mutableListOf<DivisionVoteEntity>()
                dto.ayes.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.AYE).toEntity())
                }
                dto.noes.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.NO).toEntity())
                }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            }
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    /**
     * Fetch a member's voting record from the API and cache it.
     */
    suspend fun refreshMemberVoting(memberId: Int, house: Int) {
        try {
            if (house == 2) {
                val dtos = lordsVotesApi.getMemberVoting(memberId, itemsPerPage = 100)
                val votes = dtos.mapNotNull { mapper.toDomain(it) }.map { it.toEntity() }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            } else {
                val dtos = votesApi.getMemberVoting(memberId, itemsPerPage = 100)
                val votes = dtos.mapNotNull { mapper.toDomain(it) }.map { it.toEntity() }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            }
        } catch (e: Exception) {
            // Cache is still served
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
                totalMembers = partyVotes.size,
            )
        }.sortedByDescending { it.totalMembers }
    }

    private suspend fun getVotesForDivisionSync(divisionId: Int): List<DivisionVoteEntity> =
        divisionDao.observeVotesForDivision(divisionId).first()

    private fun DivisionEntity.toDomain(): Division =
        Division(
            id = id,
            title = title,
            date = date,
            number = number,
            ayeCount = ayeCount,
            noCount = noCount,
            isDeferred = isDeferred,
            house = house,
        )

    private fun DivisionVoteEntity.toDomain(): DivisionVote =
        DivisionVote(
            divisionId = divisionId,
            memberId = memberId,
            vote = VoteType.valueOf(vote),
            memberName = memberName,
            partyName = partyName,
            partyColour = partyColour,
            constituencyName = constituencyName,
            isTeller = isTeller,
        )

    private fun DivisionVote.toEntity(): DivisionVoteEntity =
        DivisionVoteEntity(
            divisionId = divisionId,
            memberId = memberId,
            vote = vote.name,
            memberName = memberName,
            partyName = partyName ?: "",
            partyColour = partyColour ?: "",
            constituencyName = constituencyName ?: "",
            isTeller = isTeller,
        )
}
