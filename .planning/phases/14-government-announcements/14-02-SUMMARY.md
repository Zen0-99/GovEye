---
phase: 14-government-announcements
plan: 02
subsystem: python-build-scripts
tags: [python, build-scripts, tags, mp-tags, party-leaders, source-recommendations, recency-weighting, ci-workflows]
key-files:
  - goveye-data/build_tags.py
  - goveye-data/build_mp_tags.py
  - goveye-data/build_party_leaders.py
  - goveye-data/build_source_recs.py
  - goveye-data/tests/test_build_mp_tags.py
  - goveye-data/tests/test_build_party_leaders.py
  - goveye-data/tests/test_build_source_recs.py
  - goveye-data/.github/workflows/build-seed.yml
metrics:
  build_scripts_created: 3
  build_scripts_extended: 1
  test_files_created: 3
  ci_workflows_extended: 1
  tag_functions_added: 3
  tag_thresholds_added: 3
  tests_passing: 37
  commits: 3
---

# Plan 14-02 Summary — Tag Extension + MP Tags + Party Leaders + Source Recommendations

## Objective

Extend the existing tag system (TAG_DICTIONARY, 26 tags) to government announcements (publications, statements, legislation) and create 3 new post-merge build scripts: build_mp_tags.py (MP tag aggregation with recency weighting), build_party_leaders.py (party leader identification from MNIS bio_data), and build_source_recs.py (hybrid tag→department recommendation mapping). Wire all post-merge scripts into build-seed.yml.

## Commits

| Commit | Task | Description |
|--------|------|-------------|
| `8a8ef4f` | Task 1 (tracer) | Extend build_tags.py for announcement tags (publications, statements, legislation) + D-03 body temp table join |
| `b77076a` | Task 2 (auto) | Add build_mp_tags.py — MP tag aggregation with recency-weighted hits from debate speeches (D-08, Pitfall 8) |
| `ce2ea36` | Task 3 (auto) | Add build_party_leaders.py + build_source_recs.py with hybrid tag→department mapping + wire post-merge scripts into build-seed.yml (D-06, D-07) |

## What Was Done

### Task 1 — Tracer: Extend build_tags.py for announcement tags
- Extended `build_tags.py` with 3 new functions following the `build_division_tags` pattern:
  - `build_publication_tags(conn)` — SELECTs from `government_publications` LEFT JOINed with `_publication_bodies` temp table (D-03: body text not in shipped entity, stored in build-time temp table). Strips HTML from body using `BeautifulSoup(body, "html.parser").get_text()` before matching (Pitfall 6). Combines title + summary + clean body, runs `count_pattern_hits` against TAG_DICTIONARY.
  - `build_statement_tags(conn)` — SELECTs from `written_statements`, combines title + text, runs pattern matching.
  - `build_legislation_tags(conn)` — SELECTs from `legislation`, combines title only (no body text in DB), runs pattern matching.
- Added 3 threshold constants: `PUBLICATION_TAG_THRESHOLD = 1`, `STATEMENT_TAG_THRESHOLD = 1`, `LEGISLATION_TAG_THRESHOLD = 1` (announcements have shorter text than debates, single mention is meaningful).
- Extended `TABLE_NAMES` with 4 new tag table names: `publication_tags`, `statement_tags`, `legislation_tags`, `mp_tags`.
- Imported `BeautifulSoup` from `bs4` at top of file.
- Updated `build_tag_metadata` to accept publication/statement/legislation tag rows and log counts (tag_metadata table schema is fixed at 4 columns by Room entity — counts are logged, not stored).
- Updated `main()` to clear and build all 3 new tag tables, with try/except for graceful degradation on old DBs without government data tables.
- Updated `TAGS_DEPENDENCIES` to include `gov_publications`, `written_statements`, `legislation` for delta-skip logic.
- Created `test_build_mp_tags.py` with 7 placeholder tests verifying build_tags.py importability and TAG_DICTIONARY integrity (26 entries).

