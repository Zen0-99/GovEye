package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpCareerEventEntity

@Dao
interface MpCareerEventDao {
    @Query("SELECT * FROM mp_career_events WHERE mpId = :mpId ORDER BY startDate DESC")
    suspend fun getByMpId(mpId: Int): List<MpCareerEventEntity>

    @Query("SELECT * FROM mp_career_events WHERE mpId = :mpId AND category = :category ORDER BY startDate DESC")
    suspend fun getByMpIdAndCategory(mpId: Int, category: String): List<MpCareerEventEntity>

    @Query("DELETE FROM mp_career_events WHERE mpId = :mpId AND source = :source")
    suspend fun deleteByMpIdAndSource(mpId: Int, source: String)

    @Upsert
    suspend fun upsertAll(data: List<MpCareerEventEntity>)
}
