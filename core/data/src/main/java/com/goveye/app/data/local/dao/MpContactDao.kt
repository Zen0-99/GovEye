package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpContactEntity

@Dao
interface MpContactDao {
    @Query("SELECT * FROM mp_contacts WHERE mpId = :mpId")
    suspend fun getByMpId(mpId: Int): List<MpContactEntity>

    @Upsert
    suspend fun upsertAll(data: List<MpContactEntity>)
}
