# Roadmap: GovEye

## Overview

GovEye is a free, open-source Android app that makes UK Parliament as followable as a football league. The journey starts with a solid AGP 9 scaffold and data foundation, then builds the table-stakes features (MP directory, find-my-MP, voting records, bills, activity feed), then the FotMob-style differentiator (follow MPs with notifications), and finishes with financial interests and a polished, attribution-compliant release. All v1 features run serverless against UK Parliament's free, unauthenticated open data APIs.

## Phases

- [x] **Phase 1: Project Scaffold** - AGP 9 Gradle project, M3 theme, navigation shell, CI gates (completed 2026-08-16)
- [x] **Phase 2: Data Foundation** - Per-API Retrofit clients, DTOs, Room cache, cache-first repositories (completed 2026-08-17)
- [x] **Phase 3: MP Directory & Profiles** - Browse/search 650 MPs, rich "player card" profiles (role header, career timeline, related MPs) (completed 2026-08-17)
- [x] **Phase 4: Directory Search Enhancement** - FTS search (name/party/constituency), tab result count badges, Miko-style filter bottom sheet. Postcode lookup deferred to Phase 10 (bundled DB). (completed 2026-08-17)
- [x] **Phase 5: Divisions & Voting Stats** - Division browse, voting records, rebellion stats, activity score, trait bars, vote map (completed 2026-08-18)
- [x] **Phase 6: Follow MPs & Notifications** - Follow list, WorkManager polling, local notifications (completed 2026-08-18)
- [x] **Phase 7: Bill Tracking** - Bill list, progress stages timeline, Commons Library summaries (completed 2026-08-18)
- [x] **Phase 8: Activity Feed** - Aggregate divisions/debates into a "what happened today" feed + per-MP activity timelines (completed 2026-08-18)
- [x] **Phase 9: Interests & Income** - Register of Members' Financial Interests display (completed 2026-08-20)
- [ ] **Phase 10: Polish & Release** - OPL attribution, notification reliability, Play Store release
- [x] **Phase 11: Build-time Data Enrichment** - MNIS biographical data, IPSA expenses, ParlParse social/Wikipedia links, party manifestos with FTS search, Parties tab + PartyView (build-time into bundled DB). Dropped: Ayes & Noes, Public Whip, TWFY (see research/API-ENRICHMENT.md) (completed 2026-08-22)
- [ ] **Phase 12: Unified Precomputed Database** - Single post-merge build script (build_precompute.py) produces precomputed stats tables (mp_stats, peer_averages) and fixes N+1 query paths with SQL JOINs. Eliminates 5,500+ runtime DAO calls per profile open. App reads precomputed rows instead of aggregating at runtime.
- [x] **Phase 13: Expense Detail** - Rebuild IPSA parser to capture claim descriptions, add ExpenseBucketDetailScreen with per-claim detail view (following InterestBucketDetailScreen pattern) (completed 2026-08-23, verified in code)
- [x] **Phase 14: Government Announcements** - Track executive/regulatory action from GOV.UK Content API, Parliament Written Statements API, legislation.gov.uk API. Feed cards + Directory sub-tab. Tag personalization + 5-step onboarding redesign. All bundled DB (build-time, no live API).
- [ ] **Phase 15: MP Activity Feed Redesign** - Transform Activity tab from votes-only to mixed feed (votes + questions + income + expenses + committee + career). Store written question text. Move bulk votes to Stats tab as summary + VotingRecordScreen.
- [ ] **Phase 16: Debates Directory Redesign** - Replace 'Bills' directory tab with 'Debates' — umbrella for all parliamentary business (bills, SIs, motions, treaties). Each card = one work package (Level 1). Rename current 'Debate' code to 'Division'. Data layer for all types (Procedure Browser CSV for SIs/treaties, bill publications + news articles). Bills UI first, SIs/motions/treaties UI in a later phase.
- [x] **Phase 17: Polish, Bug Fixes & Feature Completion** - UAT-driven polish pass: unified feed card design (image/title/type pill/by-who/source+date/tags), clickable detail views for all card types, income/expense card redesign with party-colored icons, MP activity in feed, attendance rate fix (tenure-aware), historical member search fix (party names/images/click targets), follow/notification icon reactivity, notification expansion (income/expenses), Following screen sub-tabs, searchbar transition animation fix, light mode card color tweak, speaker matching fix in transcripts, speech cards in activity + feed.

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

- [x] 01-01-PLAN.md â€” Gradle scaffold: AGP 9.3.0, version catalog, build-logic convention plugins, wrapper 9.7.0, Spotless
- [x] 01-02-PLAN.md â€” App shell: M3 theme (Miko port, 4 schemes + Monet), Navigation 3 bottom nav (4 tabs), Hilt DI, splash, deep links, Room FTS scaffold
- [x] 01-03-PLAN.md â€” CI + hygiene: GitHub Actions gates (lint/test/spotless/build), GPL-3.0 LICENSE, README, OPL attribution, smoke test

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

