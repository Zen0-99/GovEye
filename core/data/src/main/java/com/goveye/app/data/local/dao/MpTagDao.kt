package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for MP tag table (mp_tags). Follows the [TagDao] pattern.
 *
 * hitCount is a recency-weighted score (D-08), not a raw count.
 */
@Dao
interface MpTagDao {
    @Query("SELECT tag FROM mp_tags WHERE memberId = :memberId ORDER BY hitCount DESC")
    fun observeTagsForMp(memberId: Int): Flow<List<String>>

    @Query("SELECT memberId FROM mp_tags WHERE tag = :tag ORDER BY hitCount DESC")
    fun observeMpsForTag(tag: String): Flow<List<Int>>

    @Query("SELECT memberId FROM mp_tags WHERE tag = :tag ORDER BY hitCount DESC")
    suspend fun getMpsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM mp_tags ORDER BY tag")
    fun observeAllMpTags(): Flow<List<String>>
}
