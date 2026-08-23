---
phase: 14-government-announcements
plan: 03
subsystem: android-data-layer
tags: [room, entities, daos, repository, patch-streams, schema]
key-files:
  - core/data/src/main/java/com/goveye/app/data/local/BundledDatabase.kt
  - core/data/src/main/java/com/goveye/app/data/local/LocalDatabase.kt
  - core/data/src/main/java/com/goveye/app/data/repo/GovernmentAnnouncementsRepository.kt
  - core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateApi.kt
  - core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateManager.kt
  - core/data/src/main/java/com/goveye/app/data/preference/DatabasePreferences.kt
  - app/src/main/java/com/goveye/app/di/DatabaseModule.kt
metrics:
  entities_created: 10
  daos_created: 8
  domain_models_created: 6
  db_version_bump: "19→20 (BundledDatabase), 1→2 (LocalDatabase)"
  patch_streams_added: 3
  files_modified: 5
  files_created: 24
---

# Plan 14-03 Summary — Android Data Layer for Government Announcements

## Objective

Add the Android data layer for government announcements: 9 new Room entities, 8 new DAOs, a DB-only GovernmentAnnouncementsRepository, BundledDatabase version bump (19→20), schema JSON re-export, 3 new patch streams in DatabaseUpdateManager, and a LocalDatabase cached_publications table for on-demand historical fetch (D-02).

## Commits

| Commit | Task | Description |
|--------|------|-------------|
| `06360b7` | Task 1 (tracer) | Add WrittenStatement entity + DAO + GovernmentAnnouncementsRepository (Room v20) |
| `b58b2b1` | Task 2 (auto) | Add 8 announcement entities + DAOs + TagDao UNION extension + LocalDatabase cached_publications (D-02) |
| `791c1e1` | Task 3 (auto) | Add 3 government announcement patch streams to DatabaseUpdateManager + DatabaseUpdateApi + DatabasePreferences |

## What Was Done

### Task 1 — Tracer: WrittenStatement data path
- Created `WrittenStatementEntity` (@Entity(tableName = "written_statements"), single PK `id`)
- Created `WrittenStatementDao` with observeStatements, observeStatementsByHouse, searchStatements, getStatement
- Created `WrittenStatement` domain model in core/domain
- Bumped BundledDatabase version 19→20, added entity + abstract DAO function
- Created `GovernmentAnnouncementsRepository` (@Singleton, DB-only, zero Retrofit/API imports)
- Added MIGRATION_19_20 to DatabaseModule (idempotent CREATE TABLE IF NOT EXISTS)
- Added DAO provider + repository provider to DatabaseModule
- Re-exported Room schema JSON (v20) to goveye-data/schemas/bundled_schema.json

### Task 2 — Remaining entities + DAOs + TagDao + LocalDatabase
- Created 8 new entities: GovernmentPublicationEntity, LegislationEntity, PublicationTagEntity, StatementTagEntity, LegislationTagEntity, MpTagEntity, PartyLeaderEntity, SourceRecommendationEntity
- Created CachedPublicationEntity for LocalDatabase (D-02 on-demand historical cache)
- Created 7 new DAOs: GovernmentPublicationDao, LegislationDao, AnnouncementTagDao, MpTagDao, PartyLeaderDao, SourceRecommendationDao, CachedPublicationDao
- Extended TagDao: getAllTags() UNION query now includes publication_tags, statement_tags, legislation_tags, mp_tags; added observeAllAnnouncementTags() and observeAllMpTags()
- Added all 8 new entities to BundledDatabase entities array (40 total)
- Added 6 new abstract DAO functions to BundledDatabase
- Added CachedPublicationEntity + CachedPublicationDao to LocalDatabase (version 1→2)
- Added LOCAL_MIGRATION_1_2 to DatabaseModule (preserves user follows data)
- Extended GovernmentAnnouncementsRepository with all 6 DAO injections + observe methods for publications, legislation, tags, MP tags, party leaders, source recommendations
- Created 5 new domain models: GovernmentPublication, Legislation, PartyLeader, SourceRecommendation, MpTag
- Added all new DAO providers + updated repository provider to DatabaseModule
- Re-exported schema JSON with all 9 new BundledDatabase tables + cached_publications

### Task 3 — Patch stream registration
- Added 3 new TAG constants to DatabaseUpdateApi: GOV_PUBLICATIONS_TAG, WRITTEN_STATEMENTS_TAG, LEGISLATION_TAG
- Added 3 new entries to DatabaseUpdateManager.streamTags (17 total: 14 existing + 3 new)
- Added 3 new perApiDbFileName cases: gov_publications.db, written_statements.db, legislation.db
- Added 3 new perApiTables cases: government_publications, written_statements, legislation
- Added 3 new getLocalVersion + setStreamVersion cases
- Added 3 new DataStore preferences to DatabasePreferences: govPublicationsVersion, writtenStatementsVersion, legislationVersion (Int, default null)

## Deviations

None.

## Pre-existing Issues (not caused by this plan)

- `:core:data:testDebugUnitTest` fails to compile due to pre-existing test file issues (FeedRepositoryTest, VotesRepositoryTest, DatabaseUpdateManagerTest reference constructors modified by prior uncommitted work). These test files were NOT modified by this plan. The `:core:data:compileDebugKotlin` task passes successfully.

## Self-Check

- [x] BundledDatabase version is 20 with 9 new entity classes registered (40 total)
- [x] WrittenStatementDao.observeStatements() returns Flow<List<WrittenStatementEntity>> ordered by dateMade DESC
- [x] GovernmentAnnouncementsRepository is DB-only with zero Retrofit/API imports
- [x] DatabaseUpdateManager.streamTags list contains gov-publications, written-statements, and legislation entries (17 total)
- [x] bundled_schema.json is re-exported from Room after adding new entities and matches the new identity hash
- [x] LocalDatabase contains a cached_publications table for on-demand historical publication cache (D-02)
- [x] TagDao.getAllTags() UNION query includes publication_tags, statement_tags, legislation_tags, and mp_tags
- [x] All new DAOs return Flow for observe methods and suspend for one-shot gets
- [x] All source compiles successfully (core:data, core:domain, app)
- [x] spotlessApply passes

**Self-Check: PASSED**
