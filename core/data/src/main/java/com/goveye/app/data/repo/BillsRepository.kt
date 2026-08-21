package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.BillStage
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BillsRepository @Inject constructor(private val billDao: BillDao) {
    fun observeBills(limit: Int = 50): Flow<RepositoryResult<List<Bill>>> =
        billDao.observeBills(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    fun observeBill(id: Int): Flow<RepositoryResult<Bill?>> = billDao.observeBill(id).map { entity ->
        if (entity == null) {
            RepositoryResult(null, SyncStatus.EMPTY)
        } else {
            RepositoryResult(entity.toDomain(), SyncStatus.FRESH)
        }
    }

    fun observeBillStages(billId: Int): Flow<List<BillStage>> = billDao.observeBillStages(billId).map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun searchBills(query: String, limit: Int = 50): List<Bill> =
        billDao.searchBills(query, limit).map { it.toDomain() }

    private fun BillEntity.toDomain(): Bill = Bill(
        id = id,
        shortTitle = shortTitle,
        longTitle = longTitle,
        summary = summary,
        currentHouse = currentHouse,
        originatingHouse = originatingHouse,
        isAct = isAct,
        isDefeated = isDefeated,
        billWithdrawn = billWithdrawn,
        currentStage = null
    )

    private fun BillStageEntity.toDomain(): BillStage = BillStage(
        stageId = stageId,
        description = description,
        abbreviation = abbreviation,
        house = house,
        sortOrder = sortOrder,
        sittingDates = sittingDates
    )
}
