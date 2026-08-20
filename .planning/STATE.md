---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: milestone
current_phase: 8
current_phase_name: Activity Feed
status: phase_planned
stopped_at: Phase 8 — 3 plans created (08-01, 08-02, 08-03), plan-checker APPROVED, ready for execution
last_updated: "2026-08-20T19:00:00.000Z"
last_activity: 2026-08-20
last_activity_desc: Phase 8 planned — 3 plans (08-01 data pipeline, 08-02 feed UI, 08-03 MP timeline), plan-checker approved with 0 issues
progress:
  total_phases: 8
  completed_phases: 7
  total_plans: 29
  completed_plans: 29
total_phases: 11
completed_phases: 7
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-16)

**Core value:** Make UK government activity as easy to follow as a football team — so anyone, especially younger voters, can see what their representatives are actually doing.
**Current focus:** Phase 9 — Interests & Income (7th patch stream, monetary parser, dashboard UI) — COMPLETE (09-01 through 09-04 done)

## Current Position

Phase: 9 (Interests & Income) — COMPLETE
Plans: 09-01 (build-side) COMPLETE, 09-02 (Android data) COMPLETE, 09-03 (Android UI) COMPLETE, 09-04 (CI infra) COMPLETE
Status: 4 of 4 plans complete. All Phase 9 work done — build script, data layer, UI, and CI infrastructure. All 118 Python tests + 110 Kotlin tests green.
Last activity: 2026-08-20 — Plan 09-04 complete (recess gates, checkpoint/resume, smart Bills delta)

Progress: [████████████████░░] 73% (7 of 11 phases complete, Phase 9 context gathered)

## Performance Metrics

**Velocity:**

- Total plans completed: 25
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Project Scaffold | 3/3 | - | - |
| 2. Data Foundation | 4/4 | - | - |
| 3. MP Directory & Profiles | 5/5 | - | - |
| 4. Directory Search Enhancement | 1/1 | - | - |
| 5. Divisions & Voting Stats | 5/5 | - | - |
| 6. Follow MPs & Notifications | 3/3 | - | - |
| 7. Bill Tracking | 2/2 | - | - |
| 10. Polish & Release (DB) | 6/9 | - | - |

**Recent Trend:**

- Last 5 plans: 10-04, 10-03, 07-02, 07-01, 06-03
- Trend: —

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Init]: Android-first; pure open source (no monetization); UK Parliament (Westminster) only for v1
- [Init]: FotMob-style engagement model (follow, notify, stats)
- [Init]: Material 3; follow Miko conventions; consult UI/UX design skills (ui-ux-pro-max) where Miko has no reference
- [Init]: No server in v1 — WorkManager polling + local notifications (no FCM)
- [Init]: No AI stance classification — factual data only, neutral methodology
- [Init]: AGP 9.3.0 built-in Kotlin (not the Miko AGP 8.x + kotlin-android setup) — verified current stack
- [Init]: All v1 features in scope (research recommended full vision)
- [Phase 6]: WorkManager 2.11.2 + Hilt-Work 1.4.0 for polling; Egg Timer API for recess dates
- [Phase 6]: DataStore @Named qualifiers for theme vs notification stores (fix duplicate bindings)
- [Phase 6]: NotificationDeepLinkActivity bridges PendingIntent taps to Nav3 DeepLinkNavigator
- [Phase 6]: DB v6 — added RecessDateEntity + RecessDatesMetaEntity + FollowEntity.isMuted
- [Phase 7]: Bill follows use separate BillFollowEntity (not polymorphic FollowEntity)
- [Phase 7]: Bill polling interval is 4 hours (vs 30 min for votes) — bills change less frequently
- [Phase 7]: Commons Library briefings via search URL, not Publications API (deferred to Phase 11)
- [Phase 7]: Bill notification channel is IMPORTANCE_DEFAULT (less urgent than votes IMPORTANCE_HIGH)
- [Phase 7]: DB v8 — added bill_follows table
- [Phase 10]: D-01 Python build script for GitHub Action (no JVM in CI)
- [Phase 10]: D-02 Bundle ALL historical voting data (~160MB+); past is immutable
- [Phase 10]: D-03 Daily Action fetches new divisions only
- [Phase 10]: D-04 First-launch download from GitHub Releases (Play Store 150MB AAB limit)
- [Phase 10]: D-05 Seamless SQL patches via Room transaction; full DB fallback
- [Phase 10]: D-06 Separate public goveye-data repo for DB hosting
- [Phase 10]: D-07 Include Lords divisions in bundled voting data
- [Phase 10]: D-08 Defer MNIS to Phase 11
- [Phase 10]: D-09 DB-patch notifications with 6h latency (no API polling)
- [Phase 10]: D-10 5 separate build scripts per API source (staggered schedules, independent failure)
- [Phase 10]: D-10a Hybrid 2-database architecture on Android (1 BundledDB + 1 LocalDB, not 5 RoomDatabases)
- [Phase 10]: D-11 Bundle bills+committees+recess; keep hansard+profile detail as live API
- [Phase 10]: D-12 Continue as Phase 10 plans (10-05 through 10-08)

### Pending Todos

- 06-01 Task 9: Tests (FollowDao, FollowRepository, FollowingViewModel)
- 06-02 Task 10: Tests (VotePollingWorker, SittingDayResolver, EggTimerApi parsing)

### Blockers/Concerns

- [Research flag] AGP 9 built-in Kotlin officially tested through AGP 9.1 — smoke-test at scaffold (Phase 1)
- [Research flag] Hansard API reliability unproven — probe during Phase 8 planning
- [Resolved] Sitting-day calendar for notification polling — resolved in Phase 6 (Egg Timer API + SittingDayResolver)
- [Resolved] Rebellion-rate methodology — defined in Phase 5 (neutral, documented, evidence-linked)
- [Phase 10] goveye-data repo must be created (public) and first seed DB published before app can download

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Feature | Committee peers row on MP profile | Deferred (requires N API calls per Pitfall #4) | Phase 3 |
| Feature | Committee role badges (chair/member) | Deferred (Committees API doesn't return role) | Phase 3 |
| Feature | Speech notifications | Deferred to Phase 8 (D-05) | Phase 6 |
| Feature | Postcode lookup (MPDIR-03) | Deferred — needs ONS Postcode Directory (~40MB) | Phase 10 |
| Feature | MNIS biographical data | Deferred to Phase 11 (D-08) | Phase 10 |
| Phase | 10-01 Compliance (OPL, AboutLibraries) | Deferred to after 8/9 built DB-native | Phase 10 |
| Phase | 10-02 UX polish (empty/error/offline states) | Deferred to after all features DB-native | Phase 10 |
| Phase | 10-05 Release (versioning, Play Store) | Deferred — final step | Phase 10 |
| Test | Compose UI tests with Hilt | Deferred (needs Hilt test infrastructure) | Phase 3 |
| Test | Phase 6 unit tests | Deferred to end-of-milestone test pass | Phase 6 |

## Session Continuity

Last session: 2026-08-20T16:00:00.000Z
Stopped at: Phase 9 — All 4 plans complete (09-01 through 09-04). Phase 9 done.
Resume file: .planning/ROADMAP.md (next phase selection)
