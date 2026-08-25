---
plan: 18-01
subsystem: data-pipeline
tags: python, parser, sqlite, interests
key-files:
  - build_interests.py (parse_interest_summary, map_interest_to_entity, insert_interests)
  - build_historical_interests.py (import + insert SQL)
  - migrate_interests_structured.py (new — derived-column migration script)
metrics:
  total_entries: 45359
  structured_entries: 29692
  coverage_pct: 65
---

# Plan 18-01 Summary: Python parser + DB schema for 16 structured interest fields

## What was done

Added `parse_interest_summary()` to `build_interests.py` with two-stage parsing:
- **Format B** (33% of entries): Multi-line text with `FieldName: Value` labels — extracts 16 structured fields via label→column mapping
- **Format A** (67%): Single-line text with category-specific regex patterns for cats 1-10
- **Fallback**: Unparseable entries get all-NULL structured fields, retain full summary

Updated `map_interest_to_entity()` and `insert_interests()` to 30-column tuples/SQL. Updated `build_historical_interests.py` to import and use the same parser.

Per goveye-data/AGENTS.md, wrote `migrate_interests_structured.py` instead of re-running the build script (derived column change — no API re-fetch needed). Ran against `goveye.db`: 65% coverage (29,692/45,359 entries with at least one structured field).

## Key decisions

- Used migration script instead of seed rebuild per AGENTS.md derived-column guidance
- Format A Cat 1/1.1/1.2/8 store full text as `paymentDescription` (no donor name extractable)
- Format B `Name:` label maps to `familyMemberName` (not `donorName`) for cats 9/10

## Verification

- 8 parser unit tests pass (Format A cats 2/4/6/7/9, Format B, unparseable, cat 1.1)
- `python -c "import build_interests"` succeeds
- `python -c "import build_historical_interests"` succeeds
- Spot-check across all categories returns correct structured data
