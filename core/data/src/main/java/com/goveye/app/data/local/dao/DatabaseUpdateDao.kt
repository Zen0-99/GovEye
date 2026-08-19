package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillFollowEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.local.entity.RemoteKeyEntity

/**
 * DAO for applying JSON diff patches from the goveye-data repo (D-05).
 *
 * Has @Upsert and @Query DELETE methods for all 15 writable tables.
 * The `mps_fts` table is NOT included — it is auto-synced by FTS4 triggers
 * when rows are inserted/updated/deleted in the `mps` table (Pitfall 2).
 *
 * All methods are called inside a single Room transaction by
 * [com.goveye.app.data.update.DatabaseUpdateManager.applyPatch] to ensure
 * atomicity — if any table fails, the entire patch rolls back.
 */
@Dao
interface DatabaseUpdateDao {
    // ── mps (PK: id) ──────────────────────────────────────────────────
    @Upsert
    suspend fun upsertMps(mps: List<MpEntity>)

    @Query("DELETE FROM mps WHERE id = :id")
    suspend fun deleteMp(id: Int)

    // ── divisions (PK: id) ────────────────────────────────────────────
    @Upsert
    suspend fun upsertDivisions(divisions: List<DivisionEntity>)

    @Query("DELETE FROM divisions WHERE id = :id")
    suspend fun deleteDivision(id: Int)

    // ── division_votes (composite PK: divisionId, memberId) ───────────
    @Upsert
    suspend fun upsertDivisionVotes(votes: List<DivisionVoteEntity>)

    @Query("DELETE FROM division_votes WHERE divisionId = :divisionId AND memberId = :memberId")
    suspend fun deleteDivisionVote(divisionId: Int, memberId: Int)

    // ── committees (PK: id) ───────────────────────────────────────────
    @Upsert
    suspend fun upsertCommittees(committees: List<CommitteeEntity>)

    @Query("DELETE FROM committees WHERE id = :id")
    suspend fun deleteCommittee(id: Int)

    // ── mp_committee_cross_ref (composite PK: memberId, committeeId) ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMpCommitteeCrossRef(refs: List<MpCommitteeCrossRef>)

    @Query("DELETE FROM mp_committee_cross_ref WHERE memberId = :memberId AND committeeId = :committeeId")
    suspend fun deleteMpCommitteeCrossRef(memberId: Int, committeeId: Int)

    // ── bills (PK: id) ────────────────────────────────────────────────
    @Upsert
    suspend fun upsertBills(bills: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: Int)

    // ── bill_stages (composite PK: billId, stageId) ───────────────────
    @Upsert
    suspend fun upsertBillStages(stages: List<BillStageEntity>)

    @Query("DELETE FROM bill_stages WHERE billId = :billId AND stageId = :stageId")
    suspend fun deleteBillStage(billId: Int, stageId: Int)

    // ── bill_follows (PK: billId) ─────────────────────────────────────
    @Upsert
    suspend fun upsertBillFollows(follows: List<BillFollowEntity>)

    @Query("DELETE FROM bill_follows WHERE billId = :billId")
    suspend fun deleteBillFollow(billId: Int)

    // ── hansard_contributions (PK: itemId) ────────────────────────────
    @Upsert
    suspend fun upsertHansardContributions(contributions: List<HansardContributionEntity>)

    @Query("DELETE FROM hansard_contributions WHERE itemId = :itemId")
    suspend fun deleteHansardContribution(itemId: Long)

    // ── interests (PK: id) ────────────────────────────────────────────
    @Upsert
    suspend fun upsertInterests(interests: List<InterestEntity>)

    @Query("DELETE FROM interests WHERE id = :id")
    suspend fun deleteInterest(id: Int)

    // ── follows (PK: memberId) ────────────────────────────────────────
    @Upsert
    suspend fun upsertFollows(follows: List<FollowEntity>)

    @Query("DELETE FROM follows WHERE memberId = :memberId")
    suspend fun deleteFollow(memberId: Int)

    // ── recess_dates (PK: id, autoGenerate) ───────────────────────────
    @Upsert
    suspend fun upsertRecessDates(dates: List<RecessDateEntity>)

    @Query("DELETE FROM recess_dates WHERE id = :id")
    suspend fun deleteRecessDate(id: Long)

    // ── recess_dates_meta (PK: house) ─────────────────────────────────
    @Upsert
    suspend fun upsertRecessDatesMeta(meta: List<RecessDatesMetaEntity>)

    @Query("DELETE FROM recess_dates_meta WHERE house = :house")
    suspend fun deleteRecessDatesMeta(house: Int)

    // ── mp_notification_prefs (PK: memberId) ──────────────────────────
    @Upsert
    suspend fun upsertMpNotificationPrefs(prefs: List<MpNotificationPreferenceEntity>)

    @Query("DELETE FROM mp_notification_prefs WHERE memberId = :memberId")
    suspend fun deleteMpNotificationPref(memberId: Int)

    // ── remote_keys (PK: label) ───────────────────────────────────────
    @Upsert
    suspend fun upsertRemoteKeys(keys: List<RemoteKeyEntity>)

    @Query("DELETE FROM remote_keys WHERE label = :label")
    suspend fun deleteRemoteKey(label: String)
}
