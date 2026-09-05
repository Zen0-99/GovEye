package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.ExpenseEntity

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE mpId = :mpId ORDER BY claimDate DESC")
    suspend fun getByMpId(mpId: Int): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE mpId = :mpId AND bucket = :bucket ORDER BY claimDate DESC")
    suspend fun getByMpIdAndBucket(mpId: Int, bucket: String): List<ExpenseEntity>

    @Query(
        "SELECT bucket, SUM(amountPence) as totalPence FROM expenses WHERE mpId = :mpId GROUP BY bucket ORDER BY totalPence DESC"
    )
    suspend fun getBucketTotals(mpId: Int): List<ExpenseBucketTotal>

    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("SELECT COUNT(*) FROM expenses WHERE mpId = :mpId")
    suspend fun countExpensesForMember(mpId: Int): Int

    @Query(
        """
        SELECT AVG(cnt) FROM (
            SELECT COUNT(*) as cnt FROM expenses
            WHERE mpId IN (SELECT id FROM mps WHERE isActive = 1 AND house = :house)
            GROUP BY mpId
        )
        """
    )
    suspend fun getAverageExpenseCount(house: Int): Float?
}
