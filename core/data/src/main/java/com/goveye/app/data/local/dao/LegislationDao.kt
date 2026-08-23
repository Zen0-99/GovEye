package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.LegislationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LegislationDao {
    @Query("SELECT * FROM legislation ORDER BY date DESC LIMIT :limit")
    fun observeLegislation(limit: Int = 50): Flow<List<LegislationEntity>>

    @Query("SELECT * FROM legislation WHERE type = :type ORDER BY date DESC LIMIT :limit")
    fun observeLegislationByType(type: String, limit: Int = 50): Flow<List<LegislationEntity>>

    @Query("SELECT * FROM legislation WHERE title LIKE '%' || :query || '%' ORDER BY date DESC LIMIT :limit")
    fun searchLegislation(query: String, limit: Int = 50): Flow<List<LegislationEntity>>

    @Query("SELECT * FROM legislation WHERE id = :id")
    suspend fun getLegislation(id: Int): LegislationEntity?
}
