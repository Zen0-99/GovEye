package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.EarlyDayMotionEntity
import com.goveye.app.data.local.entity.EdmSponsorEntity

@Dao
interface EdmDao {

    // EDMs where any of these members is the PRIMARY sponsor — the feed
    // rule (D-04). Signature events by followed MPs are NOT surfaced.
    @Query(
        "SELECT * FROM early_day_motions " +
            "WHERE primarySponsorMemberId IN (:memberIds) " +
            "ORDER BY dateTabled DESC LIMIT :limit"
    )
    suspend fun getEdmsPrimarySponsoredBy(memberIds: List<Int>, limit: Int = 100): List<EarlyDayMotionEntity>

    // Profile stats (D-05): "EDMs sponsored" = primary sponsor count.
    @Query("SELECT COUNT(*) FROM early_day_motions WHERE primarySponsorMemberId = :memberId")
    suspend fun countPrimarySponsoredByMember(memberId: Int): Int

    // "EDMs signed" = current signature rows (withdrawn signatures excluded —
    // they're kept in the table as facts but don't count as a live signature).
    @Query("SELECT COUNT(*) FROM edm_sponsors WHERE memberId = :memberId AND isWithdrawn = 0")
    suspend fun countSignedByMember(memberId: Int): Int

    // All signatories for one motion, tabling order (stored per D-03 for
    // future surfacing; the feed doesn't render them this phase).
    @Query("SELECT * FROM edm_sponsors WHERE edmId = :edmId ORDER BY sponsoringOrder")
    suspend fun getSponsorsForEdm(edmId: Int): List<EdmSponsorEntity>

    @Query("SELECT * FROM early_day_motions WHERE edmId = :edmId")
    suspend fun getEdmById(edmId: Int): EarlyDayMotionEntity?

    @Upsert
    suspend fun upsertEdms(edms: List<EarlyDayMotionEntity>)

    @Upsert
    suspend fun upsertSponsors(sponsors: List<EdmSponsorEntity>)
}
