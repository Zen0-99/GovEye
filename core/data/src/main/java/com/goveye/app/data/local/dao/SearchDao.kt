package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query(
        """
        SELECT mps.* FROM mps
        JOIN mps_fts ON mps.id = mps_fts.rowid
        WHERE mps_fts MATCH :query
        """,
    )
    fun searchMps(query: String): Flow<List<MpEntity>>
}
