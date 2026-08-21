# Phase 11 Plan Review Report

**Review Date:** 2026-08-21
**Reviewer:** GSD Plan Checker
**Phase:** 11 - Build-time Data Enrichment

## Executive Summary

**Overall Verdict:** APPROVE_WITH_REVISIONS

All 7 plans are well-structured and follow established patterns from Phase 10 and Phase 9. There is a **critical cross-plan issue** with database version coordination that must be resolved before implementation. Several plans have minor gaps in file completeness and validation specificity.

## Individual Plan Reviews

### 11-01: MNIS Biographical Data — PASS_WITH_NOTES
- DB version bumps to v5, conflicts with other plans. Need coordinated v9.
- Aligns with D-01 (full MNIS field set), D-02 (merge into timeline).

### 11-02: IPSA Expenses — PASS_WITH_NOTES
- DB version not specified (says "coordinate" but no final number).
- Cron inconsistency: task says weekly, risk says bi-weekly. IPSA publishes every 2 months → monthly is optimal.
- Aligns with D-09 (bucket mapping).

### 11-03: ParlParse Social Links — PASS_WITH_NOTES
- DB version not specified.
- Twitter icon uses `Icons.Outlined.AlternateEmail` (generic) — consider custom X logo vector.
- Aligns with D-10 (JSON only), D-11 (all social links).

### 11-04: Activity Score + Trait Radar — PASS
- No DB version bump needed (only adds repository, not tables).
- Performance mitigation (5-minute cache) is well-thought-out.

### 11-05: Party Manifestos + FTS — PASS_WITH_NOTES
- DB version not specified.
- FTS5 table intentionally not a Room entity — add clarifying note.
- Aligns with D-03 (monthly), D-04 (dual FTS4/FTS5), D-05 (results-only list).

### 11-06: Parties Tab + PartyView — PASS_WITH_NOTES
- DB version not specified.
- Logo download script ambiguous (separate or part of build_party_stats.py?).
- `partyLogoResId()` helper not assigned to a file.
- Aligns with D-06 (tinted cards + logos), D-07 (hybrid data), D-08 (Wikipedia description).

### 11-07: Manifesto FTS Search UI — PASS
- No DB version bump needed.
- FTS table names and column names match 11-05. ✅

## Cross-Plan Issues

### 1. Database Version Coordination (CRITICAL)
Plans 11-01, 11-02, 11-03, 11-05, 11-06 all add entities to BundledDatabase but none specify the final coordinated version.

**Resolution:** All plans specify final DB version as **v9** (current v4 + 5 new tables: bio_data, expenses, mp_links, party_manifestos, party_stats).

### 2. FTS Table Interface Consistency — VERIFIED ✅
11-05 creates `party_manifestos_fts4` and `party_manifestos_fts5` with column `manifestoText`. 11-07 queries the same. Consistent.

### 3. Party Logo Implementation Ambiguity
11-06 task 21 ambiguous about separate script vs. part of build_party_stats.py.

**Resolution:** Separate script `goveye-data/download_party_logos.py`.

### 4. Party Logo Helper Function Location
11-06 task 38 mentions `partyLogoResId()` but no file.

**Resolution:** `app/src/main/java/com/goveye/app/ui/utils/PartyLogoUtils.kt`.

### 5. IPSA Workflow Cron Inconsistency
11-02 task 31 sets weekly, risk says bi-weekly. IPSA publishes every 2 months.

**Resolution:** Monthly cron `0 8 1 * *`.

## Decision Alignment Summary

| Decision | Plan(s) | Status |
|----------|---------|--------|
| D-01: Full MNIS field set | 11-01 | ✅ |
| D-02: Merge MNIS posts into timeline | 11-01 | ✅ |
| D-03: Monthly manifesto workflow | 11-05 | ✅ |
| D-04: Dual FTS4/FTS5 | 11-05, 11-07 | ✅ |
| D-05: Results-only list | 11-07 | ✅ |
| D-06: Tinted cards + logos | 11-06 | ✅ |
| D-07: Hybrid data source | 11-06 | ✅ |
| D-08: Wikipedia description | 11-06 | ✅ |
| D-09: Bucketed IPSA categories | 11-02 | ✅ |
| D-10: Download JSON only | 11-03 | ✅ |
| D-11: All social links | 11-03 | ✅ |

## Recommended Fixes

### Critical
1. **DB version coordination:** All plans (11-01, 11-02, 11-03, 11-05, 11-06) specify final DB version as v9.

### Important
2. **11-02 cron:** Change weekly to monthly (`0 8 1 * *`).
3. **11-06 logo script:** Separate script `download_party_logos.py`.
4. **11-06 logo helper:** Add `PartyLogoUtils.kt` to Files to Create.

### Minor
5. **11-03 Twitter icon:** Consider custom X logo vector.
6. **11-05 FTS5 entity note:** Clarify FTS5 table is intentionally not a Room entity.

## REVIEW COMPLETE

**Verdict: APPROVE_WITH_REVISIONS**