- [x] 02-01-PLAN.md â€” Networking core â€” OkHttp + Retrofit 3 + kotlinx-serialization config, error mapping
- [x] 02-02-PLAN.md â€” Members + Votes API clients with DTOs and mappers
- [x] 02-03-PLAN.md â€” Bills + Hansard + Interests API clients with DTOs and mappers
- [x] 02-04-PLAN.md â€” Room database + cache-first repositories with tests

### Phase 3: MP Directory & Profiles

**Goal**: Browse and search all 650 MPs, view rich "player card" profiles â€” party/role header, bio, committees, political career timeline, related MPs (per FOTMOB-MAPPING.md).
**Depends on**: Phase 2
**Requirements**: MPDIR-01, MPDIR-02, MPDIR-04, MPDIR-05, MPDIR-06, MPDIR-07, DTAB-01, DESIGN-04
**Success Criteria** (what must be TRUE):

  1. User can browse all current MPs (paged) and search by name
  2. User can open an MP profile showing party (crest/color), parliamentary role, constituency, bio, contact, and committee memberships
  3. MP profile shows a political career timeline (roles with dates) and related MPs (same-party colleagues, committee peers)
  4. Lists scroll smoothly (Paging 3) with Coil-loaded portraits and photo fallbacks
  5. MP profile works offline from cache
  6. Directory has sub-tabs (Officials, Parties, Bills, Divisions, Debates) under the search bar â€” Officials has MP data, others are placeholders

**Plans**: 5 plans

Plans:

- [x] 03-01: MP directory screen â€” paged list, search, MP cards
- [x] 03-02: MP profile screen â€” header card (party/role/constituency/tenure), bio, photo, contact, committees
- [x] 03-03: Career timeline + related MPs â€” roles with dates, same-party/committee peers (FotMob career + teammates patterns)
- [x] 03-04: Deep links + tests + polish
- [x] 03-05: Directory sub-tabs â€” Officials/Parties/Bills/Divisions/Debates tab row with HorizontalPager (Officials has MP data, others placeholders)

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

- [x] 05-01: Division browse/search screen with party breakdown
- [x] 05-02: Voting record on MP profile + rebellion methodology module
- [x] 05-03: Stats visualizations â€” Vico charts for voting patterns, attendance, rebellion
- [x] 05-04: Activity score + trait bars â€” mechanical score computation, percentile bars vs party/Commons peers
- [x] 05-05: Vote map â€” division votes visualized, filterable by topic, color-coded vs party line

### Phase 6: Follow MPs & Notifications

**Goal**: THE differentiator â€” follow MPs, WorkManager polling (30-60 min, sitting days, jitter), diff against cache, local notifications with channels and settings.
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

- [x] 06-01: Follow infrastructure â€” follow store, follow/unfollow UI on profiles and directory
- [x] 06-02: Vote polling worker â€” WorkManager sync, division diffing, notification dispatch with channels
- [x] 06-03: Notification settings + spoke (Hansard) polling best-effort

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

- [x] 07-01: Bill list + search + bill detail with stage timeline
- [x] 07-02: Follow bills + stage-change notifications (reusing phase 6 infra)

### Phase 8: Activity Feed

**Goal**: A "what happened today" feed aggregating divisions, debates, and statements, with deep links out to Hansard/TheyWorkForYou, plus per-MP recent activity timelines (FotMob "recent matches" pattern).
**Depends on**: Phase 5, Phase 7 (data sources)
**Requirements**: FEED-01, FEED-02, FEED-03, MPDIR-08
**Success Criteria** (what must be TRUE):

  1. User sees a chronological feed of divisions, debates, and statements
  2. Feed items deep-link to full context on Hansard/TheyWorkForYou
  3. Feed degrades gracefully when Hansard API is unavailable (divisions still shown)
  4. MP profile shows a recent activity timeline with per-event activity weight

**Plans**: 3/3 plans executed

Plans:

- [x] 08-01: Feed data pipeline - DivisionWeightCalculator, relative date utility, new DAO queries, FeedRepository
- [x] 08-02: Feed UI - sticky date headers, division cards, followed highlight, filter, Debates tab, TheyWorkForYou link
- [x] 08-03: Per-MP recent activity timeline - Activity tab on MP profile with DivisionWeightCalculator badges

### Phase 9: Interests & Income

**Goal**: Register of Members' Financial Interests display on MP profiles â€” categorized income, shareholdings, land, gifts with totals.
**Depends on**: Phase 3 (MP profile)
**Requirements**: INT-01, INT-02
**Success Criteria** (what must be TRUE):

  1. MP profile shows categorized interests (employment/earnings, shareholdings, land, gifts)
  2. Income totals per category are summarized
  3. Free-text parsing handles the Interests API's typed fields array robustly

