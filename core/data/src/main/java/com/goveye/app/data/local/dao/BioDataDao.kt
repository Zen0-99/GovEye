package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.BioDataEntity

@Dao
interface BioDataDao {
    @Query("SELECT * FROM bio_data WHERE mpId = :mpId")
    suspend fun getByMpId(mpId: Int): BioDataEntity?

    @Upsert
    suspend fun upsertAll(data: List<BioDataEntity>)
}
