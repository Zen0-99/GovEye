# Phase 11: Build-time Data Enrichment - Research

**Date:** 2026-08-21
**Status:** Complete

## Existing Architecture (Phase 10/9 patterns)

### Per-API Build Script Pattern
All build scripts in `goveye-data/` follow the same pattern:
- **Seed mode**: Full historical fetch, checkpoint/resume support
- **Delta mode**: Copy previous DB, fetch only new records since max ID
- **Schema validation**: Build against `schemas/bundled_schema.json` (Room exportSchema output)
- **Output**: Per-API SQLite DB + patch.json + manifest.json, published to GitHub Releases
- **Reference files**: `build_mps.py`, `build_interests.py`, `build_commons_votes.py`, `api_helper.py`

### Patch Stream Pattern (Android)
`DatabaseUpdateManager.kt` (line 580) has `streamTags` list — each entry is `(releaseTag, streamName)`:
```kotlin
private val streamTags = listOf(
    DatabaseUpdateApi.MPS_TAG to "mps",
    DatabaseUpdateApi.COMMONS_VOTES_TAG to "commons-votes",
    DatabaseUpdateApi.LORDS_VOTES_TAG to "lords-votes",
    DatabaseUpdateApi.BILLS_TAG to "bills",
    DatabaseUpdateApi.COMMITTEES_TAG to "committees",
    DatabaseUpdateApi.RECESS_TAG to "recess",
    DatabaseUpdateApi.INTERESTS_TAG to "interests",
    DatabaseUpdateApi.DEBATES_TAG to "debates"
)
```
Phase 11 adds new entries for: manifestos, bio_data, expenses, mp_links, party_stats.

### Bucket Mapping Pattern (Phase 9)
`build_interests.py` maps raw API categories to high-level buckets:
- `BUCKET_MAPPING` dictionary maps category IDs to bucket labels
- `get_bucket()` function applies the mapping
- Bucket stored as a column in the DB — Android reads it directly, no Kotlin mapping logic

### Directory Screen
`DirectoryScreen.kt` (line 69-74) has `DirectoryTab` enum with OFFICIALS, PARTIES, BILLS, DIVISIONS.
Line 182: `DirectoryTab.PARTIES -> PlaceholderTabContent("Parties")` — currently a placeholder.
`DirectoryViewModel.kt` already has `distinctParties` flow for the filter bottom sheet.

## Plan-by-Plan Research

### 11-01: MNIS Biographical Data

