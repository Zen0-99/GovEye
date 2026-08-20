package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.InterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests WHERE memberId = :memberId ORDER BY publishedDate DESC")
    fun observeInterestsForMember(memberId: Int): Flow<List<InterestEntity>>

    @Query("""
        SELECT * FROM interests
        WHERE memberId = :memberId
          AND (:fromDate IS NULL OR publishedDate >= :fromDate)
          AND (:toDate IS NULL OR publishedDate <= :toDate)
        ORDER BY publishedDate DESC
    """)
    fun observeInterestsForMemberInRange(
        memberId: Int,
        fromDate: String?,
        toDate: String?,
    ): Flow<List<InterestEntity>>

    @Upsert
    suspend fun upsertAll(interests: List<InterestEntity>)

    @Query("SELECT MIN(lastUpdated) FROM interests WHERE memberId = :memberId")
    suspend fun getOldestTimestampForMember(memberId: Int): Long?
}
