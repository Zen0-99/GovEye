# Roadmap: GovEye

## Overview

GovEye is a free, open-source Android app that makes UK Parliament as followable as a football league. The journey starts with a solid AGP 9 scaffold and data foundation, then builds the table-stakes features (MP directory, find-my-MP, voting records, bills, activity feed), then the FotMob-style differentiator (follow MPs with notifications), and finishes with financial interests and a polished, attribution-compliant release. All v1 features run serverless against UK Parliament's free, unauthenticated open data APIs.

## Phases

- [x] **Phase 1: Project Scaffold** - AGP 9 Gradle project, M3 theme, navigation shell, CI gates (completed 2026-08-16)
- [x] **Phase 2: Data Foundation** - Per-API Retrofit clients, DTOs, Room cache, cache-first repositories (completed 2026-08-17)
- [x] **Phase 3: MP Directory & Profiles** - Browse/search 650 MPs, rich "player card" profiles (role header, career timeline, related MPs) (completed 2026-08-17)
- [x] **Phase 4: Directory Search Enhancement** - FTS search (name/party/constituency), tab result count badges, Miko-style filter bottom sheet. Postcode lookup deferred to Phase 10 (bundled DB). (completed 2026-08-17)
- [ ] **Phase 5: Divisions & Voting Stats** - Division browse, voting records, rebellion stats, activity score, trait bars, vote map
- [ ] **Phase 6: Follow MPs & Notifications** - Follow list, WorkManager polling, local notifications
- [ ] **Phase 7: Bill Tracking** - Bill list, progress stages timeline, Commons Library summaries
- [ ] **Phase 8: Activity Feed** - Aggregate divisions/debates into a "what happened today" feed + per-MP activity timelines
- [ ] **Phase 9: Interests & Income** - Register of Members' Financial Interests display
- [ ] **Phase 10: Polish & Release** - OPL attribution, notification reliability, Play Store release
- [ ] **Phase 11: API Enrichment** - Re-integrate MNIS + Ayes & Noes, add Public Whip, IPSA, ParlParse, evaluate TheyWorkForYou (see research/API-ENRICHMENT.md)

## Phase Details

### Phase 1: Project Scaffold

**Goal**: A compiling Android project with the full v1 stack: AGP 9.3.0 (built-in Kotlin), Compose + Material 3, Hilt, Room, Retrofit, version catalog, build-logic convention plugins, M3 theme, navigation shell, and CI lint/test gates.
**Depends on**: Nothing (first phase)
**Requirements**: DESIGN-01, DESIGN-02, OPEN-01, OPEN-03
**Success Criteria** (what must be TRUE):

  1. `./gradlew assembleDebug` builds successfully with AGP 9 built-in Kotlin (no kotlin-android plugin)
  2. App launches with M3 theme (dark/light/dynamic color) and a bottom-nav shell (Directory, Feed, Settings tabs)
  3. Lint + unit test tasks pass in CI
  4. .gitignore excludes .planning/; LICENSE and README with OPL attribution stub present

**Plans**: 3/3 plans executed

Plans:

- [x] 01-01-PLAN.md — Gradle scaffold: AGP 9.3.0, version catalog, build-logic convention plugins, wrapper 9.7.0, Spotless
- [x] 01-02-PLAN.md — App shell: M3 theme (Miko port, 4 schemes + Monet), Navigation 3 bottom nav (4 tabs), Hilt DI, splash, deep links, Room FTS scaffold
- [x] 01-03-PLAN.md — CI + hygiene: GitHub Actions gates (lint/test/spotless/build), GPL-3.0 LICENSE, README, OPL attribution, smoke test

### Phase 2: Data Foundation

**Goal**: Five Retrofit API clients (Members, Votes, Bills, Hansard, Interests) with per-API DTOs, mappers, Room database, and cache-first repositories. The data layer every feature reads through.
**Depends on**: Phase 1
**Requirements**: DESIGN-03
**Success Criteria** (what must be TRUE):

  1. Each API client fetches real data (Members list, division search, bill list) with correct envelope parsing
  2. Room schema caches MPs, divisions, bills, Hansard contributions, interests, follows
  3. Repositories read cache-first with stale-while-revalidate; offline shows cached data, never blank
  4. Unit tests cover DTO mapping with MockWebServer fixtures

**Plans**: 4/4 plans executed

Plans:

