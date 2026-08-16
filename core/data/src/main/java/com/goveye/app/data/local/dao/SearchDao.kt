package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

/**
 * Full-text search DAO for MP entities (D-28).
 *
 * Uses SQLite FTS4 MATCH to search across name, constituency, and party.
 * The JOIN against the FTS shadow table ([mps_fts]) retrieves the full
 * row from [mps]. Phase 2 will add BM25 ranking via matchinfo().
 */
@Dao
interface SearchDao {
    @Query(
        """
        SELECT mps.* FROM mps
        JOIN mps_fts ON mps.id = mps_fts.rowid
        WHERE mps_fts MATCH :query
        """
    )
    fun searchMps(query: String): Flow<List<MpEntity>>
}
