---
plan: 15-01
title: Written question text storage
status: complete
tasks_total: 14
tasks_completed: 14
---

# Plan 15-01 Summary: Written Question Text Storage

## What Was Built

Created a complete data pipeline for storing individual written questions (with full question text) from the Parliament Written Questions API. This replaces the old approach where `build_hansard.py` only stored per-MP question *counts* — now the actual question text, answering body, date tabled, and UIN are stored per question, enabling the Phase 15 MP activity feed to display question content. The build script uses the proven 255-char truncation fallback pattern from `build_written_statements.py`, filters questions to known MPs via mps.db, and integrates into the CI pipeline with a weekly update workflow and seed merge integration.

## Tasks Completed

- [x] 15-01-01: Created `goveye-data/build_written_questions.py` — paginated API fetch, 255-char truncation fallback, MP filtering, seed/delta modes
- [x] 15-01-02: Created `WrittenQuestionEntity.kt` — Room entity with 9 fields (id, memberId, uin, dateTabled, answeringBodyId, answeringBodyName, questionText, house, lastUpdated)
- [x] 15-01-03: Created `WrittenQuestionDao.kt` — DAO with observe/search/get queries plus activity feed queries (getByMemberId, getByMemberIdAndDateRange)
- [x] 15-01-04: `WrittenQuestionsRepository.kt` — already existed from prior commit 60243eb, verified correct content
- [x] 15-01-05: Synced `bundled_schema.json` to Room v23 export — includes written_questions table definition with 9 fields and PK
- [x] 15-01-06: `BundledDatabase.kt` — already had WrittenQuestionEntity + WrittenQuestionDao at version 23 (prior commit 60243eb + Phase 17 bumps)
- [x] 15-01-07: `DatabaseUpdateDao.kt` — already had upsertWrittenQuestions + deleteWrittenQuestion (prior commit 60243eb)
- [x] 15-01-08: `DatabaseUpdateApi.kt` — already had WRITTEN_QUESTIONS_TAG (prior commit 60243eb)
- [x] 15-01-09: `DatabasePreferences.kt` — already had writtenQuestionsVersion Flow, setter, and key (prior commit 60243eb)
- [x] 15-01-10: `DatabaseUpdateManager.kt` — already had all 7 written-questions entries in streamTags, applyTableChanges, perApiDbFileName, perApiTables, getLocalVersion, setStreamVersion (prior commit 60243eb)
- [x] 15-01-11: Updated `merge_dbs.py` — added written_questions_db to PER_API_TABLES, function signature, source_dbs dict, argparse, merge_dbs call
- [x] 15-01-12: Created `update-written-questions.yml` CI workflow — weekly Monday 7am UTC, seed/delta modes, diff patch, manifest, published to written-questions-latest release tag
- [x] 15-01-13: Updated `build-seed.yml` — manifest download, check_seed arg, hashFiles, case statement, verify list, merge arg, generate_seed_manifest arg
- [x] 15-01-14: Updated `check_seed.py` — added written_questions to PER_API list, --written-questions-manifest arg, manifest paths dict

## Files Modified

### GovEye (Android)
- `core/data/src/main/java/com/goveye/app/data/local/entity/WrittenQuestionEntity.kt`: Enhanced KDoc (entity already existed from commit 60243eb)
- `core/data/src/main/java/com/goveye/app/data/local/dao/WrittenQuestionDao.kt`: Formatting improvements (DAO already existed from commit 60243eb)
- `core/data/src/main/java/com/goveye/app/data/repo/WrittenQuestionsRepository.kt`: Already existed from commit 60243eb, no changes needed
- `core/data/src/main/java/com/goveye/app/data/local/BundledDatabase.kt`: Already had WrittenQuestionEntity at v23 (no changes needed)
- `core/data/src/main/java/com/goveye/app/data/local/dao/DatabaseUpdateDao.kt`: Already had upsert/delete (no changes needed)
- `core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateApi.kt`: Already had WRITTEN_QUESTIONS_TAG (no changes needed)
- `core/data/src/main/java/com/goveye/app/data/preference/DatabasePreferences.kt`: Already had writtenQuestionsVersion (no changes needed)
- `core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateManager.kt`: Already had all 7 written-questions entries (no changes needed)

### goveye-data (Python)
- `build_written_questions.py`: **NEW** — 321-line build script with fetch_full_question_text, fetch_written_questions (paginated), map_question_to_entity, insert_questions, build_seed, build_delta, main
- `schemas/bundled_schema.json`: Synced from Room v23 export (was v20) — includes written_questions table + all Phase 17 schema changes
- `merge_dbs.py`: Added written_questions_db to PER_API_TABLES, function signature, source_dbs, argparse, merge_dbs call
- `.github/workflows/update-written-questions.yml`: **NEW** — weekly CI workflow with seed/delta modes, diff patch, manifest, GitHub Releases publish
- `.github/workflows/build-seed.yml`: Added written-questions manifest download, check_seed arg, hashFiles, case statement, verify list, merge arg, generate_seed_manifest arg
- `check_seed.py`: Added written_questions to PER_API list, --written-questions-manifest arg, manifest paths dict

## Decisions Made

- **Schema sync approach**: Instead of manually inserting the written_questions table definition into the v20 bundled_schema.json, copied the Room-exported v23 schema JSON directly. This brings the goveye-data schema fully up to date with all Phase 17 changes (v20→v23) and ensures the written_questions table definition matches the Room entity exactly.
- **DB version**: Plan specified bumping v20→v21, but the DB was already at v23 (Phase 17 added MIGRATION_22_23). The written_questions table was already added at v21 by prior commit 60243eb, and Phase 17 bumped to v23. No new migration was needed.
- **API pagination**: Written Questions API uses skip/take pagination (not date range like Written Statements API). Implemented with API_BATCH_SIZE=100 per page.
- **MP filtering**: Questions are filtered to only those whose askingMemberId is in the mps.db Commons MP set (house=1), matching the build_hansard.py pattern.

## Issues Encountered

- **Prior commit 60243eb**: A prior commit had already added the Android-side written_questions support (entity, DAO, repository, BundledDatabase, DatabaseUpdateDao, DatabaseUpdateApi, DatabasePreferences, DatabaseUpdateManager). This was discovered when task 15-01-04 (repository) had nothing to commit. The prior commit used v21, which Phase 17 later bumped to v23. The entity/DAO files were reformatted with improved KDoc and single-line query annotations, but the repository file was already identical to the plan's specification.
- **Schema version mismatch**: The goveye-data bundled_schema.json was at v20 while the Android BundledDatabase was at v23. Resolved by syncing the Room-exported v23 schema JSON to goveye-data.
