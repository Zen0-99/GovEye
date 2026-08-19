package com.goveye.app.data.repo

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
) {
    fun observeCommitteesForMember(memberId: Int): Flow<RepositoryResult<List<Committee>>> =
        committeeDao.observeCommitteesForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(
                    entities.map { CommitteeMapper.toDomain(it) },
                    SyncStatus.FRESH,
                )
            }
        }
}
