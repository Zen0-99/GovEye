# GovEye — Crash Log

## Crash 1: OutOfMemoryError during seed DB download
**Date:** 2025-08-22
**Symptom:** App crashed with `OutOfMemoryError: Failed to allocate a 8208 byte allocation` during seed DB download (557 MB).
**Root cause:** `HttpLoggingInterceptor` with `Level.BODY` was inherited by `dbDownloadClient` via `okHttpClient.newBuilder()`. The BODY logging level buffers the entire response body in memory for logging, which causes OOM on large file downloads.
**Fix:** `dbDownloadClient` in `NetworkModule.kt` is now built from scratch (not via `newBuilder()`) without any logging interceptor — only User-Agent header + 10-minute timeouts.
**File:** `app/src/main/java/com/goveye/app/di/NetworkModule.kt`

## Crash 2: App vanishes after seed DB download completes
**Date:** 2025-08-22
**Symptom:** Seed DB download succeeds (584 MB, ~42s), but app disappears from screen. Android restarts only the WorkManager service, not the Activity.
**Root cause:** `DatabaseDownloadWorker` called `Process.killProcess(Process.myPid())` after download completion to force a fresh Room instance (InvalidationTracker was broken from `database.close()`). However, `killProcess` kills the entire process and Android only restarts the WorkManager bound service — the Activity is force-removed with no saved state and not relaunched.
**Fix:** Replaced `Process.killProcess()` with `context.startActivity(Intent for MainActivity with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)`. This recreates the Activity and all ViewModels, which creates new Flow collectors that trigger Room to reopen with a fresh InvalidationTracker.
**File:** `app/src/main/java/com/goveye/app/work/DatabaseDownloadWorker.kt`

## CI Failures: 5 goveye-data GitHub Actions workflows failing
**Date:** 2026-08-24
**Symptom:** Build Seed, Update Gov Publications, Update Interests, Update Committees workflows all failing.
**Root causes:**
1. **check_seed.py + generate_seed_manifest.py**: Missing `--gov-publications-manifest`, `--written-statements-manifest`, `--legislation-manifest` args — Phase 14 added 3 new per-API DBs but the seed check/manifest scripts weren't updated to accept them.
2. **build_gov_publications.py**: gov.uk Search API's `aggregate_organisations=prefix` parameter now returns 422 — the API changed. Replaced with `/api/organisations` endpoint with pagination, filtering for Ministerial department format and non-superseded organisations.
3. **build_committees.py delta**: Previous DB had old schema without `purpose` column. `build_delta` copies previous DB and tries to insert into columns that don't exist.
4. **build_interests.py delta + build_historical_interests.py**: Previous DB / mps.db didn't have `interests` table. Same stale schema issue.
5. **build-seed.yml**: Cache miss when cache key changes (new manifests added) — only changed DBs downloaded, non-changed DBs missing from empty cache.
**Fix:** Added `ensure_schema()` to `schema.py` — creates missing tables/columns in existing DBs, updates room_master_table identity hash + user_version. Applied to all delta build functions. Added `download_if_missing` fallback in build-seed.yml. Replaced gov.uk API call with `/api/organisations` endpoint.
**Files:** `check_seed.py`, `generate_seed_manifest.py`, `schema.py`, `build_committees.py`, `build_interests.py`, `build_historical_interests.py`, `build_gov_publications.py`, `build_written_statements.py`, `build_legislation.py`, `.github/workflows/build-seed.yml`

## CI Failure: merge_interests.py missing
**Date:** 2026-08-24
**Symptom:** Update Interests workflow fails at "Merge live + historical interests" step with `can't open file 'merge_interests.py': No such file or directory`.
**Root cause:** The `update-interests.yml` workflow references `merge_interests.py` but the file was never created — it was part of the historical interests feature but the merge script was missing from the repo.
**Fix:** Created `merge_interests.py` — copies the live DB as base, attaches the historical DB via SQLite ATTACH, and uses `INSERT OR IGNORE` to add historical interests without overwriting live data (live data is more current/authoritative). VACUUMs the output.
**File:** `merge_interests.py`

