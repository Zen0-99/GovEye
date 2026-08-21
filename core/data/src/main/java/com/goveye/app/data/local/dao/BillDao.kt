package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY lastUpdate DESC LIMIT :limit")
    fun observeBills(limit: Int = 50): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeBill(id: Int): Flow<BillEntity?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBill(id: Int): BillEntity?

    @Query("SELECT * FROM bills WHERE id IN (:ids)")
    suspend fun getBillsByIds(ids: List<Int>): List<BillEntity>

    @Query(
        "SELECT * FROM bills WHERE shortTitle LIKE '%' || :query || '%' OR longTitle LIKE '%' || :query || '%' ORDER BY lastUpdate DESC LIMIT :limit"
    )
    suspend fun searchBills(query: String, limit: Int = 50): List<BillEntity>

    @Upsert
    suspend fun upsertAll(bills: List<BillEntity>)

    @Query("SELECT * FROM bill_stages WHERE billId = :billId ORDER BY sortOrder")
    fun observeBillStages(billId: Int): Flow<List<BillStageEntity>>

    @Upsert
    suspend fun upsertStages(stages: List<BillStageEntity>)

    @Query("SELECT MIN(lastUpdated) FROM bills")
    suspend fun getOldestTimestamp(): Long?
}
