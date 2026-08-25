---
plan: 18-02
subsystem: android-data
tags: room, migration, entity, domain-model
key-files:
  - InterestEntity.kt (16 new String? fields)
  - Interest.kt (16 new String? fields)
  - InterestsRepository.kt (toDomain mapper updated)
  - DatabaseModule.kt (MIGRATION_23_24)
  - BundledDatabase.kt (version 24)
  - 24.json (Room-generated schema)
---

# Plan 18-02 Summary: Android entity + Room migration v23→v24

## What was done

- Added 16 nullable `String?` fields to `InterestEntity` and `Interest` domain model
- Updated `InterestsRepository.toDomain()` mapper to pass all 16 fields
- Added idempotent `MIGRATION_23_24` (checks `PRAGMA table_info(interests)` before each `ALTER TABLE`)
- Bumped `BundledDatabase` to version 24
- Room generated `24.json` schema — synced to `goveye-data/schemas/bundled_schema.json`
- Updated bundled `goveye.db` with parsed structured data

## Verification

- `:core:data:compileDebugKotlin` passes
- `:app:compileDebugKotlin` passes
- `24.json` generated with 30 fields on interests entity