## CI Failure: Interests seed build too slow + merge_interests WAL lock
**Date:** 2026-08-24
**Symptom:** Interests workflow fails — first with missing `merge_interests.py`, then with `database hist is locked` in the merge step. Seed mode fetches all 650 MPs from Parliament API (no "only changed" endpoint) which takes >45 min timeout.
**Root cause:** 1. `merge_interests.py` was never created (workflow referenced it). 2. ATTACH DATABASE on the historical DB failed because `build_historical_interests.py` left an uncheckpointed WAL file. 3. Seed mode is inherently slow (650 MPs × full interests fetch).
**Fix:** 1. Created `merge_interests.py`. 2. Checkpoint WAL before ATTACH. 3. Bootstrapped `interests-latest` release by extracting 4097 interests from the old seed DB (Aug 22) — future runs now use delta mode (copy previous + upsert) instead of seed mode.
**File:** `merge_interests.py`, `extract_interests.py` (temp, deleted)

## CI Failure: Gov Publications seed build exceeds 60-min free tier timeout
**Date:** 2026-08-24
**Symptom:** Gov Publications seed build cancelled at 45-min timeout, then at 90-min (exceeds GitHub free tier 60-min limit). Checkpoint resume existed but was ineffective — batched all publications in memory and only inserted at the end, so a timeout lost everything. On resume it re-fetched all organisations from scratch.
**Root cause:** 1. All publications collected in `all_publications` list, inserted only after all orgs fetched — timeout = zero progress saved. 2. No org-skip logic on resume — re-fetched already-processed orgs. 3. 90-min timeout exceeds free tier.
**Fix:** 1. Insert and commit per-organisation so checkpoint preserves progress. 2. On resume, skip orgs already in checkpoint via `SELECT DISTINCT organisationSlug`. 3. Set timeout to 58 min (free tier limit is 60, 2 min buffer). Multi-run completion via checkpoint/resume.
**File:** `build_gov_publications.py`, `.github/workflows/update-gov-publications.yml`

## CI Failure: Gov Publications imageUrl dict + slow seed build
**Date:** 2026-08-24
**Symptom:** Seed build fails with `Error binding parameter 10: type 'dict' is not supported` — imageUrl from GOV.UK Content API is a dict, not a string. Even after fix, seed build takes ~50 min (5766 publications × 0.2s delay per Content API fetch) — too slow for CI iteration.
**Root cause:** 1. Content API `details.image` field can be a dict `{url, alt_text, ...}` but `imageUrl` column is TEXT. 2. Seed build is inherently slow — 25 departments, 5766 publications, each needs a Content API detail fetch.
**Fix:** 1. Extract `url` from dict if `image` is a dict. 2. Built locally and published directly to `gov-publications-latest` release (same approach as Interests bootstrap). Future runs use delta mode (copy previous + upsert).
**File:** `build_gov_publications.py`

