package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.DebateSpeechEntity
import kotlinx.coroutines.flow.Flow

/**
 * A speech joined with its parent division's metadata, used by the feed
 * and activity tab to render speech cards with division context (title,
 * date, tags).
 */
data class SpeechWithDivision(
    val speechText: String,
    val divisionId: Int,
    val divisionTitle: String,
    val divisionDate: String,
    val memberId: Int
)

@Dao
interface DebateSpeechDao {

    @Query("SELECT * FROM debate_speeches WHERE divisionId = :divisionId ORDER BY speechOrder ASC")
    fun observeSpeechesForDivision(divisionId: Int): Flow<List<DebateSpeechEntity>>

    @Query("SELECT * FROM debate_speeches WHERE divisionId = :divisionId ORDER BY speechOrder ASC")
    suspend fun getSpeechesForDivision(divisionId: Int): List<DebateSpeechEntity>

    @Query("SELECT COUNT(*) FROM debate_speeches WHERE divisionId = :divisionId AND speakerName != ''")
    suspend fun countSpeechesForDivision(divisionId: Int): Int

    @Upsert
    suspend fun upsertAll(speeches: List<DebateSpeechEntity>)

    @Query("SELECT COUNT(*) FROM debate_speeches")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM debate_speeches WHERE memberId = :memberId AND isIntervention = 0")
    suspend fun countSpeechesForMember(memberId: Int): Int

    /**
     * Fetches recent speeches by a single member, joined with the parent
     * division's title and date. Only non-empty speech text is returned,
     * ordered by division date descending.
     */
    @Query(
        """
        SELECT ds.speechText AS speechText, d.id AS divisionId, d.title AS divisionTitle,
               d.date AS divisionDate, ds.memberId AS memberId
        FROM debate_speeches ds
        JOIN divisions d ON ds.divisionId = d.id
        WHERE ds.memberId = :memberId
          AND ds.speechText IS NOT NULL
          AND length(ds.speechText) > 0
        ORDER BY d.date DESC
        LIMIT :limit
        """
    )
    suspend fun getSpeechesByMember(memberId: Int, limit: Int = 50): List<SpeechWithDivision>

    /**
     * Fetches recent speeches for a set of members (the followed MPs),
     * joined with the parent division's title and date. Used by the feed
     * to render [FeedItem.SpeechItem] cards for followed MPs.
     */
    @Query(
        """
        SELECT ds.speechText AS speechText, d.id AS divisionId, d.title AS divisionTitle,
               d.date AS divisionDate, ds.memberId AS memberId
        FROM debate_speeches ds
        JOIN divisions d ON ds.divisionId = d.id
        WHERE ds.memberId IN (:memberIds)
          AND ds.speechText IS NOT NULL
          AND length(ds.speechText) > 0
        ORDER BY d.date DESC
        LIMIT :limit
        """
    )
    suspend fun getSpeechesByMemberIds(memberIds: List<Int>, limit: Int = 100): List<SpeechWithDivision>
}
