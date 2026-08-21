package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.DebateSpeechEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebateSpeechDao {

    @Query("SELECT * FROM debate_speeches WHERE divisionId = :divisionId ORDER BY speechOrder ASC")
    fun observeSpeechesForDivision(divisionId: Int): Flow<List<DebateSpeechEntity>>

    @Query("SELECT * FROM debate_speeches WHERE divisionId = :divisionId ORDER BY speechOrder ASC")
    suspend fun getSpeechesForDivision(divisionId: Int): List<DebateSpeechEntity>

    @Query("SELECT COUNT(*) FROM debate_speeches WHERE divisionId = :divisionId")
    suspend fun countSpeechesForDivision(divisionId: Int): Int

    @Upsert
    suspend fun upsertAll(speeches: List<DebateSpeechEntity>)

    @Query("SELECT COUNT(*) FROM debate_speeches")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM debate_speeches WHERE memberId = :memberId AND isIntervention = 0")
    suspend fun countSpeechesForMember(memberId: Int): Int
}