- [x] 02-01-PLAN.md — Networking core — OkHttp + Retrofit 3 + kotlinx-serialization config, error mapping
- [x] 02-02-PLAN.md — Members + Votes API clients with DTOs and mappers
- [x] 02-03-PLAN.md — Bills + Hansard + Interests API clients with DTOs and mappers
- [x] 02-04-PLAN.md — Room database + cache-first repositories with tests

### Phase 3: MP Directory & Profiles

**Goal**: Browse and search all 650 MPs, view rich "player card" profiles — party/role header, bio, committees, political career timeline, related MPs (per FOTMOB-MAPPING.md).
**Depends on**: Phase 2
**Requirements**: MPDIR-01, MPDIR-02, MPDIR-04, MPDIR-05, MPDIR-06, MPDIR-07, DTAB-01, DESIGN-04
**Success Criteria** (what must be TRUE):

  1. User can browse all current MPs (paged) and search by name
  2. User can open an MP profile showing party (crest/color), parliamentary role, constituency, bio, contact, and committee memberships
  3. MP profile shows a political career timeline (roles with dates) and related MPs (same-party colleagues, committee peers)
  4. Lists scroll smoothly (Paging 3) with Coil-loaded portraits and photo fallbacks
  5. MP profile works offline from cache
  6. Directory has sub-tabs (Officials, Parties, Bills, Divisions, Debates) under the search bar — Officials has MP data, others are placeholders

**Plans**: 5 plans

Plans:

- [x] 03-01: MP directory screen — paged list, search, MP cards
- [x] 03-02: MP profile screen — header card (party/role/constituency/tenure), bio, photo, contact, committees
- [x] 03-03: Career timeline + related MPs — roles with dates, same-party/committee peers (FotMob career + teammates patterns)
- [x] 03-04: Deep links + tests + polish
- [x] 03-05: Directory sub-tabs — Officials/Parties/Bills/Divisions/Debates tab row with HorizontalPager (Officials has MP data, others placeholders)

### Phase 4: Directory Search Enhancement

**Goal**: Enhance Directory search from name-only to expansive FTS search across name, party, and constituency. Add tab result count badges and a Miko-style filter bottom sheet. Postcode lookup deferred to Phase 10 (bundled DB).
**Depends on**: Phase 3
**Requirements**: MPDIR-01 (enhanced), DTAB-01 (count badges)
**Success Criteria** (what must be TRUE):

  1. User can search by MP name, party name, or constituency name using FTS tokenized matching
  2. When searching, each directory tab shows a count badge with the number of matching results
  3. Filter bottom sheet (ported from Miko) allows filtering by party, house, and member status
  4. Filter state persists across app restarts via DataStore
  5. Filters apply on top of text search (results match both query and filters)

**Plans**: 1 plan

Plans:

- [x] 04-01: FTS search + tab count badges + filter bottom sheet

### Phase 5: Divisions & Voting Stats

**Goal**: Division browse/search with party breakdown, per-MP voting records, rebellion rate and voting stats with neutral methodology, Vico charts, and the FotMob-style stats layer (activity score, trait bars, vote map, standing chart).
**Depends on**: Phase 2 (and Phase 3 for MP context)
**Requirements**: VOTES-01, VOTES-02, VOTES-03, VOTES-04, VOTES-05, VOTES-06, VOTES-07, VOTES-08, OPEN-03, DESIGN-04
**Success Criteria** (what must be TRUE):

  1. User can browse and search divisions with party breakdown (Ayes/Noes fetched on demand)
  2. User can view an MP's voting record with division context
  3. Rebellion rate is computed with documented neutral methodology and links to division evidence
  4. MP profile shows a mechanical parliamentary activity score with published methodology (transparency tool, no editorial judgment)
  5. MP profile shows percentile trait bars vs peers (rebellion, participation, questions, speeches, committee workload)
  6. MP profile shows a filterable vote map (division votes color-coded vs party line)
  7. Stats render as Vico charts; terminology is neutral (no editorial labels)

**Plans**: 5 plans

Plans:

- [ ] 05-01: Division browse/search screen with party breakdown
- [ ] 05-02: Voting record on MP profile + rebellion methodology module
- [ ] 05-03: Stats visualizations — Vico charts for voting patterns, attendance, rebellion
- [ ] 05-04: Activity score + trait bars — mechanical score computation, percentile bars vs party/Commons peers
- [ ] 05-05: Vote map — division votes visualized, filterable by topic, color-coded vs party line

### Phase 6: Follow MPs & Notifications

