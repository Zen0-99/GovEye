# Phase 9: Interests & Income — Research

**Researched:** 2026-08-20
**Status:** Complete

## 1. Interests API Structure

### Endpoint
- **Base URL:** `https://interests-api.parliament.uk/api/v1/`
- **List endpoint:** `GET /Interests`
- **Query params:** `MemberId` (Int), `Take` (default 20), `Skip` (default 0), `SortOrder` (default `"PublishingDateDescending"`)
- **Response:** `InterestsResponse` with `items: List<InterestDto>`, `totalResults: Int`

### DTO Hierarchy (from `InterestDtos.kt`)
```
InterestsResponse
  └─ items: List<InterestDto>
       ├─ id: Int                          ← PK
       ├─ summary: String
       ├─ parentInterestId: Int?           ← for amended entries
       ├─ registrationDate: String?        ← ISO date
       ├─ publishedDate: String?           ← ISO date (used for date filter)
       ├─ category: InterestCategoryDto
       │    ├─ id: Int
       │    ├─ number: String              ← "1", "1.1", "1.2", "2", ... "10"
       │    ├─ name: String                ← "Employment and earnings", etc.
       │    └─ type: String
       ├─ member: InterestMemberDto?
       ├─ fields: List<InterestFieldDto>   ← the free-text data
       │    ├─ name: String
       │    ├─ type: String                ← field type hint
       │    ├─ value: JsonElement?         ← structured value (may be string, number, object)
       │    ├─ values: List<List<InterestFieldDto>>?  ← nested field groups
       │    ├─ typeInfo: InterestTypeInfoDto?
       │    │    └─ currencyCode: String?  ← e.g. "GBP"
       │    └─ description: String?
       └─ rectified: Boolean
```

### Key insight: `fields` is the rich data
The `fieldsJson` column in `InterestEntity` stores `json.encodeToString(List<InterestFieldDto>)`. The monetary parser needs to deserialize this JSON and check:
1. `field.typeInfo?.currencyCode` — structured currency hint
2. `field.value` — may be a string containing "£5,000" or a number
3. `field.name` — field name gives context (e.g. "Amount", "Hours", "Rate")

### Current Entity (`InterestEntity.kt`)
```kotlin
@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val summary: String,
    val categoryId: Int,
    val categoryNumber: String,    // "1", "1.1", "2", etc.
    val categoryName: String,      // "Employment and earnings"
    val registrationDate: String?,
    val publishedDate: String?,
    val rectified: Boolean,
    val fieldsJson: String,        // serialized List<InterestFieldDto>
    val lastUpdated: Long,
)
```

**Needs 3 new columns:** `parsedAmountPence: Long?`, `currencyCode: String?`, `bucket: String?`

### Current DAO (`InterestDao.kt`)
- `observeInterestsForMember(memberId): Flow<List<InterestEntity>>` — ordered by `publishedDate DESC`
- `upsertAll(interests: List<InterestEntity>)` — for patch application
- `getOldestTimestampForMember(memberId): Long?`

**Needs:** new query `observeInterestsForMemberInRange(memberId, fromDate, toDate)` for date filter.

### Current Repository (`InterestsRepository.kt`)
- Injects `InterestDao`, `InterestsApi`, `InterestMapper`
- Has `refresh(memberId)` that calls live API → **must be deleted**
- Has `CacheTtl.INTERESTS_MS` staleness check → **must be deleted**
- `observeInterestsForMember()` returns `RepositoryResult` with `SyncStatus.STALE` or `FRESH`

**Target:** Inject only `InterestDao`. No API, no mapper, no refresh, no staleness. `SyncStatus.FRESH` if data exists, `EMPTY` if not.

## 2. Build Script Pattern

### Reference: `build_mps.py` (and `build_commons_votes.py`)
Standard structure for all per-API build scripts:
```
1. Constants: BASE_URL, PAGE_SIZE, TABLE_NAMES
2. fetch_all_*(): paginate with skip/itemsPerPage, sleep API_DELAY between pages
3. map_*_to_entity(): map API DTO to DB row tuple
4. build_seed(): create fresh DB from schema, insert all rows
5. build_delta(): copy previous DB, upsert all rows (INSERT OR REPLACE)
6. argparse: --output, --schema, --mode seed|delta, --previous-db
```

