package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpSynopsisEntity

@Dao
interface MpSynopsisDao {
    @Query("SELECT * FROM mp_synopsis WHERE mpId = :mpId")
    suspend fun getByMpId(mpId: Int): MpSynopsisEntity?

    @Upsert
    suspend fun upsertAll(data: List<MpSynopsisEntity>)
}
