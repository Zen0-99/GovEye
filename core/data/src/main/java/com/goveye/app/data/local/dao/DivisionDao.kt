package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DivisionDao {
    @Query("SELECT * FROM divisions ORDER BY date DESC LIMIT :limit")
    fun observeDivisions(limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE id = :id")
    fun observeDivision(id: Int): Flow<DivisionEntity?>

    @Query("SELECT * FROM divisions WHERE id = :id")
    suspend fun getDivision(id: Int): DivisionEntity?

    @Upsert
    suspend fun upsertAll(divisions: List<DivisionEntity>)

    @Query("SELECT * FROM division_votes WHERE divisionId = :divisionId")
    fun observeVotesForDivision(divisionId: Int): Flow<List<DivisionVoteEntity>>

    @Upsert
    suspend fun upsertVotes(votes: List<DivisionVoteEntity>)

    @Query("SELECT MIN(lastUpdated) FROM divisions")
    suspend fun getOldestTimestamp(): Long?
}
