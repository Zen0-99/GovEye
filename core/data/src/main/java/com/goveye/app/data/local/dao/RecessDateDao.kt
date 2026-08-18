package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity

@Dao
interface RecessDateDao {
    @Query("SELECT * FROM recess_dates WHERE house = :house")
    suspend fun getRecessDatesForHouse(house: Int): List<RecessDateEntity>

    @Query("SELECT * FROM recess_dates_meta WHERE house = :house")
    suspend fun getMeta(house: Int): RecessDatesMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: RecessDatesMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dates: List<RecessDateEntity>)

    @Query("DELETE FROM recess_dates WHERE house = :house")
    suspend fun deleteForHouse(house: Int)
}
