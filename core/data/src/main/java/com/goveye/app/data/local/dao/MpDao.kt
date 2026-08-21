package com.goveye.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MpDao {
    @Query("SELECT * FROM mps WHERE isActive = 1 ORDER BY nameListAs")
    fun observeAllMps(): Flow<List<MpEntity>>

    @Query("SELECT * FROM mps WHERE isActive = 1 ORDER BY nameListAs")
    fun pagingSource(): PagingSource<Int, MpEntity>

    @Query("SELECT * FROM mps WHERE partyId = :partyId AND isActive = 1 ORDER BY nameListAs")
    fun pagingSourceByParty(partyId: Int): PagingSource<Int, MpEntity>

    @Query(
        """SELECT partyId, partyName, partyAbbreviation, partyBackgroundColour,
           partyForegroundColour, COUNT(*) as seats
           FROM mps WHERE isActive = 1 AND partyId IS NOT NULL AND partyId > 0
           GROUP BY partyId ORDER BY partyName"""
    )
    suspend fun getActiveParties(): List<PartySummary>

    @Query("SELECT * FROM mps WHERE id = :id")
    fun observeMp(id: Int): Flow<MpEntity?>

    @Query("SELECT * FROM mps WHERE id = :id")
    suspend fun getMp(id: Int): MpEntity?

    @Query("SELECT * FROM mps WHERE id IN (:ids)")
    suspend fun getMpsByIds(ids: List<Int>): List<MpEntity>

    @Query(
        "SELECT * FROM mps WHERE partyId = :partyId AND id != :excludeId AND isActive = 1 ORDER BY nameListAs LIMIT :limit"
    )
    suspend fun getMpsByParty(partyId: Int, excludeId: Int, limit: Int = 8): List<MpEntity>

    @Query(
        "SELECT * FROM mps WHERE nameListAs LIKE '%' || :query || '%' OR nameDisplayAs LIKE '%' || :query || '%' OR constituencyName LIKE '%' || :query || '%' ORDER BY nameListAs LIMIT 50"
    )
    suspend fun searchMpsLocal(query: String): List<MpEntity>

    @Upsert
    suspend fun upsertAll(mps: List<MpEntity>)

    @Query("SELECT DISTINCT partyName FROM mps WHERE isActive = 1 ORDER BY partyName")
    fun observeDistinctParties(): Flow<List<String>>

    @Query("SELECT MIN(lastUpdated) FROM mps WHERE isActive = 1")
    suspend fun getOldestTimestamp(): Long?

    @Query("DELETE FROM mps")
    suspend fun clearAll()
}
