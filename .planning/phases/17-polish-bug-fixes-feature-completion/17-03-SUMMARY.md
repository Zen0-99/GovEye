---
plan: 17-03
title: Attendance rate + activity score tenure fix
status: complete
tasks_total: 4
tasks_completed: 4
---

# Plan 17-03 Summary: Attendance Rate + Zack Polanski

## What Was Built

Fixed the attendance rate (voteParticipationRate) calculation to use each MP's actual tenure start date instead of a fixed 2016 cutoff. New MPs like Hannah Spencer are no longer penalized for 9 years of divisions they couldn't vote in (her rate went from 0.048 to 0.794). Also corrected the Green Party leader fallback in build_party_leaders.py — the previous hardcoded memberId (5515) did not exist in the mps table.

## Tasks Completed

- [x] 17-03-01: Modified build_precompute.py to add `get_tenure_start` helper and make voteParticipationRate + rebellionRate tenure-aware (date-filtered voted_count, total_divisions, and rebellion queries)
- [x] 17-03-02: Fixed Green Party leader fallback in build_party_leaders.py HARDCODED_LEADERS — corrected broken memberId 5515 → 5320 (Adrian Ramsay, current Green MP), with comment noting Zack Polanski is the actual current leader but is not an MP (London Assembly Member)
- [x] 17-03-03: Updated StatsRepository.kt fallback path to be tenure-aware (uses maidenSpeechDate from BioDataDao, filters divisions by date), added new DAO queries (BioDataDao.getMaidenSpeechDate, DivisionDao.countDivisionsByHouseSince, DivisionDao.getDivisionIdsForMemberSince)
- [x] 17-03-04: Ran tenure-aware SQL UPDATE directly against goveye.db (per goveye-data AGENTS.md — no full build re-run), recomputed voteParticipationRate, activityScore, and participationPercentile for all 650 MPs; ran build_party_leaders.py to update party_leaders table

## Files Modified

### GovEye (Android)
- `core/data/src/main/java/com/goveye/app/data/repo/StatsRepository.kt`: Injected BioDataDao, added DEFAULT_TENURE_START constant, updated getVoteParticipationRate fallback to filter divisions by tenure start date
- `core/data/src/main/java/com/goveye/app/data/local/dao/BioDataDao.kt`: Added `getMaidenSpeechDate` query method
- `core/data/src/main/java/com/goveye/app/data/local/dao/DivisionDao.kt`: Added `countDivisionsByHouseSince` and `getDivisionIdsForMemberSince` query methods

### goveye-data (Python)
- `build_precompute.py`: Added `get_tenure_start` helper function (bio_data.maidenSpeechDate → historical_members.startDate → "2016-01-01" fallback), added DEFAULT_TENURE_START constant, updated compute_per_mp_metrics to filter voted_count, total_divisions, and rebellion queries by tenure start date
- `build_party_leaders.py`: Fixed Green Party (partyId 44) HARDCODED_LEADERS entry from broken memberId 5515 → 5320 (Adrian Ramsay), added comment explaining Zack Polanski is the current leader but is not an MP

### goveye.db (in-place, not git-tracked)
- `mp_stats` table: 650 rows updated with tenure-aware voteParticipationRate, activityScore, and participationPercentile
- `party_leaders` table: Green Party (44) now references MP 5320 (Adrian Ramsay) instead of non-existent 5515

## Decisions Made

- **Zack Polanski is not an MP**: He is a London Assembly Member (twfyPersonId 26031), not a Parliament MP. The party_leaders table references mps.id, so he cannot be directly added. The fallback was corrected to use Adrian Ramsay (5320, former co-leader and current Green MP) with a comment documenting the situation. When Zack Polanski becomes an MP, his memberId should replace 5320.
- **Previous hardcoded memberId 5515 was broken**: It did not exist in the mps table at all — the Green Party leader entry was silently pointing to a non-existent MP.
- **Direct SQL UPDATE per AGENTS.md**: The goveye.db was updated in-place via direct SQL (not by re-running build_precompute.py) since this was a derived-column logic change, not a source data change.

## Issues Encountered

- PowerShell does not support `&&` as a command separator — used `;` instead for chained git commands.
- Initial Python one-liner queries failed due to PowerShell escaping of `%` in LIKE patterns — switched to temporary script files for DB queries (cleaned up after use).
- Hannah Spencer's attendance rate improved dramatically (0.048 → 0.794) confirming the tenure fix works as intended. Diane Abbott's rate stayed at 0.644 — her maidenSpeechDate gives a tenure start well before 2016, and the divisions table data starts around 2016, so the denominator didn't change significantly for long-serving MPs.
