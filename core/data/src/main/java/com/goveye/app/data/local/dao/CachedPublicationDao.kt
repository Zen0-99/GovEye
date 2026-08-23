package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goveye.app.data.local.entity.CachedPublicationEntity

/**
 * DAO for the cached_publications table in [com.goveye.app.data.local.LocalDatabase].
 *
 * Used for on-demand historical publication fetch + cache (D-02).
 * Publications older than 90 days are fetched from the GOV.UK Content API
 * per-user and cached locally.
 */
@Dao
interface CachedPublicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedPublication(entity: CachedPublicationEntity)

    @Query("SELECT * FROM cached_publications WHERE url = :url")
    suspend fun getCachedPublication(url: String): CachedPublicationEntity?

    @Query("DELETE FROM cached_publications WHERE fetchedAt < :cutoffTimestamp")
    suspend fun deleteCachedPublicationsOlderThan(cutoffTimestamp: Long)
}
