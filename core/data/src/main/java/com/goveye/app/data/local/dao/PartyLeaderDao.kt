package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.PartyLeaderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyLeaderDao {
    @Query("SELECT * FROM party_leaders ORDER BY partyId")
    fun observePartyLeaders(): Flow<List<PartyLeaderEntity>>

    @Query("SELECT * FROM party_leaders WHERE partyId = :partyId")
    suspend fun getLeaderForParty(partyId: Int): PartyLeaderEntity?
}
