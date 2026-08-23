package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.mapper.CommitteeMapper
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CommitteesRepository @Inject constructor(
    private val committeeDao: CommitteeDao,
    private val memberMapper: MemberMapper
) {
    fun observeCommitteesForMember(memberId: Int): Flow<RepositoryResult<List<Committee>>> =
        committeeDao.observeCommitteesForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(
                    entities.map { CommitteeMapper.toDomain(it) },
                    SyncStatus.FRESH
                )
            }
        }

    suspend fun getCommittee(committeeId: Int): Committee? =
        committeeDao.getCommittee(committeeId)?.let { CommitteeMapper.toDomain(it) }

    fun observeCommittee(committeeId: Int): Flow<Committee?> =
        committeeDao.observeCommittee(committeeId).map { it?.let { CommitteeMapper.toDomain(it) } }

    fun observeCommitteeMembers(committeeId: Int): Flow<List<Mp>> =
        committeeDao.observeCommitteeMembers(committeeId).map { entities ->
            entities.map { memberMapper.toDomainMp(it) }
        }

    suspend fun getCommitteeMembers(committeeId: Int): List<Mp> =
        committeeDao.getCommitteeMembers(committeeId).map { memberMapper.toDomainMp(it) }

    suspend fun getMemberCount(committeeId: Int): Int = committeeDao.getMemberCount(committeeId)
}
