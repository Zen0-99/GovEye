package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
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
}
