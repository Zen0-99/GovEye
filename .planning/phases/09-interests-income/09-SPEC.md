# Phase 9: Interests & Income — Specification

**Created:** 2026-08-19
**Ambiguity score:** 0.143 (gate: ≤ 0.20)
**Requirements:** 5 locked

## Goal

MP profiles display the Register of Members' Financial Interests as a 6th patch stream in the bundled DB, with nested category display (high-level buckets → API native categories), build-side monetary amount parsing, and a date filter for historical interests.

## Background

The Interests API client (`InterestsApi.kt`), DTOs (`InterestDtos.kt`), DAO (`InterestDao.kt`), entity (`InterestEntity.kt`), mapper (`InterestMapper.kt`), and repository (`InterestsRepository.kt`) all exist from Phase 2. The `interests` table is in `BundledDatabase` (12 entities). However:

1. **No build script exists** — there is no `build_interests.py`. The interests table in the seed DB is empty and never patched. The `InterestsRepository.refresh()` method tries to upsert into BundledDB via live API, breaking the read-only invariant established in Phase 10 (D-10a).
2. **No UI exists** — the MP profile has 4 tabs (Profile, Career, Committees, Votes). No Interests tab is present. `ProfileViewModel` does not inject or use `InterestsRepository`.
3. **No categorization or totals** — the `InterestEntity` has `categoryName` and `categoryNumber` but no high-level bucket mapping. The `fieldsJson` is stored as raw JSON with no parsed monetary amounts.

Phase 10's hybrid 2-database architecture established the pattern: 5 per-API build scripts produce patch streams, merged into one `goveye.db`. Phase 9 extends this to a 6th stream for interests.

## Requirements

1. **6th patch stream**: `build_interests.py` fetches all interests from the Interests API with pagination (Take/Skip), parses monetary amounts from `fieldsJson` using regex/pattern matching, and stores raw `fieldsJson` + parsed amounts in `interests.db`. `update-interests.yml` runs weekly in delta mode (poll for diffs, not full rebuild). `merge_dbs.py` includes `interests.db` in the seed merge. `InterestsApi.kt` deleted from Android app. `InterestsRepository` rewritten to read from BundledDatabase only.
   - Current: No `build_interests.py` exists. `InterestsRepository.refresh()` calls live API and upserts into BundledDB (breaks read-only invariant). No `update-interests.yml` workflow. `merge_dbs.py` merges 5 DBs, not 6.
   - Target: `build_interests.py` builds `interests.db` with all interests + parsed amounts. `update-interests.yml` runs weekly in delta mode. `merge_dbs.py` merges 6 DBs. `InterestsApi.kt` deleted from Android. `InterestsRepository` reads from DB only (no API dependency).
   - Acceptance: `build_interests.py --output interests.db --schema schemas/bundled_schema.json --mode seed` produces a valid DB with interests data. `update-interests.yml` workflow exists and runs in delta mode. `merge_dbs.py --output goveye.db ... --interests-db interests.db` succeeds. `InterestsRepository` has no `InterestsApi` parameter. `./gradlew assembleDebug` passes.

2. **Interests tab on MP profile**: New "Interests" tab in `ProfileTab` enum (after Votes). Shows categorized interests with nested display — high-level summary buckets (Employment/Earnings, Financial Support, Shareholdings, Land/Property, Gifts, Other) containing the API's native categories as sub-groups. Empty state shown when MP has no interests.
   - Current: `ProfileTab` has 4 entries (PROFILE, CAREER, COMMITTEES, VOTES). No Interests tab. `ProfileViewModel` does not inject `InterestsRepository`. No interests UI components exist.
   - Target: `ProfileTab` has 5 entries (add INTERESTS). `ProfileViewModel` injects `InterestsRepository` and loads interests into `ProfileUiState`. New `InterestsTabContent` composable with nested category display. Empty state with "No registered interests" message.
   - Acceptance: MP profile shows 5 tabs. Interests tab renders for an MP with interests (categorized, nested). Interests tab shows empty state for an MP with 0 interests. `./gradlew assembleDebug` passes.

