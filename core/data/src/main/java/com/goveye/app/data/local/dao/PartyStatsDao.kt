package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.PartyStatsEntity

@Dao
interface PartyStatsDao {
    @Query("SELECT * FROM party_stats WHERE partyId = :partyId")
    suspend fun getByPartyId(partyId: Int): PartyStatsEntity?

    @Upsert
    suspend fun upsertAll(stats: List<PartyStatsEntity>)
}
