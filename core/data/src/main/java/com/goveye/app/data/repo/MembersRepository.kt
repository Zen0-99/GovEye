package com.goveye.app.data.repo

import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.MpDao
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
class MembersRepository @Inject constructor(
    private val mpDao: MpDao,
    private val membersApi: MembersApi,
    private val mapper: MemberMapper,
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

    suspend fun refresh() = refreshMps()

    suspend fun refreshMps() {
        try {
            val response = membersApi.searchMembers(itemsPerPage = 200, skip = 0)
            val entities = response.items.map { item ->
                val mp = mapper.toDomain(item.value)
                mp.toEntity(System.currentTimeMillis())
            }
            mpDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served via Flow; error is silent
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

    private fun Mp.toEntity(timestamp: Long): MpEntity =
        MpEntity(
            id = id,
            nameListAs = nameListAs,
            nameDisplayAs = nameDisplayAs,
            nameFullTitle = nameFullTitle,
            nameAddressAs = null,
            gender = gender,
            partyId = party?.id ?: 0,
            partyName = party?.name ?: "",
            partyAbbreviation = party?.abbreviation ?: "",
            partyBackgroundColour = party?.backgroundColour ?: "",
            partyForegroundColour = party?.foregroundColour ?: "",
            constituencyId = constituency?.id ?: 0,
            constituencyName = constituency?.name ?: "",
            house = house,
            membershipStartDate = membershipStartDate,
            membershipEndDate = null,
            isActive = isActive,
            thumbnailUrl = thumbnailUrl,
            lastUpdated = timestamp,
        )
}
