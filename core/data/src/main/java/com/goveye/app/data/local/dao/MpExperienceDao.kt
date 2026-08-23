package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpExperienceEntity

@Dao
interface MpExperienceDao {
    @Query("SELECT * FROM mp_experience WHERE mpId = :mpId ORDER BY startYear DESC")
    suspend fun getByMpId(mpId: Int): List<MpExperienceEntity>

    @Upsert
    suspend fun upsertAll(data: List<MpExperienceEntity>)
}