**Plans**: 4/4 plans executed

Plans:

- [x] 09-01: Build-side — build_interests.py, monetary parser, update-interests.yml, merge_dbs.py, schema JSON
- [x] 09-02: Android data layer — entity migration (v1→v2), repository rewrite, 7th patch stream, DAO date query
- [x] 09-03: Android UI — Interests tab, dashboard grid, monthly navigation, date filter, bucket detail screen
- [x] 09-04: CI infrastructure — recess-gated workflows, seed automation, checkpoint/resume, Bills smart delta

### Phase 10: Polish & Release

**Goal**: Release readiness â€” OPL attribution, AboutLibraries, empty/error/offline states everywhere, notification reliability audit, bundled seed DB with daily GitHub Action updates, postcode lookup against bundled data, Play Store release.
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
  9. Postcode lookup works offline against bundled DB (postcode â†’ constituency â†’ MP), with postcodeâ†’constituency mapping cached permanently (no postcode storage per UK GDPR)

**Plans**: 9 plans

Plans:

- [ ] 10-01: Compliance â€” OPL attribution, AboutLibraries, licensing review
- [ ] 10-02: UX polish â€” empty/error/offline states, edge cases, notification reliability audit
- [x] 10-03: Bundled database + daily updates — Python build script (seed DB + daily delta), GitHub Action, schema validation, JSON diff patches, manifest generation
- [x] 10-04: Voting data pre-load â€“ Android DatabaseUpdateManager: first-launch download, patch application via Room transaction, full DB fallback, WorkManager background checks
- [x] 10-05: Multi-database build scripts â€” refactor single build_db.py into 5 per-API scripts (mps/votes/bills/committees/recess) + merge_dbs.py for seed, separate GitHub Action workflows with staggered schedules (D-10)
- [x] 10-06: Android hybrid 2-database architecture â€” split GovEyeDatabase into BundledDatabase + LocalDatabase, extend DatabaseUpdateManager for 5 patch streams (D-10a)
- [x] 10-07: Rewrite repositories to read from DB â€” remove API refresh methods, delete MpRemoteMediator + SittingDayResolver + RemoteKeyDao, keep profile detail + hansard as live API (D-09, D-11)
- [x] 10-08: Rewrite workers for DB-patch notifications â€” VotePollingWorker + BillPollingWorker as one-shot workers triggered by DatabaseUpdateWorker after patch application (D-09)
- [ ] 10-09: Release â€” versioning, release build, Play Store listing prep

### Phase 11: Build-time Data Enrichment

**Goal**: Enrich MP profiles with biographical, financial, and social data from external sources, integrated at build time into the bundled DB via Python scripts. MNIS for biographical details (maiden speeches, posts, honours, DOB), IPSA for expense claims, ParlParse for social media and Wikipedia links. Add a Parties tab to the directory with tinted party cards (using existing partyBackgroundColour from mps.db), a PartyView screen with 4 tabs (Info, Members, Stats, Manifesto), and bundled party manifestos with full-text search. Dropped sources documented in research/API-ENRICHMENT.md as fallback.
**Depends on**: Phase 10 (core UX finalized, bundled DB infrastructure in place)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04 (enriched data feeds into bundled DB)
**Success Criteria** (what must be TRUE):

  1. MNIS build script fetches maiden speech dates, government/opposition posts with dates, honours, DOB, and committee chairing details; stores in bundled DB; enriches career timeline and profile
  2. IPSA build script downloads CSV expense claims (published every 2 months), parses and stores in bundled DB; expense cards appear in the Interests tab on MP profiles
  3. ParlParse build script clones repo, extracts social media links and Wikipedia URLs; stores in bundled DB; adds links to MP profile
  4. Activity Score and Trait Radar on MP profile are wired to real peer aggregation data (Hansard speeches, committee counts, self-computed rebellion rates) with peer averages across all same-house MPs
  5. All enriched data feeds into the bundled DB daily update pipeline (Phase 10 infra) as new per-API streams
  6. Parties tab in directory shows all 17 active parties as tinted cards (partyBackgroundColour fill, abbreviation badge, party name, seat count), sorted alphabetically by party name
  7. PartyView screen opens on card click with 4 tabs: Info (party name, abbreviation, seat count, description), Members (same paged list as Officials tab, filtered by partyId), Stats (vote share at last election, seat count history), Manifesto (full text with FTS search)
  8. Party manifesto build script downloads plain-text manifestos for 7 major parties (Conservative, Labour, Lib Dem, Green, Plaid Cymru, SNP, Reform UK) from Lancaster Wmatrix or party websites; stores in manifestos.db as a new per-API stream; total ~815KB
  9. Manifesto tab has a top bar search field; typing queries FTS (Room @Fts4 with snippet() and BM25 ranking), shows matching snippets with highlighted terms (Compose AnnotatedString with SpanStyle), tapping a snippet scrolls to that section in the full text
  10. Party pill on MP profile is clickable and navigates to PartyView for that party

