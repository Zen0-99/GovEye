package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpApiIdEntity

@Dao
interface MpApiIdDao {
    @Query("SELECT * FROM mp_api_ids WHERE memberId = :memberId")
    suspend fun getApiIds(memberId: Int): MpApiIdEntity?

    @Query("SELECT memberId FROM mp_api_ids WHERE twfyPersonId = :twfyId")
    suspend fun getMemberIdByTwfyId(twfyId: Int): Int?

    @Query("SELECT memberId FROM mp_api_ids WHERE mnisId = :mnisId")
    suspend fun getMemberIdByMnisId(mnisId: Int): Int?

    @Upsert
    suspend fun upsertApiIds(ids: List<MpApiIdEntity>)

    @Query("SELECT COUNT(*) FROM mp_api_ids")
    suspend fun count(): Int
}
