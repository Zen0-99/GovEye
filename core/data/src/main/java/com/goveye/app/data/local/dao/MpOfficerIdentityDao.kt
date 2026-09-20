package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpOfficerIdentityEntity

@Dao
interface MpOfficerIdentityDao {
    @Query("SELECT * FROM mp_officer_identity WHERE mpId = :mpId")
    suspend fun getByMpId(mpId: Int): MpOfficerIdentityEntity?

    @Upsert
    suspend fun upsertAll(data: List<MpOfficerIdentityEntity>)
}
