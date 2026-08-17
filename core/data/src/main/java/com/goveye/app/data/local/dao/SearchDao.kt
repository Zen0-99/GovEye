package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    // Existing LIKE search — kept for backward compatibility with existing tests
    @Query(
        """
        SELECT * FROM mps
        WHERE nameListAs LIKE '%' || :query || '%'
           OR nameDisplayAs LIKE '%' || :query || '%'
           OR constituencyName LIKE '%' || :query || '%'
        ORDER BY nameListAs
        LIMIT 50
        """,
    )
    fun searchMps(query: String): Flow<List<MpEntity>>

    // FTS search using MATCH operator against mps_fts table.
    // Joins mps_fts to mps via mps.id = mps_fts.rowid (external content mode).
    // The query parameter should be pre-sanitized (prefix-match each token, strip FTS special chars)
    // by the caller (MembersRepository) before reaching this DAO.
    @Query(
        """
        SELECT mps.* FROM mps
        JOIN mps_fts ON mps.id = mps_fts.rowid
        WHERE mps_fts MATCH :query
        ORDER BY mps.nameListAs
        LIMIT 50
        """,
    )
    fun searchMpsFts(query: String): Flow<List<MpEntity>>
}
