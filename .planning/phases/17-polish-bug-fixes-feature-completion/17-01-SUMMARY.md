---
plan: 17-01
title: Unified feed card redesign
status: complete
tasks_total: 5
tasks_completed: 5
---

# Plan 17-01 Summary: Unified Feed Card Redesign

## What Was Built

Replaced the four separate feed card composables (FeedDivisionCard, FeedPublicationCard, FeedStatementCard, FeedLegislationCard) with a single UnifiedFeedCard composable that renders all feed types with a consistent LOCKED layout: image (if present) at top, title left + type pill right in a Row, "by who" line, division bar (thin 4dp, if applicable), source bottom-left + date bottom-right (DD/MM/YYYY), and tags at bottom. Added detail screen routes and composables for publication, statement, and legislation cards with clickable navigation. Populated the previously-empty announcementTags map so tags appear on all card types.

## Tasks Completed

- [x] 17-01-01: Created UnifiedFeedCard.kt composable with LOCKED card layout — moved TypeBadge and formatDivisionDate from FeedDivisionCard.kt, added height/modifier parameters to DivisionResultBar for the 4dp feed variant
- [x] 17-01-02: Populated announcementTags map in FeedViewModel — added suspend one-shot tag query methods to AnnouncementTagDao and GovernmentAnnouncementsRepository, tags now passed to all FeedItem subtypes
- [x] 17-01-03: Added PublicationDetailRoute, StatementDetailRoute, LegislationDetailRoute to Routes.kt, created PublicationDetailScreen/StatementDetailScreen/LegislationDetailScreen composables with shared AnnouncementDetailViewModel, wired routes in GovEyeApp.kt navigation graph
- [x] 17-01-04: Replaced FeedItemCard with UnifiedFeedCard call, added onNavigateToPublicationDetail/onNavigateToStatementDetail/onNavigateToLegislationDetail callbacks to FeedScreen, wired callbacks in GovEyeApp.kt
- [x] 17-01-05: Deleted FeedDivisionCard.kt, FeedPublicationCard.kt, FeedStatementCard.kt, FeedLegislationCard.kt, removed CardIconBadge, migrated all callers (GovernmentTabContent, DivisionBrowseScreen, FeedEmptyState) to UnifiedFeedCard

## Files Modified

- `app/.../feed/UnifiedFeedCard.kt` (new): Single composable rendering all FeedItem subtypes with LOCKED layout; contains TypeBadge, formatDivisionDate, CardTypeData helper
- `app/.../feed/AnnouncementDetailViewModel.kt` (new): Shared ViewModel for publication/statement/legislation detail screens
- `app/.../feed/PublicationDetailScreen.kt` (new): Publication detail screen with image, summary, tags, GOV.UK link; contains shared DetailSectionCard
- `app/.../feed/StatementDetailScreen.kt` (new): Written statement detail screen with full text, member role, tags
- `app/.../feed/LegislationDetailScreen.kt` (new): Legislation detail screen with type/year/number, tags, legislation.gov.uk link
- `app/.../feed/FeedViewModel.kt`: Populates announcementTags map, passes tags to all FeedItem subtypes
- `app/.../feed/FeedUiState.kt`: Unchanged (announcementTags field already existed)
- `app/.../FeedScreen.kt`: FeedItemCard replaced with UnifiedFeedCard, new navigation callback parameters added
- `app/.../navigation/Routes.kt`: Added PublicationDetailRoute, StatementDetailRoute, LegislationDetailRoute
- `app/.../GovEyeApp.kt`: Navigation graph entries for 3 new detail routes, FeedScreen callbacks wired
- `app/.../divisions/DivisionBrowseScreen.kt`: DivisionResultBar gained height+modifier params, FeedDivisionCard replaced with UnifiedFeedCard
- `app/.../directory/GovernmentTabContent.kt`: FeedPublicationCard/FeedStatementCard/FeedLegislationCard replaced with UnifiedFeedCard
- `app/.../feed/FeedEmptyState.kt`: FeedDivisionCard replaced with UnifiedFeedCard
- `core/data/.../dao/AnnouncementTagDao.kt`: Added suspend getTagsForPublication/Statement/Legislation methods
- `core/data/.../repo/GovernmentAnnouncementsRepository.kt`: Added suspend getTagsForPublication/Statement/Legislation wrappers
- Deleted: `app/.../feed/FeedDivisionCard.kt`, `FeedPublicationCard.kt`, `FeedStatementCard.kt`, `FeedLegislationCard.kt`

## Decisions Made

- Used a shared AnnouncementDetailViewModel for all three detail screens rather than 3 separate ViewModels — reduces boilerplate while keeping each screen's load method distinct
- Added a `barHeight` parameter (default 6dp) and `modifier` parameter to DivisionResultBar rather than creating a separate thin variant — the feed passes 4dp, the detail view keeps its 6dp default
- DetailSectionCard composable made `internal` (package-level) in PublicationDetailScreen.kt and shared by StatementDetailScreen — avoids duplication of the titled section card pattern
- Used `AsyncImage` (simpler) in PublicationDetailScreen instead of `SubcomposeAsyncImage` — the detail screen doesn't need a custom loading placeholder slot

## Issues Encountered

- TypeBadge conflict: Moving TypeBadge to UnifiedFeedCard.kt while it still existed in FeedDivisionCard.kt caused "Conflicting overloads" — resolved by removing the duplicate from FeedDivisionCard.kt in task 17-01-01 (earlier than the planned task 17-01-05 cleanup)
- Dangling KDoc lint error: A leftover KDoc comment in FeedDivisionCard.kt after removing TypeBadge triggered ktlint(standard:kdoc) — resolved by converting to a regular `//` comment
- `typePrefix` unresolved reference: Initially used `publication.typePrefix` in FeedViewModel but typePrefix is a FeedItem property, not a domain model property — resolved by using literal prefix strings ("publication-", "statement-", "legislation-")
- Accidental `git add -A` included STATE.md/ROADMAP.md in task 17-01-05 commit — reset and recommitted with only the relevant files staged