### Shared helpers (`api_helper.py`)
- `api_get(url, params, timeout)` — requests.get with retry logic
- `API_DELAY` — sleep between API calls (rate limiting)
- `BATCH_SIZE` — batch insert size
- `logger` — standard logger

### Schema module (`schema.py`)
- Creates SQLite tables from `bundled_schema.json`
- Sets the Room identity hash in `room_master_table`
- `create_schema(conn, schema_path)` — creates all tables + triggers + hash

### `build_interests.py` specifics
- **API:** `GET https://interests-api.parliament.uk/api/v1/Interests?Take=20&Skip=N`
- **Pagination:** Need to fetch ALL interests for ALL MPs. The API requires `MemberId` param — so we need to:
  1. Fetch all MP IDs from `mps.db` (or the Members API)
  2. For each MP, paginate through their interests
  3. This is ~650 MPs × variable interests per MP
- **Seed mode:** Full fetch of all interests for all 650 MPs
- **Delta mode:** Re-fetch all interests, upsert (interests can be amended/rectified)
- **Tables:** `interests` only (no FTS needed for interests)

### `merge_dbs.py` update
Currently accepts 6 per-API DBs (mps, commons-votes, lords-votes, bills, committees, recess). Needs 7th: `--interests-db interests.db`.

### `bundled_schema.json` update
Must add 3 columns to the `interests` table definition and update the Room identity hash to match `BundledDatabase` v2.

## 3. Monetary Parser

### mySociety approach (confirmed via web research)
mySociety's `parl_register_interests` dataset has an `extracted_sum` column — "Semi-colon separated list of monetary values extracted from free text." They use basic NLP on the `free_text` field.

### Confirmed category codes (from mySociety dataset)
```
1   = Employment and earnings
1.1 = Employment and earnings - Ad hoc payments
1.2 = Employment and earnings - Ongoing paid employment
2   = Donations and other support (including loans) for activities as an MP
3   = Gifts, benefits and hospitality from UK sources
4   = Visits outside the UK
5   = Gifts from sources outside the UK
6   = Land and property (within or outside the UK)
7   = Shareholdings
8   = Miscellaneous
9   = Family members employed
10  = Family members engaged in third-party lobbying
```

### Two-tier parser design (D-01)
**Tier 1 — Structured fields:**
- Check `field.typeInfo?.currencyCode` — if present, the field has structured currency info
- Check `field.value` — if it's a JSON number, use directly. If it's a string, try Tier 2.
- Check `field.name` — "Amount", "Value", "Rate" fields are likely monetary

