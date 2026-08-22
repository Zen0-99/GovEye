package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query(
        "SELECT * FROM divisions WHERE title LIKE '%' || :query || '%' AND house = :house ORDER BY date DESC LIMIT :limit"
    )
    fun searchDivisionsByHouse(query: String, house: Int, limit: Int = 50): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions WHERE id = :id")
    fun observeDivision(id: Int): Flow<DivisionEntity?>

    @Query("SELECT * FROM divisions WHERE id = :id")
    suspend fun getDivision(id: Int): DivisionEntity?

    @Query("SELECT * FROM divisions WHERE id IN (:ids) ORDER BY date DESC")
    suspend fun getDivisionsByIds(ids: List<Int>): List<DivisionEntity>

    @Upsert
    suspend fun upsertAll(divisions: List<DivisionEntity>)

    @Query("SELECT * FROM division_votes WHERE divisionId = :divisionId")
    fun observeVotesForDivision(divisionId: Int): Flow<List<DivisionVoteEntity>>

    @Query("SELECT * FROM division_votes WHERE memberId = :memberId ORDER BY divisionId DESC")
    fun observeVotesForMember(memberId: Int): Flow<List<DivisionVoteEntity>>

    @Query(
        """
        SELECT
            v.divisionId AS divisionId,
            d.title AS divisionTitle,
            d.date AS divisionDate,
            d.house AS house,
            d.ayeCount AS ayeCount,
            d.noCount AS noCount,
            v.vote AS vote,
            v.isTeller AS isTeller
        FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId
        ORDER BY d.date DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPagedMemberVoting(
        memberId: Int,
        limit: Int,
        offset: Int
    ): List<com.goveye.app.data.local.dao.MemberVoteWithDivisionRow>

    @Query(
        """
        SELECT
            v.divisionId AS divisionId,
            d.title AS divisionTitle,
            d.date AS divisionDate,
            d.house AS house,
            d.ayeCount AS ayeCount,
            d.noCount AS noCount,
            v.vote AS vote,
            v.isTeller AS isTeller
        FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId
            AND d.title LIKE '%' || :query || '%'
        ORDER BY d.date DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchPagedMemberVoting(
        memberId: Int,
        query: String,
        limit: Int,
        offset: Int
    ): List<com.goveye.app.data.local.dao.MemberVoteWithDivisionRow>

    @Query("SELECT COUNT(*) FROM division_votes WHERE memberId = :memberId")
    suspend fun countVotesForMember(memberId: Int): Int

    @Query(
        """
        SELECT COUNT(*) FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId AND d.title LIKE '%' || :query || '%'
        """
    )
    suspend fun countSearchVotesForMember(memberId: Int, query: String): Int

    /**
     * Single SQL JOIN that returns ALL of a member's votes with division context.
     * Replaces the N+1 path in getMemberVotingWithDivisions() — one query instead
     * of (1 + N) where N is the number of divisions the MP voted in (~200 for
     * active MPs).
     */
    @Query(
        """
        SELECT
            v.divisionId AS divisionId,
            d.title AS divisionTitle,
            d.date AS divisionDate,
            d.house AS house,
            d.ayeCount AS ayeCount,
            d.noCount AS noCount,
            v.vote AS vote,
            v.isTeller AS isTeller
        FROM division_votes v
        INNER JOIN divisions d ON v.divisionId = d.id
        WHERE v.memberId = :memberId
        ORDER BY d.date DESC
        """
    )
    suspend fun getAllMemberVoting(memberId: Int): List<MemberVoteWithDivisionRow>

    /**
     * SQL GROUP BY that returns party-level vote breakdown for a division.
     * Replaces loading 650 vote entities and grouping in Kotlin — one query
     * returns one row per party with aye/no/total counts.
     *
     * Note: vote values are stored uppercase ('AYE', 'NO', 'NOVOTERECORDED')
     * by the data build scripts. Use UPPER() for case-insensitive matching
     * so both bundled and runtime-inserted votes are counted correctly.
     */
    @Query(
        """
        SELECT
            partyName AS partyName,
            partyColour AS partyColour,
            SUM(CASE WHEN UPPER(vote) = 'AYE' THEN 1 ELSE 0 END) AS ayeCount,
            SUM(CASE WHEN UPPER(vote) = 'NO' THEN 1 ELSE 0 END) AS noCount,
            COUNT(*) AS totalMembers
        FROM division_votes
        WHERE divisionId = :divisionId
        GROUP BY partyName, partyColour
        ORDER BY totalMembers DESC
        """
    )
    suspend fun getPartyBreakdownForDivision(divisionId: Int): List<PartyBreakdownRow>

    /**
     * Batch query: returns the most recent vote for each member in [memberIds].
     * Replaces N+1 calls to getRecentVoteForMember — one query regardless of
     * how many MPs are followed.
     */
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
        WHERE v.memberId IN (:memberIds)
            AND v.divisionId = (
                SELECT MAX(v2.divisionId) FROM division_votes v2
                WHERE v2.memberId = v.memberId
            )
        """
    )
    suspend fun getRecentVotesForMembers(memberIds: List<Int>): List<MemberRecentVote>

    @Query("SELECT * FROM division_votes WHERE memberId = :memberId")
    suspend fun getVotesForMember(memberId: Int): List<DivisionVoteEntity>

    @Query("SELECT * FROM division_votes WHERE divisionId = :divisionId")
    suspend fun getVotesForDivision(divisionId: Int): List<DivisionVoteEntity>

    @Query("SELECT MAX(id) FROM divisions")
    suspend fun getMaxDivisionId(): Int?

    @Query("SELECT * FROM divisions WHERE id > :id ORDER BY id")
    suspend fun getDivisionsAfterId(id: Int): List<DivisionEntity>

    @Query("SELECT * FROM division_votes WHERE divisionId IN (:divisionIds)")
    suspend fun getVotesForDivisions(divisionIds: List<Int>): List<DivisionVoteEntity>

    @Upsert
    suspend fun upsertVotes(votes: List<DivisionVoteEntity>)

    /**
     * Insert votes only if the (divisionId, memberId) row doesn't already exist.
     * Used by member voting refresh — avoids overwriting member info (name,
     * party, constituency) that was populated by the division detail fetch.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVotesIfAbsent(votes: List<DivisionVoteEntity>)

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
        """
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
        """
    )
    fun observeRecentVoteForMember(memberId: Int): Flow<MemberRecentVote?>

    @Query(
        """
        SELECT DISTINCT divisionId FROM division_votes
        WHERE memberId IN (:memberIds)
        ORDER BY divisionId DESC
        """
    )
    suspend fun getDivisionIdsWithMemberVotes(memberIds: List<Int>): List<Int>

    /**
     * Aggregated party vote counts per division — used for rebellion rate
     * computation. Returns one row per division with the party's aye/no counts.
     * This replaces loading all 650 votes per division (130k+ entities) with
     * a single SQL GROUP BY that returns ~200 rows.
     */
    @Query(
        """
        SELECT
            divisionId AS divisionId,
            SUM(CASE WHEN UPPER(vote) = 'AYE' THEN 1 ELSE 0 END) AS partyAyes,
            SUM(CASE WHEN UPPER(vote) = 'NO' THEN 1 ELSE 0 END) AS partyNoes
        FROM division_votes
        WHERE divisionId IN (:divisionIds)
            AND partyName = :partyName
            AND UPPER(vote) NOT IN ('NOVOTERECORDED', 'NO VOTE RECORDED')
        GROUP BY divisionId
        """
    )
    suspend fun getPartyVoteCounts(divisionIds: List<Int>, partyName: String): List<PartyVoteCount>
}

/**
 * Aggregated party vote counts for a single division.
 * Used by rebellion rate calculation — avoids loading all individual vote entities.
 */
data class PartyVoteCount(val divisionId: Int, val partyAyes: Int, val partyNoes: Int)

/**
 * Flat row for member voting with division context.
 * Used by paged queries to avoid N+1 division lookups.
 */
data class MemberVoteWithDivisionRow(
    val divisionId: Int,
    val divisionTitle: String,
    val divisionDate: String,
    val house: Int,
    val ayeCount: Int,
    val noCount: Int,
    val vote: String,
    val isTeller: Boolean
)

/**
 * Aggregated party breakdown row for a single division.
 * Used by SQL GROUP BY query — replaces loading 650 vote entities in Kotlin.
 */
data class PartyBreakdownRow(
    val partyName: String,
    val partyColour: String,
    val ayeCount: Int,
    val noCount: Int,
    val totalMembers: Int
)
