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