**Goal**: THE differentiator — follow MPs, WorkManager polling (30-60 min, sitting days, jitter), diff against cache, local notifications with channels and settings.
**Depends on**: Phase 3, Phase 5
**Requirements**: FOLLOW-01, FOLLOW-02, FOLLOW-03, FOLLOW-04, FOLLOW-05
**Success Criteria** (what must be TRUE):

  1. User can follow/unfollow MPs; follows persist on-device
  2. User receives a notification when a followed MP votes in a new division (within the hour)
  3. "Spoke" notifications work best-effort from Hansard with graceful degradation
  4. WorkManager uses unique periodic work with jitter; no duplicate syncs; respects sitting days
  5. Notification settings let the user control per-type and per-MP notifications (POST_NOTIFICATIONS handled properly)

**Plans**: 3 plans

Plans:

- [ ] 06-01: Follow infrastructure — follow store, follow/unfollow UI on profiles and directory
- [ ] 06-02: Vote polling worker — WorkManager sync, division diffing, notification dispatch with channels
- [ ] 06-03: Notification settings + spoke (Hansard) polling best-effort

### Phase 7: Bill Tracking

**Goal**: Bill list with search, bill detail with progress timeline, plain-English summaries via Commons Library briefings links.
**Depends on**: Phase 2
**Requirements**: BILLS-01, BILLS-02, BILLS-03, BILLS-04
**Success Criteria** (what must be TRUE):

  1. User can browse/search ~2,700 bills
  2. Bill detail shows the full stage progression timeline with dates
  3. Each bill links to its Commons Library briefing for plain-English summary
  4. Follow-bill notifications work on the phase 6 notification infrastructure (stage-change diffing)

**Plans**: 2 plans

Plans:

- [ ] 07-01: Bill list + search + bill detail with stage timeline
- [ ] 07-02: Follow bills + stage-change notifications (reusing phase 6 infra)

### Phase 8: Activity Feed

**Goal**: A "what happened today" feed aggregating divisions, debates, and statements, with deep links out to Hansard/TheyWorkForYou, plus per-MP recent activity timelines (FotMob "recent matches" pattern).
**Depends on**: Phase 5, Phase 7 (data sources)
**Requirements**: FEED-01, FEED-02, FEED-03, MPDIR-08
**Success Criteria** (what must be TRUE):

  1. User sees a chronological feed of divisions, debates, and statements
  2. Feed items deep-link to full context on Hansard/TheyWorkForYou
  3. Feed degrades gracefully when Hansard API is unavailable (divisions still shown)
  4. MP profile shows a recent activity timeline with per-event activity weight

**Plans**: 3 plans

Plans:

- [ ] 08-01: Feed data pipeline — aggregate divisions + Hansard contributions with fallbacks
- [ ] 08-02: Feed UI — timeline cards, filters, deep links
- [ ] 08-03: Per-MP recent activity timeline — reuses feed pipeline, per-event weights on MP profile (MPDIR-08)

### Phase 9: Interests & Income

**Goal**: Register of Members' Financial Interests display on MP profiles — categorized income, shareholdings, land, gifts with totals.
**Depends on**: Phase 3 (MP profile)
**Requirements**: INT-01, INT-02
**Success Criteria** (what must be TRUE):

  1. MP profile shows categorized interests (employment/earnings, shareholdings, land, gifts)
  2. Income totals per category are summarized
  3. Free-text parsing handles the Interests API's typed fields array robustly

**Plans**: 1 plan

Plans:

- [ ] 09-01: Interests integration — API client usage, categorization, profile display

### Phase 10: Polish & Release

**Goal**: Release readiness — OPL attribution, AboutLibraries, empty/error/offline states everywhere, notification reliability audit, bundled seed DB with daily GitHub Action updates, postcode lookup against bundled data, Play Store release.
**Depends on**: Phases 1-9
**Requirements**: DESIGN-01, DESIGN-02, OPEN-02, DATA-01, DATA-02, DATA-03, DATA-04, MPDIR-03
**Success Criteria** (what must be TRUE):

  1. OPL v3.0 attribution statement + link present in-app; app icon does not use Royal Arms/Crowned Portcullis
  2. All screens have designed empty/error/offline states (no blank screens)
  3. Notification pipeline is audited: no spam, dedupe, proper channels, settings respected
  4. targetSdk 36 release build passes Play requirements; APK/AAB builds clean
  5. UI reviewed against Miko conventions + UI/UX design skills (ui-ux-pro-max)
  6. App bundles a seed SQLite DB (~2-3MB) in APK for instant offline access to all 650 MPs
  7. Daily GitHub Action publishes incremental DB patches + full DB fallback to release tag
  8. App checks for DB updates on startup (manifest hash), downloads patch or full DB as needed
  9. Postcode lookup works offline against bundled DB (postcode → constituency → MP), with postcode→constituency mapping cached permanently (no postcode storage per UK GDPR)