**API**: MNIS (Members' Names Information System) at `data.parliament.uk/membersdataplatform/services/mnis/members/query/`
- XML format (JSON available via Accept header)
- Endpoint: `memberquery.aspx` with output parameters
- Output params: `FullBiog`, `Committees`, `GovernmentPosts`, `Honours`, `MaidenSpeeches`
- Example URL: `http://data.parliament.uk/membersdataplatform/services/mnis/members/query/ids={ids}/FullBiog|Committees|GovernmentPosts|Honours|MaidenSpeeches`

**Data fields available**:
- Maiden speech date
- Government posts (title, dept, start/end dates)
- Opposition posts (same structure)
- Honours (title, date)
- Date of birth
- Committee memberships + chairing (committee name, start/end dates, chair flag)
- Town/country of birth

**Build script**: `build_mnis.py`
- Fetch all 650 MPs by ID (batch in groups of 50, API_DELAY between batches)
- Parse XML using `xml.etree.ElementTree` (stdlib, no extra deps)
- Store in `bio_data` table: `mpId, maidenSpeechDate, dateOfBirth, townOfBirth, countryOfBirth, honoursJson, postsJson, committeesJson`
- Posts and honours stored as JSON arrays for flexible timeline rendering
- Seed mode: fetch all 650 MPs. Delta mode: fetch only new/changed MPs (compare lastUpdated)

**Android integration**:
- New `BioDataEntity` in BundledDatabase, bump DB version
- `BioDataDao` with `getByMpId()` query
- `BioDataRepository` (DB-only, no API)
- Career timeline on MP profile merges parliamentary roles + MNIS posts chronologically
- D-02: Merge into existing timeline, no separate bio section

**Risks**: MNIS API can be slow/unreliable. Batch with retries. Cache aggressively.

### 11-02: IPSA Expenses

**Data source**: IPSA (Independent Parliamentary Standards Authority) publishes expense claims as CSV.
- URL: `https://www.theipsa.org.uk/mp-costs/annual-claims/` (or published data CSV download)
- CSV format: MP name, constituency, category, claim amount, date, status
- Categories: Staffing, Office Costs, Accommodation/Travel, Stationery, Communications, Equipment, Staffing Travel, Family Travel
- Published every 2 months

**Bucket mapping** (D-09):
```python
IPSA_BUCKET_MAPPING = {
    "Staffing": "Staffing",
    "Office Costs": "Office",
    "Accommodation/Travel": "Travel",
    "Stationery": "Office",
    "Communications": "Office",
    "Equipment": "Office",
    "Staffing Travel": "Travel",
    "Family Travel": "Travel",
    # Fallback
    "Other": "Other"
}
```

**Build script**: `build_ipsa.py`
- Download CSV from IPSA website
- Parse with Python `csv` module
- Map MP names to MP IDs (name matching against mps.db — same pattern as build_debates.py speaker matching)
- Store in `expenses` table: `id, mpId, category, bucket, amountPence, claimDate, status`
- Amount stored as integer pence (same as Phase 9 D-03)

**Android integration**:
- New `ExpenseEntity` in BundledDatabase
- `ExpenseDao` with `getByMpId()` query
- Expense cards displayed in Interests tab below existing interests
- Reuse the bucket card pattern from Interests tab

**Risks**: MP name matching may have edge cases (name changes, typos). Use fuzzy matching fallback.

### 11-03: ParlParse Enrichment

**Data source**: ParlParse `people.json` — Popolo format JSON
- Download URL: `https://parser.theyworkforyou.com/people.json` (or GitHub raw URL)
- D-10: Download JSON only, no git clone
- ~5-10MB file, contains all MPs/Lords

**JSON structure** (Popolo format):
```json
{
  "persons": [
    {
      "id": "uk.org.publicwhip/person/12345",
      "name": "John Smith",
      "identifiers": [
        { "scheme": "twitter", "identifier": "johnsmith" },
        { "scheme": "facebook", "identifier": "johnsmith.mp" },
        { "scheme": "wikipedia", "identifier": "John_Smith_(politician)" }
      ],
      "links": [
        { "url": "https://twitter.com/johnsmith", "note": "twitter" },
        { "url": "https://en.wikipedia.org/wiki/John_Smith", "note": "wikipedia" },
        { "url": "https://johnsmith.org.uk", "note": "personal" }
      ]
    }
  ]
}
```

**Build script**: `build_parlparse.py`
- Download `people.json`
- Parse JSON, extract identifiers and links per person
- Match to MP IDs using ParlParse person ID (already stored in mps.db if available, or match by name)
- Store in `mp_links` table: `mpId, twitterHandle, facebookUrl, instagramUrl, linkedinUrl, wikipediaUrl, personalWebsiteUrl`

**Android integration**:
- New `MpLinkEntity` in BundledDatabase
- `MpLinkDao` with `getByMpId()` query
- Social links icon row on MP profile (D-11: all social links)
- Icons: Twitter/X, Facebook, Instagram, LinkedIn, Wikipedia, Web

**Risks**: ParlParse person ID may not match Parliament MP ID. Need a mapping step (name-based matching as fallback).

### 11-04: Activity Score + Trait Radar

**Existing calculators** (all pure functions, no DI):

`ActivityScoreCalculator.kt`:
- Input: `voteParticipationRate: Float, questionCount: Int, speechCount: Int, committeeCount: Int, peerAverages: PeerAverages`
- Output: `ActivityScore(score: Int, breakdown: ScoreBreakdown)`
- Weights: Vote 40%, Questions 20%, Speeches 20%, Committees 20%
- `normalize()` function: 2× average = full marks

`TraitBarCalculator.kt`:
- Input: MP's 5 metrics + peer value lists for each metric + peer averages
- Output: `List<TraitBar>` with label, percentile, mpValue, peerAverage
- 5 bars: Rebellion, Participation, Questions, Speeches, Committees
- Uses `PercentileCalculator.computePercentile()`

`PercentileCalculator.kt`:
- Input: value + list of peer values
- Output: percentile rank (0-100)

**Data sources needed**:
1. **Vote participation rate**: Already computed — `DivisionRepository` has vote counts. Need `COUNT(votes) / COUNT(divisions)` per MP.
2. **Rebellion rate**: Already computed — `RebellionCalculator` exists. Need per-MP rebellion rate.
3. **Question count**: From Hansard API or `HansardContributionEntity` — count contributions of type "question"
4. **Speech count**: From `DebateSpeechEntity` (Phase 1 debates table) — count speeches per MP
5. **Committee count**: From `MpCommitteeCrossRef` — count committees per MP
6. **Peer averages**: Aggregate across all same-house active MPs

**Implementation approach**:
- New `StatsRepository` that queries existing DAOs and aggregates
- `getPeerAverages(house: Int): PeerAverages` — AVG across all active MPs
- `getPeerValues(metric: String, house: Int): List<Float>` — all MPs' values for percentile calculation
- Wire `ActivityScoreComponents.kt` and `TraitRadarChart.kt` (UI components) to call `StatsRepository`
- Re-enable on MP profile (currently disabled/placeholder)

**Risks**: Peer aggregation queries may be expensive (650 MPs × 5 metrics). Consider caching or pre-computing at build time.

### 11-05: Party Manifestos Build Script

**Data source**: Lancaster Wmatrix (ucrel.lancs.ac.uk/wmatrix/ukmanifestos2024/)
- Pre-edited plain-text TXT files for 7 major parties
- Word counts: Con 25K, Lab 24K, LD 21K, Green 19K, Plaid 16K, SNP 8K, Reform 7K
- Total: ~120K words, ~815KB plain text
- Also available: PDF versions from party websites

**Party ID mapping** (from mps.db):
| Party | partyId | Abbrev |
|-------|---------|--------|
| Conservative | 4 | Con |
| Labour | 15 | Lab |
| Liberal Democrat | 17 | LD |
| Green Party | 44 | Green |
| Plaid Cymru | 22 | PC |
| Scottish National Party | 29 | SNP |
| Reform UK | 1036 | RUK |

**Build script**: `build_manifestos.py`
- Download TXT files from Lancaster Wmatrix (or party websites as fallback)
- Store in `party_manifestos` table: `partyId, manifestoText, manifestoYear, wordCount, source`
- Build FTS4 virtual table: `party_manifestos_fts4` (Room @Fts4 compatible)
- Build FTS5 virtual table: `party_manifestos_fts5` (raw SQLite, for API 21+)
- Both tables index `manifestoText` column
- D-03: Monthly auto-check workflow (`update-manifestos.yml`, cron `0 1 1 * *`)

**Schema**:
```sql
CREATE TABLE party_manifestos (
    partyId INTEGER PRIMARY KEY,
    manifestoText TEXT NOT NULL,
    manifestoYear INTEGER NOT NULL,
    wordCount INTEGER NOT NULL,
    source TEXT
);

-- FTS4 (Room @Fts4, all API levels)
CREATE VIRTUAL TABLE party_manifestos_fts4 USING fts4(manifestoText, content='party_manifestos');

-- FTS5 (raw SQLite, API 21+ only)
CREATE VIRTUAL TABLE party_manifestos_fts5 USING fts5(manifestoText, content='party_manifestos');
```

**Workflow**: `update-manifestos.yml`
- Monthly cron: `0 1 1 * *` (1st of month, 01:00 UTC)
- Download TXT files, check if changed (hash compare)
- If changed: rebuild DB, publish to `manifestos-latest` release tag
- If unchanged: skip

**Risks**: Lancaster Wmatrix URLs may change. Have fallback to party website PDFs with text extraction.

### 11-06: Parties Tab + PartyView Screen

**Directory integration**:
- `DirectoryScreen.kt` line 182: Replace `PlaceholderTabContent("Parties")` with `PartiesTabContent`
- `DirectoryViewModel.kt`: Add `parties` StateFlow — query `SELECT partyId, partyName, partyAbbreviation, partyBackgroundColour, partyForegroundColour, COUNT(*) as seats FROM mps WHERE isActive=1 GROUP BY partyId ORDER BY partyName`
- Already has `distinctParties` flow (used by filter bottom sheet) — reuse for party list

**Party cards** (D-06: tinted + logo):
- Card fill: `Color(parseHex(partyBackgroundColour))`
- Logo: SVG from Wikipedia/Wikimedia, converted to VectorDrawable, bundled in APK `res/drawable/`
- Logo sourcing: build script downloads SVGs from Wikipedia, converts to VectorDrawable XML
- 17 active parties → 17 logos to source
- Fallback: if logo not found, show abbreviation text

**PartyView screen** (4 tabs):
1. **Info**: Party name, abbreviation, seat count, description (Wikipedia intro, D-08), founded date, leader
2. **Members**: Reuse `OfficialsTabContent` pattern, filtered by `partyId` — paged list of MPs
3. **Stats**: Vote share at last election (Parliament API), seat count history, visual bar chart
4. **Manifesto**: Full text display + FTS search (11-07)

**Navigation**:
- `GovEyeApp.kt`: Add `PartyRoute(partyId)` to navigation graph
- Party card click → `onNavigateToParty(partyId)` → `PartyRoute`
- MP profile party pill click → same route
- Deep link friendly: `goveye://party/{partyId}`

**Party stats data** (D-07: hybrid):
- Parliament election results API: `api.parliament.uk/uk-general-elections/parties/{id}` — vote share, seats won
- Wikipedia: party description (intro paragraph), founded date, current leader
- Build script `build_party_stats.py` fetches both, stores in `party_stats` table

**Risks**: Wikipedia SVG logos have varying licenses. Some may be non-free/trademarked. Need to verify usage rights. Fallback to abbreviation text if logo can't be used.

### 11-07: Manifesto FTS Search UI

**Reference pattern** (Odysseus Vault):
- `session_search.py`: FTS5 with `MATCH`, `snippet()` for context excerpts, `bm25()` ranking
- `_sanitize_fts_query()`: strips FTS operators, quotes phrases, safe MATCH syntax
- `_hlSearch()` in `documentLibrary.js`: splits query on whitespace, sorts tokens longest-first, wraps each match in `<mark>`

**Android implementation**:

**FTS version detection** (D-04: dual FTS4/FTS5):
```kotlin
val useFts5 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP // API 21+
// Actually FTS5 is available from API 21 (SQLite 3.9+), but Room @Fts4 is safer
// Use raw SQLite for FTS5 queries, Room @Query for FTS4
```

**Query sanitization** (Kotlin port of _sanitize_fts_query):
```kotlin
fun sanitizeFtsQuery(query: String): String? {
    val tokens = mutableListOf<String>()
    val regex = Regex(""""([^"]+)"|[\w][\w._-]*""")
    for (match in regex.findAll(query)) {
        val phrase = match.groupValues[1]
        if (phrase.isNotEmpty()) {
            tokens.add("\"${phrase.replace("\"", "\"\"")}\"")
        } else {
            val token = match.value.trim('.', '_', '-')
            if (token.isNotEmpty()) {
                tokens.add(if (token.any { it in "._-" }) "\"${token}\"" else token)
            }
        }
    }
    return if (tokens.isEmpty()) null else tokens.joinToString(" ")
}
```

**Snippet highlighting** (Compose AnnotatedString + SpanStyle):
```kotlin
fun highlightSearchTerms(text: String, query: String): AnnotatedString {
    val terms = query.split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .sortedByDescending { it.length }  // longest-first
        .map { Regex.escape(it) }
    if (terms.isEmpty()) return AnnotatedString(text)
    val regex = Regex("(${terms.joinToString("|")})", RegexOption.IGNORE_CASE)
    return buildAnnotatedString {
        var lastEnd = 0
        for (match in regex.findAll(text)) {
            append(text.substring(lastEnd, match.range.first))
            withStyle(SpanStyle(background = Color.Yellow)) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        append(text.substring(lastEnd))
    }
}
```

**Search UI** (D-05: results-only list):
- Top bar search field on Manifesto tab
- When empty: show full manifesto text in scrollable column
- When typing: query FTS, show list of snippets with highlighted terms
- Each result: snippet text (from `snippet()` function) + highlighted terms
- Tap a snippet → expand to show full context around the match
- Empty state when no matches: "No results for '{query}' in this manifesto"

**DAO queries**:
```kotlin
@Query("""
    SELECT pm.partyId, snippet(party_manifestos_fts4, 0, '<b>', '</b>', '...', 32) as snippet
    FROM party_manifestos_fts4
    JOIN party_manifestos pm ON pm.rowid = party_manifestos_fts4.rowid
    WHERE party_manifestos_fts4 MATCH :query
    AND pm.partyId = :partyId
    ORDER BY bm25(party_manifestos_fts4)
    LIMIT 50
""")
fun searchManifestoFts4(partyId: Int, query: String): List<ManifestoSearchResult>
```

**Risks**: FTS5 `snippet()` and `bm25()` may not be available on all Android API levels via Room. FTS4 `snippet()` works but ranking is different (no bm25, use matchinfo). Need to handle both query paths.

## Dependencies Between Plans

```
11-01 (MNIS)          → independent
11-02 (IPSA)          → independent
11-03 (ParlParse)     → independent
11-04 (Activity Score)→ depends on debate_speeches table (Phase 1, already built)
11-05 (Manifestos)    → independent (build script + workflow)
11-06 (Parties tab)   → depends on 11-05 (manifestos data for Manifesto tab)
                       → depends on party_stats data (can be part of 11-06 or separate)
11-07 (FTS search UI) → depends on 11-05 (FTS tables must exist)
                       → depends on 11-06 (Manifesto tab must exist in PartyView)
```

**Execution order**: 11-01, 11-02, 11-03, 11-04 can run in parallel. 11-05 must complete before 11-06 and 11-07. 11-06 must complete before 11-07.

## Validation Architecture

- **Build scripts**: Python unit tests for parsing (MNIS XML, IPSA CSV, ParlParse JSON, manifesto text)
- **Android data layer**: Room migration tests for new entities
- **Android UI**: Compose UI tests for PartyView, manifesto search
- **Integration**: End-to-end test — build script produces DB, Android reads it correctly

## RESEARCH COMPLETE
