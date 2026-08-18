package com.goveye.app.data.repo

import com.goveye.app.data.api.BillsApi
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.mapper.BillMapper
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.BillStage
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillsRepository @Inject constructor(
    private val billDao: BillDao,
    private val billsApi: BillsApi,
    private val mapper: BillMapper,
) {
    fun observeBills(limit: Int = 50): Flow<RepositoryResult<List<Bill>>> =
        billDao.observeBills(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.BILLS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeBill(id: Int): Flow<RepositoryResult<Bill?>> =
        billDao.observeBill(id).map { entity ->
            if (entity == null) {
                RepositoryResult(null, SyncStatus.EMPTY)
            } else {
                val isStale = System.currentTimeMillis() - entity.lastUpdated > CacheTtl.BILLS_MS
                RepositoryResult(entity.toDomain(), if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeBillStages(billId: Int): Flow<List<BillStage>> =
        billDao.observeBillStages(billId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun searchBills(query: String, limit: Int = 50): List<Bill> =
        billDao.searchBills(query, limit).map { it.toDomain() }

    suspend fun refresh() {
        try {
            val response = billsApi.getBills(itemsPerPage = 50)
            val entities = response.items.map { dto ->
                BillEntity(
                    id = dto.billId,
                    shortTitle = dto.shortTitle,
                    longTitle = dto.longTitle,
                    summary = dto.summary,
                    currentHouse = dto.currentHouse,
                    originatingHouse = dto.originatingHouse,
                    lastUpdate = dto.lastUpdate,
                    billWithdrawn = dto.billWithdrawn,
                    isDefeated = dto.isDefeated,
                    isAct = dto.isAct,
                    billTypeId = dto.billTypeId,
                    currentStageDescription = dto.currentStage?.description,
                    currentStageAbbreviation = dto.currentStage?.abbreviation,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
            billDao.upsertAll(entities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    suspend fun refreshBillDetail(billId: Int) {
        try {
            val dto = billsApi.getBill(billId)
            val entity = BillEntity(
                id = dto.billId,
                shortTitle = dto.shortTitle,
                longTitle = dto.longTitle,
                summary = dto.summary,
                currentHouse = dto.currentHouse,
                originatingHouse = dto.originatingHouse,
                lastUpdate = dto.lastUpdate,
                billWithdrawn = dto.billWithdrawn,
                isDefeated = dto.isDefeated,
                isAct = dto.isAct,
                billTypeId = dto.billTypeId,
                currentStageDescription = dto.currentStage?.description,
                currentStageAbbreviation = dto.currentStage?.abbreviation,
                lastUpdated = System.currentTimeMillis(),
            )
            billDao.upsertAll(listOf(entity))
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    suspend fun refreshBillStages(billId: Int) {
        try {
            val response = billsApi.getBillStages(billId)
            val entities = response.items.map { dto ->
                BillStageEntity(
                    billId = billId,
                    stageId = dto.stageId,
                    description = dto.description,
                    abbreviation = dto.abbreviation ?: "",
                    house = dto.house,
                    sortOrder = dto.sortOrder,
                    sessionId = dto.sessionId,
                    sittingDates = dto.stageSittings.mapNotNull { it.date },
                    lastUpdated = System.currentTimeMillis(),
                )
            }
            if (entities.isNotEmpty()) billDao.upsertStages(entities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    private fun BillEntity.toDomain(): Bill =
        Bill(
            id = id,
            shortTitle = shortTitle,
            longTitle = longTitle,
            summary = summary,
            currentHouse = currentHouse,
            originatingHouse = originatingHouse,
            isAct = isAct,
            currentStage = null,
        )

    private fun BillStageEntity.toDomain(): BillStage =
        BillStage(
            stageId = stageId,
            description = description,
            abbreviation = abbreviation,
            house = house,
            sortOrder = sortOrder,
            sittingDates = sittingDates,
        )
}
