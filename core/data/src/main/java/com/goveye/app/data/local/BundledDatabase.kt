package com.goveye.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.ExpenseDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.dao.PartyStatsDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DebateSpeechEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.HistoricalMemberEntity
import com.goveye.app.data.local.entity.HistoricalMemberFts4Entity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpFtsEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.data.local.entity.PartyManifestoFts4Entity
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity

/**
 * Room database for the 12 read-only bundled tables (D-10a).
 *
 * This database holds all pre-built parliamentary data (MPs, votes, bills,
 * committees, recess dates, hansard, interests) that is downloaded from the
 * goveye-data repo on first launch and updated via 7 patch streams
 * (mps-latest, commons-votes-latest, lords-votes-latest, bills-latest,
 * committees-latest, recess-latest, interests-latest).
 *
 * User-data tables (follows, bill_follows, mp_notification_prefs) live in
 * [LocalDatabase] — they are never touched by patches or seed DB swaps.
 *
 * DB name is "goveye.db" — the first-launch download places the seed DB at
 * this path before Room opens (D-04).
 */
@Database(
    entities = [
        MpEntity::class,
        MpFtsEntity::class,
        CommitteeEntity::class,
        MpCommitteeCrossRef::class,
        DivisionEntity::class,
        DivisionVoteEntity::class,
        BillEntity::class,
        BillStageEntity::class,
        RecessDateEntity::class,
        RecessDatesMetaEntity::class,
        HansardContributionEntity::class,
        InterestEntity::class,
        DebateSpeechEntity::class,
        BioDataEntity::class,
        ExpenseEntity::class,
        MpLinkEntity::class,
        PartyManifestoEntity::class,
        PartyManifestoFts4Entity::class,
        PartyStatsEntity::class,
        HistoricalMemberEntity::class,
        HistoricalMemberFts4Entity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BundledDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao
    abstract fun mpDao(): MpDao
    abstract fun committeeDao(): CommitteeDao
    abstract fun divisionDao(): DivisionDao
    abstract fun billDao(): BillDao
    abstract fun hansardDao(): HansardDao
    abstract fun interestDao(): InterestDao
    abstract fun recessDateDao(): RecessDateDao
    abstract fun debateSpeechDao(): DebateSpeechDao
    abstract fun bioDataDao(): BioDataDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun mpLinkDao(): MpLinkDao
    abstract fun manifestoDao(): ManifestoDao
    abstract fun partyStatsDao(): PartyStatsDao
    abstract fun historicalMemberDao(): HistoricalMemberDao
    abstract fun databaseUpdateDao(): DatabaseUpdateDao

    companion object {
        const val DATABASE_NAME = "goveye.db"
    }
}
