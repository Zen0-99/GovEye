# Phase 9: Interests & Income — Discussion Log

**Date:** 2026-08-20
**Mode:** Default (interactive)
**SPEC.md:** Found — 5 requirements locked

## Areas Discussed

### 1. Parser Patterns

**Question:** Which parser approach should `build_interests.py` use?

**Options presented:**
- Two-tier (structured + regex) — Check InterestFieldDto.type first, then fall back to regex on fieldsJson
- Pure regex only — Regex on fieldsJson, ignore structured field types
- Structured only (no regex) — Use API's structured fields exclusively

**User selected:** Two-tier (structured + regex)

**Research context:**
- mySociety's parl_register_interests dataset already extracts monetary sums from free text using `extracted_sum` column (regex + basic NLP)
- ScrapingAnt hybrid approach: regex for candidate detection, ML for semantic extraction
- UK currency regex: `£\s?\d{1,3}(,\d{3})*(\.\d+)?\s*(million|billion|thousand|m|bn|k)?`

**Notes:** Two-tier matches mySociety's approach — structured fields give highest precision, regex catches the rest.

---

### 2. Bucket Mapping

**Question:** Where should the API category → high-level bucket mapping live?

**Options presented:**
- Build-side (Python) — Store `bucket` column in interests table, mapping in build script
- Android-side (Kotlin) — Store only API categoryNumber, mapping in Kotlin
- Both (redundant) — Store both bucket column and raw API category

**User selected:** Build-side (Python)

**Notes:** Mapping: 10 API categories → 6 buckets (1→Employment, 2→Financial Support, 3+4+5→Gifts, 6→Land/Property, 7→Shareholdings, 8+9+10→Other). Based on TheyWorkForYou's confirmed category structure.

---

### 3. UI Layout

**Question:** How should the Interests tab be laid out?

**Options presented:**
- Expandable bucket cards — 6 expandable cards in LazyColumn
- Summary chips + flat list — Chips at top, flat list below
- Dashboard grid + detail screen — 2-column grid of bucket cards, tap to navigate to detail

**User selected:** Other — "C, but also show a section at the top of the whole sum. In the same section there will be the sum of every month - navigatable in the same way as the vote map squares do - so it will show the sum gotten this month, with a red/green percentage and up/down arrow on the right showing if they earned less than previous month or more"

**Follow-up:** How should the monthly navigation work?

**Options presented:**
- Month chips + detail below — Horizontal row of month chips
- Arrow navigation (‹ Mar ›) — Left/right arrows with current month in center
- Scrollable month cards — Horizontal scroll of month cards

**User selected:** Arrow navigation (‹ Mar ›)

**Notes:** User clarified the vote map reference was about navigation aspect (yearly → monthly), not the visual squares. Date filter uses existing filter button + bottom sheet pattern (like directory search).

---

### 4. Schema Migration (Grilled)

**Method:** AI self-grilling protocol (batch mode)

**Facts established:**
- `BundledDatabase` is version 1, uses `fallbackToDestructiveMigration(dropAllTables = true)`
- Seed DB downloaded from `seed-latest` release on first launch or when stream is multiple versions behind
- `bundled_schema.json` is single source of truth for build scripts
- `validate_schema.py` checks identity hash in `room_master_table`
- `DatabaseUpdateDao` already has `upsertInterests` + `deleteInterest`

**Decisions grilled:**
- D1: Add columns to existing `interests` table (not new table) — **accepted**
- D2: Update `bundled_schema.json` to match Room schema v2 — **accepted**
- D3: Architecture is already scalable — no ground-up redesign needed — **accepted**
- D4: 6th patch stream follows exact same pattern as existing 5 — **accepted**

**User selected:** Accept grilling result

**Notes:** User's concern was "if the seed DB buckles under new schema, we must redesign for scalability." Grilling confirmed the Phase 10 architecture (fallbackToDestructiveMigration + seed DB swap + per-API patch streams) already handles this. Future datasources add new tables + streams without restructuring.

---

## Deferred Ideas

None — discussion stayed within phase scope.

---

*Discussion completed: 2026-08-20*
*All 4 areas resolved, ready for CONTEXT.md and planning.*