### Task 2 — build_mp_tags.py with recency weighting (D-08)
- Created `build_mp_tags.py` following the `build_precompute.py` post-merge pattern:
  - `create_mp_tags_table(conn)` — CREATE TABLE IF NOT EXISTS `mp_tags` (memberId, tag, hitCount) with composite PK (memberId, tag), matching Room's MpTagEntity schema exactly.
  - `parse_date(date_string)` — parses ISO date strings (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS) to datetime.date using `datetime.date.fromisoformat(s[:10])`.
  - `build_mp_tags(conn)` — SELECTs debate_speeches JOINed with divisions on divisionId (Pitfall 8: debate_speeches has no date column, use division date via JOIN). Filters: memberId > 0 AND isIntervention = 0. For each speech: computes `recency_weight = math.exp(-days_ago / 365.0)` (D-08 exponential decay, half-life ~1 year). Aggregates `hit_count * recency_weight` per (memberId, tag) using `defaultdict(lambda: defaultdict(float))`.
  - `populate_mp_tags(conn, mp_tag_scores)` — converts float scores to int, INSERT OR REPLACE in batches with BATCH_SIZE.
  - `main()` — argparse with `--output` (required), `--schema` (optional). Checks file exists, connects, creates table, clears, builds, populates in try/finally block.
- Expanded `test_build_mp_tags.py` to 18 tests: TAG_DICTIONARY integrity (7), parse_date formats (5), recency weighting verification (recent > old for same tag), memberId=0 skip, isIntervention=1 skip, composite PK no-duplicates, main() --output required.

### Task 3 — build_party_leaders.py + build_source_recs.py + build-seed.yml
- Created `build_party_leaders.py` following `build_precompute.py` pattern:
  - `create_party_leaders_table(conn)` — CREATE TABLE IF NOT EXISTS `party_leaders` (partyId, memberId, title) with PK(partyId), matching Room's PartyLeaderEntity.
  - `LEADER_TITLES` — list of 10 leader title strings (Prime Minister, Leader of the Opposition, party leaders for Labour/Conservative/Lib Dem/SNP/DUP/Plaid Cymru/Green/Reform UK).
  - `HARDCODED_LEADERS` — fallback dict mapping partyId → (memberId, title) for 8 major parties (Labour=15, Conservative=4, Lib Dem=17, SNP=29, DUP=7, Plaid Cymru=22, Green=44, Reform UK=1036). Party IDs verified from build_party_stats.py.
  - `build_party_leaders(conn)` — SELECTs bio_data JOINed with mps (bio_data uses `mpId` and `postsJson`, not `memberId`/`governmentPosts` as plan stated — JOIN with mps on mpId=id to get partyId). Parses postsJson, checks if any post title matches LEADER_TITLES (case-insensitive contains). Applies HARDCODED_LEADERS fallback for parties without a bio_data leader.
  - `main()` — argparse with `--output` (required), `--schema` (optional).
- Created `build_source_recs.py` following `build_precompute.py` pattern:
  - `create_source_recs_table(conn)` — CREATE TABLE IF NOT EXISTS `source_recommendations` (tag, organisationSlug, organisationName, hitCount, isRecommended) with composite PK(tag, organisationSlug), matching Room's SourceRecommendationEntity.
  - `TAG_TO_DEPARTMENTS` — hardcoded base mapping for all 26 tags → lists of (organisationSlug, organisationName) tuples. Each tag maps to its relevant GOV.UK departments (e.g. Taxation → HMRC + Treasury, NHS → DHSC, Defence → MoD, Climate & Environment → DESNZ + DEFRA).
  - `build_source_recs(conn)` — hybrid approach (D-06): (1) inserts hardcoded base entries with isRecommended=1, (2) queries publication_tags JOINed with government_publications for data-driven hit counts per (tag, organisationSlug), (3) merges: departments with hit counts >= DATA_DRIVEN_THRESHOLD (3) that aren't in hardcoded mapping get isRecommended=1.
  - `main()` — argparse with `--output` (required), `--schema` (optional).
