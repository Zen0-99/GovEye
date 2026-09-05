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

    // Migration 20 → 21: Add written_questions table (Phase 15 — Written Questions API).
    // The seed DB (v20) doesn't have this table; the migration creates it empty.
    // Data is populated by the update-written-questions workflow at runtime.
    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `written_questions` (
                    `id` INTEGER NOT NULL,
                    `memberId` INTEGER NOT NULL,
                    `uin` TEXT NOT NULL,
                    `dateTabled` TEXT NOT NULL,
                    `answeringBodyId` INTEGER NOT NULL,
                    `answeringBodyName` TEXT NOT NULL,
                    `questionText` TEXT NOT NULL,
                    `house` INTEGER NOT NULL,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    // Migration 21 → 22: Split interests buckets from 6 to 10 categories.
    // Category 4 (Visits outside the UK) was lumped under "Gifts" → now
    // "Overseas Visits". Category 5 (Overseas gifts) was under "Gifts" →
    // now "Overseas Gifts". Categories 8/9/10 were under "Other" → now
    // "Miscellaneous", "Family Employed", "Family Lobbying" respectively.
    // Idempotent — re-running on a DB already at v22 is a no-op since
    // the CASE only matches old bucket values.
    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE interests SET bucket = CASE
                    WHEN categoryNumber IN ('1', '1.1', '1.2') THEN 'Employment/Earnings'
                    WHEN categoryNumber = '2' THEN 'Financial Support'
                    WHEN categoryNumber = '3' THEN 'Gifts'
                    WHEN categoryNumber = '4' THEN 'Overseas Visits'
                    WHEN categoryNumber = '5' THEN 'Overseas Gifts'
                    WHEN categoryNumber = '6' THEN 'Land/Property'
                    WHEN categoryNumber = '7' THEN 'Shareholdings'
                    WHEN categoryNumber = '8' THEN 'Miscellaneous'
                    WHEN categoryNumber = '9' THEN 'Family Employed'
                    WHEN categoryNumber = '10' THEN 'Family Lobbying'
                    ELSE bucket
                END
                """.trimIndent()
            )
        }
    }

    // Migration 22 → 23: Add partyAbbreviation and partyColourHex columns to
    // historical_members table (Phase 17 — historical member search fix).
    // Idempotent — the seed DB may already include these columns if built
    // with a newer bundled_schema.json.
    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val existingColumns = db.query("PRAGMA table_info(historical_members)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            if ("partyAbbreviation" !in existingColumns) {
                db.execSQL("ALTER TABLE historical_members ADD COLUMN partyAbbreviation TEXT")
            }
            if ("partyColourHex" !in existingColumns) {
                db.execSQL("ALTER TABLE historical_members ADD COLUMN partyColourHex TEXT")
            }
        }
    }

    // Migration 23 → 24: Add 16 structured interest fields (Phase 18).
    // Idempotent — the seed DB may already include these columns if built
    // with a newer bundled_schema.json.
    private val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val existingColumns = db.query("PRAGMA table_info(interests)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            val newColumns = listOf(
                "donorName",
                "paymentType",
                "paymentDescription",
                "donorStatus",
                "donorAddress",
                "donorCompanyIdentifier",
                "destination",
                "visitPurpose",
                "organisationName",
                "organisationDescription",
                "propertyLocation",
                "propertyType",
                "hoursWorked",
                "familyMemberName",
                "familyMemberRelationship",
                "familyMemberRole"
            )

            for (colName in newColumns) {
                if (colName !in existingColumns) {
                    db.execSQL("ALTER TABLE interests ADD COLUMN `$colName` TEXT")
                }
            }
        }
    }

    // Migration 24 → 25: Add bodyText to government_publications + fullCategoryName to interests.
    // Idempotent — the seed DB may already include these columns if built
    // with a newer bundled_schema.json.
    private val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // bodyText on government_publications
            val pubColumns = db.query("PRAGMA table_info(government_publications)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            if ("bodyText" !in pubColumns) {
                db.execSQL("ALTER TABLE government_publications ADD COLUMN `bodyText` TEXT")
            }

            // fullCategoryName on interests (short category name in categoryName,
            // full Parliament API name in fullCategoryName)
            val interestColumns = db.query("PRAGMA table_info(interests)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            if ("fullCategoryName" !in interestColumns) {
                db.execSQL("ALTER TABLE interests ADD COLUMN `fullCategoryName` TEXT")
            }

            // written_questions table may not exist in older seed DBs
            val tableExists = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='written_questions'"
            ).use { it.moveToFirst() }

            if (!tableExists) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `written_questions` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `memberId` INTEGER NOT NULL,
                        `uin` TEXT NOT NULL,
                        `dateTabled` TEXT NOT NULL,
                        `answeringBodyId` INTEGER NOT NULL,
                        `answeringBodyName` TEXT NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `house` INTEGER NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )"""
                )
            }
        }
    }

    // Migration 25 → 26: Add bodyText to legislation table + create written_questions
    // table (was missing from seed DB in earlier builds).
    // Idempotent — the seed DB may already include these.
    private val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // bodyText on legislation
            val legColumns = db.query("PRAGMA table_info(legislation)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                }
            }.toSet()

            if ("bodyText" !in legColumns) {
                db.execSQL("ALTER TABLE legislation ADD COLUMN `bodyText` TEXT")
            }

            // written_questions table — may not exist in older seed DBs
            val tables = db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }.toSet()

            if ("written_questions" !in tables) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `written_questions` (
                        `id` INTEGER NOT NULL,
                        `memberId` INTEGER NOT NULL,
                        `uin` TEXT NOT NULL,
                        `dateTabled` TEXT NOT NULL,
                        `answeringBodyId` INTEGER NOT NULL,
                        `answeringBodyName` TEXT NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `house` INTEGER NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }

    // Migration 26 → 27: Add index on division_votes.memberId.
    // Without this index, queries filtering by memberId (e.g. getRecentVotesForMembers)
    // do full table scans on a table with hundreds of thousands of rows, causing
    // the Following tab to hang indefinitely on skeletons.
    // Idempotent — the index may already exist if the seed DB was built at v27.
    private val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val indexes = db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='division_votes'"
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }.toSet()

            if ("index_division_votes_memberId" !in indexes) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_division_votes_memberId` ON `division_votes` (`memberId`)"
                )
            }
        }
    }

    // Fix stale party_leaders member IDs. The seed DB (v27) had wrong IDs
    // from HARDCODED_LEADERS in build_party_leaders.py — e.g. 4513 was Mims
    // Davies (Conservative!) not Keir Starmer, 4981 was Ashley Dalton (Labour!)
    // not Nigel Farage. This migration corrects all 8 entries with verified
    // member IDs from the mps table.
    private val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Labour (15) — Andy Burnham, Prime Minister
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (15, 1427, 'Prime Minister')"
            )
            // Conservative (4) — Kemi Badenoch, Leader of the Opposition
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (4, 4597, 'Leader of the Opposition')"
            )
            // Liberal Democrats (17) — Ed Davey
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (17, 188, 'Leader of the Liberal Democrats')"
            )
            // SNP (29) — Pete Wishart (Westminster leader)
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (29, 1440, 'Leader of the Scottish National Party')"
            )
            // DUP (7) — Gavin Robinson
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (7, 4360, 'Leader of the Democratic Unionist Party')"
            )
            // Plaid Cymru (22) — Liz Saville Roberts
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (22, 4521, 'Leader of Plaid Cymru')"
            )
            // Green Party (44) — Adrian Ramsay (co-leader; Zack Polanski not an MP)
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (44, 5320, 'Leader of the Green Party')"
            )
            // Reform UK (1036) — Nigel Farage
            db.execSQL(
                "INSERT OR REPLACE INTO party_leaders (partyId, memberId, title) VALUES (1036, 5091, 'Leader of Reform UK')"
            )
        }
    }

    @Provides
    @Singleton
    fun provideBundledDatabase(@ApplicationContext context: Context): BundledDatabase {
        // Ensure the databases directory exists so that the first-launch download
        // can place the DB file at Room's expected path before Room opens (D-04).
        context.getDatabasePath(BundledDatabase.DATABASE_NAME).parentFile?.mkdirs()

        // Clean up stale WAL/SHM files before Room opens the DB.
        // After a migration (e.g. v20→v26), the WAL may be checkpointed to 0 bytes
        // but the SHM file can be left in an inconsistent state. This causes Room's
        // InvalidationTracker to silently block all DAO flows — the app shows
        // loading spinners forever with no errors. Deleting the stale SHM/WAL
        // files lets SQLite recreate them fresh on open.
        val dbPath = context.getDatabasePath(BundledDatabase.DATABASE_NAME)
        if (dbPath.exists()) {
            val walFile = java.io.File(dbPath.parentFile, "${BundledDatabase.DATABASE_NAME}-wal")
            val shmFile = java.io.File(dbPath.parentFile, "${BundledDatabase.DATABASE_NAME}-shm")
            if (walFile.exists() && walFile.length() == 0L && shmFile.exists()) {
                android.util.Log.w("GovEye/DbModule", "Deleting stale WAL (0 bytes) + SHM before Room opens")
                walFile.delete()
                shmFile.delete()
            }
        }

        return Room
            .databaseBuilder(
                context,
                BundledDatabase::class.java,
                BundledDatabase.DATABASE_NAME
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    android.util.Log.i(
                        "GovEye/DbModule",
                        "BundledDatabase onOpen — version=${db.version} path=${db.path}"
                    )
                }
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    android.util.Log.i("GovEye/DbModule", "BundledDatabase onCreate")
                }
            })
            .addMigrations(
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28
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

    // Migration 2 → 3 for LocalDatabase: Add incomeEnabled and expensesEnabled
    // columns to mp_notification_prefs (issue #8 — Income and Expenses notification types).
    private val LOCAL_MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE mp_notification_prefs ADD COLUMN incomeEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE mp_notification_prefs ADD COLUMN expensesEnabled INTEGER NOT NULL DEFAULT 0")
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
            .addMigrations(LOCAL_MIGRATION_1_2, LOCAL_MIGRATION_2_3)
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

    @Provides
    fun provideWrittenQuestionDao(database: BundledDatabase): com.goveye.app.data.local.dao.WrittenQuestionDao =
        database.writtenQuestionDao()

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
        mpStatsDao: com.goveye.app.data.local.dao.MpStatsDao,
        bioDataDao: BioDataDao,
        interestDao: com.goveye.app.data.local.dao.InterestDao,
        expenseDao: com.goveye.app.data.local.dao.ExpenseDao
    ): StatsRepository = StatsRepository(
        divisionDao, committeeDao, debateSpeechDao, hansardDao,
        mpDao, mpStatsDao, bioDataDao, interestDao, expenseDao
    )

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
        sourceRecommendationDao: SourceRecommendationDao,
        mpDao: MpDao,
        bioDataDao: BioDataDao
    ): GovernmentAnnouncementsRepository = GovernmentAnnouncementsRepository(
        writtenStatementDao,
        governmentPublicationDao,
        legislationDao,
        announcementTagDao,
        mpTagDao,
        partyLeaderDao,
        sourceRecommendationDao,
        mpDao,
        bioDataDao
    )
}