## CI Failure: Build Seed merge_dbs no such table: mps
**Date:** 2026-08-24
**Symptom:** Build Seed workflow fails at "Merge per-API DBs" step with `no such table: mps`.
**Root cause:** `merge_dbs.py` post-merge step tries `SELECT id FROM mps` on the interests DB, but the bootstrapped interests DB (extracted from old seed) only has the `interests` table, not `mps`. The CI-built interests DB has an `mps` table with placeholder MP records for former PMs, but our bootstrap didn't include it.
**Fix:** Check if `mps` table exists in interests DB before querying. Skip gracefully if not — the placeholder MP copy is optional (only needed for former PMs' financial interests).
**File:** `merge_dbs.py`

## Seed DB rebuilt locally with all Phase 14 tables
**Date:** 2026-08-24
**Summary:** Built full `goveye.db` locally from all per-API releases. 53 tables, 2.6M rows. Includes all Phase 14 tables: government_publications (5766), written_statements (20), legislation (400), interests (45359 with historical), mp_tags (32079), party_leaders (8), source_recommendations (37), publication_tags (10893), legislation_tags (141), statement_tags (12). Published to `seed-latest` release.

## Bug: check_seed.py and generate_seed_manifest.py missing Phase 14 APIs in PER_API list
**Date:** 2026-08-24
**Symptom:** The seed manifest was missing gov_publications, written_statements, and legislation hashes. check_seed.py would never detect when Phase 14 data changed, so the seed would never rebuild.
**Root cause:** The `PER_API` list in both scripts (which drives the hash comparison loop) had 16 entries but was missing the 3 Phase 14 APIs. The CLI args were wired up (`--gov-publications-manifest` etc.) but the loop never iterated over them.
**Fix:** Added `gov_publications`, `written_statements`, and `legislation` to the `PER_API` list in both `check_seed.py` and `generate_seed_manifest.py`.
**File:** `check_seed.py`, `generate_seed_manifest.py`

## Bug: Published seed had _publication_bodies temp table (not shipped per D-03)
**Date:** 2026-08-24
**Symptom:** First local seed publish included 5,259 rows of publication body text in `_publication_bodies` table. The CI workflow drops this table before publishing (D-03 — body text not shipped).
**Fix:** Dropped `_publication_bodies` and VACUUMed the DB. Re-published cleaned seed (saved ~42MB).

## Bug: Financial card logic leaps — 5 calculation/display errors
**Date:** 2026-08-25
**Symptom:** Financial totals for MPs with amended interests were inflated ~2x; monthly groupings showed phantom spikes; expense monthly buckets were broken; negative expense totals displayed poorly; entry counts included non-monetary entries.
**Root causes:**
1. **Duplicate interest entries inflate total:** Parliament API re-registers the same interest on amendment (same donor + amount + category). `sumOf { parsedAmountPence }` summed all entries including duplicates, doubling the displayed total for any MP who amended registrations.
2. **Monthly grouping used publishedDate not registrationDate:** `extractMonths()` and `sumPenceForMonth()` filtered by `publishedDate`, which can be much later than `registrationDate` due to re-publication. Old interests re-published in a later month created phantom spikes.
3. **Expense DD/MM/YYYY date parsing broken:** `claimDate` stored as DD/MM/YYYY but `loadExpenseEntries()` used `.take(7)` assuming YYYY-MM-DD, producing garbage month buckets like "31/03/2".
4. **Negative expense totals displayed as "£-X":** Refunds (negative amounts) produced "£-726.34" instead of "-£726.34".
5. **Entry count included non-monetary entries:** Property, shareholdings, and miscellaneous role entries (null `parsedAmountPence`) were counted in the total entry count, overstating monetary declarations.
**Fix:**
1. Added `deduplicateInterests()` — groups by (donorName/summary, parsedAmountPence, categoryNumber) and keeps only the latest entry per group.
2. Changed `extractMonths()` and `sumPenceForMonth()` to use `registrationDate` instead of `publishedDate`.
3. Added `normalizeDdMmYyyyToIso()` in `MpProfileViewModel` to convert DD/MM/YYYY → YYYY-MM-DD before grouping/filtering.
4. Updated `formatPence()` to use "-£" prefix for negative values.
5. Changed `totalEntryCount` to count only entries with non-null `parsedAmountPence`.
**Files:** `InterestsTabContent.kt`, `MpProfileViewModel.kt`

## Bug: Card expand/collapse animation jumped on close
**Date:** 2026-08-25
**Symptom:** Expanding a financial card animated smoothly, but collapsing it was instant (no animation).
**Root cause:** Used `animateContentSize()` which doesn't animate `AnimatedVisibility` exit — the content disappeared immediately. The vault's SyncStone convention specifies pure height morph (expandVertically/shrinkVertically) with no fade for card collapse.
**Fix:** Replaced `animateContentSize()` with `AnimatedVisibility(enter = expandVertically(), exit = shrinkVertically())` — pure height morph, no fade, per SyncStone convention.
**File:** `UnifiedFinancialCard.kt`

## Bug: Touch ripple showed hard edges (not rounded like card)
**Date:** 2026-08-25
**Symptom:** The default Android press overlay (ripple) on financial cards showed hard rectangular edges instead of following the card's rounded corners, and was too bright in dark mode.
**Root cause:** `.clickable()` was applied without `.clip(RoundedCornerShape())` before it, so the ripple drew as a rectangle extending beyond the card's rounded shape.
**Fix:** Added `.clip(RoundedCornerShape(Ndp))` before `.clickable()` on all financial card Surfaces so the ripple is clipped to the card's rounded shape.
**Files:** `UnifiedFinancialCard.kt`, `InterestsTabContent.kt` (BucketSummaryCard, ExpenseBucketSummaryCard)

## Bug: Financial card logic leaps — source DB fix (round 2)
**Date:** 2026-08-25
**Symptom:** App-side dedup didn't fix the total — user still saw £246k for Hannah Spencer. The fix needed to be at the source DB level, not in the app.
**Root cause:** The Parliament API re-registers the same interest on amendment, producing multiple entries with different `id`s but same donor+amount+category. `INSERT OR REPLACE` uses `id` as primary key, so all entries were kept. The app-side dedup was correct logic but the user wanted it fixed at the source.
**Fix:**
1. Ran `fix_interests_source.py` against `goveye.db` — deleted 7,362 duplicate interests (45,359 → 37,997). Hannah Spencer's total dropped from £246,123 to £123,644.
2. Shortened category names in the DB (e.g. "Donations and other support (including loans) for activities as an MP" → "Donations"). Full names stored in new `fullCategoryName` column.
3. Added dedup logic to `build_interests.py` `insert_interests()` for future builds — groups by (memberId, donorName/summary, parsedAmountPence, categoryNumber) and keeps latest by publishedDate.
4. Added `SHORT_CATEGORY_NAMES` mapping and `get_short_category_name()` to `build_interests.py`.
5. Pushed fixed DB to device via adb.
**Files:** `goveye-data/build_interests.py`, `goveye-data/goveye.db`

## Bug: Expandable detail showed everything in italics
**Date:** 2026-08-25
**Symptom:** When expanding a financial card, all content was rendered in italic plain text instead of structured bold-label: value fields.
**Root cause:** `formatInterestStructuredFields()` skipped `paymentDescription`, `visitPurpose`, and `organisationDescription` (assuming they were shown in the card's description line). But the description line is truncated to 2 lines, and for most interests these were the ONLY populated structured fields. So the structured fields list was empty, and the card fell back to `expandableContent` (the full summary) which was rendered as italic paragraphs.
**Fix:** Include `paymentDescription`, `visitPurpose`, and `organisationDescription` in the expandable fields list (with "Description" / "Purpose" labels). The card's description line still shows a truncated version; the expansion shows the full text.
**File:** `InterestStructuredFields.kt`

## Bug: Card collapse had extra height adjustment after animation
**Date:** 2026-08-25
**Symptom:** When collapsing a financial card, the animation finished and then the card height adjusted one more time (a small jump).
**Root cause:** A `Spacer(modifier = Modifier.size(4.dp))` was placed inside the `AnimatedVisibility` content, before the fields. When the animation finished and the content was removed, the spacer's height was removed too, causing the extra adjustment.
**Fix:** Removed the Spacer and extra padding from inside the `AnimatedVisibility`. The `Column` inside uses `Arrangement.spacedBy(4.dp)` for spacing, which handles it cleanly.
**File:** `UnifiedFinancialCard.kt`

## Crash: App crash on launch — Room migration failed for fullCategoryName + written_questions
**Date:** 2026-08-25
**Symptom:** App crashed on launch with `IllegalStateException: Migration didn't properly handle: interests` and `written_questions`.
**Root cause:** Two issues:
1. The source DB had a `fullCategoryName` column in `interests` (added by the dedup fix script) but the Room entity didn't declare it, and the migration didn't add it. Room saw an extra column and failed validation.
2. The `written_questions` table didn't exist in the source DB (it was added to the app entities but never built in goveye-data). Room expected the table but found nothing.
**Fix:**
1. Added `fullCategoryName` to `InterestEntity`, `Interest` domain model, and `InterestsRepository.toDomain()`.
2. Updated `MIGRATION_24_25` to add `fullCategoryName` to `interests` if missing (idempotent).
3. Updated `MIGRATION_24_25` to create `written_questions` table if it doesn't exist.
4. Updated source DB's `room_master_table` identity hash to match v25 schema (`dd8ec5c317bd490cc1934365b8772b26`) and set `user_version = 24` so the migration runs.
**Files:** `DatabaseModule.kt`, `InterestEntity.kt`, `Interest.kt`, `InterestsRepository.kt`, `goveye-data/goveye.db`

## Bug: Delta patch system never worked — 3 root causes
**Date:** 2026-08-25
**Symptom:** App detects patches are available (manifest version mismatch) and logs "Applying N patches" but patch application fails silently. Preferences never update to the new version.
**Root causes:**
1. **Room entities lacked @Serializable:** `applyTableChanges()` uses `json.decodeFromJsonElement<MpEntity>(it)` which requires kotlinx.serialization. The `kotlin.serialization` plugin was applied but none of the 45 @Entity-annotated data classes had the `@Serializable` annotation. Error: `Serializer for class 'MpEntity' is not found.`
2. **SQLite boolean 0/1 vs JSON true/false:** `diff_db.py` reads SQLite rows where Boolean columns (isActive, isDeferred, isAct, isDefeated, isTeller, isIntervention, isPreferred, isWebAddress, isRecommended, isMuted) are stored as INTEGER 0/1. The patch JSON contained `"isActive": 1` but kotlinx.serialization expects `"isActive": true` for Boolean fields. Error: `Failed to parse literal '1' as a boolean value at path: $.isActive`
3. **App reinstall clears DataStore preferences:** `installDebug` or `pm clear` wipes the DataStore protobuf file. On next launch, `seedVersion == null` → `isFirstLaunch() == true` → app forces a full 600MB redownload even though the DB file already exists at the correct schema version.
**Fix:**
1. Added `@Serializable` annotation + `import kotlinx.serialization.Serializable` to all 45 @Entity-annotated data classes across 42 files in `core/data/.../entity/`.
2. Fixed `diff_db.py` `get_table_rows()` to convert 0/1 to Python `bool` for columns whose name starts with `is`, so `json.dump` emits `true`/`false`.
3. Added reinstall recovery in `DatabaseUpdateManager.checkForUpdates()`: if `seedVersion == null` but the DB file exists, set `seedVersion = CURRENT_SEED_VERSION` and fall through to the patch check instead of forcing `NeedsFullDownload`. Same fix in `isFirstLaunch()`.
**Files:** `core/data/.../entity/*.kt` (42 files), `core/data/.../update/DatabaseUpdateManager.kt`, `goveye-data/diff_db.py`

## Crash: Feed scroll crash — duplicate LazyColumn keys
**Date:** 2026-08-25
**Symptom:** App crashed when scrolling to a certain point in the feed.
**Root cause:** `FeedItem.FinancialItem.id` was `listOf(memberId, amount, date).hashCode()`. Two expenses from the same MP with the same amount and date (e.g. two £0.00 claims on the same day) produced the same `id`, causing duplicate keys in the LazyColumn `key = { item -> "${item.typePrefix}-${item.id}" }` parameter. Compose throws `IllegalArgumentException: Key "financial-123" was already used` on duplicate keys.
**Fix:** Added `whoOrWhere` and `description` to the hashCode inputs for `FinancialItem`, and `speechText.take(50)` for `SpeechItem`, making the IDs unique even when multiple items share the same member/date/amount.
**Files:** `FeedUiState.kt`

## Bug: MP names black in dark mode on onboarding follow screen
**Date:** 2026-08-25
**Symptom:** MP names in the onboarding "Follow MPs" step appeared black in dark mode instead of white.
**Root cause:** The onboarding screen uses `.background(MaterialTheme.colorScheme.background)` instead of `Surface`, so `LocalContentColor` is never set to `onBackground`. The `Text` components for MP names in `RecommendedMpRow`, `MpListRowWithFollow`, and `PartyLeaderCard` had no explicit `color` parameter, so they defaulted to the unset `LocalContentColor` (black).
**Fix:** Added explicit `color = MaterialTheme.colorScheme.onBackground` to all MP name `Text` components in `MPsStep.kt`.
**Files:** `MPsStep.kt`

## Bug: Feed showing 2024/2025 MP financial activity
**Date:** 2026-08-25
**Symptom:** The feed showed MP interest entries from 2024 and 2025, mixed in with 2026 content.
**Root cause:** `interests.take(20)` and `expenses.take(20)` took the first 20 entries regardless of date — these could be historical entries from years ago.
**Fix:** Filtered interests and expenses to only include 2026 entries (`publishedDate.take(4) == "2026"` for interests, `claimDate.take(4) == "2026"` for expenses), and reduced the take limit from 20 to 10.
**Files:** `FeedViewModel.kt`

## Crash: Migration 25→26 — written_questions table missing from seed DB
**Date:** 2026-08-25
**Symptom:** App crashed on launch with `IllegalStateException: Migration didn't properly handle: written_questions`.
**Root cause:** The `written_questions` table was declared as a Room entity (`WrittenQuestionEntity`) but was never created in the seed DB (`goveye.db`). When the DB was upgraded from v25→v26 (to add `bodyText` to `legislation`), Room validated all registered entities and found the `written_questions` table had 0 columns.
**Fix:** Added `CREATE TABLE IF NOT EXISTS written_questions` to `MIGRATION_25_26` in `DatabaseModule.kt`, with the full column definitions matching `WrittenQuestionEntity`.
**Files:** `DatabaseModule.kt`, `BundledDatabase.kt` (version 25→26), `LegislationEntity.kt` (+bodyText), `Legislation.kt` (+bodyText), `GovernmentAnnouncementsRepository.kt` (mapping)

