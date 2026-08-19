# Phase 9: Interests & Income - Context

**Gathered:** 2026-08-20
**Status:** Ready for planning

<domain>
## Phase Boundary

MP profiles display the Register of Members' Financial Interests as a 6th patch stream in the bundled DB, with nested category display (high-level buckets → API native categories), build-side monetary amount parsing, and a date filter for historical interests.

</domain>

<spec_lock>
## Requirements (locked via SPEC.md)

**5 requirements are locked.** See `09-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `09-SPEC.md` before planning or implementing. Requirements are not duplicated here.

**In scope (from SPEC.md):**
- `build_interests.py` — Python build script for interests data (seed + delta modes)
- `update-interests.yml` — GitHub Actions workflow (weekly, delta mode)
- `merge_dbs.py` update — include `interests.db` in seed merge (6th DB)
- `InterestEntity` schema migration — add `parsedAmountPence` + `currencyCode` + `bucket` columns
- `InterestsRepository` rewrite — remove API, read from DB only
- `InterestsApi.kt` deletion from Android app
- `InterestsApiModule.kt` deletion (Hilt DI module)
- `ProfileTab` — add INTERESTS tab
- `ProfileViewModel` — inject `InterestsRepository`, load interests
- `InterestsTabContent` composable — nested category display with totals
- Date filter component — from/to selector for historical interests
- `InterestDao` — add date-range query
- Build-side monetary parser — regex/pattern matching in `build_interests.py`
- High-level bucket mapping — map API categories to 6 buckets
- Empty states — no interests, no matches for date filter
- Unit tests for parser, repository, ViewModel

**Out of scope (from SPEC.md):**
- IPSA expense claims — separate data source, deferred to Phase 11
- Interest notifications — no polling worker, deferred to Phase 11
- Cross-MP interest search — aggregation feature, deferred to Phase 11
- Export interests (CSV/PDF) — no user demand identified
- Live API fallback for interests — bundled data only

</spec_lock>

<decisions>
## Implementation Decisions

### Parser Patterns
- **D-01:** Two-tier parser in `build_interests.py` — check `InterestFieldDto.type` + `InterestTypeInfoDto.currencyCode` (structured fields) first, then fall back to regex on `fieldsJson` free text. — **Reversibility:** reversible — parser logic is isolated in a Python function, can be refined without schema changes.
- **D-02:** Regex patterns based on mySociety's proven approach (they already extract `extracted_sum` from UK register free text). Core pattern: `£\s?\d{1,3}(,\d{3})*(\.\d+)?\s*(million|billion|thousand|m|bn|k)?` with extensions for ranges (`£X to £Y`, `£X–£Y`), hourly rates (`£X/hour`, `£X per hour`), and no-amount fields (returns `None`, never fabricates). — **Reversibility:** reversible
- **D-03:** Parsed amounts stored as integer pence (`parsedAmountPence: Long?`) to avoid float precision loss. Currency code stored separately (`currencyCode: String?`). Multi-currency handled via `InterestTypeInfoDto.currencyCode` from the API's structured fields. — **Reversibility:** one-way — changing the storage type after data is built would require a full seed DB rebuild and schema migration.

### Bucket Mapping
- **D-04:** API category → high-level bucket mapping lives in `build_interests.py` (Python, build-side). The `interests` table stores a `bucket` column with the pre-computed bucket label. Android app reads the bucket column directly — no Kotlin mapping logic. — **Reversibility:** costly — changing the mapping requires rebuilding the interests DB and re-patching, but the Android app needs no changes.
- **D-05:** Mapping (10 API categories → 6 buckets):
  - Category 1 (Employment and earnings, incl. 1.1 ad hoc, 1.2 ongoing) → **Employment/Earnings**
  - Category 2 (Donations and other support) → **Financial Support**
  - Category 3 (Gifts, benefits from UK sources) → **Gifts**
  - Category 4 (Visits outside the UK) → **Gifts**
  - Category 5 (Gifts from sources outside the UK) → **Gifts**
  - Category 6 (Land and property) → **Land/Property**
  - Category 7 (Shareholdings) → **Shareholdings**
  - Category 8 (Miscellaneous) → **Other**
  - Category 9 (Family members employed) → **Other**
  - Category 10 (Family members engaged in third-party lobbying) → **Other**

### UI Layout
- **D-06:** Dashboard grid + detail screen layout. Top section shows total sum of all interests + monthly navigation. Below that, a 2-column grid of bucket summary cards (bucket icon + total). Tapping a card navigates to a detail screen showing the bucket's entries grouped by API sub-category. — **Reversibility:** reversible — UI layout is Compose code, can be restructured without DB changes.
- **D-07:** Monthly navigation uses left/right arrows with current month displayed in center (`‹ March 2025 ›`). Below the month selector: `£X,XXX ▲ 15% vs Feb` (green for increase, red for decrease, up/down arrow). The dashboard grid below filters to show only interests from the selected month. — **Reversibility:** reversible
- **D-08:** Date filter uses the existing filter button + bottom sheet pattern (same as directory search's `FilterBottomSheet`). Filter button in the top bar opens a bottom sheet with from/to date selectors. Filter is per-MP (not persisted across profiles). — **Reversibility:** reversible

### Schema Migration
- **D-09:** Add 3 nullable columns (`parsedAmountPence: Long?`, `currencyCode: String?`, `bucket: String?`) to the existing `interests` table. Bump `BundledDatabase` from version 1 to version 2. Do NOT create a new table — the existing table has the right PK, indexes, and DAO methods. — **Reversibility:** one-way — schema version bumps are permanent; existing users get `fallbackToDestructiveMigration` which drops all tables and re-downloads the seed DB (v2).
- **D-10:** Update `bundled_schema.json` in goveye-data to match the new Room schema (v2). The build scripts use this JSON to create tables. `validate_schema.py` checks the identity hash — the schema JSON must match Room's computed hash exactly. Copy the schema from Room's `exportSchema = true` output. — **Reversibility:** one-way — the identity hash is computed from the schema; once published, old DBs won't match.
- **D-11:** Add 6th patch stream (`interests-latest`) following the exact same pattern as the existing 5 streams. Add `INTERESTS_TAG` to `DatabaseUpdateApi`, add `streamTags` entry, add `getLocalVersion`/`setStreamVersion` cases, add `DatabasePreferences.interestsVersion`, add `applyUpserts`/`applyDeletes` cases for "interests" table. `DatabaseUpdateDao` already has `upsertInterests` and `deleteInterest` — patch infrastructure is ready. — **Reversibility:** reversible — adding a stream is additive; removing it would just skip the manifest check.
- **D-12:** No ground-up redesign needed. The Phase 10 architecture (`fallbackToDestructiveMigration` + seed DB swap + per-API patch streams) is already scalable. Future datasources add new tables + patch streams + schema version bumps following the same pattern. The "buckling" scenario (schema change breaking the seed DB) is handled by destructive migration + seed re-download. User data in `LocalDatabase` is never affected. — **Reversibility:** N/A — this is an architectural validation, not a change.

### Claude's Discretion
- Specific regex patterns for edge cases (dividends, "X hours at £Y/hour", multi-currency display formatting) — Claude can refine the parser based on real API data samples.
- Bucket icon selection in the dashboard grid — Claude can choose appropriate Material Icons.
- Color scheme for monthly trend (green/red) — follow existing app conventions.
- Empty state copy — Claude can draft, user will review.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 9 Specification
- `.planning/phases/09-interests-income/09-SPEC.md` — Locked requirements (5), boundaries, acceptance criteria, edge coverage, prohibitions. MUST read before planning.

### Phase 10 Architecture (prior phase — pattern source)
- `.planning/phases/10-polish-release/10-CONTEXT.md` — Hybrid 2-database architecture, 5 patch streams, DatabaseUpdateManager design.
- `core/data/src/main/java/com/goveye/app/data/local/BundledDatabase.kt` — 12-entity read-only DB, version 1, `fallbackToDestructiveMigration`.
- `core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateManager.kt` — 5-stream parallel manifest check, patch application, seed DB download.
- `core/data/src/main/java/com/goveye/app/data/update/DatabaseUpdateApi.kt` — 5 release tag constants (MPS_TAG, VOTES_TAG, etc.), GitHub Releases API.
- `core/data/src/main/java/com/goveye/app/data/local/dao/DatabaseUpdateDao.kt` — Already has `upsertInterests` + `deleteInterest` (lines 96-99).
- `app/src/main/java/com/goveye/app/di/DatabaseModule.kt` — `fallbackToDestructiveMigration(dropAllTables = true)` at line 51.

### Existing Interests Code (from Phase 2)
- `core/data/src/main/java/com/goveye/app/data/api/InterestsApi.kt` — Retrofit API interface (to be deleted).
- `core/data/src/main/java/com/goveye/app/data/di/InterestsApiModule.kt` — Hilt DI module (to be deleted).
- `core/data/src/main/java/com/goveye/app/data/local/dao/InterestDao.kt` — Room DAO with `observeInterestsForMember()`.
- `core/data/src/main/java/com/goveye/app/data/local/entity/InterestEntity.kt` — Entity (needs 3 new columns).
- `core/data/src/main/java/com/goveye/app/data/repository/InterestsRepository.kt` — Repository (to be rewritten, remove API).
- `core/data/src/main/java/com/goveye/app/data/mapper/InterestMapper.kt` — Mapper (to be deleted, mapping moves to build script).

### Build Scripts (goveye-data repo — pattern source)
- `goveye-data/build_mps.py` — Reference for per-API build script pattern (seed + delta modes).
- `goveye-data/merge_dbs.py` — Merges 5 DBs into seed; needs 6th (interests.db).
- `goveye-data/schemas/bundled_schema.json` — Schema JSON (must be updated to v2).
- `goveye-data/validate_schema.py` — Identity hash validation.
- `goveye-data/.github/workflows/update-mps.yml` — Reference for per-API workflow pattern.

### UI Patterns (existing — reuse)
- `app/src/main/java/com/goveye/app/ui/screens/mp/MpProfileScreen.kt` — 4-tab profile screen (add 5th tab).
- `app/src/main/java/com/goveye/app/ui/screens/directory/FilterBottomSheet.kt` — Filter bottom sheet pattern (reuse for date filter).
- `app/src/main/java/com/goveye/app/ui/screens/divisions/DivisionBrowseScreen.kt` — Filter button + house filter pattern.

### External Research
- mySociety parl_register_interests dataset — proven approach for extracting monetary sums from UK register free text (`extracted_sum` column, regex + basic NLP).
- TheyWorkForYou categories — confirmed 10 API categories with sub-categories (1.1, 1.2).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DatabaseUpdateDao.upsertInterests()` + `deleteInterest()` — already implemented, ready for 6th patch stream.
- `FilterBottomSheet` — reusable filter bottom sheet component, can be adapted for date range.
- `DatabaseUpdateManager` — 5-stream parallel check, adding a 6th is a copy-paste with table-specific changes.
- `DatabaseUpdateApi` — GitHub Releases API interface, just needs `INTERESTS_TAG` constant.
- `build_mps.py` — reference build script with seed/delta modes, pagination, schema validation.

