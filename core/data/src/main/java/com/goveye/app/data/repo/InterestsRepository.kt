package com.goveye.app.data.repo

import com.goveye.app.data.api.InterestsApi
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.mapper.InterestMapper
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterestsRepository @Inject constructor(
    private val interestDao: InterestDao,
    private val interestsApi: InterestsApi,
    private val mapper: InterestMapper,
) {
    fun observeInterestsForMember(memberId: Int): Flow<RepositoryResult<List<Interest>>> =
        interestDao.observeInterestsForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.INTERESTS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    suspend fun refresh(memberId: Int) {
        try {
            val response = interestsApi.getInterests(memberId = memberId)
            val entities = response.items.map { dto ->
                val domain = mapper.toDomain(dto, memberId)
                InterestEntity(
                    id = domain.id,
                    memberId = domain.memberId,
                    summary = domain.summary,
                    categoryId = dto.category.id,
                    categoryNumber = domain.categoryNumber,
                    categoryName = domain.categoryName,
                    registrationDate = domain.registrationDate,
                    publishedDate = domain.publishedDate,
                    rectified = dto.rectified,
                    fieldsJson = domain.fieldsJson,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
            interestDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    private fun InterestEntity.toDomain(): Interest =
        Interest(
            id = id,
            memberId = memberId,
            summary = summary,
            categoryName = categoryName,
            categoryNumber = categoryNumber,
            registrationDate = registrationDate,
            publishedDate = publishedDate,
            fieldsJson = fieldsJson,
        )
}
