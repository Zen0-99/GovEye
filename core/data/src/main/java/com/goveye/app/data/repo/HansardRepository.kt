package com.goveye.app.data.repo

import com.goveye.app.data.api.HansardApi
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.mapper.HansardMapper
import com.goveye.app.domain.model.HansardContribution
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HansardRepository @Inject constructor(
    private val hansardDao: HansardDao,
    private val hansardApi: HansardApi,
    private val mapper: HansardMapper,
) {
    fun observeContributionsForMember(memberId: Int): Flow<RepositoryResult<List<HansardContribution>>> =
        hansardDao.observeContributionsForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.HANSARD_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    suspend fun refresh(memberId: Int, memberName: String) {
        runCatching {
            val response = hansardApi.search(searchTerm = memberName, itemsPerPage = 20)
            val entities = response.contributions
                .filter { it.memberId == memberId }
                .map { dto ->
                    HansardContributionEntity(
                        itemId = dto.itemId,
                        memberId = dto.memberId,
                        memberName = dto.memberName,
                        contributionText = dto.contributionText,
                        sittingDate = dto.sittingDate,
                        house = dto.house,
                        debateSection = dto.debateSection,
                        debateSectionId = dto.debateSectionId,
                        lastUpdated = System.currentTimeMillis(),
                    )
                }
            if (entities.isNotEmpty()) {
                hansardDao.upsertAll(entities)
            }
        }
    }

    private fun HansardContributionEntity.toDomain(): HansardContribution =
        HansardContribution(
            itemId = itemId,
            memberId = memberId,
            memberName = memberName,
            contributionText = contributionText,
            sittingDate = sittingDate,
            house = house,
            debateSection = debateSection,
        )
}