### Established Patterns
- Per-API build script: `build_<api>.py --output <api>.db --schema schemas/bundled_schema.json --mode seed|delta`
- Per-API workflow: `update-<api>.yml` runs on schedule, builds DB, creates delta patch, publishes to `<api>-latest` release tag.
- Patch application: `DatabaseUpdateManager.applyPatches()` dispatches by table name to `DatabaseUpdateDao` upsert/delete methods.
- Schema validation: `validate_schema.py` checks identity hash in `room_master_table` against `bundled_schema.json`.
- Repository pattern: inject DAO only (no API), return `RepositoryResult` with `SyncStatus.FRESH` if data exists, `EMPTY` if not.

### Integration Points
- `BundledDatabase` entity list — add `InterestEntity` (already present, just needs new columns).
- `DatabaseModule.provideInterestDao()` — already wired, no changes needed.
- `ProfileTab` enum — add `INTERESTS` entry.
- `ProfileViewModel` — inject `InterestsRepository`, add interests to `ProfileUiState`.
- `MpProfileScreen` — add `InterestsTabContent` composable for the 5th tab.
- `DatabaseUpdateManager.streamTags` — add 6th entry.
- `DatabasePreferences` — add `interestsVersion` Flow + setter.
- `merge_dbs.py` — add `--interests-db` parameter, merge 6th DB.

</code_context>

<specifics>
## Specific Ideas

- Monthly trend navigation inspired by the vote map's yearly navigation — user wants monthly granularity with `‹ Mar 2025 ›` arrow navigation and percentage change vs previous month (green▲/red▼).
- Dashboard grid layout for bucket summary cards (2-column) — user explicitly chose this over expandable cards or flat list.
- Bucket mapping based on TheyWorkForYou's confirmed category structure (10 categories → 6 buckets).
- Parser based on mySociety's proven `extracted_sum` approach — they already do this for the UK register.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 09-Interests & Income*
*Context gathered: 2026-08-20*
