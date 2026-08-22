package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.MpStatsEntity
import com.goveye.app.data.local.entity.PeerAveragesEntity

/**
 * DAO for reading precomputed MP statistics and peer averages.
 *
 * These tables are populated at build time by `build_precompute.py`.
 * When empty (old DB without precompute step), StatsRepository falls
 * back to runtime computation.
 */
@Dao
interface MpStatsDao {
    @Query("SELECT * FROM mp_stats WHERE memberId = :memberId")
    suspend fun getStats(memberId: Int): MpStatsEntity?

    @Query("SELECT * FROM mp_stats WHERE house = :house")
    suspend fun getStatsByHouse(house: Int): List<MpStatsEntity>

    @Query("SELECT * FROM peer_averages WHERE house = :house")
    suspend fun getPeerAverages(house: Int): PeerAveragesEntity?

    @Query("SELECT COUNT(*) FROM mp_stats")
    suspend fun countStats(): Int
}