3. **Monetary totals per category**: Build-side parser in `build_interests.py` extracts amounts from `fieldsJson` using regex/pattern matching. Parsed amounts stored as integer pence in a new `parsedAmountPence` column (nullable). Currency code stored in `currencyCode` column. UI shows total amount per category where extractable ("£X,XXX (Y entries)"), count-only fallback where not ("Y entries"). Multi-currency handling via `InterestTypeInfoDto.currencyCode`.
   - Current: `InterestEntity` has no parsed amount column. `fieldsJson` is raw JSON only. No monetary parsing exists anywhere.
   - Target: `InterestEntity` has `parsedAmountPence: Long?` and `currencyCode: String?` columns. `build_interests.py` includes a `parse_amount()` function that extracts monetary amounts from free-text fields. UI shows per-category totals (sum of `parsedAmountPence` where non-null, grouped by `currencyCode`).
   - Acceptance: `build_interests.py` extracts amounts from at least 50% of fields with monetary data (validated against a test fixture). `InterestEntity` schema includes `parsedAmountPence` and `currencyCode`. UI displays "£X,XXX (Y entries)" for categories with parsed amounts, "Y entries" for categories without. Parser returns `None` for unparseable fields (never fabricates). `./gradlew assembleDebug` passes.

4. **Date filter for historical interests**: From/to date selector using the existing filter pattern from other screens (e.g. division browse). Filters interests by `publishedDate`. Inclusive on both ends (`from ≤ publishedDate ≤ to`). Empty state shown when no interests match the selected date range. Filter is per-MP (not persisted across profiles).
   - Current: No date filter exists for interests. `InterestDao.observeInterestsForMember()` returns all interests ordered by `publishedDate DESC` with no filtering.
   - Target: `InterestDao` has a new query `observeInterestsForMemberInRange(memberId, fromDate, toDate)` that filters by `publishedDate`. UI has a date range selector component. Filtered results update reactively via Flow.
   - Acceptance: Date filter with from=2024-01-01, to=2024-12-31 returns only interests with `publishedDate` in 2024. Date filter with from=2099-01-01 (future) returns empty state. Clearing the filter shows all interests. `./gradlew assembleDebug` passes.

5. **InterestsRepository rewrite**: Remove live API refresh method, remove `InterestsApi` dependency, remove `InterestMapper` dependency (mapping now done in build script). Read from BundledDatabase only via `InterestDao`. `observeInterestsForMember()` returns `RepositoryResult` from DB with `SyncStatus` based on DB presence (no staleness check since data is patch-updated). Add `observeInterestsForMemberInRange()` for the date filter.
   - Current: `InterestsRepository` injects `InterestsApi` and `InterestMapper`. Has `refresh(memberId)` method that calls API. Uses `CacheTtl.INTERESTS_MS` for staleness.
   - Target: `InterestsRepository` injects only `InterestDao`. No `refresh()` method. No `CacheTtl` staleness check. `SyncStatus` is `FRESH` if data exists, `EMPTY` if not.
   - Acceptance: `InterestsRepository` constructor has only `InterestDao` parameter. No `refresh()` method exists. No `CacheTtl` reference. `./gradlew assembleDebug` passes. Unit tests pass.

## Boundaries

**In scope:**
- `build_interests.py` — Python build script for interests data (seed + delta modes)
- `update-interests.yml` — GitHub Actions workflow (weekly, delta mode)
- `merge_dbs.py` update — include `interests.db` in seed merge (6th DB)
- `InterestEntity` schema migration — add `parsedAmountPence` + `currencyCode` columns
- `InterestsRepository` rewrite — remove API, read from DB only
- `InterestsApi.kt` deletion from Android app
- `InterestsApiModule.kt` deletion (Hilt DI module)
- `ProfileTab` — add INTERESTS tab
- `ProfileViewModel` — inject `InterestsRepository`, load interests
- `InterestsTabContent` composable — nested category display with totals
- Date filter component — from/to selector for historical interests
- `InterestDao` — add date-range query
- Build-side monetary parser — regex/pattern matching in `build_interests.py`
- High-level bucket mapping — map API categories to 6 buckets (Employment/Earnings, Financial Support, Shareholdings, Land/Property, Gifts, Other)
- Empty states — no interests, no matches for date filter
- Unit tests for parser, repository, ViewModel

