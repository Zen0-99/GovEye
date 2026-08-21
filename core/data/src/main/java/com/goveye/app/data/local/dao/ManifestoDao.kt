package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.PartyManifestoEntity

@Dao
interface ManifestoDao {
    @Query("SELECT * FROM party_manifestos WHERE partyId = :partyId")
    suspend fun getManifesto(partyId: Int): PartyManifestoEntity?

    @Query("SELECT manifestoText FROM party_manifestos WHERE partyId = :partyId")
    suspend fun getManifestoText(partyId: Int): String?

    @Query(
        """
        SELECT pm.partyId as partyId,
               snippet(party_manifestos_fts4, 0, '<b>', '</b>', '...', 32) as snippetText,
               bm25(party_manifestos_fts4) as searchRank
        FROM party_manifestos_fts4
        JOIN party_manifestos pm ON pm.rowid = party_manifestos_fts4.rowid
        WHERE party_manifestos_fts4 MATCH :query AND pm.partyId = :partyId
        ORDER BY searchRank LIMIT 50
        """
    )
    suspend fun searchManifestoFts4(partyId: Int, query: String): List<ManifestoSearchResult>

    @Upsert
    suspend fun upsertAll(manifestos: List<PartyManifestoEntity>)
}
