---
plan: 17-04
title: Historical member search fix
status: complete
tasks_total: 5
tasks_completed: 5
---

# Plan 17-04 Summary: Historical Member Search Fix

## What Was Built

Fixed five issues with historical member search (UAT issue #6): party names now display as proper-case abbreviations with correct colours instead of lowercase raw strings, historical members with a valid parliamentMemberId show Parliament API portrait photos, the search filter no longer drops members with null parliamentMemberId, stray trailing numbers are stripped from display names at build time, and the "crossbentch" misspelling is mapped to "Crossbench". A schema migration (v22 to v23) adds the new `partyAbbreviation` and `partyColourHex` columns to the `historical_members` table.

## Tasks Completed

- [x] 17-04-01: Added `normalize_party_name` and `clean_display_name` functions to build_historical_members.py, added `partyAbbreviation` and `partyColourHex` columns to CREATE TABLE/INSERT, and added migration ALTER TABLE statements for existing DBs
- [x] 17-04-02: Added `partyAbbreviation: String?` and `partyColourHex: String?` fields to HistoricalMemberEntity.kt
- [x] 17-04-03: Added MIGRATION_22_23 (additive ALTER TABLE) to DatabaseModule.kt, bumped BundledDatabase to v23, and fixed pre-existing StatsRepository bioDataDao wiring gap from plan 17-03
- [x] 17-04-04: Fixed toDomainMp to use partyAbbreviation/partyColourHex and Parliament API photo URL; fixed searchAllMembersFts filter to include historical members with null parliamentMemberId
- [x] 17-04-05: Ran SQL UPDATE directly against goveye.db to add columns and populate all 4067 rows with normalized party names, abbreviations, colours, and cleaned display names

## Files Modified

- `C:/Users/karol/goveye-data/build_historical_members.py`: Added `normalize_party_name()` (maps raw ParlParse party identifiers to proper-case name + abbreviation + colour hex), `clean_display_name()` (strips trailing numbers), `PARTY_NORMALIZATION` map, `partyAbbreviation`/`partyColourHex` columns in CREATE TABLE and INSERT, and migration ALTER TABLE statements
- `core/data/src/main/java/com/goveye/app/data/local/entity/HistoricalMemberEntity.kt`: Added `partyAbbreviation: String? = null` and `partyColourHex: String? = null` fields
- `core/data/src/main/java/com/goveye/app/data/local/BundledDatabase.kt`: Bumped version from 22 to 23
- `app/src/main/java/com/goveye/app/di/DatabaseModule.kt`: Added `MIGRATION_22_23` (idempotent ALTER TABLE for partyAbbreviation and partyColourHex), added to migration chain, and fixed `provideStatsRepository` to pass `bioDataDao` parameter (pre-existing gap from 17-03)
- `core/data/src/main/java/com/goveye/app/data/repo/MembersRepository.kt`: Updated `toDomainMp` to use `partyAbbreviation` for abbreviation, `partyColourHex` for colour (fallback "#808080"), Parliament API Portrait URL for `thumbnailUrl` when `parliamentMemberId` is not null; changed `searchAllMembersFts` filter from `parliamentMemberId != null && parliamentMemberId !in currentIds` to `parliamentMemberId == null || parliamentMemberId !in currentIds`
- `C:/Users/karol/goveye-data/goveye.db`: Added `partyAbbreviation` and `partyColourHex` columns; updated all 4067 rows with normalized party names (proper case), abbreviations, colour hex values, and cleaned display names (not git-tracked — `*.db` is gitignored)

## Decisions Made

- Used `partyColourHex` directly from the DB column (populated at build time) with fallback to "#808080" instead of importing `partyNameToColorHex` from `core/ui` — `core/data` does not depend on `core/ui`, avoiding a circular dependency
- Used Parliament API Portrait URL (`https://members-api.parliament.uk/api/Members/{id}/Portrait`) for historical member thumbnails instead of the `photo` BLOB column — simpler, consistent with current MPs, and avoids needing BLOB-to-Coil conversion on Android
- The `clean_display_name` function uses regex `re.sub(r'\s+\d+$', '', name)` to strip trailing numbers — applied at build time in the Python script and as a SQL UPDATE on the existing DB
- Fallback party normalization for unknown parties: title-case the raw string with hyphens replaced by spaces, derive abbreviation from first letters (max 4 chars), use "a0a0a0" (gray) as colour

## Issues Encountered

- **Pre-existing build failure from plan 17-03**: `provideStatsRepository` in DatabaseModule.kt was missing the `bioDataDao` parameter that 17-03 added to the `StatsRepository` constructor. Fixed by adding `bioDataDao: BioDataDao` to the provider function parameters and passing it to the constructor. This was necessary for `:app:compileDebugKotlin` to succeed.
- **"crossbentch" misspelling not found in current DB**: The goveye.db already had "crossbench" (correctly spelled). The `normalize_party_name` function still maps "crossbentch" to "Crossbench" for future-proofing.
- **No stray numbers in current DB displayName values**: The `displayName GLOB '*[0-9]'` query returned 0 rows. The `clean_display_name` function is still applied at build time for future data quality. The stray numbers the user saw may have been from FTS matching artifacts or a different data version.
- **goveye.db is not git-tracked**: `*.db` is in `.gitignore` in the goveye-data repo. The SQL UPDATE was applied directly to the local DB file. The Room migration (MIGRATION_22_23) handles the same schema change on user devices.
