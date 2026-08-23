package com.goveye.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.LocalDatabase
import com.goveye.app.data.local.dao.AnnouncementTagDao
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.dao.CachedPublicationDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.ExpenseDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.GovernmentPublicationDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.LegislationDao
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.MpContactDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpExperienceDao
import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.MpStatsDao
import com.goveye.app.data.local.dao.MpSynopsisDao
import com.goveye.app.data.local.dao.MpTagDao
import com.goveye.app.data.local.dao.PartyLeaderDao
import com.goveye.app.data.local.dao.PartyStatsDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.dao.SourceRecommendationDao
import com.goveye.app.data.local.dao.WrittenStatementDao
import com.goveye.app.data.mapper.HansardMapper
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.data.repo.BioDataRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
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
    // Migration 11 → 12: Fix mp_stats.activityScore column type (INTEGER → REAL)
    // and identity hash update. The seed DB at v11 has activityScore as INTEGER
    // but Room expects REAL. SQLite can't ALTER COLUMN, so recreate the table.
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_stats_new` (
                    `memberId` INTEGER NOT NULL,
                    `house` INTEGER NOT NULL,
                    `questionCount` INTEGER NOT NULL,
                    `speechCount` INTEGER NOT NULL,
                    `committeeCount` INTEGER NOT NULL,
                    `voteParticipationRate` REAL NOT NULL,
                    `rebellionRate` REAL NOT NULL,
                    `rebellionCount` INTEGER NOT NULL,
                    `totalDivisionsVoted` INTEGER NOT NULL,
                    `activityScore` REAL NOT NULL,
                    `rebellionPercentile` INTEGER NOT NULL,
                    `participationPercentile` INTEGER NOT NULL,
                    `questionsPercentile` INTEGER NOT NULL,
                    `speechesPercentile` INTEGER NOT NULL,
                    `committeesPercentile` INTEGER NOT NULL,
                    PRIMARY KEY(`memberId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """INSERT INTO `mp_stats_new` SELECT
                    `memberId`, `house`, `questionCount`, `speechCount`,
                    `committeeCount`, `voteParticipationRate`, `rebellionRate`,
                    `rebellionCount`, `totalDivisionsVoted`, `activityScore`,
                    `rebellionPercentile`, `participationPercentile`,
                    `questionsPercentile`, `speechesPercentile`,
                    `committeesPercentile`
                FROM `mp_stats`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `mp_stats`")
            db.execSQL("ALTER TABLE `mp_stats_new` RENAME TO `mp_stats`")
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

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_api_ids` (
                    `memberId` INTEGER NOT NULL,
                    `mnisId` INTEGER,
                    `twfyPersonId` INTEGER,
                    `ipsaMemberId` TEXT,
                    `publicWhipId` TEXT,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`memberId`)
                )
                """.trimIndent()
            )
        }
    }

    // Migration 18 → 19: Add IPSA descriptive fields to expenses table.
    // Idempotent — the seed DB may already include these columns if built
    // with a newer bundled_schema.json.
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val existingColumns = db.query("PRAGMA table_info(expenses)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }
            val newColumns = listOf(
                "shortDescription" to "TEXT",
                "details" to "TEXT",
                "claimNumber" to "TEXT",
                "journeyType" to "TEXT",
                "journeyFrom" to "TEXT",
                "journeyTo" to "TEXT",
                "travel" to "TEXT",
                "nights" to "TEXT",
                "mileage" to "TEXT",
                "amountPaidPence" to "INTEGER",
                "amountNotPaidPence" to "INTEGER",
                "amountRepaidPence" to "INTEGER",
                "reasonIfNotPaid" to "TEXT",
                "supplyMonth" to "TEXT",
                "supplyPeriod" to "TEXT"
            )
            for ((colName, colType) in newColumns) {
                if (colName !in existingColumns) {
                    db.execSQL("ALTER TABLE `expenses` ADD COLUMN `$colName` $colType")
                }
            }
        }
    }

    // Migration 19 → 20: Add government announcement tables (Phase 14).
    // Idempotent — the seed DB may already include these tables if built
    // with a newer bundled_schema.json.
    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `written_statements` (
                    `id` INTEGER NOT NULL,
                    `memberId` INTEGER NOT NULL,
                    `memberRole` TEXT NOT NULL,
                    `uin` TEXT NOT NULL,
                    `dateMade` TEXT NOT NULL,
                    `answeringBodyId` INTEGER NOT NULL,
                    `answeringBodyName` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `house` INTEGER NOT NULL,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `government_publications` (
                    `id` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `summary` TEXT NOT NULL,
                    `url` TEXT NOT NULL,
                    `documentType` TEXT NOT NULL,
                    `organisation` TEXT NOT NULL,
                    `organisationSlug` TEXT NOT NULL,
                    `firstPublishedAt` TEXT NOT NULL,
                    `publicUpdatedAt` TEXT NOT NULL,
                    `imageUrl` TEXT,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `legislation` (
                    `id` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `year` INTEGER NOT NULL,
                    `number` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `url` TEXT NOT NULL,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `publication_tags` (`publicationId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`publicationId`, `tag`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `statement_tags` (`statementId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`statementId`, `tag`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `legislation_tags` (`legislationId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`legislationId`, `tag`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mp_tags` (`memberId` INTEGER NOT NULL, `tag` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, PRIMARY KEY(`memberId`, `tag`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `party_leaders` (`partyId` INTEGER NOT NULL, `memberId` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`partyId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `source_recommendations` (`tag` TEXT NOT NULL, `organisationSlug` TEXT NOT NULL, `organisationName` TEXT NOT NULL, `hitCount` INTEGER NOT NULL, `isRecommended` INTEGER NOT NULL, PRIMARY KEY(`tag`, `organisationSlug`))"
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
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // Migration 1 → 2 for LocalDatabase: Add cached_publications table (D-02).
    // Idempotent — the table may already exist if the DB was created at v2.
    private val LOCAL_MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `cached_publications` (
                    `url` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `summary` TEXT NOT NULL,
                    `bodyText` TEXT NOT NULL,
                    `documentType` TEXT NOT NULL,
                    `organisation` TEXT NOT NULL,
                    `imageUrl` TEXT,
                    `firstPublishedAt` TEXT NOT NULL,
                    `fetchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`url`)
                )
                """.trimIndent()
            )
        }
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
            )
            .addMigrations(LOCAL_MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
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
    fun provideCouncilDao(database: BundledDatabase): com.goveye.app.data.local.dao.CouncilDao = database.councilDao()

    @Provides
    fun provideMpApiIdDao(database: BundledDatabase): com.goveye.app.data.local.dao.MpApiIdDao = database.mpApiIdDao()

    @Provides
    fun provideDatabaseUpdateDao(database: BundledDatabase): DatabaseUpdateDao = database.databaseUpdateDao()

    @Provides
    fun provideWrittenStatementDao(database: BundledDatabase): WrittenStatementDao = database.writtenStatementDao()

    @Provides
    fun provideGovernmentPublicationDao(database: BundledDatabase): GovernmentPublicationDao =
        database.governmentPublicationDao()

    @Provides
    fun provideLegislationDao(database: BundledDatabase): LegislationDao = database.legislationDao()

    @Provides
    fun provideAnnouncementTagDao(database: BundledDatabase): AnnouncementTagDao = database.announcementTagDao()

    @Provides
    fun provideMpTagDao(database: BundledDatabase): MpTagDao = database.mpTagDao()

    @Provides
    fun providePartyLeaderDao(database: BundledDatabase): PartyLeaderDao = database.partyLeaderDao()

    @Provides
    fun provideSourceRecommendationDao(database: BundledDatabase): SourceRecommendationDao =
        database.sourceRecommendationDao()

    // ── User-data DAOs (from LocalDatabase) ────────────────────────────

    @Provides
    fun provideFollowDao(database: LocalDatabase): FollowDao = database.followDao()

    @Provides
    fun provideBillFollowDao(database: LocalDatabase): BillFollowDao = database.billFollowDao()

    @Provides
    fun provideMpNotificationPreferenceDao(database: LocalDatabase): MpNotificationPreferenceDao =
        database.mpNotificationPreferenceDao()

    @Provides
    fun provideCachedPublicationDao(database: LocalDatabase): CachedPublicationDao = database.cachedPublicationDao()

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
    ): MembersRepository = MembersRepository(
        mpDao,
        searchDao,
        membersApi,
        memberMapper,
        historicalMemberDao,
        mpSynopsisDao,
        mpContactDao,
        mpExperienceDao
    )

    @Provides
    @Singleton
    fun provideCommitteesRepository(committeeDao: CommitteeDao, memberMapper: MemberMapper): CommitteesRepository =
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
    ): FeedRepository = FeedRepository(divisionDao, followDao, recessDateDao, tagDao)

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

    @Provides
    @Singleton
    fun provideGovernmentAnnouncementsRepository(
        writtenStatementDao: WrittenStatementDao,
        governmentPublicationDao: GovernmentPublicationDao,
        legislationDao: LegislationDao,
        announcementTagDao: AnnouncementTagDao,
        mpTagDao: MpTagDao,
        partyLeaderDao: PartyLeaderDao,
        sourceRecommendationDao: SourceRecommendationDao
    ): GovernmentAnnouncementsRepository = GovernmentAnnouncementsRepository(
        writtenStatementDao,
        governmentPublicationDao,
        legislationDao,
        announcementTagDao,
        mpTagDao,
        partyLeaderDao,
        sourceRecommendationDao
    )
}