- Created `test_build_party_leaders.py` — 10 tests: LEADER_TITLES has ≥10 entries, HARDCODED_LEADERS has major parties, leader identified from bio_data, fallback used when no leader, case-insensitive match, invalid JSON skipped, main() --output required.
- Created `test_build_source_recs.py` — 9 tests: TAG_TO_DEPARTMENTS covers all 26 tags, departments are (slug, name) tuples, hardcoded departments isRecommended=1, data-driven high hits recommended, data-driven low hits not recommended, hit counts updated from publication_tags, main() --output required.
- Updated `build-seed.yml`: replaced the 3 placeholder steps (continue-on-error + if-file-exists guards) with direct script invocations with `if: success()` condition. Scripts run after build_tags.py and before the D-03 temp table drop.

## Deviations

- **bio_data schema mismatch**: The plan's action referenced `SELECT memberId, partyId, governmentPosts FROM bio_data`, but the actual bio_data table (defined by BioDataEntity in 14-03 and build_mnis.py) uses `mpId` (not `memberId`), `postsJson` (not `governmentPosts`), and has no `partyId` column. build_party_leaders.py was adapted to JOIN bio_data with mps on `bd.mpId = m.id` to get partyId, and parse `postsJson` (which contains both GovernmentPosts + OppositionPosts as a JSON array with `title`/`department`/`startDate`/`endDate` fields).
- **government_publications body column**: The plan's action referenced `SELECT id, title, summary, body FROM government_publications`, but government_publications has no `body` column (D-03: body text not shipped). Body text is in the `_publication_bodies` build-time temp table (created by build_gov_publications.py in 14-01). build_publication_tags LEFT JOINs government_publications with `_publication_bodies` on id to get the body for tag matching. This temp table is available at build time (dropped after post-merge scripts in build-seed.yml).
- **tag_metadata columns**: The plan said to update build_tag_metadata to include publication/statement/legislation tag counts "in the tag_metadata table", but the tag_metadata table schema is fixed at 4 columns (tag, description, divisionCount, billCount) by TagMetadataEntity (14-03). Adding columns would require a Room schema migration (out of scope for this Python-only plan). The counts are computed and logged instead. The app can COUNT them from the respective tag tables at runtime if needed.

## Self-Check

- [x] build_tags.py contains build_publication_tags, build_statement_tags, build_legislation_tags functions
- [x] PUBLICATION_TAG_THRESHOLD, STATEMENT_TAG_THRESHOLD, LEGISLATION_TAG_THRESHOLD constants exist (all = 1)
- [x] BeautifulSoup is imported and used in build_publication_tags for HTML stripping (Pitfall 6)
- [x] TABLE_NAMES includes "publication_tags", "statement_tags", "legislation_tags", "mp_tags"
- [x] main() calls all 3 new functions after build_division_tags and build_bill_tags
- [x] TAG_DICTIONARY has 26 entries (unchanged — no new tags added)
- [x] build_mp_tags.py exists with create_mp_tags_table, build_mp_tags, parse_date, populate_mp_tags, main functions
- [x] SQL query JOINs debate_speeches with divisions on divisionId (Pitfall 8)
- [x] Recency weighting uses math.exp(-days_ago / 365.0) (D-08)
- [x] Only memberId > 0 AND isIntervention = 0 speeches are processed
- [x] test_build_mp_tags.py passes: recent speech has higher hitCount than old speech for same tag, memberId=0 skipped, isIntervention=1 skipped
- [x] main() has --output required argument
- [x] build_party_leaders.py exists with create_party_leaders_table, build_party_leaders, main
- [x] LEADER_TITLES list contains 10 entries
- [x] Fallback HARDCODED_LEADERS dict exists with 8 major party entries
- [x] build_source_recs.py exists with create_source_recs_table, build_source_recs, main
- [x] TAG_TO_DEPARTMENTS dict has entries for all 26 tags
- [x] test_build_party_leaders.py passes (leader identified from bio_data, fallback works, case-insensitive match, invalid JSON skipped)
- [x] test_build_source_recs.py passes (hardcoded + data-driven recommendations, hit counts updated)
- [x] build-seed.yml has 3 new post-merge steps for build_mp_tags.py, build_party_leaders.py, build_source_recs.py and is valid YAML
- [x] All 37 Python tests pass (18 mp_tags + 10 party_leaders + 9 source_recs)

**Self-Check: PASSED**
