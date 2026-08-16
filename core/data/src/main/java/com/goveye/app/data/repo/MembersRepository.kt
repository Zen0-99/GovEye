package com.goveye.app.data.repo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.RemoteKeyDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalPagingApi::class)
class MembersRepository @Inject constructor(
    private val mpDao: MpDao,
    private val membersApi: MembersApi,
    private val mapper: MemberMapper,
    private val remoteMediator: MpRemoteMediator,
    private val remoteKeyDao: RemoteKeyDao,
) {
    fun observeAllMps(): Flow<RepositoryResult<List<Mp>>> =
        mpDao.observeAllMps().map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.MPS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeMp(id: Int): Flow<RepositoryResult<Mp?>> =
        mpDao.observeMp(id).map { entity ->
            if (entity == null) {
                RepositoryResult(null, SyncStatus.EMPTY)
            } else {
                val isStale = System.currentTimeMillis() - entity.lastUpdated > CacheTtl.MPS_MS
                RepositoryResult(entity.toDomain(), if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observePagedMps(): Flow<PagingData<Mp>> =
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                initialLoadSize = 40,
                enablePlaceholders = false,
            ),
            remoteMediator = remoteMediator,
            pagingSourceFactory = { mpDao.pagingSource() },
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }

    suspend fun refresh() = refreshMps()

    suspend fun refreshMps() {
        try {
            val response = membersApi.searchMembers(itemsPerPage = 200, skip = 0)
            val entities = response.items.map { item ->
                val mp = mapper.toDomain(item.value)
                mapper.toEntity(mp, System.currentTimeMillis())
            }
            mpDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served via Flow; error is silent
        }
    }

    suspend fun refreshMp(id: Int) {
        try {
            val response = membersApi.getMember(id)
            val mp = mapper.toDomain(response.value)
            mpDao.upsertAll(listOf(mapper.toEntity(mp, System.currentTimeMillis())))
        } catch (e: Exception) {
            // Cache is still served via Flow
        }
    }

    private fun MpEntity.toDomain(): Mp =
        Mp(
            id = id,
            nameListAs = nameListAs,
            nameDisplayAs = nameDisplayAs,
            nameFullTitle = nameFullTitle,
            gender = gender,
            party = com.goveye.app.domain.model.Party(partyId, partyName, partyAbbreviation, partyBackgroundColour, partyForegroundColour),
            constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
            house = house,
            membershipStartDate = membershipStartDate,
            isActive = isActive,
            thumbnailUrl = thumbnailUrl,
        )
}
