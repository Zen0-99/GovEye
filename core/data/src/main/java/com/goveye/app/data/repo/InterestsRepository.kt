package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class InterestsRepository @Inject constructor(private val interestDao: InterestDao) {
    fun observeInterestsForMember(memberId: Int): Flow<RepositoryResult<List<Interest>>> =
        interestDao.observeInterestsForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    fun observeInterestsForMemberInRange(
        memberId: Int,
        fromDate: String?,
        toDate: String?
    ): Flow<RepositoryResult<List<Interest>>> =
        interestDao.observeInterestsForMemberInRange(memberId, fromDate, toDate).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    private fun InterestEntity.toDomain(): Interest = Interest(
        id = id,
        memberId = memberId,
        summary = summary,
        categoryName = categoryName,
        categoryNumber = categoryNumber,
        registrationDate = registrationDate,
        publishedDate = publishedDate,
        fieldsJson = fieldsJson,
        parsedAmountPence = parsedAmountPence,
        currencyCode = currencyCode,
        bucket = bucket
    )
}
