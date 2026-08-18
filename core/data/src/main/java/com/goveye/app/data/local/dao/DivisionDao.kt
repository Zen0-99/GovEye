package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.MemberRecentVote
import kotlinx.coroutines.flow.Flow

@Dao
interface DivisionDao {
    @Query("SELECT * FROM divisions ORDER BY date DESC LIMIT :limit")
    fun observeDivisions(limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE house = :house ORDER BY date DESC LIMIT :limit")
    fun observeDivisionsByHouse(house: Int, limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE house = :house ORDER BY date DESC")
    suspend fun getAllDivisionsByHouse(house: Int): List<DivisionEntity>

    @Query("SELECT * FROM divisions WHERE title LIKE '%' || :query || '%' ORDER BY date DESC LIMIT :limit")
    fun searchDivisions(query: String, limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE title LIKE '%' || :query || '%' AND house = :house ORDER BY date DESC LIMIT :limit")
    fun searchDivisionsByHouse(query: String, house: Int, limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE id = :id")
    fun observeDivision(id: Int): Flow<DivisionEntity?>

    @Query("SELECT * FROM divisions WHERE id = :id")
    suspend fun getDivision(id: Int): DivisionEntity?

    @Upsert
    suspend fun upsertAll(divisions: List<DivisionEntity>)

    @Query("SELECT * FROM division_votes WHERE divisionId = :divisionId")
    fun observeVotesForDivision(divisionId: Int): Flow<List<DivisionVoteEntity>>

    @Query("SELECT * FROM division_votes WHERE memberId = :memberId ORDER BY divisionId DESC")
    fun observeVotesForMember(memberId: Int): Flow<List<DivisionVoteEntity>>

    @Query("SELECT * FROM division_votes WHERE memberId = :memberId")
    suspend fun getVotesForMember(memberId: Int): List<DivisionVoteEntity>

    @Query("SELECT * FROM division_votes WHERE divisionId = :divisionId")
    suspend fun getVotesForDivision(divisionId: Int): List<DivisionVoteEntity>

    @Upsert
    suspend fun upsertVotes(votes: List<DivisionVoteEntity>)

    @Query("SELECT COUNT(*) FROM division_votes WHERE divisionId = :divisionId")
    suspend fun countVotesForDivision(divisionId: Int): Int

    @Query("SELECT MIN(lastUpdated) FROM divisions")
    suspend fun getOldestDivisionTimestamp(): Long?

    @Query("SELECT divisionId FROM division_votes WHERE memberId = :memberId")
    suspend fun getDivisionIdsForMember(memberId: Int): List<Int>

    @Query(
        """
        SELECT
            v.memberId AS memberId,
            v.divisionId AS divisionId,
            d.house AS house,
            d.title AS title,
            d.date AS date,
            v.vote AS vote
        FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId
        ORDER BY d.date DESC
        LIMIT 1
        """,
    )
    suspend fun getRecentVoteForMember(memberId: Int): MemberRecentVote?

    @Query(
        """
        SELECT
            v.memberId AS memberId,
            v.divisionId AS divisionId,
            d.house AS house,
            d.title AS title,
            d.date AS date,
            v.vote AS vote
        FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId
        ORDER BY d.date DESC
        LIMIT 1
        """,
    )
    fun observeRecentVoteForMember(memberId: Int): Flow<MemberRecentVote?>
}