**Out of scope:**
- IPSA expense claims — separate data source (CSV downloads, not Interests API). Deferred to Phase 11 (API Enrichment).
- Interest notifications — no polling worker for new interest registrations. Deferred to Phase 11.
- Cross-MP interest search — aggregation feature (e.g. "show all MPs with shareholdings in X"). Deferred to Phase 11.
- Export interests (CSV/PDF) — not in original scope, no user demand identified.
- Live API fallback for interests — interests are bundled data only, no live API calls from the Android app.

## Constraints

- **Build frequency**: Weekly (delta mode). The Register of Members' Financial Interests is updated periodically — MPs register new interests within 28 days. Weekly polling is sufficient.
- **Monetary precision**: Parsed amounts stored as integer pence (not floats) to avoid precision loss. Display as `£X,XXX.XX`.
- **Schema migration**: Adding `parsedAmountPence` and `currencyCode` to `InterestEntity` requires a Room migration (version bump from 1 to 2 for BundledDatabase). The `bundled_schema.json` must be updated to match.
- **API pagination**: The Interests API supports `Take`/`Skip` pagination. The build script must paginate through all results (totalResults can be large for active MPs).
- **Pattern matching**: The monetary parser uses regex patterns to extract amounts from free-text fields. It must handle common UK formats (£, GBP, "X pounds", ranges, hourly rates, dividends). Unparseable fields return `None` — never fabricate.
- **6th patch stream**: The `DatabaseUpdateManager` (from Phase 10) must be extended to check a 6th manifest and download/apply a 6th patch. `DatabasePreferences` needs a 6th version key.

## Acceptance Criteria