**Plans**: 7 plans (tentative — will be detailed when phase is planned)

Plans:

- [ ] 11-01: MNIS biographical data — build_mnis.py (XML parse), bio_data table in bundled DB, career timeline + profile enrichment (maiden speeches, govt/opposition posts, honours, DOB)
- [ ] 11-02: IPSA expenses — build_ipsa.py (CSV download + parse), expenses table in bundled DB, expense cards in Interests tab on MP profile
- [ ] 11-03: ParlParse enrichment — build_parlparse.py (git clone + XML/JSON parse), mp_links table in bundled DB, social media + Wikipedia links on MP profile
- [ ] 11-04: Activity Score + Trait Radar — wire existing ActivityScoreComponents.kt and TraitRadarChart.kt to real peer aggregation data (questions from Hansard, speeches from debate_speeches table, committee counts, self-computed rebellion rates). Requires peer averages computed across all same-house MPs. Domain calculators (ActivityScoreCalculator, TraitBarCalculator, PercentileCalculator) already exist — this plan wires them to real data and re-enables them on the MP profile.
- [ ] 11-05: Party manifestos build script — build_manifestos.py (download 7 plain-text manifestos from Lancaster Wmatrix/party websites), party_manifestos table + party_manifestos_fts (FTS4) in manifestos.db, update-manifestos.yml workflow, merge into bundled DB as new per-API stream
- [ ] 11-06: Parties tab + PartyView screen — Parties tab in directory (tinted cards from partyBackgroundColour, abbreviation badge, seat count, alphabetical sort), PartyView with 4 tabs (Info: name/abbrev/seats/description, Members: reuse Officials paged list filtered by partyId, Stats: vote share + seat history, Manifesto: full text display + FTS search with snippet highlighting), party pill click on MP profile navigates to PartyView
- [ ] 11-07: Manifesto FTS search UI — top bar search field on Manifesto tab, Room @Fts4 query with snippet() and BM25 ranking, results list with highlighted terms (AnnotatedString + SpanStyle pattern from Odysseus Vault _hlSearch), tap snippet to scroll to full text section, empty state when no matches

### Phase 12: Unified Precomputed Database

**Goal**: Eliminate all runtime aggregation/computation on bundled DB data by precomputing derived stats at build time and fixing N+1 query paths with SQL JOINs. A single post-merge build script (build_precompute.py) reads the merged goveye.db and produces precomputed tables. The app reads precomputed rows instead of iterating 650 MPs or loading 130k+ vote entities.
**Depends on**: Phase 10 (bundled DB infrastructure), Phase 11 (enrichment data — bio_data, expenses, debate_speeches feed into stats)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04 (precomputed data feeds into bundled DB)

**Success Criteria** (what must be TRUE):

  1. `build_precompute.py` runs after all per-API DBs are merged into goveye.db, produces `mp_stats` and `peer_averages` tables via SQL aggregation (no API calls)
  2. `mp_stats` table has one row per MP with: questionCount, speechCount, committeeCount, voteParticipationRate, rebellionRate, rebellionCount, totalDivisionsVoted, activityScore, and 5 trait percentiles (rebellion, participation, questions, speeches, committees)
  3. `peer_averages` table has one row per house with: avgQuestions, avgSpeeches, avgCommittees, avgParticipation, avgRebellion, mpCount
  4. `StatsRepository` reads from precomputed tables — `getTraitBars()` and `getActivityScore()` become 2 DB queries instead of 5,500+ DAO calls
  5. `VotesRepository.getMemberVotingWithDivisions()` replaced with SQL JOIN (eliminates N+1 queries)
  6. `VotesRepository.getPartyBreakdown()` replaced with SQL GROUP BY (eliminates loading 650 vote entities)
  7. `FollowingViewModel.followedWithVotes` uses batch SQL query instead of N+1 recent vote lookups
  8. MP profile opens in under 1 second (down from 10+ seconds)
  9. Fallback: if precomputed tables are empty (old DB), app falls back to current runtime computation path
  10. No raw tables are dropped from the shipped DB — precomputed tables are additive

**Plans**: 4 plans (tentative — will be detailed when phase is planned)

Plans:

