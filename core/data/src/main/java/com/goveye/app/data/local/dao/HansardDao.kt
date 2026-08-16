package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.HansardContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HansardDao {
    @Query("SELECT * FROM hansard_contributions WHERE memberId = :memberId ORDER BY sittingDate DESC LIMIT :limit")
    fun observeContributionsForMember(memberId: Int, limit: Int = 20): Flow<List<HansardContributionEntity>>

    @Upsert
    suspend fun upsertAll(contributions: List<HansardContributionEntity>)

    @Query("SELECT MIN(lastUpdated) FROM hansard_contributions")
    suspend fun getOldestTimestamp(): Long?
}
