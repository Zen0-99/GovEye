package com.goveye.app.data.repo

import com.goveye.app.data.api.LordsVotesApi
import com.goveye.app.data.api.VotesApi
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.mapper.DivisionMapper
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
    private val votesApi: VotesApi,
    private val lordsVotesApi: LordsVotesApi,
    private val mapper: DivisionMapper,
) {
    fun observeDivisions(limit: Int = 200): Flow<RepositoryResult<List<Division>>> =
        divisionDao.observeDivisions(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.DIVISIONS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeDivisionsByHouse(house: Int, limit: Int = 200): Flow<RepositoryResult<List<Division>>> =
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
            val pageSize = 100
            var skip = 0
            val allEntities = mutableListOf<DivisionEntity>()

            while (true) {
                val divisions = votesApi.searchDivisions(itemsPerPage = pageSize, skip = skip)
                if (divisions.isEmpty()) break
                allEntities.addAll(divisions.map { dto ->
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
                })
                if (divisions.size < pageSize) break
                skip += pageSize
            }

            if (allEntities.isNotEmpty()) divisionDao.upsertAll(allEntities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    suspend fun refreshLords() {
        try {
            val pageSize = 100
            var skip = 0
            val allEntities = mutableListOf<DivisionEntity>()

            while (true) {
                val divisions = lordsVotesApi.searchDivisions(itemsPerPage = pageSize, skip = skip)
                if (divisions.isEmpty()) break
                allEntities.addAll(divisions.map { dto ->
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
                })
                if (divisions.size < pageSize) break
                skip += pageSize
            }

            if (allEntities.isNotEmpty()) divisionDao.upsertAll(allEntities)
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
     * Fetch a member's full voting record from the API and cache it.
     * Paginates through all available votes (API returns newest first).
     * Also stores division metadata so charts can join votes to dates.
     */
    suspend fun refreshMemberVoting(memberId: Int, house: Int) {
        try {
            val pageSize = 100
            var skip = 0
            val allVotes = mutableListOf<DivisionVoteEntity>()
            val allDivisions = mutableListOf<DivisionEntity>()

            if (house == 2) {
                // Lords API
                while (true) {
                    val dtos = lordsVotesApi.getMemberVoting(memberId, itemsPerPage = pageSize, skip = skip)
                    if (dtos.isEmpty()) break
                    dtos.forEach { dto ->
                        val vote = mapper.toDomain(dto) ?: return@forEach
                        allVotes.add(vote.toEntity())
                        // Store division metadata from the embedded PublishedDivision
                        val pub = dto.publishedDivision ?: return@forEach
                        allDivisions.add(
                            DivisionEntity(
                                id = pub.divisionId,
                                title = pub.title,
                                date = pub.date,
                                publicationUpdated = null,
                                number = null,
                                isDeferred = false,
                                ayeCount = pub.memberContentCount,
                                noCount = pub.memberNotContentCount,
                                house = 2,
                                lastUpdated = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (dtos.size < pageSize) break
                    skip += pageSize
                }
            } else {
                // Commons API
                while (true) {
                    val dtos = votesApi.getMemberVoting(memberId, itemsPerPage = pageSize, skip = skip)
                    if (dtos.isEmpty()) break
                    dtos.forEach { dto ->
                        val vote = mapper.toDomain(dto) ?: return@forEach
                        allVotes.add(vote.toEntity())
                        // Store division metadata from the embedded PublishedDivision
                        val pub = dto.publishedDivision ?: return@forEach
                        allDivisions.add(
                            DivisionEntity(
                                id = pub.divisionId,
                                title = pub.title,
                                date = pub.date,
                                publicationUpdated = null,
                                number = null,
                                isDeferred = false,
                                ayeCount = pub.ayeCount,
                                noCount = pub.noCount,
                                house = 1,
                                lastUpdated = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (dtos.size < pageSize) break
                    skip += pageSize
                }
            }

            if (allDivisions.isNotEmpty()) divisionDao.upsertAll(allDivisions)
            if (allVotes.isNotEmpty()) divisionDao.upsertVotes(allVotes)
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
            MemberVoteWithDivision(
                divisionId = division.id,
                divisionTitle = division.title,
                divisionDate = division.date,
                house = division.house,
                ayeCount = division.ayeCount,
                noCount = division.noCount,
                vote = VoteType.valueOf(voteEntity.vote),
                isTeller = voteEntity.isTeller,
            )
        }.sortedByDescending { it.divisionDate }
    }

    /**
     * Get all votes for a set of divisions (used for rebellion computation).
     * Returns a map of divisionId → list of all votes in that division.
     */
    suspend fun getAllVotesForDivisions(divisionIds: List<Int>): Map<Int, List<DivisionVote>> {
        return divisionIds.associateWith { id ->
            divisionDao.getVotesForDivision(id).map { it.toDomain() }
        }
    }

    /**
     * Get all votes for a single division (suspend, one-shot).
     */
    suspend fun getVotesForDivision(divisionId: Int): List<DivisionVote> =
        divisionDao.getVotesForDivision(divisionId).map { it.toDomain() }

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
