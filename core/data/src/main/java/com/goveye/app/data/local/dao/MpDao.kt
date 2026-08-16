package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MpDao {
    @Query("SELECT * FROM mps WHERE isActive = 1 ORDER BY nameListAs")
    fun observeAllMps(): Flow<List<MpEntity>>

    @Query("SELECT * FROM mps WHERE id = :id")
    fun observeMp(id: Int): Flow<MpEntity?>

    @Query("SELECT * FROM mps WHERE id = :id")
    suspend fun getMp(id: Int): MpEntity?

    @Upsert
    suspend fun upsertAll(mps: List<MpEntity>)

    @Query("SELECT MIN(lastUpdated) FROM mps WHERE isActive = 1")
    suspend fun getOldestTimestamp(): Long?
}