- [ ] `build_interests.py --output interests.db --schema schemas/bundled_schema.json --mode seed` produces a valid DB with interests data fetched from the Interests API
- [ ] `build_interests.py --mode delta --previous-db prev_interests.db` produces a correct diff patch
- [ ] `update-interests.yml` workflow exists, runs weekly, and publishes patches to a release tag
- [ ] `merge_dbs.py` includes `interests.db` in the seed merge (6 input DBs)
- [ ] `InterestsApi.kt` and `InterestsApiModule.kt` are deleted from the Android app
- [ ] `InterestsRepository` constructor has only `InterestDao` parameter (no `InterestsApi`, no `InterestMapper`)
- [ ] `InterestsRepository` has no `refresh()` method
- [ ] `InterestEntity` has `parsedAmountPence: Long?` and `currencyCode: String?` columns
- [ ] `BundledDatabase` schema version bumped to 2 with migration for new columns
- [ ] `bundled_schema.json` updated to match new schema (version 2)
- [ ] `DatabaseUpdateManager` checks 6th manifest (interests) and applies patches
- [ ] `DatabasePreferences` has 6th per-API version key (`interestsVersion`)
- [ ] MP profile shows 5 tabs (Profile, Career, Committees, Votes, Interests)
- [ ] Interests tab renders categorized interests with nested display (high-level buckets → API categories)
- [ ] Interests tab shows empty state ("No registered interests") for MP with 0 interests
- [ ] Per-category totals shown: "£X,XXX (Y entries)" where amounts parsed, "Y entries" where not
- [ ] Date filter (from/to selector) filters interests by `publishedDate` (inclusive on both ends)
- [ ] Date filter with no matches shows empty state
- [ ] Clearing date filter shows all interests
- [ ] `parse_amount()` in `build_interests.py` returns `None` for unparseable fields (never fabricates)
- [ ] Interests with `parsedAmountPence = null` are still displayed (count-only, never silently dropped)
- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew testDebugUnitTest :core:data:testDebugUnitTest` passes
- [ ] Python tests for `build_interests.py` and `parse_amount()` pass

## Edge Coverage

**Coverage:** 14/14 applicable edges resolved · 0 unresolved

| Category | Requirement | Status | Resolution / Reason |
|----------|-------------|--------|---------------------|
| adjacency | R1 | ✅ covered | diff_db.py handles interest entries by id — same id = update, new id = insert (existing pattern from Phase 10) |
| empty | R1 | ✅ covered | build_interests.py handles MPs with 0 interests without error (empty table is valid) |
| encoding | R1 | ✅ covered | fieldsJson stored as UTF-8 text in SQLite, no encoding issues (existing pattern) |
| ordering | R1 | ✅ covered | interests ordered by publishedDate DESC in DAO query (existing query) |
| idempotency | R1 | ✅ covered | running build_interests.py twice in delta mode produces an empty patch (no changes) |
| concurrency | R1 | ✅ covered | GitHub Action runs single-job; interrupted build leaves previous release tag intact |
| concurrency | R2 | ✅ covered | Interests tab renders from Flow, always shows current DB state (Room handles concurrent reads) |
| boundary | R3 | ✅ covered | parser handles 0 (£0), negative (-£X), and large amounts correctly; null for unparseable |
| precision | R3 | ✅ covered | monetary amounts stored as integer pence (not floats), displayed as £X.XX |
| adjacency | R4 | ✅ covered | date filter is inclusive on both ends (from ≤ publishedDate ≤ to) |
| empty | R4 | ✅ covered | empty state shown when no interests match the selected date range |
| ordering | R4 | ✅ covered | filtered interests ordered by publishedDate DESC (same as unfiltered) |
| idempotency | R5 | ✅ covered | repository is read-only (no refresh), no side effects on repeated calls |
| concurrency | R5 | ✅ covered | repository uses Flow, safe for concurrent observation (Room guarantee) |

## Prohibitions (must-NOT)

**Coverage:** 3/3 applicable prohibitions resolved · 0 unresolved

| Prohibition (must-NOT statement) | Requirement | Status | Verification / Reason |
|----------------------------------|-------------|--------|------------------------|
| MUST NOT fabricate monetary amounts — parser returns `None` for unparseable fields, never a guessed amount | R3 | resolved | verification: test — negative test: `parse_amount("no money here")` returns `None`; `parse_amount("£5,000")` returns `500000` |
| MUST NOT silently drop interests with unparseable amounts — they are displayed count-only | R3 | resolved | verification: test — negative test: interests with `parsedAmountPence = null` are returned by the DAO and rendered in UI |
| MUST NOT make live API calls for interests from the Android app — all data comes from BundledDatabase via patch streams | R5 | resolved | verification: test — negative test: `InterestsRepository` class has no `InterestsApi` import or parameter |

## Ambiguity Report

| Dimension          | Score | Min  | Status | Notes                                      |
|--------------------|-------|------|--------|--------------------------------------------|
| Goal Clarity       | 0.92  | 0.75 | ✓      | 6th patch stream, nested categories, parser |
| Boundary Clarity   | 0.90  | 0.70 | ✓      | 5 explicit out-of-scope items with reasons  |
| Constraint Clarity | 0.80  | 0.65 | ✓      | Weekly delta, pence precision, schema v2    |
| Acceptance Criteria| 0.75  | 0.70 | ✓      | 24 pass/fail criteria                       |
| **Ambiguity**      | 0.143 | ≤0.20| ✓      |                                             |

## Interview Log

| Round | Perspective      | Question summary                                           | Decision locked                                                                    |
|-------|------------------|------------------------------------------------------------|------------------------------------------------------------------------------------|
| 1     | Researcher       | Interests table in BundledDB but no build script — conflict | 6th patch stream: build_interests.py + update-interests.yml, interests become bundled data |
| 1     | Researcher       | All historical interests or current only?                  | All historical, with date filter (from/to selector) using existing filter pattern  |
| 2     | Simplifier       | Use API categories or map to fewer buckets?                | Both (nested): high-level buckets as summary cards, API categories as sub-groups   |
| 2     | Simplifier       | What kind of totals — counts, monetary, or none?           | Monetary totals where parseable, with build-side deciphering parser (pattern matching) |
| 3     | Boundary Keeper  | How often should interests patch stream run?               | Weekly, delta mode (poll for diffs, not full rebuild)                              |
| 3     | Boundary Keeper  | What's explicitly out of scope?                             | IPSA expenses, interest notifications, cross-MP search, export — all out (A/B/C to Phase 11) |

---

*Phase: 09-interests-income*
*Spec created: 2026-08-19*
*Next step: /gsd-discuss-phase 9 — implementation decisions (parser patterns, bucket mapping, UI layout, migration strategy)*