- [ ] 12-01: build_precompute.py — post-merge build script that produces mp_stats + peer_averages tables via SQL aggregation (rebellion rates, question/speech/committee counts, vote participation, activity scores, trait percentiles, peer averages per house)
- [ ] 12-02: Android schema + DAO + repository rewrite — add MpStatsDao, PeerAveragesDao, rewrite StatsRepository to read precomputed tables, add fallback for old DBs
- [ ] 12-03: SQL JOIN fixes — replace N+1 queries in VotesRepository (getMemberVotingWithDivisions, getPartyBreakdown, checkIfRebel) and FollowingViewModel (batch recent vote query) with single SQL queries
- [ ] 12-04: CI integration + validation — add build_precompute.py to build_db.py merge pipeline, verify profile open time under 1 second, verify precomputed stats match runtime computation results

### Phase 13: Expense Detail

**Goal**: Make expense cards on MP profiles clickable, showing individual claims with descriptions. Rebuild the IPSA CSV parser to capture fields currently discarded (Short Description, Details, journey info, payment breakdown) and add an ExpenseBucketDetailScreen following the InterestBucketDetailScreen pattern.
**Depends on**: Phase 9 (Interests & Income — expense data infrastructure), Phase 11 (IPSA build script)
**Requirements**: INT-02 (expense detail parity with interests detail)

**Success Criteria** (what must be TRUE):

  1. `build_ipsa.py` captures Short Description, Details, Claim Number, journey details (From/To/Mileage/Journey Type), and payment breakdown (Amount Paid/Not Paid/Repaid/Reason If Not Paid) from the IPSA CSV
  2. `ExpenseEntity` has the new columns; Room schema bumped (additive — nullable columns on existing table)
  3. `ExpenseBucketCard` in InterestsTabContent has an onClick handler that navigates to ExpenseBucketDetailScreen
  4. `ExpenseBucketDetailScreen` shows individual claims grouped by date, with description, amount, status, and journey details where relevant
  5. Global search bar works on the detail screen (same pattern as InterestBucketDetailScreen)
  6. New `ExpenseBucketDetailRoute` wired in Routes.kt and GovEyeApp.kt navigation graph

**Plans**: 2/2 plans executed (verified in code — no SUMMARY.md files, predates convention)

Plans:

- [x] 13-01: Rebuild IPSA parser — capture all discarded CSV fields, update ExpenseEntity schema, Room migration, update build_ipsa.py and merge_dbs.py
- [x] 13-02: Expense detail UI — FinancialBucketDetailScreen (renamed from InterestBucketDetailScreen), InterestBucketDetailRoute with entryType, onClick wiring, search bar integration

### Phase 14: Government Announcements

**Goal**: Track executive/regulatory action from three free government data sources (GOV.UK Content API, Parliament Written Statements API, legislation.gov.uk API). Display as Feed cards and a new Directory sub-tab. Extend the existing tag system to announcements and MPs. Redesign onboarding to 5 steps with tag-based personalization. All data stored in bundled DB (build-time, no live API — scalability).
**Depends on**: Phase 10 (bundled DB infrastructure), Phase 11 (build pipeline patterns), Phase 12 (precompute infrastructure)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04 (new data sources into bundled DB), FEED-01 (feed includes government announcements)

**Success Criteria** (what must be TRUE):

  1. `build_gov_publications.py` fetches GOV.UK publications across all ~25 departments (news articles, consultations, speeches, guidance, SIs) and stores in bundled DB
  2. `build_written_statements.py` fetches written ministerial statements from Parliament Written Statements API (both Commons and Lords)
  3. `build_legislation.py` fetches new SIs and Acts from legislation.gov.uk API
  4. `build_tags.py` extended to pattern-match announcement text against TAG_DICTIONARY — new tables: publication_tags, statement_tags, legislation_tags
  5. `build_mp_tags.py` aggregates tags from MP debate speeches — mp_tags table (memberId, tag, hitCount) for onboarding recommendations
  6. New Feed card types: FeedPublicationCard, FeedStatementCard, FeedLegislationCard — mixed with existing division and debate cards
  7. New "Government" sub-tab in Directory — browse announcements by source type, filter by tag/department, search
  8. Onboarding redesigned to 5 steps: Government → Tags/Interests → Sources (recommended + all) → Parties → MPs (curated by tag match + party leaders first). Seed download runs in background throughout.
  9. Tag-based filtering in Feed — user's selected onboarding tags prioritize relevant announcements
  10. All announcement data in BundledDatabase, updated via CI with ~12h freshness delay

**Plans**: 5 plans (tentative — will be detailed when phase is planned)

Plans:

