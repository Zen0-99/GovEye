---
phase: 14-government-announcements
plan: 01
subsystem: python-build-scripts
tags: [python, build-scripts, gov-uk, parliament-api, legislation, ci-workflows, merge-dbs, schema]
key-files:
  - goveye-data/build_written_statements.py
  - goveye-data/build_gov_publications.py
  - goveye-data/build_legislation.py
  - goveye-data/merge_dbs.py
  - goveye-data/schema.py
  - goveye-data/schemas/bundled_schema.json
  - goveye-data/.github/workflows/update-gov-publications.yml
  - goveye-data/.github/workflows/update-written-statements.yml
  - goveye-data/.github/workflows/update-legislation.yml
  - goveye-data/.github/workflows/build-seed.yml
metrics:
  build_scripts_created: 3
  test_files_created: 3
  ci_workflows_created: 3
  ci_workflows_extended: 1
  files_extended: 2
  schema_version: "v20 (40 tables)"
  tests_passing: 28
  commits: 3
---

# Plan 14-01 Summary — Government Data Triad Build Scripts

## Objective

Create 3 Python build scripts for the government data triad (GOV.UK publications, Parliament Written Statements, legislation.gov.uk), extend merge_dbs.py and schema.py to merge the new per-API DBs, and create 3 CI workflows for daily updates. This plan produces the data that flows into the bundled DB via the patch streams registered in 14-03.

## Commits

| Commit | Task | Description |
|--------|------|-------------|
| `a2adc7e` | Task 1 (tracer) | Add written statements build script + extend merge_dbs/schema for gov data triad |
| `5b3626e` | Task 2 (auto) | Add GOV.UK publications + legislation build scripts with HTML stripping and D-03 body temp table |
| `bcc92af` | Task 3 (auto) | Add 3 government data CI workflows + extend build-seed.yml with gov triad merge |

## What Was Done

### Task 1 — Tracer: Written Statements build script + merge_dbs/schema extensions
- Created `build_written_statements.py` following the build_mps.py pattern exactly
  - Constants: STATEMENTS_API, TABLE_NAMES = ["written_statements"]
  - `fetch_written_statements(start_date, end_date)` — calls Parliament Written Statements API
  - `fetch_full_statement_text(statement_id)` — Pitfall 4: fetches full text when len(text)==255
  - `map_statement_to_entity(stmt, timestamp_millis)` — maps to WrittenStatementEntity fields
  - `insert_statements(conn, statements, timestamp_millis)` — INSERT OR REPLACE, batch with BATCH_SIZE
  - `build_seed(output_path, schema_path, days=90, checkpoint_db=None)` — last 90 days per D-02
  - `build_delta(output_path, previous_db, schema_path, days=90)` — copy + upsert
  - `main()` — argparse with --output, --schema, --mode, --previous-db, --checkpoint-db, --days
- Extended `merge_dbs.py`: added 3 new PER_API_TABLES entries (gov_publications_db, written_statements_db, legislation_db), 3 new function params, 3 new source_dbs entries, 3 new argparse args, --councils-db arg (was missing), and build-time temp table creation logic for _publication_bodies
- Extended `schema.py` API_TABLE_NAMES: added gov_publications, written_statements, legislation entries
- Copied v20 schema JSON (40 tables) from core/data/schemas to goveye-data/schemas/bundled_schema.json (14-03 re-export did not land in goveye-data)
- Created `test_build_written_statements.py` — 9 tests: DB creation, field mapping, INSERT OR REPLACE upsert, Pitfall 4 truncation handling (triggers full fetch), checkpoint resume, defaults

### Task 2 — GOV.UK Publications + Legislation build scripts
- Created `build_gov_publications.py` following build_mps.py pattern
  - Constants: GOVUK_SEARCH, GOVUK_CONTENT, TABLE_NAMES = ["government_publications"]
  - `fetch_organisation_slugs()` — enumerates ministerial departments via aggregate_organisations
  - `fetch_publications_for_org(org_slug, start_date, end_date)` — paginated Search API with filter_organisations + filter_public_timestamp (D-01: no document_type filtering)
  - `fetch_publication_details(path)` — Content API for full details (body, image)
  - `strip_html_for_tag_matching(html_text)` — BeautifulSoup HTML stripping (Pitfall 6)
  - `map_publication_to_entity(search_item, content_details, timestamp_millis, pub_id)` — D-03: body NOT in entity tuple
  - `insert_publications(conn, publications, bodies, timestamp_millis)` — inserts into government_publications + _publication_bodies temp table
  - `build_seed` / `build_delta` — two-step fetch (Search → Content), body stored in temp table for build_tags.py (14-02)
  - `main()` — argparse with --output, --schema, --mode, --previous-db, --checkpoint-db, --days
- Created `build_legislation.py` following build_mps.py pattern
  - Constants: LEGISLATION_NEW = "https://www.legislation.gov.uk/new/data.feed" (Pitfall 5: /new/ path), ATOM_NS, LEG_NS
  - `fetch_new_legislation(max_pages=20)` — Atom XML parsing with xml.etree.ElementTree, pagination via atom:link rel="next"
  - `map_legislation_to_entity(entry_data, timestamp_millis, leg_id)` — maps to LegislationEntity fields, URL falls back to entry id
  - `insert_legislation(conn, legislation, timestamp_millis)` — INSERT OR REPLACE, batch with BATCH_SIZE
  - `build_seed` / `build_delta` — fetch + insert + VACUUM
  - `main()` — argparse with --output, --schema, --mode, --previous-db, --checkpoint-db, --max-pages
