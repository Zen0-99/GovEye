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
import com.goveye.app.data.local.dao.ConstituencyElectionDao
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
import com.goveye.app.data.local.dao.MpAppointmentDao
import com.goveye.app.data.local.dao.MpCareerEventDao
import com.goveye.app.data.local.dao.MpContactDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpExperienceDao
import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.MpOfficerIdentityDao
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

    // Add leaderSinceDate column to party_leaders and populate it with known
    // leader start dates. Also backfill bio_data.dateOfBirth for leaders where
    // the MNIS API returned null (Wikidata values from build_ages.py).
    private val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add leaderSinceDate column
            db.execSQL("ALTER TABLE party_leaders ADD COLUMN leaderSinceDate TEXT")

            // Populate leaderSinceDate with known dates for each leader
            // Andy Burnham (PM) — from MNIS GovernmentPosts
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2026-07-20' WHERE partyId = 15")
            // Kemi Badenoch (Opposition) — from MNIS OppositionPosts
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2024-11-02' WHERE partyId = 4")
            // Ed Davey (Lib Dem leader) — became leader 2019-12-22
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2019-12-22' WHERE partyId = 17")
            // Pete Wishart (SNP Westminster leader) — from MNIS OppositionPosts
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2024-07-10' WHERE partyId = 29")
            // Gavin Robinson (DUP leader) — became interim leader 2024-02-03
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2024-02-03' WHERE partyId = 7")
            // Liz Saville Roberts (Plaid Cymru Westminster leader) — from MNIS
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2017-06-08' WHERE partyId = 22")
            // Adrian Ramsay (Green co-leader) — became co-leader 2021-09-04
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2021-09-04' WHERE partyId = 44")
            // Nigel Farage (Reform UK leader) — became leader 2024-06-11
            db.execSQL("UPDATE party_leaders SET leaderSinceDate = '2024-06-11' WHERE partyId = 1036")

            // Backfill bio_data.dateOfBirth for leaders where MNIS returned null.
            // Values from Wikidata (build_ages.py).
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1970-01-07' WHERE mpId = 1427 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1980-01-02' WHERE mpId = 4597 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1965-12-25' WHERE mpId = 188 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1962-03-09' WHERE mpId = 1440 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1984-11-22' WHERE mpId = 4360 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1964-12-16' WHERE mpId = 4521 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
            db.execSQL(
                "UPDATE bio_data SET dateOfBirth = '1964-04-03' WHERE mpId = 5091 AND (dateOfBirth IS NULL OR dateOfBirth = '')"
            )
        }
    }

    // Backfill bio_data.dateOfBirth for all 249 MPs where the MNIS API returned
    // null. Values sourced from Wikidata via build_ages.py (not run by CI).
    // Uses a temp table for efficient bulk update.
    private val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TEMP TABLE temp_dob_fix (mpId INTEGER, dateOfBirth TEXT)")
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (39,'1959-10-16'),(40,'1959-04-09'),(54,'1951-09-26'),(55,'1956-08-20'),(87,'1943-08-20'),
                (146,'1957-03-10'),(152,'1954-04-09'),(163,'1955-07-29'),(165,'1958-07-10'),(172,'1953-09-27'),
                (177,'1967-07-15'),(178,'1951-09-08'),(185,'1949-05-26'),(188,'1965-12-25'),(193,'1960-02-20'),
                (206,'1972-07-19'),(221,'1960-01-28'),(227,'1958-12-02'),(242,'1947-05-19'),(249,'1953-03-23'),
                (345,'1950-07-20'),(350,'1958-06-23'),(373,'1948-12-23'),(394,'1950-01-13'),(400,'1960-02-13'),
                (410,'1950-07-02'),(413,'1953-11-26'),(415,'1955-04-12'),(420,'1969-03-20'),(429,'1959-07-09'),
                (449,'1950-02-17'),(467,'1957-06-10'),(473,'1958-11-02'),(483,'1961-02-17'),(491,'1961-02-17'),
                (529,'1957-07-08'),(632,'1967-10-26'),(1171,'1970-10-02'),(1211,'1956-03-23'),(1383,'1962-10-03'),
                (1409,'1953-02-15'),(1427,'1970-01-07'),(1440,'1962-03-09'),(1442,'1965-07-15'),(1444,'1965-08-14'),
                (1446,'1962-01-11'),(1447,'1966-03-17'),(1466,'1961-04-24'),(1482,'1962-03-12'),(1489,'1969-11-03')"""
            )
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (1491,'1965-03-26'),(1508,'1960-04-30'),(1510,'1969-12-24'),(1512,'1962-05-27'),(1516,'1960-09-29'),
                (1521,'1966-04-01'),(1524,'1969-02-14'),(1533,'1966-07-25'),(1536,'1960-07-27'),(1541,'1956-12-04'),
                (1548,'1968-05-29'),(1560,'1972-10-24'),(1572,'1966-11-01'),(1576,'1966-11-22'),(1579,'1967-12-02'),
                (1587,'1965-03-26'),(1591,'1970-05-27'),(1593,'1953-04-04'),(3909,'1984-06-24'),(3912,'1962-05-30'),
                (3914,'1980-09-17'),(3924,'1963-07-05'),(3928,'1960-01-14'),(3935,'1961-09-30'),(3948,'1967-12-22'),
                (3952,'1970-08-12'),(3957,'1950-09-13'),(3966,'1976-08-10'),(3969,'1969-11-27'),(3973,'1961-03-13'),
                (3991,'1962-06-23'),(3997,'1976-01-10'),(4005,'1956-04-26'),(4008,'1971-10-28'),(4018,'1961-09-28'),
                (4020,'1967-07-12'),(4026,'1971-06-11'),(4029,'1966-03-26'),(4030,'1971-04-15'),(4031,'1979-02-13'),
                (4032,'1971-11-25'),(4038,'1975-04-17'),(4040,'1977-07-07'),(4046,'1983-12-19'),(4048,'1972-06-26'),
                (4051,'1974-04-01'),(4056,'1955-07-10'),(4061,'1966-10-27'),(4066,'1972-03-29'),(4074,'1963-02-26')"""
            )
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (4076,'1954-12-07'),(4077,'1977-11-02'),(4082,'1979-08-09'),(4083,'1980-12-30'),(4084,'1967-10-24'),
                (4088,'1977-04-05'),(4089,'1959-10-16'),(4095,'1972-05-03'),(4107,'1960-05-02'),(4108,'1976-06-25'),
                (4110,'1970-03-12'),(4118,'1971-08-30'),(4119,'1980-08-28'),(4124,'1965-04-12'),(4125,'1976-06-08'),
                (4126,'1957-01-13'),(4131,'1955-03-25'),(4138,'1975-03-14'),(4139,'1963-01-06'),(4212,'1960-09-15'),
                (4243,'1972-11-30'),(4245,'1967-06-10'),(4253,'1972-08-07'),(4263,'1974-10-10'),(4264,'1980-04-15'),
                (4267,'1969-07-10'),(4268,'1963-11-12'),(4269,'1958-03-08'),(4277,'1978-11-08'),(4316,'1969-01-09'),
                (4320,'1982-01-09'),(4356,'1980-03-28'),(4357,'1986-03-20'),(4358,'1967-11-09'),(4359,'1970-01-01'),
                (4360,'1984-11-22'),(4362,'1977-12-09'),(4366,'1969-09-04'),(4368,'1978-12-30'),(4370,'1981-10-09'),
                (4371,'1963-04-27'),(4382,'1956-11-09'),(4384,'1976-03-17'),(4389,'1959-05-14'),(4391,'1967-06-26'),
                (4394,'1978-06-07'),(4396,'1979-09-22'),(4397,'1957-06-20'),(4399,'1976-03-22'),(4403,'1969-10-21')"""
            )
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (4407,'1970-10-13'),(4409,'1973-11-13'),(4418,'1972-11-22'),(4425,'1966-09-06'),(4436,'1985-06-16'),
                (4439,'1971-11-13'),(4441,'1978-08-01'),(4444,'1964-09-08'),(4449,'1978-10-09'),(4456,'1963-01-26'),
                (4457,'1947-04-27'),(4460,'1972-09-01'),(4462,'1973-06-27'),(4464,'1979-06-19'),(4471,'1972-07-05'),
                (4473,'1987-07-22'),(4474,'1963-09-28'),(4475,'1980-04-03'),(4479,'1980-05-26'),(4480,'1960-09-18'),
                (4483,'1980-05-12'),(4484,'1983-11-19'),(4491,'1977-03-09'),(4493,'1980-09-19'),(4494,'1969-06-28'),
                (4495,'1966-10-27'),(4500,'1971-09-11'),(4501,'1970-08-21'),(4503,'1976-07-06'),(4504,'1983-01-21'),
                (4505,'1970-09-09'),(4510,'1974-08-08'),(4511,'1972-04-02'),(4513,'1975-06-02'),(4514,'1962-09-02'),
                (4515,'1968-08-15'),(4518,'1982-09-16'),(4519,'1974-04-30'),(4520,'1982-10-29'),(4521,'1964-12-16'),
                (4523,'1966-09-14'),(4527,'1976-06-23'),(4569,'1980-07-07'),(4571,'1957-03-14'),(4572,'1983-12-23'),
                (4573,'1978-05-10'),(4591,'1977-01-01'),(4592,'1977-12-31'),(4595,'1986-01-01'),(4597,'1980-01-02')"""
            )
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (4598,'1971-10-15'),(4601,'1987-05-28'),(4602,'1960-02-08'),(4603,'1972-11-01'),(4607,'1986-12-19'),
                (4608,'1976-04-15'),(4610,'1967-12-02'),(4612,'1954-06-16'),(4613,'1977-09-07'),(4616,'1971-07-01'),
                (4617,'1962-11-01'),(4618,'1956-07-10'),(4620,'1980-12-11'),(4621,'1986-11-13'),(4623,'1971-10-05'),
                (4630,'1993-01-22'),(4631,'1972-12-20'),(4632,'1971-01-01'),(4634,'1960-11-24'),(4636,'1984-08-20'),
                (4637,'1977-04-23'),(4638,'1978-08-17'),(4641,'1984-02-04'),(4645,'1979-09-16'),(4647,'1984-06-04'),
                (4651,'1986-10-28'),(4653,'1981-01-17'),(4654,'1966-12-15'),(4656,'1982-09-12'),(4657,'1978-03-16'),
                (4658,'1975-04-26'),(4671,'1958-04-05'),(4673,'1973-06-15'),(4676,'1976-01-23'),(4679,'1978-11-06'),
                (4682,'1980-04-10'),(4697,'1991-12-19'),(4698,'1970-12-15'),(4798,'1972-04-07'),(4827,'1980-06-19'),
                (4846,'1983-04-30'),(4857,'1985-02-28'),(4870,'1980-09-18'),(5190,'1958-09-24'),(5253,'1974-05-29'),
                (5262,'1969-05-19'),(5314,'1974-07-09'),(5319,'1971-09-24'),(5356,'1953-04-02')"""
            )
            db.execSQL(
                """UPDATE bio_data SET dateOfBirth = (
                    SELECT dateOfBirth FROM temp_dob_fix WHERE temp_dob_fix.mpId = bio_data.mpId
                ) WHERE dateOfBirth IS NULL OR dateOfBirth = ''"""
            )
            db.execSQL("DROP TABLE temp_dob_fix")
        }
    }

    // Backfill bio_data.dateOfBirth for 328 additional MPs (primarily the 2024
    // cohort) whose DOB was not in ParlParse Wikidata Q-IDs. Values sourced
    // from Wikidata API name-search via build_ages.py fallback.
    // Uses a temp table for efficient bulk update.
    private val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TEMP TABLE temp_dob_fix (mpId INTEGER, dateOfBirth TEXT)")
            db.execSQL(
                """INSERT INTO temp_dob_fix VALUES
                (4716,'1962-04-01'),(4736,'1973-03-04'),(4739,'1970-04-11'),(4742,'1976-07-17'),(4743,'1967-01-06'),
                (4747,'1971-01-01'),(4753,'1968-02-27'),(4759,'1981-01-30'),(4764,'1985-03-01'),(4765,'1976-12-20'),
                (4769,'1981-10-29'),(4776,'1978-04-26'),(4777,'1983-01-11'),(4778,'1959-10-08'),(4779,'1992-07-22'),
                (4780,'1984-12-07'),(4781,'1983-01-10'),(4783,'1966-06-18'),(4785,'1981-12-11'),(4786,'1993-10-31'),
                (4787,'1977-01-05'),(4788,'1971-02-06'),(4790,'1990-05-25'),(4799,'1991-04-06'),(4803,'1968-02-18'),
                (4804,'1978-04-04'),(4805,'1987-11-11'),(4806,'1985-07-08'),(4811,'1989-08-22'),(4813,'1985-03-11'),
                (4818,'1985-06-18'),(4820,'1983-02-08'),(4822,'1979-01-06'),(4828,'1972-05-09'),(4831,'1972-05-10'),
                (4844,'1983-09-24'),(4849,'1989-04-05'),(4850,'1988-09-17'),(4853,'1969-01-01'),(4858,'1974-10-23'),
                (4860,'1984-06-06'),(4861,'1984-11-28'),(4864,'1990-03-10'),(4869,'1996-08-29'),(4871,'1984-11-09'),
                (4872,'1976-02-22'),(4873,'1972-12-30'),(4874,'1970-10-19'),(4918,'1982-04-25'),(4923,'1976-05-01'),
                (4932,'1988-02-14'),(4934,'1975-01-01'),(4938,'1962-11-23'),(4942,'1978-02-13'),(4943,'1980-12-15'),
                (4976,'1965-09-01'),(4979,'1985-03-18'),(4981,'1972-08-15'),(4993,'1998-01-01'),(4995,'1971-11-01'),
                (4998,'1967-08-26'),(5000,'1990-03-05'),(5001,'1988-06-17'),(5010,'1982-07-08'),(5011,'1995-05-05'),
                (5025,'1986-04-26'),(5028,'1983-01-01'),(5029,'1987-01-01'),(5031,'1985-03-11'),(5032,'1994-02-26'),
                (5033,'1987-03-01'),(5036,'1972-07-01'),(5037,'1988-01-01'),(5038,'1976-02-08'),(5039,'1989-11-01'),
                (5041,'1986-01-01'),(5042,'1970-01-01'),(5044,'1990-11-30'),(5046,'1960-02-19'),(5047,'1988-07-20'),
                (5049,'1970-12-01'),(5050,'1992-01-01'),(5051,'1977-08-01'),(5052,'1976-08-25'),(5053,'1984-05-24'),
                (5054,'1978-01-01'),(5055,'1984-03-16'),(5056,'1986-01-01'),(5057,'1973-09-01'),(5059,'1972-08-01'),
                (5061,'1987-01-07'),(5062,'1980-01-01'),(5063,'1979-03-01'),(5064,'1983-01-01'),(5065,'1964-10-13'),
                (5066,'1969-01-01'),(5067,'1982-03-23'),(5068,'1994-01-01'),(5069,'1982-09-01'),(5071,'1967-01-01'),
                (5072,'1984-10-01'),(5073,'1977-01-01'),(5076,'1978-06-03'),(5077,'1997-05-01'),(5078,'1964-07-01'),
                (5079,'1985-12-01'),(5080,'1984-04-01'),(5082,'1989-07-05'),(5083,'1990-01-01'),(5084,'1990-08-01'),
                (5085,'1982-01-01'),(5086,'1993-06-20'),(5088,'1991-11-01'),(5089,'1979-09-01'),(5090,'1982-05-01'),
                (5091,'1964-04-03'),(5092,'1979-06-25'),(5094,'1968-12-11'),(5095,'1991-01-01'),(5096,'1981-11-01'),
                (5099,'1979-01-01'),(5101,'1969-11-15'),(5102,'1985-09-24'),(5105,'1964-01-22'),(5106,'1977-11-01'),
                (5107,'1977-01-01'),(5108,'1984-01-01'),(5110,'2000-01-01'),(5111,'1962-04-01'),(5112,'1972-03-02'),
                (5113,'1979-01-01'),(5115,'1977-07-29'),(5116,'1975-10-14'),(5117,'1968-11-22'),(5118,'1994-03-17'),
                (5119,'1983-01-01'),(5120,'1972-11-29'),(5122,'1974-02-01'),(5123,'1967-01-01'),(5124,'1943-08-23'),
                (5125,'1985-10-01'),(5126,'1945-10-10'),(5127,'1975-01-01'),(5128,'1987-01-01'),(5129,'1986-02-01'),
                (5130,'1969-01-01'),(5131,'1943-05-25'),(5132,'1988-03-01'),(5133,'1983-04-01'),(5134,'1965-04-29'),
                (5135,'1995-04-22'),(5137,'1958-09-01'),(5138,'1968-01-01'),(5140,'1974-03-01'),(5141,'1981-06-02'),
                (5142,'1980-05-01'),(5144,'1971-08-01'),(5145,'1964-01-01'),(5147,'1995-05-05'),(5148,'1985-12-20'),
                (5149,'1976-07-01'),(5150,'1979-12-01'),(5152,'1975-01-01'),(5153,'1982-12-01'),(5155,'1976-01-01'),
                (5156,'1981-04-30'),(5157,'1950-05-12'),(5158,'1957-10-31'),(5159,'1992-11-01'),(5160,'1986-01-01'),
                (5161,'1964-09-13'),(5162,'1988-01-01'),(5163,'1958-04-08'),(5164,'1984-05-12'),(5165,'1993-01-01'),
                (5166,'1990-01-06'),(5167,'1982-01-01'),(5168,'1991-12-01'),(5169,'1980-06-04'),(5172,'1984-08-24'),
                (5173,'1983-01-01'),(5175,'1986-07-01'),(5176,'1972-11-01'),(5179,'1975-07-01'),(5180,'1988-09-01'),
                (5181,'1994-10-01'),(5182,'1958-06-01'),(5183,'1988-01-01'),(5184,'1976-12-01'),(5185,'1978-04-06'),
                (5186,'1965-11-30'),(5187,'2000-01-01'),(5188,'1972-07-03'),(5189,'1994-09-30'),(5191,'1992-06-13'),
                (5192,'1994-01-01'),(5193,'1991-01-01'),(5195,'1984-02-01'),(5196,'1982-07-01'),(5197,'1983-01-01'),
                (5199,'1995-08-01'),(5200,'1964-01-01'),(5202,'1986-05-01'),(5203,'1969-06-01'),(5204,'1984-10-01'),
                (5205,'1990-08-01'),(5206,'1965-11-01'),(5207,'1952-12-10'),(5208,'1970-04-02'),(5209,'1978-08-01'),
                (5210,'1983-02-01'),(5211,'1975-07-02'),(5212,'1969-06-08'),(5213,'1981-01-01'),(5214,'1952-01-09'),
                (5215,'1986-02-26'),(5216,'1968-11-22'),(5217,'1995-01-01'),(5218,'1962-11-27'),(5219,'1976-03-17'),
                (5222,'1992-02-01'),(5223,'1964-01-01'),(5224,'1994-04-01'),(5227,'1963-09-01'),(5228,'1988-01-01'),
                (5231,'1989-07-01'),(5232,'1962-11-01'),(5233,'1977-07-18'),(5234,'1988-01-01'),(5235,'1980-03-27'),
                (5236,'1990-12-01'),(5237,'1983-06-24'),(5238,'1986-11-21'),(5239,'1966-04-01'),(5240,'1981-02-01'),
                (5241,'1978-07-18'),(5242,'1960-11-05'),(5243,'1973-04-24'),(5244,'1993-12-01'),(5245,'1987-01-01'),
                (5246,'1964-12-01'),(5247,'1975-01-01'),(5248,'1989-10-21'),(5249,'1975-03-07'),(5250,'1965-01-01'),
                (5252,'1964-10-13'),(5254,'1965-10-01'),(5255,'1984-01-01'),(5256,'1988-04-05'),(5257,'1979-09-01'),
                (5259,'1990-09-01'),(5260,'1981-01-01'),(5261,'1965-06-02'),(5264,'1991-03-03'),(5266,'1985-02-01'),
                (5267,'1968-11-06'),(5268,'1971-01-01'),(5271,'1984-09-01'),(5275,'1980-12-01'),(5276,'1983-01-01'),
                (5279,'1972-09-26'),(5280,'1958-01-04'),(5282,'1992-07-01'),(5283,'1965-10-11'),(5284,'1943-08-19'),
                (5285,'1991-08-01'),(5287,'1985-11-01'),(5291,'1970-01-01'),(5292,'1990-01-01'),(5296,'1990-01-01'),
                (5297,'1979-01-01'),(5299,'1972-01-01'),(5300,'1974-01-01'),(5301,'1981-06-17'),(5302,'1994-01-01'),
                (5303,'1988-05-08'),(5304,'1982-01-12'),(5305,'1986-05-18'),(5306,'1988-07-16'),(5308,'2002-01-01'),
                (5309,'1971-10-01'),(5310,'1983-01-01'),(5311,'1990-01-01'),(5312,'1984-12-17'),(5313,'1981-06-01'),
                (5315,'1970-05-15'),(5317,'1985-03-27'),(5320,'1982-01-01'),(5321,'1959-03-01'),(5322,'1985-01-01'),
                (5323,'1984-11-01'),(5324,'1972-12-20'),(5325,'1971-02-01'),(5327,'1972-12-17'),(5328,'1986-04-25'),
                (5329,'1960-01-01'),(5330,'1986-02-03'),(5331,'1997-07-28'),(5332,'1968-02-02'),(5333,'1983-01-01'),
                (5335,'1990-08-01'),(5336,'1977-07-11'),(5337,'1986-09-17'),(5338,'1985-08-01'),(5339,'1988-01-01'),
                (5340,'1992-08-19'),(5341,'1983-01-01'),(5343,'1999-01-13'),(5344,'1961-11-14'),(5345,'1957-09-16'),
                (5347,'1989-12-01'),(5348,'1969-11-16'),(5349,'1985-06-06'),(5350,'1991-08-10'),(5351,'1974-08-27'),
                (5352,'1967-12-23'),(5353,'1986-07-01'),(5354,'1982-12-01'),(5355,'1988-01-01'),(5357,'1973-05-01'),
                (5359,'1960-01-01'),(5360,'1974-06-01'),(5361,'1986-01-01'),(5362,'1992-10-15'),(5403,'1969-01-01'),
                (5447,'1991-04-19'),(5449,'1998-01-01'),(5450,'1971-08-01')"""
            )
            db.execSQL(
                """UPDATE bio_data SET dateOfBirth = (
                    SELECT dateOfBirth FROM temp_dob_fix WHERE temp_dob_fix.mpId = bio_data.mpId
                ) WHERE dateOfBirth IS NULL OR dateOfBirth = ''"""
            )
            db.execSQL("DROP TABLE temp_dob_fix")
        }
    }

    // Migration 31 → 32: Add hasLinkedStatements + linkedStatementsJson columns
    // to written_statements for statement linking (e.g. joint statements).
    // Idempotent — the seed DB may already have these columns if it was built
    // with a newer merge_dbs.py that added them manually.
    private val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val wsCols = db.query("PRAGMA table_info(written_statements)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(1))
                }
            }
            if ("hasLinkedStatements" !in wsCols) {
                db.execSQL("ALTER TABLE written_statements ADD COLUMN hasLinkedStatements INTEGER NOT NULL DEFAULT 0")
            }
            if ("linkedStatementsJson" !in wsCols) {
                db.execSQL("ALTER TABLE written_statements ADD COLUMN linkedStatementsJson TEXT")
            }
        }
    }

    // Migration 32 → 33: Add answer/response columns to written_questions
    // for statement response integration (answerText, dateAnswered, etc.).
    // Also creates the missing division_votes memberId index that the seed DB
    // (built with an older schema JSON) may not have.
    // Idempotent — the seed DB may already have these columns/index.
    private val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val wqCols = db.query("PRAGMA table_info(written_questions)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(1))
                }
            }
            val newCols = listOf(
                "heading" to "TEXT NOT NULL DEFAULT ''",
                "dateForAnswer" to "TEXT NOT NULL DEFAULT ''",
                "dateAnswered" to "TEXT NOT NULL DEFAULT ''",
                "answerText" to "TEXT NOT NULL DEFAULT ''",
                "answeringMemberId" to "INTEGER NOT NULL DEFAULT 0",
                "isWithdrawn" to "INTEGER NOT NULL DEFAULT 0",
                "answerIsHolding" to "INTEGER NOT NULL DEFAULT 0",
                "answerIsCorrection" to "INTEGER NOT NULL DEFAULT 0"
            )
            for ((colName, colDef) in newCols) {
                if (colName !in wqCols) {
                    db.execSQL("ALTER TABLE written_questions ADD COLUMN $colName $colDef")
                }
            }
            // Create division_votes index if missing (seed DB may lack it)
            val dvIndexExists = db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_division_votes_memberId'"
            ).use { it.moveToFirst() }
            if (!dvIndexExists) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_division_votes_memberId ON division_votes(memberId)")
            }
        }
    }

    // Migration 33 → 34: Add mp_career_events table for the Parliament Biography API
    // endpoint data (government posts, opposition posts, committees, representations,
    // party affiliations, house memberships) and Wikipedia/Wikidata career data.
    // Idempotent — the seed DB may already have this table.
    private val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_career_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mpId` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `name` TEXT,
                    `house` INTEGER,
                    `startDate` TEXT,
                    `endDate` TEXT,
                    `additionalInfo` TEXT,
                    `additionalInfoLink` TEXT,
                    `constituencyName` TEXT,
                    `constituencyId` INTEGER,
                    `source` TEXT NOT NULL,
                    `lastUpdated` INTEGER NOT NULL
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_mp_career_events_mpId ON mp_career_events(mpId)"
            )
        }
    }

    // Migration 34 → 35: Recreate mp_career_events without DEFAULT on source
    // column. The table was left in a bad state by destructive fallback on
    // prior builds where @ColumnInfo(defaultValue) was added then removed.
    private val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `mp_career_events`")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_career_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mpId` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `name` TEXT,
                    `house` INTEGER,
                    `startDate` TEXT,
                    `endDate` TEXT,
                    `additionalInfo` TEXT,
                    `additionalInfoLink` TEXT,
                    `constituencyName` TEXT,
                    `constituencyId` INTEGER,
                    `source` TEXT NOT NULL,
                    `lastUpdated` INTEGER NOT NULL
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_mp_career_events_mpId ON mp_career_events(mpId)"
            )
            // Seed DBs built at schema v33+ never ran the 26→27/32→33
            // migrations that create this index (the seed pipeline doesn't
            // create user indices) — create it here so validation passes.
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_division_votes_memberId ON division_votes(memberId)"
            )
        }
    }

    // v35 → v36: Companies House officer layer — matched officer identity
    // per MP + their full appointment history.
    private val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_officer_identity` (
                    `mpId` INTEGER NOT NULL PRIMARY KEY,
                    `officerId` TEXT NOT NULL,
                    `officerName` TEXT NOT NULL,
                    `dobMonth` INTEGER,
                    `dobYear` INTEGER,
                    `nationality` TEXT,
                    `countryOfResidence` TEXT,
                    `matchMethod` TEXT,
                    `matchConfidence` REAL,
                    `isDisqualified` INTEGER NOT NULL,
                    `disqualificationJson` TEXT,
                    `lastUpdated` INTEGER NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `mp_appointments` (
                    `mpId` INTEGER NOT NULL,
                    `companyNumber` TEXT NOT NULL,
                    `officerRole` TEXT NOT NULL,
                    `appointedOn` TEXT NOT NULL,
                    `officerId` TEXT,
                    `companyName` TEXT NOT NULL,
                    `resignedOn` TEXT,
                    `isCurrent` INTEGER NOT NULL,
                    `companyStatus` TEXT,
                    `companyType` TEXT,
                    `companySicCodes` TEXT,
                    `companyIncorporated` TEXT,
                    `pscNatures` TEXT,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`mpId`, `companyNumber`, `officerRole`, `appointedOn`)
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_mp_appointments_mpId ON mp_appointments(mpId)"
            )
        }
    }

    // v36 → v37: per-constituency election results (latest + history) with
    // per-candidate rows. member_details stream tables (D-01/D-03).
    private val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `constituency_elections` (
                    `constituencyId` INTEGER NOT NULL,
                    `electionId` INTEGER NOT NULL,
                    `result` TEXT,
                    `isNotional` INTEGER NOT NULL,
                    `electorate` INTEGER,
                    `turnout` INTEGER,
                    `majority` INTEGER,
                    `winningPartyId` INTEGER,
                    `winningPartyName` TEXT,
                    `winningPartyColour` TEXT,
                    `electionTitle` TEXT,
                    `electionDate` TEXT,
                    `isGeneralElection` INTEGER NOT NULL,
                    `constituencyName` TEXT,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`constituencyId`, `electionId`)
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_constituency_elections_constituencyId ON constituency_elections(constituencyId)"
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `constituency_election_candidates` (
                    `constituencyId` INTEGER NOT NULL,
                    `electionId` INTEGER NOT NULL,
                    `rankOrder` INTEGER NOT NULL,
                    `memberId` INTEGER,
                    `name` TEXT,
                    `partyId` INTEGER,
                    `partyName` TEXT,
                    `partyAbbreviation` TEXT,
                    `partyColour` TEXT,
                    `resultChange` TEXT,
                    `votes` INTEGER,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`constituencyId`, `electionId`, `rankOrder`)
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_constituency_election_candidates_memberId ON constituency_election_candidates(memberId)"
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
                MIGRATION_27_28,
                MIGRATION_28_29,
                MIGRATION_29_30,
                MIGRATION_30_31,
                MIGRATION_31_32,
                MIGRATION_32_33,
                MIGRATION_33_34,
                MIGRATION_34_35,
                MIGRATION_35_36,
                MIGRATION_36_37
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
    fun provideMpCareerEventDao(database: BundledDatabase): MpCareerEventDao = database.mpCareerEventDao()

    @Provides
    fun provideConstituencyElectionDao(database: BundledDatabase): ConstituencyElectionDao =
        database.constituencyElectionDao()

    @Provides
    fun provideMpOfficerIdentityDao(database: BundledDatabase): MpOfficerIdentityDao = database.mpOfficerIdentityDao()

    @Provides
    fun provideMpAppointmentDao(database: BundledDatabase): MpAppointmentDao = database.mpAppointmentDao()

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
        mpCareerEventDao: com.goveye.app.data.local.dao.MpCareerEventDao,
        constituencyElectionDao: ConstituencyElectionDao,
        mpExperienceDao: com.goveye.app.data.local.dao.MpExperienceDao,
        mpOfficerIdentityDao: MpOfficerIdentityDao,
        mpAppointmentDao: MpAppointmentDao
    ): MembersRepository = MembersRepository(
        mpDao,
        searchDao,
        membersApi,
        memberMapper,
        historicalMemberDao,
        mpSynopsisDao,
        mpContactDao,
        mpCareerEventDao,
        constituencyElectionDao,
        mpExperienceDao,
        mpOfficerIdentityDao,
        mpAppointmentDao
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
        bioDataDao: BioDataDao,
        cachedPublicationDao: com.goveye.app.data.local.dao.CachedPublicationDao,
        okHttpClient: okhttp3.OkHttpClient
    ): GovernmentAnnouncementsRepository = GovernmentAnnouncementsRepository(
        writtenStatementDao,
        governmentPublicationDao,
        legislationDao,
        announcementTagDao,
        mpTagDao,
        partyLeaderDao,
        sourceRecommendationDao,
        mpDao,
        bioDataDao,
        cachedPublicationDao,
        okHttpClient
    )
}
