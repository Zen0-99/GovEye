package com.goveye.app.data.repo

import com.goveye.app.data.api.VotesApi
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.mapper.DivisionMapper
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VotesRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val votesApi: VotesApi,
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

    fun observeDivision(id: Int): Flow<RepositoryResult<Division?>> =
        divisionDao.observeDivision(id).map { entity ->
            if (entity == null) {
                RepositoryResult(null, SyncStatus.EMPTY)
            } else {
                val isStale = System.currentTimeMillis() - entity.lastUpdated > CacheTtl.DIVISIONS_MS
                RepositoryResult(entity.toDomain(), if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeMemberVoting(memberId: Int): Flow<RepositoryResult<List<DivisionVote>>> =
        divisionDao.observeVotesForDivision(0).map { entities ->
            RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
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
            vote = com.goveye.app.domain.model.VoteType.valueOf(vote),
            memberName = memberName,
            partyName = partyName,
            partyColour = partyColour,
            constituencyName = constituencyName,
            isTeller = isTeller,
        )
}
