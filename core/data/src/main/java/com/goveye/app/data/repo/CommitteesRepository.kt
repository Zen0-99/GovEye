package com.goveye.app.data.repo

import com.goveye.app.data.api.CommitteesApi
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.mapper.CommitteeMapper
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CommitteesRepository @Inject constructor(
    private val committeeDao: CommitteeDao,
    private val committeesApi: CommitteesApi,
) {
    fun observeCommitteesForMember(memberId: Int): Flow<RepositoryResult<List<Committee>>> =
        committeeDao.observeCommitteesForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = committeeDao.getOldestTimestampForMember(memberId) ?: System.currentTimeMillis()
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.MPS_MS
                RepositoryResult(
                    entities.map { CommitteeMapper.toDomain(it) },
                    if (isStale) SyncStatus.STALE else SyncStatus.FRESH,
                )
            }
        }

    suspend fun refresh(memberId: Int) {
        try {
            val response = committeesApi.getCommitteesForMember(memberId)
            val timestamp = System.currentTimeMillis()
            val entities = response.items.map { CommitteeMapper.toEntity(it, timestamp) }
            val crossRefs = response.items.map { CommitteeMapper.toCrossRef(memberId, it.id, timestamp) }

            committeeDao.deleteCrossRefsForMember(memberId)
            committeeDao.upsertCommittees(entities)
            committeeDao.upsertCrossRefs(crossRefs)
        } catch (e: Exception) {
            // Cache is still served via Flow
        }
    }
}