- [ ] 14-01: Build scripts for government triad — build_gov_publications.py (GOV.UK Content API), build_written_statements.py (Parliament Written Statements API), build_legislation.py (legislation.gov.uk API), CI workflows, merge into bundled DB
- [ ] 14-02: Tag extension + MP tagging — extend build_tags.py for announcement text, new build_mp_tags.py for MP tag aggregation, new tag tables
- [ ] 14-03: Android data layer — entities, DAOs, GovernmentAnnouncementsRepository, all in BundledDatabase
- [ ] 14-04: Android UI — Feed card types (publication/statement/legislation), Government sub-tab in Directory, tag filtering
- [ ] 14-05: Onboarding redesign — 5-step flow (Government → Tags → Sources → Parties → MPs), tag selection UI, source recommendation logic, MP curation by tag match, seed download in background

### Phase 15: MP Activity Feed Redesign

**Goal**: Transform the MP profile Activity tab from votes-only into a mixed chronological feed of all MP activity (votes, written questions, income declarations, expense claims, committee joins, career milestones). Move bulk votes to Stats tab as a summary + dedicated VotingRecordScreen. Store written question text (currently only counts are stored).
**Depends on**: Phase 13 (expense descriptions for activity feed), Phase 14 (MP tags for optional filtering)
**Requirements**: MPDIR-08 (per-MP activity timeline), FEED-02 (activity feed includes all MP actions)

**Success Criteria** (what must be TRUE):

  1. `build_hansard.py` stores questionText and answerText from mySociety CSV (with fallback to per-question API for 255-char truncated entries)
  2. MP profile Activity tab shows a mixed chronological feed: votes, written questions (with text), income declarations, expense claims (with descriptions), committee joins/leaves, career milestones
  3. Each activity type has a distinct card design; date headers group items chronologically (existing FeedDateHeader pattern)
  4. Activity feed is filterable by activity type
  5. Stats tab has a "Recent votes" summary section below the vote map — last 5-10 votes with "See all" link
  6. New VotingRecordScreen — full voting record (relocated from Activity tab), accessible from Stats tab "See all" link
  7. Written questions show the actual question text, answering body, and date tabled

**Plans**: 3 plans (tentative — will be detailed when phase is planned)

Plans:

- [ ] 15-01: Written question text storage — modify build_hansard.py to capture questionText/answerText from mySociety CSV, 255-char truncation fallback, new schema columns or written_questions table
- [ ] 15-02: Activity feed redesign — mixed activity feed on MP profile (votes + questions + income + expenses + committee + career), distinct card designs per type, date grouping, activity type filter
- [ ] 15-03: Votes relocation to Stats tab — Recent votes summary below vote map, VotingRecordScreen (full voting record), navigation wiring

### Phase 16: Debates Directory Redesign

**Goal**: Replace the 'Bills' directory tab with 'Debates' — the umbrella for all parliamentary business (bills, SIs, motions, treaties). Each card represents a single work package (Level 1, not topic-based). Rename current 'Debate'/'DebateCard' code references to 'Division'/'DivisionCard'. Build the data layer for all types (Procedure Browser CSV for SIs/treaties, Bills API publications + news articles, division-to-bill title matching). Build the bills UI first; SIs/motions/treaties UI deferred to a later phase (seed: debates-directory-expansion).
**Depends on**: Phase 10 (bundled DB infrastructure), Phase 14 (tag system, Feed card patterns)
**Requirements**: BILLS-01, BILLS-02, BILLS-03, BILLS-04 (enhanced), FEED-01 (bill activity in feed)

**Success Criteria** (what must be TRUE):

  1. Current 'Debate'/'DebateCard' code references renamed to 'Division'/'DivisionCard' throughout the Android codebase (DB table `debate_speeches` stays as-is)
  2. 'Bills' directory tab replaced with 'Debates' — browsable catalog sorted by recent activity, with type chips (Bills/SIs/Motions/Treaties) + tag filters via bottom sheet + search bar
  3. `build_si_metadata.py` downloads Procedure Browser CSVs (7,550 SIs + 333 treaties) and stores in bundled DB with procedure types (Draft affirmative, Made negative, etc.)
  4. `build_bill_publications.py` fetches bill publications + news articles from Bills API, stores in new tables (bill_publications, bill_news_articles)
  5. `build_bill_divisions.py` links divisions to bills via title matching (same approach as build_tags.py), stores in bill_divisions mapping table
  6. Debates directory card: type chip, last activity date, progress indicator (stage X of Y for bills, Approved/Before Parliament for SIs)
  7. Bill detail screen: full stage timeline with dates (fixed date formatting — "December 9th, 2025" + ranges for multiple sittings), divisions per stage, publications, news articles, related items section
  8. Related items determined by same work package/enabling Act + tag overlap, hardcoded in seed, updated via delta
  9. Following a bill → new activity shows in Feed as update card (current progress + smaller previous stage), clicking takes user to relevant section
  10. Data layer includes all types (bills + SIs + motions) even though UI only shows bills in this phase

**Plans**: TBD (will be detailed when phase is planned)

