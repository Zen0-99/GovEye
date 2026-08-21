package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpLinkEntity

@Dao
interface MpLinkDao {
    @Query("SELECT * FROM mp_links WHERE mpId = :mpId")
    suspend fun getByMpId(mpId: Int): MpLinkEntity?

    @Upsert
    suspend fun upsertAll(links: List<MpLinkEntity>)
}
