package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goveye.app.data.local.entity.BillFollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillFollowDao {
    @Query("SELECT * FROM bill_follows ORDER BY followedAt DESC")
    fun observeAllBillFollows(): Flow<List<BillFollowEntity>>

    @Query("SELECT billId FROM bill_follows ORDER BY followedAt DESC")
    fun observeFollowedBillIds(): Flow<List<Int>>

    @Query("SELECT billId FROM bill_follows")
    suspend fun getFollowedBillIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(follow: BillFollowEntity)

    @Query("DELETE FROM bill_follows WHERE billId = :billId")
    suspend fun delete(billId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bill_follows WHERE billId = :billId)")
    suspend fun isFollowing(billId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bill_follows WHERE billId = :billId)")
    fun observeIsFollowing(billId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM bill_follows")
    suspend fun getFollowCount(): Int
}