Plans:

- [ ] 16-01: Rename Debate→Division in code — rename all 'Debate'/'DebateCard' references to 'Division'/'DivisionCard' in Android codebase, update navigation routes
- [ ] 16-02: Build scripts for SI/treaty metadata + bill publications/news + bill-division linkage — build_si_metadata.py (Procedure Browser CSV), build_bill_publications.py (Bills API), build_bill_divisions.py (title matching), new tables, CI integration
- [ ] 16-03: Android data layer — entities, DAOs, DebatesRepository, all in BundledDatabase, schema migration
- [ ] 16-04: Debates directory UI — replace Bills tab with Debates, card design (type chip + date + progress), type filter chips, tag filter bottom sheet, search bar
- [ ] 16-05: Bill detail screen redesign — fixed timeline component (reusable), date formatting utility, divisions per stage, publications section, news articles per stage, related items section
- [ ] 16-06: Feed integration — bill activity update cards (current progress + previous stage), follow-bill → feed card wiring

### Dropped sources (kept in research/API-ENRICHMENT.md as fallback)

- **Ayes & Noes** (ayesandnoes.co.uk) — dropped: 90% redundant. We already compute recorded vote counts, rebellion counts, voting summaries, and party breakdowns from raw Parliament Votes API data. Only unique value is EDMs (Early Day Motions), which should come from the Parliament Questions API if wanted. API kept as fallback reference.
- **Public Whip** (publicwhip.org.uk) — dropped for v1: provides historical rebellion/attendance back to 1992, but we compute current stats ourselves. Revisit for v2 if users request historical depth. Bulk XML kept as fallback reference.
- **TheyWorkForYou API** — obsolete: we already scrape TWFY at build time for debate transcripts (build_debates.py). The API's remaining value (issue-based voting positions) conflicts with our neutral methodology stance. TWFY API kept as fallback reference for future use.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 â†’ 2 â†’ 3 â†’ 4 â†’ 5 â†’ 6 â†’ 7 â†’ 8 â†’ 9 â†’ 10 â†’ 11 â†’ 12 â†’ 13 â†’ 14 â†’ 15

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Project Scaffold | 3/3 | Complete    | 2026-08-16 |
| 2. Data Foundation | 4/4 | Complete (verified) | 2026-08-17 |
| 3. MP Directory & Profiles | 5/5 | Complete (verified) | 2026-08-17 |
| 4. Directory Search Enhancement | 1/1 | Complete | 2026-08-17 |
| 5. Divisions & Voting Stats | 5/5 | Complete | 2026-08-18 |
| 6. Follow MPs & Notifications | 3/3 | Complete | 2026-08-18 |
| 7. Bill Tracking | 2/2 | Complete | 2026-08-18 |
| 8. Activity Feed | 3/3 | Complete | 2026-08-18 |
| 9. Interests & Income | 4/4 | Complete | 2026-08-20 |
| 10. Polish & Release | 6/9 | 10-03â†’10-08 executed | - |
| 11. Build-time Data Enrichment | 7/7 | Complete (verified in code) | 2026-08-22 |
| 12. Unified Precomputed Database | 4/4 | Complete (build passes) | 2026-08-22 |
| 13. Expense Detail | 2/2 | Complete (verified in code) | 2026-08-23 |
| 14. Government Announcements | 5/5 | Complete | 2026-08-24 |
| 15. MP Activity Feed Redesign | 0/3 | Not planned | - |
| 16. Debates Directory Redesign | 0/6 | Not planned | - |
| 17. Polish, Bug Fixes & Feature Completion | 7/7 | Complete | 2026-08-25 |

### Phase 17: Polish, Bug Fixes & Feature Completion

**Goal**: UAT-driven polish pass addressing 15 issues found during real-device testing. Covers unified feed card design, clickable detail views, income/expense card redesign, MP activity in feed, attendance rate calculation fix, historical member search corruption, follow/notification icon reactivity, notification type expansion, Following screen sub-tabs, searchbar animation glitches, light mode card color, speaker matching in transcripts, and speech cards in activity feed.
**Depends on**: Phase 14 (feed card types, government announcements), Phase 15 (activity feed redesign — shares card design patterns)
**Requirements**: FEED-01, FEED-02, MPDIR-08, FOLLOW-02, FOLLOW-04, DESIGN-04

