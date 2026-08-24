package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DebateSpeechEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.local.entity.WrittenQuestionEntity

/**
 * DAO for applying JSON diff patches from the goveye-data repo (D-05, D-10a).
 *
 * Has @Upsert and @Query DELETE methods for the 12 bundled tables in
 * [com.goveye.app.data.local.BundledDatabase]. The `mps_fts` table is NOT
 * included — it is auto-synced by FTS4 triggers when rows are
 * inserted/updated/deleted in the `mps` table (Pitfall 2).
 *
 * User-data tables (follows, bill_follows, mp_notification_prefs) are NOT
 * here — they live in [com.goveye.app.data.local.LocalDatabase] and are
 * never patched (D-10a).
 *
 * All methods are called inside a single Room transaction by
 * [com.goveye.app.data.update.DatabaseUpdateManager.applyPatches] to ensure
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

    // ── debate_speeches (composite PK: debateGid, speechGid) ──────────
    @Upsert
    suspend fun upsertDebateSpeeches(speeches: List<DebateSpeechEntity>)

    @Query("DELETE FROM debate_speeches WHERE debateGid = :debateGid AND speechGid = :speechGid")
    suspend fun deleteDebateSpeech(debateGid: String, speechGid: String)

    // ── bio_data (PK: mpId) ───────────────────────────────────────────
    @Upsert
    suspend fun upsertBioData(data: List<BioDataEntity>)

    @Query("DELETE FROM bio_data WHERE mpId = :mpId")
    suspend fun deleteBioData(mpId: Int)

    // ── expenses (PK: id, autoGenerate) ───────────────────────────────
    @Upsert
    suspend fun upsertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Int)

    // ── mp_links (PK: mpId) ───────────────────────────────────────────
    @Upsert
    suspend fun upsertMpLinks(links: List<MpLinkEntity>)

    @Query("DELETE FROM mp_links WHERE mpId = :mpId")
    suspend fun deleteMpLink(mpId: Int)

    // ── party_manifestos (PK: partyId) — FTS auto-synced by triggers ──
    @Upsert
    suspend fun upsertManifestos(manifestos: List<PartyManifestoEntity>)

    @Query("DELETE FROM party_manifestos WHERE partyId = :partyId")
    suspend fun deleteManifesto(partyId: Int)

    // ── party_stats (PK: partyId) ─────────────────────────────────────
    @Upsert
    suspend fun upsertPartyStats(stats: List<PartyStatsEntity>)

    @Query("DELETE FROM party_stats WHERE partyId = :partyId")
    suspend fun deletePartyStats(partyId: Int)

    // ── written_questions (PK: id) ────────────────────────────────────
    @Upsert
    suspend fun upsertWrittenQuestions(questions: List<WrittenQuestionEntity>)

    @Query("DELETE FROM written_questions WHERE id = :id")
    suspend fun deleteWrittenQuestion(id: Int)
}
