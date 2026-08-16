package com.goveye.app.data.repo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.RemoteKeyDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.RemoteKeyEntity
import com.goveye.app.data.mapper.MemberMapper
import javax.inject.Inject

private const val MPS_PAGE_SIZE = 20
private const val MPS_REMOTE_KEY = "mps"

@OptIn(ExperimentalPagingApi::class)
class MpRemoteMediator @Inject constructor(
    private val mpDao: MpDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val membersApi: MembersApi,
    private val mapper: MemberMapper,
) : RemoteMediator<Int, MpEntity>() {

    override suspend fun initialize(): InitializeAction {
        val remoteKey = remoteKeyDao.getRemoteKey(MPS_REMOTE_KEY)
        return if (remoteKey == null) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MpEntity>,
    ): MediatorResult {
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKey = remoteKeyDao.getRemoteKey(MPS_REMOTE_KEY)
                remoteKey?.nextKey?.minus(1) ?: 0
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = remoteKeyDao.getRemoteKey(MPS_REMOTE_KEY)
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                remoteKey.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val skip = page * MPS_PAGE_SIZE
            val response = membersApi.searchMembers(
                house = 1,
                isCurrentMember = true,
                itemsPerPage = MPS_PAGE_SIZE,
                skip = skip,
            )
            val entities = response.items.map { item ->
                val mp = mapper.toDomain(item.value)
                mapper.toEntity(mp, System.currentTimeMillis())
            }

            if (loadType == LoadType.REFRESH) {
                remoteKeyDao.deleteByLabel(MPS_REMOTE_KEY)
                mpDao.clearAll()
            }

            mpDao.upsertAll(entities)

            val endOfPaginationReached = entities.size < MPS_PAGE_SIZE
            val nextKey = if (endOfPaginationReached) null else page + 1
            remoteKeyDao.upsert(
                RemoteKeyEntity(
                    label = MPS_REMOTE_KEY,
                    nextKey = nextKey,
                    lastUpdated = System.currentTimeMillis(),
                ),
            )

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