**Success Criteria** (what must be TRUE):

  1. **Unified feed card design** — All feed card types (division, publication, statement, legislation) share one design top-to-bottom: (a) image if present, (b) title left + type pill (Division/Legislation/Statement etc) right, (c) "by who" line in-line, (d) division bar (thinner than division detail view) if applicable, (e) source bottom-left + date bottom-right in DD/MM/YYYY, (f) relevant tags at the bottom. Division type pill is in the same position as other types (currently inconsistent). Title does not overlap top-right icons.
  2. **Clickable detail views** — Publication, statement, and legislation cards in the feed are clickable and navigate to a detail view (currently only division cards are clickable).
  3. **Income/expense card redesign** — Unified card design used in MP activity tab, income view, expense view, and feed: (a) money amount as title, (b) sub-text of who/where, (c) "Income" or "Expense" text + icon top-right colored in the official's party color, (d) short description (truncated) below, (e) date bottom-right DD/MM/YYYY, (f) category name bottom-left. Clicking opens microview. When in feed, official's profile icon (with colored circle) appears in-line before the title.
  4. **MP activity in feed** — Followed MP's income, expenses, and speeches appear in the user's feed using the unified card designs. Speech cards show three text lines of the speech + tags inherited from the parent division.
  5. **Attendance rate fix** — Attendance rate is calculated relative to the MP's actual tenure start date (from `bio_data` or `historical_members`), not from 2016. An MP who started this year (e.g. Hannah Spencer) is not penalized for 9 years of non-attendance. Activity score logic is also tenure-aware.
  6. **Zack Polanski added** — Green Party leader Zack Polanski is in the database (build script fix or manual data entry).
  7. **Historical member search fix** — Searching beyond the 650 current MPs (e.g. "tony") returns correct results: proper party names (not lowercase), proper abbreviations, correct images, correct click targets (clicking "Tony Blair" opens Tony Blair, not John D.Taylor). No stray numbers appended to names. "crossbench" spelled correctly.
  8. **Follow/notification icon reactivity** — Tapping the follow icon on an MP profile immediately updates the icon state (no need to leave and return). Same for notification toggle.
  9. **Notification type expansion** — Notification settings include Income and Expenses in addition to votes and speeches. User can toggle per-type notifications for followed MPs.
  10. **Following screen sub-tabs** — Following screen uses global sub-tab design (Officials, Parties, Sources, Tags) instead of the current section layout. Followed officials do not show a three-dot icon.
  11. **Searchbar transition animation fix** — Navigating back from profile to directory: the searchbar animates smoothly (right side does not jump). Partial-back (Android predictive back) does not prematurely swap searchbar text or show the filter button. Light/dark mode toggle does not lag the searchbar behind the rest of the screen.
  12. **Light mode card color** — Card color across the whole app in light mode is slightly darker than current.
  13. **Speaker matching fix** — Clicking a speaker name in a transcript (e.g. "Baroness Stedman-Scott") navigates to the correct MP profile with correct name, party color, and party name (not "Deborah" with gray "conservative" pill).
  14. **Speeches in activity + feed** — Speeches from transcripts appear in the MP's activity tab and in the feed of users following that MP, using a card design with three lines of speech text + inherited division tags.

**Plans**: 7/7 plans executed

Plans:

- [x] 17-01-PLAN.md
- [x] 17-02-PLAN.md
- [x] 17-03-PLAN.md
- [x] 17-04-PLAN.md
- [x] 17-05-PLAN.md
- [x] 17-06-PLAN.md
- [x] 17-07-PLAN.md

- [x] 17-01: Unified feed card redesign — single card component for all feed types (division/publication/statement/legislation), image top, title+type pill row, by-who line, division bar (thin), source+date bottom, tags bottom; fix type pill position consistency; fix title overlap with top-right icons; make all cards clickable to detail views
- [x] 17-02: Income/expense card redesign + feed integration — unified income/expense card (amount title, who/where subtext, party-colored icon top-right, description, date, category), used in MP activity/income/expense views and feed; followed MP activity (income/expenses/speeches) appears in feed; speech cards with 3-line text + inherited tags
- [x] 17-03: Attendance rate + activity score tenure fix — calculate attendance from MP's actual tenure start (bio_data/historical_members), not fixed 2016; make activity score tenure-aware; add Zack Polanski to Green Party leader data
- [x] 17-04: Historical member search fix — fix party name display (proper case + abbreviations), fix images, fix click target mapping (search result → correct profile), remove stray numbers, fix "crossbench" spelling
- [x] 17-05: Follow/notification reactivity + notification expansion + Following sub-tabs — immediate icon state update on follow/unfollow and notification toggle; add Income + Expenses notification types; Following screen sub-tabs (Officials/Parties/Sources/Tags) using global sub-tab design; remove three-dot icon from followed officials
- [x] 17-06: Searchbar animation + light mode card color — fix right-side searchbar transition (no jump on back navigation), fix predictive-back premature text swap, fix light/dark mode searchbar lag; darken card color in light mode
- [x] 17-07: Speaker matching fix + speech activity cards — fix transcript speaker → MP profile mapping (correct name, party color, party name); speech cards in MP activity tab and feed (3-line text + inherited division tags)
