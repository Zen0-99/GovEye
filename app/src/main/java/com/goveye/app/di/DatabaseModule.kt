package com.goveye.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.LocalDatabase
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.ExpenseDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.MpContactDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpExperienceDao
import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.MpStatsDao
import com.goveye.app.data.local.dao.MpSynopsisDao
import com.goveye.app.data.local.dao.PartyStatsDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.mapper.HansardMapper
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.data.repo.BioDataRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.HansardRepository
import com.goveye.app.data.repo.HistoricalMemberRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.ManifestoRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.MpLinksRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.PartyStatsRepository
import com.goveye.app.data.repo.StatsRepository
import com.goveye.app.data.repo.VotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Migration 11 → 12: No schema changes (identity hash update only).
    // Room still requires a migration to bump the version number.
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op — schema unchanged, only identity hash differs
        }
    }

    // Migration 12 → 13: Add mp_synopsis, mp_contacts, mp_experience tables.
    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mp_synopsis` (`mpId` INTEGER NOT NULL, `synopsisText` TEXT, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`mpId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mp_contacts` (`mpId` INTEGER NOT NULL, `typeId` INTEGER NOT NULL, `type` TEXT, `isPreferred` INTEGER, `isWebAddress` INTEGER, `line1` TEXT, `line2` TEXT, `line3` TEXT, `line4` TEXT, `line5` TEXT, `postcode` TEXT, `phone` TEXT, `email` TEXT, `website` TEXT, `openingHours` TEXT, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`mpId`, `typeId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mp_experience` (`id` INTEGER NOT NULL, `mpId` INTEGER NOT NULL, `type` TEXT, `typeId` INTEGER, `title` TEXT, `organisation` TEXT, `startMonth` INTEGER, `startYear` INTEGER, `endMonth` INTEGER, `endYear` INTEGER, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    // Migration 13 → 14: Add purpose/contact/website columns to committees table.
    // Idempotent — the seed DB may already include these columns if it was
    // built with a newer bundled_schema.json.
    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val existingColumns = db.query("PRAGMA table_info(committees)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            val newColumns = listOf(
                "purpose",
                "contactEmail",
                "contactPhone",
                "contactAddress",
                "websiteUrl"
            )
            for (col in newColumns) {
                if (col !in existingColumns) {
                    db.execSQL("ALTER TABLE committees ADD COLUMN $col TEXT")
                }
            }
        }
    }

    // Migration 14 → 15: Add division_tags and bill_tags tables.
    // Idempotent — the seed DB may already include these tables.
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `division_tags` (`divisionId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`divisionId`, `tag`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `bill_tags` (`billId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`billId`, `tag`))"
            )
        }
    }

    // Migration 15 → 16: Add councils table for local authority data.
    // Idempotent — the seed DB may already include this table.
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `councils` (`id` INTEGER NOT NULL, `reference` TEXT NOT NULL, `name` TEXT NOT NULL, `website` TEXT, `region` TEXT, `localAuthorityType` TEXT, `statisticalGeography` TEXT, `wikidata` TEXT, `twitter` TEXT, `contactEmail` TEXT, `contactPhone` TEXT, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    // Migration 16 → 17: Add tag_metadata table.
    // Idempotent — the seed DB may already include this table.
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `tag_metadata` (`tag` TEXT NOT NULL, `description` TEXT NOT NULL, `divisionCount` INTEGER NOT NULL, `billCount` INTEGER NOT NULL, PRIMARY KEY(`tag`))"
            )
        }
    }

    @Provides
    @Singleton
    fun provideBundledDatabase(@ApplicationContext context: Context): BundledDatabase {
        // Ensure the databases directory exists so that the first-launch download
        // can place the DB file at Room's expected path before Room opens (D-04).
        context.getDatabasePath(BundledDatabase.DATABASE_NAME).parentFile?.mkdirs()
        return Room
            .databaseBuilder(
                context,
                BundledDatabase::class.java,
                BundledDatabase.DATABASE_NAME
            )
            .addMigrations(
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase {
        // LocalDatabase is created fresh on first launch by Room (no pre-placed file).
        // User data persists across all DB updates and seed DB swaps (D-10a).
        return Room
            .databaseBuilder(
                context,
                LocalDatabase::class.java,
                LocalDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // ── Bundled table DAOs (from BundledDatabase) ──────────────────────

    @Provides
    fun provideSearchDao(database: BundledDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideMpDao(database: BundledDatabase): MpDao = database.mpDao()

    @Provides
    fun provideCommitteeDao(database: BundledDatabase): CommitteeDao = database.committeeDao()

    @Provides
    fun provideDivisionDao(database: BundledDatabase): DivisionDao = database.divisionDao()

    @Provides
    fun provideBillDao(database: BundledDatabase): BillDao = database.billDao()

    @Provides
    fun provideHansardDao(database: BundledDatabase): HansardDao = database.hansardDao()

    @Provides
    fun provideInterestDao(database: BundledDatabase): InterestDao = database.interestDao()

    @Provides
    fun provideRecessDateDao(database: BundledDatabase): RecessDateDao = database.recessDateDao()

    @Provides
    fun provideDebateSpeechDao(database: BundledDatabase): DebateSpeechDao = database.debateSpeechDao()

    @Provides
    fun provideBioDataDao(database: BundledDatabase): BioDataDao = database.bioDataDao()

    @Provides
    fun provideExpenseDao(database: BundledDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideMpLinkDao(database: BundledDatabase): MpLinkDao = database.mpLinkDao()

    @Provides
    fun provideManifestoDao(database: BundledDatabase): ManifestoDao = database.manifestoDao()

    @Provides
    fun providePartyStatsDao(database: BundledDatabase): PartyStatsDao = database.partyStatsDao()

    @Provides
    fun provideHistoricalMemberDao(database: BundledDatabase): HistoricalMemberDao = database.historicalMemberDao()

    @Provides
    fun provideMpStatsDao(database: BundledDatabase): MpStatsDao = database.mpStatsDao()

    @Provides
    fun provideMpSynopsisDao(database: BundledDatabase): MpSynopsisDao = database.mpSynopsisDao()

    @Provides
    fun provideMpContactDao(database: BundledDatabase): MpContactDao = database.mpContactDao()

    @Provides
    fun provideMpExperienceDao(database: BundledDatabase): MpExperienceDao = database.mpExperienceDao()

    @Provides
    fun provideTagDao(database: BundledDatabase): com.goveye.app.data.local.dao.TagDao = database.tagDao()

    @Provides
    fun provideDatabaseUpdateDao(database: BundledDatabase): DatabaseUpdateDao = database.databaseUpdateDao()

    // ── User-data DAOs (from LocalDatabase) ────────────────────────────

    @Provides
    fun provideFollowDao(database: LocalDatabase): FollowDao = database.followDao()

    @Provides
    fun provideBillFollowDao(database: LocalDatabase): BillFollowDao = database.billFollowDao()

    @Provides
    fun provideMpNotificationPreferenceDao(database: LocalDatabase): MpNotificationPreferenceDao =
        database.mpNotificationPreferenceDao()

    // ── Mappers ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMemberMapper(): MemberMapper = MemberMapper

    @Provides
    @Singleton
    fun provideHansardMapper(): HansardMapper = HansardMapper

    // ── Repositories ───────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMembersRepository(
        mpDao: MpDao,
        searchDao: SearchDao,
        membersApi: com.goveye.app.data.api.MembersApi,
        memberMapper: MemberMapper,
        historicalMemberDao: com.goveye.app.data.local.dao.HistoricalMemberDao,
        mpSynopsisDao: com.goveye.app.data.local.dao.MpSynopsisDao,
        mpContactDao: com.goveye.app.data.local.dao.MpContactDao,
        mpExperienceDao: com.goveye.app.data.local.dao.MpExperienceDao
    ): MembersRepository = MembersRepository(mpDao, searchDao, membersApi, memberMapper, historicalMemberDao, mpSynopsisDao, mpContactDao, mpExperienceDao)

    @Provides
    @Singleton
    fun provideCommitteesRepository(
        committeeDao: CommitteeDao,
        memberMapper: MemberMapper
    ): CommitteesRepository =
        CommitteesRepository(committeeDao, memberMapper)

    @Provides
    @Singleton
    fun provideVotesRepository(divisionDao: DivisionDao, debateSpeechDao: DebateSpeechDao): VotesRepository =
        VotesRepository(divisionDao, debateSpeechDao)

    @Provides
    @Singleton
    fun provideFeedRepository(
        divisionDao: DivisionDao,
        followDao: FollowDao,
        recessDateDao: RecessDateDao,
        tagDao: com.goveye.app.data.local.dao.TagDao
    ): FeedRepository = FeedRepository(divisionDao, followDao, recessDateDao)

    @Provides
    @Singleton
    fun provideBillsRepository(billDao: BillDao): BillsRepository = BillsRepository(billDao)

    @Provides
    @Singleton
    fun provideHansardRepository(
        hansardDao: HansardDao,
        hansardApi: com.goveye.app.data.api.HansardApi,
        hansardMapper: HansardMapper
    ): HansardRepository = HansardRepository(hansardDao, hansardApi, hansardMapper)

    @Provides
    @Singleton
    fun provideInterestsRepository(interestDao: InterestDao): InterestsRepository = InterestsRepository(interestDao)

    @Provides
    @Singleton
    fun provideBioDataRepository(bioDataDao: BioDataDao): BioDataRepository = BioDataRepository(bioDataDao)

    @Provides
    @Singleton
    fun provideExpensesRepository(expenseDao: ExpenseDao): ExpensesRepository = ExpensesRepository(expenseDao)

    @Provides
    @Singleton
    fun provideMpLinksRepository(mpLinkDao: MpLinkDao): MpLinksRepository = MpLinksRepository(mpLinkDao)

    @Provides
    @Singleton
    fun provideManifestoRepository(manifestoDao: ManifestoDao): ManifestoRepository = ManifestoRepository(manifestoDao)

    @Provides
    @Singleton
    fun providePartyStatsRepository(partyStatsDao: PartyStatsDao): PartyStatsRepository =
        PartyStatsRepository(partyStatsDao)

    @Provides
    @Singleton
    fun provideHistoricalMemberRepository(historicalMemberDao: HistoricalMemberDao): HistoricalMemberRepository =
        HistoricalMemberRepository(historicalMemberDao)

    @Provides
    @Singleton
    fun provideStatsRepository(
        divisionDao: DivisionDao,
        committeeDao: CommitteeDao,
        debateSpeechDao: DebateSpeechDao,
        hansardDao: HansardDao,
        mpDao: MpDao,
        mpStatsDao: com.goveye.app.data.local.dao.MpStatsDao
    ): StatsRepository = StatsRepository(divisionDao, committeeDao, debateSpeechDao, hansardDao, mpDao, mpStatsDao)

    @Provides
    @Singleton
    fun provideFollowRepository(followDao: FollowDao, mpDao: MpDao): FollowRepository =
        FollowRepository(followDao, mpDao)

    @Provides
    @Singleton
    fun provideBillFollowRepository(billFollowDao: BillFollowDao): BillFollowRepository =
        BillFollowRepository(billFollowDao)
}