- Created `test_build_gov_publications.py` — 10 tests: DB creation, HTML stripping (Pitfall 6), entity mapping, D-03 body discard, image_url extraction, seed build with mocked APIs, D-01 all document types
- Created `test_build_legislation.py` — 9 tests: DB creation, XML parsing, pagination via next link, max_pages limit, field mapping, seed build, delta upsert

### Task 3 — CI workflows + build-seed.yml extension
- Created `update-written-statements.yml` — cron 4x daily, tag written-statements-latest, diff tables written_statements, no recess check, no downstream trigger
- Created `update-gov-publications.yml` — cron 4x daily, tag gov-publications-latest, diff tables government_publications, no recess check, no downstream trigger
- Created `update-legislation.yml` — cron 4x daily, tag legislation-latest, diff tables legislation, no recess check, no downstream trigger
- Extended `build-seed.yml` at 6 extension points:
  1. Download manifests: 3 new gh release download commands
  2. check_seed.py call: 3 new --*-manifest args
  3. Download changed per-API DBs: 3 new case branches (gov_publications, written_statements, legislation)
  4. Verify all DBs present: 3 new DB names added to verification loop (19 total)
  5. Merge command: 3 new --*-db arguments
  6. Post-merge steps: placeholder steps for build_mp_tags.py, build_party_leaders.py, build_source_recs.py (14-02) + _publication_bodies temp table drop (D-03)
- All 4 YAML files validated with yaml.safe_load

## Deviations

- **Schema JSON copy**: The 14-03 plan summary claimed the v20 schema JSON was re-exported to goveye-data/schemas/bundled_schema.json, but the goveye-data repo was clean (no changes). The v20 schema existed in core/data/schemas/20.json but was never copied to goveye-data. This plan copied it as a prerequisite — without it, create_database_with_tables could not create the new tables (government_publications, written_statements, legislation).
- **_publication_bodies temp table**: D-03 requires body text for tag matching but not in shipped DB. The government_publications Room entity has no body column, so a separate `_publication_bodies` temp table was created in the per-API DB. merge_dbs.py was extended to create missing tables from source schema (generic solution for build-time temp tables). build-seed.yml drops this table before publishing (D-03 compliance).
- **merge_dbs.py --councils-db**: The councils_db entry existed in PER_API_TABLES but was missing from the argparse arguments and source_dbs dict. Fixed as part of this plan's merge_dbs.py extension.

## Pre-existing Issues (not caused by this plan)

- `test_merge_dbs.py::TestMergeDbs::test_merge_copies_data` fails because the test creates an interests DB with only the `interests` table, but merge_dbs.py tries to `SELECT id FROM mps` from it (placeholder MP copy logic). This failure exists on the clean main branch before this plan's changes.

## Self-Check

- [x] build_written_statements.py exists with fetch_written_statements, fetch_full_statement_text, map_statement_to_entity, insert_statements, build_seed, build_delta, main functions
- [x] build_gov_publications.py exists with fetch_organisation_slugs, fetch_publications_for_org, fetch_publication_details, strip_html_for_tag_matching, map_publication_to_entity, insert_publications, build_seed, build_delta, main functions
- [x] build_legislation.py exists with fetch_new_legislation, map_legislation_to_entity, insert_legislation, build_seed, build_delta, main functions
- [x] python build_written_statements.py --help shows --output, --schema, --mode, --previous-db, --checkpoint-db, --days arguments
- [x] python build_gov_publications.py --help shows --output, --schema, --mode, --previous-db, --checkpoint-db, --days arguments
- [x] python build_legislation.py --help shows --output, --schema, --mode, --previous-db, --checkpoint-db, --max-pages arguments
- [x] merge_dbs.py PER_API_TABLES contains gov_publications_db, written_statements_db, legislation_db keys
- [x] merge_dbs.py accepts --gov-publications-db, --written-statements-db, --legislation-db arguments
- [x] schema.py API_TABLE_NAMES contains gov_publications, written_statements, legislation entries
- [x] GOV.UK Search API uses filter_organisations + filter_public_timestamp (not filter_content_store_document_type per D-01)
- [x] legislation.gov.uk uses /new/data.feed (not default feed per Pitfall 5)
- [x] Entity tuples do NOT contain body text field (D-03)
- [x] BeautifulSoup is imported and used for HTML stripping (Pitfall 6)
- [x] Statement with len(text)==255 triggers fetch_full_statement_text call (Pitfall 4)
- [x] 3 CI workflows exist and are valid YAML with cron, workflow_dispatch, permissions, setup-python 3.12, pip install, build seed/delta, diff_db.py, manifest.py, softprops/action-gh-release@v3
- [x] build-seed.yml merge command includes --gov-publications-db, --written-statements-db, --legislation-db
- [x] build-seed.yml download section has case branches for gov_publications, written_statements, legislation
- [x] All 28 Python tests pass (9 written_statements + 10 gov_publications + 9 legislation)

**Self-Check: PASSED**
