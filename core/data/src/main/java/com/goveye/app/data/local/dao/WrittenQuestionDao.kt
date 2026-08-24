package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.WrittenQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrittenQuestionDao {
    @Query("SELECT * FROM written_questions ORDER BY dateTabled DESC LIMIT :limit")
    fun observeQuestions(limit: Int = 50): Flow<List<WrittenQuestionEntity>>

    @Query("SELECT * FROM written_questions WHERE house = :house ORDER BY dateTabled DESC LIMIT :limit")
    fun observeQuestionsByHouse(house: Int, limit: Int = 50): Flow<List<WrittenQuestionEntity>>

    @Query(
        "SELECT * FROM written_questions WHERE questionText LIKE '%' || :query || '%' ORDER BY dateTabled DESC LIMIT :limit"
    )
    fun searchQuestions(query: String, limit: Int = 50): Flow<List<WrittenQuestionEntity>>

    @Query("SELECT * FROM written_questions WHERE id = :id")
    suspend fun getQuestion(id: Int): WrittenQuestionEntity?

    @Query("SELECT * FROM written_questions WHERE memberId = :memberId ORDER BY dateTabled DESC")
    suspend fun getByMemberId(memberId: Int): List<WrittenQuestionEntity>

    @Query(
        "SELECT * FROM written_questions WHERE memberId = :memberId AND dateTabled >= :startDate ORDER BY dateTabled DESC"
    )
    suspend fun getByMemberIdAndDateRange(memberId: Int, startDate: String): List<WrittenQuestionEntity>
}