**Plans**: 5 plans

Plans:

- [ ] 10-01: Compliance — OPL attribution, AboutLibraries, licensing review
- [ ] 10-02: UX polish — empty/error/offline states, edge cases, notification reliability audit
- [ ] 10-03: Bundled database + daily updates — seed DB in APK, GitHub Action build/diff/publish, DatabaseUpdateManager (see research/BUNDLED-DB-STRATEGY.md)
- [ ] 10-04: Voting data pre-load — one-time bulk fetch of all 650 MPs' full voting records (paginated API calls, 25/page) baked into the seed DB. Daily GitHub Action incrementally fetches new divisions and updates vote counts. Eliminates the multi-minute API fetch on first MP profile open. The Commons Votes API caps at 25 items/page with no bulk endpoint, so the GitHub Action must loop through all MPs × all pages sequentially with rate limiting.
- [ ] 10-05: Release — versioning, release build, Play Store listing prep

### Phase 11: API Enrichment

**Goal**: Enrich MP profiles and stats with data from external APIs beyond the core Parliament Members API. Re-integrate MNIS and Ayes & Noes (removed from Phase 3 as premature), add Public Whip bulk data, IPSA expenses, ParlParse social media links, and evaluate TheyWorkForYou behind a credential adapter.
**Depends on**: Phase 10 (core UX finalized, bundled DB infrastructure in place)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04 (enriched data feeds into bundled DB)
**Success Criteria** (what must be TRUE):

  1. MNIS API provides maiden speech dates, government/opposition posts, honours, DOB, and committee chairing details on MP profiles
  2. Ayes & Noes API provides recorded vote counts, rebellion counts, and voting summaries
  3. Public Whip bulk data provides attendance rates and historical rebellion data
  4. IPSA CSV parsing provides expense claims data (periodic download)
  5. ParlParse provides social media and Wikipedia links on MP profiles
  6. TheyWorkForYou is behind a credential/config adapter (not enabled by default due to quota)
  7. All enriched data feeds into the bundled DB daily update pipeline (Phase 10 infra)

**Plans**: 6 plans (tentative — will be detailed when phase is planned)

Plans:

- [ ] 11-01: MNIS re-integration — biographical data (maiden speeches, posts, honours, DOB)
- [ ] 11-02: Ayes & Noes re-integration — voting stats, rebellion counts, EDMs
- [ ] 11-03: Public Whip + IPSA — bulk rebellion/attendance data, expense claims
- [ ] 11-04: ParlParse — social media links, Wikipedia URLs, minister history
- [ ] 11-05: TheyWorkForYou adapter — credential-gated, quota-managed
- [ ] 11-06: Activity Score + Trait Radar — wire ActivityScoreCalculator and TraitRadarChart with real peer aggregation data (questions from Hansard, speeches from Hansard, committee counts, rebellion rates from Public Whip). Requires peer averages computed across all same-house MPs. The UI components (ActivityScoreComponents.kt, TraitRadarChart.kt) and domain calculators (ActivityScoreCalculator, TraitBarCalculator, PercentileCalculator) already exist — this plan wires them to real data and re-enables them on the MP profile.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Project Scaffold | 3/3 | Complete    | 2026-08-16 |
| 2. Data Foundation | 4/4 | Complete (verified) | 2026-08-17 |
| 3. MP Directory & Profiles | 5/5 | Complete (verified) | 2026-08-17 |
| 4. Directory Search Enhancement | 0/1 | Planned | 04-01 |
| 5. Divisions & Voting Stats | 0/5 | Not started | - |
| 6. Follow MPs & Notifications | 0/3 | Not started | - |
| 7. Bill Tracking | 0/2 | Not started | - |
| 8. Activity Feed | 0/3 | Not started | - |
| 9. Interests & Income | 0/1 | Not started | - |
| 10. Polish & Release | 0/4 | Not started | - |
| 11. API Enrichment | 0/5 | Not started | - |
