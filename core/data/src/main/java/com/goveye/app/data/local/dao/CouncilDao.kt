package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.CouncilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CouncilDao {
    @Query(
        """SELECT id, name, website, localAuthorityType, region
           FROM councils ORDER BY name"""
    )
    fun observeAllCouncils(): Flow<List<CouncilSummary>>

    @Query(
        """SELECT id, name, website, localAuthorityType, region
           FROM councils ORDER BY name"""
    )
    suspend fun getAllCouncils(): List<CouncilSummary>

    @Query("SELECT * FROM councils WHERE id = :id")
    suspend fun getCouncil(id: Int): CouncilEntity?

    @Query("SELECT * FROM councils WHERE name = :name LIMIT 1")
    suspend fun getCouncilByName(name: String): CouncilEntity?

    @Upsert
    suspend fun upsertAll(councils: List<CouncilEntity>)

    @Query("DELETE FROM councils")
    suspend fun clearAll()
}
