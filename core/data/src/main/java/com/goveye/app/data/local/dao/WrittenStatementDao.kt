package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.WrittenStatementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrittenStatementDao {
    @Query("SELECT * FROM written_statements ORDER BY dateMade DESC LIMIT :limit")
    fun observeStatements(limit: Int = 50): Flow<List<WrittenStatementEntity>>

    @Query("SELECT * FROM written_statements WHERE house = :house ORDER BY dateMade DESC LIMIT :limit")
    fun observeStatementsByHouse(house: Int, limit: Int = 50): Flow<List<WrittenStatementEntity>>

    @Query("SELECT * FROM written_statements WHERE title LIKE '%' || :query || '%' ORDER BY dateMade DESC LIMIT :limit")
    fun searchStatements(query: String, limit: Int = 50): Flow<List<WrittenStatementEntity>>

    @Query("SELECT * FROM written_statements WHERE id = :id")
    suspend fun getStatement(id: Int): WrittenStatementEntity?
}