**Tier 2 — Regex on free text:**
Core pattern (based on mySociety's proven approach):
```python
# Base: £ followed by number with optional thousands separators
£\s?\d{1,3}(,\d{3})*(\.\d+)?

# Extensions:
# Ranges: £X to £Y, £X–£Y, £X-£Y
# Hourly rates: £X/hour, £X per hour, £X/hr
# Multipliers: £X million, £X bn, £Xk, £X,XXX
# No-amount: returns None (never fabricates)
```

### Parser function signature
```python
def parse_amount(fields_json: str) -> tuple[int | None, str | None]:
    """Extract monetary amount from fields JSON.
    
    Returns (parsed_amount_pence, currency_code) or (None, None).
    Amount is integer pence (e.g. £5,000 → 500000).
    Never fabricates — returns None for unparseable fields.
    """
```

### Edge cases (from SPEC.md)
- `£0` → 0 pence (valid)
- Negative amounts (`-£X`) → unlikely in register, but handle gracefully
- Large amounts (`£1.5 million`) → 150000000 pence
- Ranges (`£5,000 to £10,000`) → take the higher value (conservative)
- Hourly rates (`£50/hour`) → store the hourly rate, not annualized
- Multi-currency → store `currencyCode` from `typeInfo.currencyCode`
- No money mentioned → `None` (never fabricate)

## 4. Schema Migration

### Current state
- `BundledDatabase` version = 1, 13 entities (including `InterestEntity`)
- `fallbackToDestructiveMigration(dropAllTables = true)` in `DatabaseModule.kt`
- `bundled_schema.json` has the v1 schema with Room identity hash

### Migration plan (D-09, D-10)
1. Add 3 nullable columns to `InterestEntity`:
   ```kotlin
   val parsedAmountPence: Long? = null,
   val currencyCode: String? = null,
   val bucket: String? = null,
   ```
2. Bump `BundledDatabase` to version 2
3. `fallbackToDestructiveMigration` handles the transition — existing users get tables dropped and re-download seed DB (v2). User data in `LocalDatabase` is untouched.
4. Update `bundled_schema.json`:
   - Add 3 columns to `interests` table definition
   - Update Room identity hash (copy from Room's `exportSchema = true` output after building v2)
5. `validate_schema.py` will check the new hash

### Why destructive migration is OK here
- The seed DB is read-only (no user data in it)
- First-launch downloads the seed anyway
- The "buckling" scenario (schema change breaking seed) is handled by seed re-download
- User data (follows, notification prefs) lives in `LocalDatabase` — never affected

## 5. UI Patterns

### Profile tab structure (`MpProfileScreen.kt`)
```kotlin
private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
    VOTES("Votes"),
}
```
- Tab row + `HorizontalPager` with `rememberPagerState(pageCount = { ProfileTab.entries.size })`
- Each tab content is a composable function: `ProfileTabContent`, `CareerTabContent`, `CommitteesTabContent`, `VotesTabContent`
- **Add:** `INTERESTS("Interests")` after `VOTES`, and `InterestsTabContent(...)` composable

### ProfileViewModel (`MpProfileViewModel.kt`)
```kotlin
data class ProfileUiState(
    val mp: Mp? = null,
    val synopsis: String? = null,
    val contacts: List<Contact> = emptyList(),
    val committees: List<Committee> = emptyList(),
    val experiences: List<BiographyExperience> = emptyList(),
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    // ... notification flags ...
)
```
- **Add:** `interests: List<Interest> = emptyList()` to `ProfileUiState`
- **Add:** Inject `InterestsRepository` into `ProfileViewModel` constructor
- **Add:** `viewModelScope.launch { interestsRepository.observeInterestsForMember(memberId).collect { ... } }` in `loadProfile()`

### Filter bottom sheet (`FilterBottomSheet.kt`)
- Uses `ModalBottomSheet` with `rememberModalBottomSheetState()`
- Has `FilterTabType` enum to control which sections show
- **Reuse pattern:** Create a `DateRangeFilterBottomSheet` or extend `FilterBottomSheet` with a `DATE_RANGE` tab type
- Date selectors: use Material3 `DatePicker` or simple text fields with date picker dialogs

### Dashboard layout (D-06, D-07)
- Top section: total sum of all interests + monthly navigation (`‹ Mar 2025 ›`)
- 2-column grid of bucket summary cards (bucket icon + total)
- Tapping a card → detail screen showing entries grouped by API sub-category
- Monthly trend: `£X,XXX ▲ 15% vs Feb` (green▲/red▼)

## 6. 7th Patch Stream Integration

### Current state (after votes split — 6 streams)
- `DatabaseUpdateApi.kt`: 6 tag constants (MPS_TAG, COMMONS_VOTES_TAG, LORDS_VOTES_TAG, BILLS_TAG, COMMITTEES_TAG, RECESS_TAG)
- `DatabaseUpdateManager.kt`: 6-entry `streamTags` array, 6-entry `releases` array, `getLocalVersion` + `setStreamVersion` with 6 cases
- `DatabasePreferences.kt`: 6 version Flow + setter pairs
- `DatabaseUpdateDao.kt`: already has `upsertInterests` + `deleteInterest` (lines 96-99)

### Changes needed for 7th stream (interests)
1. **`DatabaseUpdateApi.kt`**: Add `INTERESTS_TAG = "interests-latest"`
2. **`DatabaseUpdateManager.kt`**:
   - Add `INTERESTS_TAG` to `streamTags` array (now 7 entries)
   - Update `releases` array size to 7
   - Add `interestsVersion` case to `getLocalVersion` and `setStreamVersion`
   - Add `"interests"` case to `applyUpserts` and `applyDeletes` (uses existing `upsertInterests`/`deleteInterest`)
3. **`DatabasePreferences.kt`**: Add `interestsVersion: Flow<Long?>` + `setInterestsVersion()` 
4. **`DatabaseUpdateWorker.kt`**: No change needed — the worker already triggers on any stream update

### Build-side workflow
- `update-interests.yml`: weekly cron (e.g. `0 6 * * 1` — Monday 6am), delta mode
- Release tag: `interests-latest`
- Assets: `interests.db`, `patch.json`, `manifest.json`

## 7. Risk Assessment

### High risk
- **Interests API volume:** Fetching all interests for all 650 MPs in seed mode could be slow. Each MP may have dozens of interests. ~650 MPs × 20/page × API_DELAY = could take 30-60 min. **Mitigation:** Set workflow timeout to 120 min (like Bills).
- **Parser accuracy:** The regex parser won't catch every format. SPEC.md requires 50% extraction rate. **Mitigation:** Start with the core £ pattern, add edge cases iteratively. Test against real API data samples.

### Medium risk
- **Room identity hash mismatch:** If `bundled_schema.json` doesn't exactly match Room's computed hash, `validate_schema.py` fails. **Mitigation:** Build the Android app first with `exportSchema = true`, copy the hash from the exported schema JSON.
- **Monthly trend UI:** The monthly navigation with percentage change requires grouping interests by month and computing deltas. This is UI logic, not DB logic. **Mitigation:** Keep it simple — group by `publishedDate` month, sum `parsedAmountPence`, compare to previous month.

### Low risk
- **Schema migration:** `fallbackToDestructiveMigration` makes this safe. Existing users re-download seed DB.
- **7th stream addition:** Purely additive. Existing 6 streams are unaffected.

## 8. Recommendations for Planner

### Task sequencing (dependencies)
1. **Schema first:** Add columns to `InterestEntity`, bump `BundledDatabase` to v2, update `bundled_schema.json`. Build and get the new identity hash.
2. **Build script:** Create `build_interests.py` with the parser. Test parser against sample API data.
3. **Merge + workflow:** Update `merge_dbs.py` for 7th DB. Create `update-interests.yml`. Update `build-seed.yml` for 7th DB.
4. **Repository rewrite:** Delete `InterestsApi.kt`, `InterestsApiModule.kt`, `InterestMapper.kt`. Rewrite `InterestsRepository.kt` to use DAO only. Add date-range query to `InterestDao`.
5. **7th patch stream:** Add `INTERESTS_TAG` to `DatabaseUpdateApi`, update `DatabaseUpdateManager` (7 streams), `DatabasePreferences` (7th version key).
6. **UI:** Add `INTERESTS` to `ProfileTab`, create `InterestsTabContent` composable, add interests to `ProfileUiState` + `ProfileViewModel`.
7. **Date filter:** Create date range filter component, wire to `InterestDao.observeInterestsForMemberInRange()`.
8. **Tests:** Python tests for parser + build script. Android tests for repository + ViewModel.

### Suggested plan splits
- **09-01:** Build-side (build_interests.py + parser + merge_dbs.py + workflow + schema JSON)
- **09-02:** Android data layer (entity migration + repository rewrite + 7th patch stream + DAO date query)
- **09-03:** Android UI (Interests tab + dashboard grid + monthly navigation + date filter)
- **09-04:** Tests (Python parser tests + Android repository/ViewModel tests)

---

*Phase: 09-Interests & Income*
*Research completed: 2026-08-20*
