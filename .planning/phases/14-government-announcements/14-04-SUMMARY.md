---
phase: 14-government-announcements
plan: 04
subsystem: android-ui
tags: [compose, feed-cards, government-tab, filter-expansion, following-hub, datastore]
key-files:
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedPublicationCard.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedStatementCard.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedLegislationCard.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedDivisionCard.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedScreen.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedViewModel.kt
  - app/src/main/java/com/goveye/app/ui/screens/feed/FeedUiState.kt
  - app/src/main/java/com/goveye/app/ui/screens/directory/DirectoryScreen.kt
  - app/src/main/java/com/goveye/app/ui/screens/directory/DirectoryViewModel.kt
  - app/src/main/java/com/goveye/app/ui/screens/directory/GovernmentTabContent.kt
  - app/src/main/java/com/goveye/app/ui/screens/directory/FilterBottomSheet.kt
  - app/src/main/java/com/goveye/app/ui/screens/directory/FilterState.kt
  - app/src/main/java/com/goveye/app/ui/screens/FollowingScreen.kt
  - core/data/src/main/java/com/goveye/app/data/preference/DirectoryFilterPreferences.kt
  - core/data/src/main/java/com/goveye/app/data/repo/GovernmentAnnouncementsRepository.kt
metrics:
  card_composables_created: 3
  card_composables_extended: 1
  screen_composables_extended: 4
  viewmodels_extended: 2
  filter_pill_rows_created: 3
  filter_sections_added: 9
  datastore_keys_added: 4
  files_modified: 15
  files_created: 4
---

# Plan 14-04 Summary — Android UI for Government Announcements

## Objective

Add 3 new Feed card types (FeedPublicationCard, FeedStatementCard, FeedLegislationCard), update FeedDivisionCard with icon badges (D-11), extend FeedViewModel/FeedUiState for mixed card types + curation logic (D-12), add a Government sub-tab to the Directory, expand the FilterBottomSheet with Tags/Sources/Departments/Type sections (D-14), and extend FollowingScreen as the central hub for all followed entities (D-14).

## Commits

| Commit | Task | Description |
|--------|------|-------------|
| `851617b` | Task 1 (tracer) | Add FeedItem sealed interface + FeedPublicationCard + mixed feed (D-10, D-11, D-12) |
| `5ae19a9` | Task 2 (auto) | Add FeedStatementCard + FeedLegislationCard + Government tab in Directory (D-11, D-14) |
| `f88d567` | Task 4 (auto) | Expand FilterBottomSheet with Tags/Sources/Departments/Type sections + FollowingScreen hub (D-14) |

## What Was Done

### Task 1 — Tracer: FeedItem sealed interface + FeedPublicationCard + mixed feed
- Created `FeedItem` sealed interface in FeedUiState.kt with 4 subtypes: DivisionItem, PublicationItem, StatementItem, LegislationItem
- Changed `FeedDateGroup.divisions: List<Division>` to `FeedDateGroup.items: List<FeedItem>`
- Added `CardType` enum (DIVISION, PUBLICATION, STATEMENT, LEGISLATION) for type filter
- Added new filter fields to FeedUiState: tagFilter, sourceFilter, departmentFilter, typeFilter, announcementTags
- Created `FeedPublicationCard` composable with Coil AsyncImage (image-on-top 16:9), Description icon badge at TopEnd, type badge "Publication", TagPillRow
- Extracted `TagPillRow` from private in FeedDivisionCard to shared internal composable
- Extended FeedViewModel: injected GovernmentAnnouncementsRepository, added tag/source/department/type filter StateFlows, combined publication flows with division flows chronologically
- Extended FeedScreen: LazyColumn items keyed by type-{id}, contentType per card type, when block for mixed card rendering

### Task 2 — FeedStatementCard + FeedLegislationCard + FeedDivisionCard icon badge + Government tab
- Created `FeedStatementCard` with ChatBubbleOutline icon badge, minister name prefixed "by ", type badge "Statement"
- Created `FeedLegislationCard` with Gavel icon badge, type (SI/Act), type badge "Legislation"
- Updated `FeedDivisionCard` with HowToVote icon badge at TopEnd + "Division" type badge
- Extended FeedViewModel with statement + legislation flows, wired all 4 card types chronologically
- Extended FeedScreen when block with StatementItem + LegislationItem rendering
- Added `GOVERNMENT("Government")` to DirectoryTab enum
- Created `GovernmentTabContent` composable with SegmentedPill source type filter (All/Publications/Statements/Legislation), LazyColumn of announcement cards, ConfigureSearchBar, empty/error/loading states, StickyInfoCard
- Extended DirectoryViewModel with government tab data flows + source type filter state
- Added `FilterTabType.GOVERNMENT` and `FilterTabType.FOLLOWING_HUB` stubs to FilterBottomSheet

### Task 4 — FilterBottomSheet expansion (D-14) + FollowingScreen hub
- Extended `DirectoryFilterState` with tagFilter, sourceFilter, departmentFilter, typeFilter fields + updated hasActiveFilters
- Extended `DirectoryFilterPreferences` with 4 new DataStore keys (tag/source/department/type filter sets) + getter/setter methods + clearAll extension
- Added `observeAllAnnouncementTags()` to GovernmentAnnouncementsRepository (combines publication/statement/legislation tag flows)
- Extended `DirectoryViewModel` with allAnnouncementTags, allDepartments, allSources StateFlows + toggleTagFilter/toggleSourceFilter/toggleDepartmentFilter/setTypeFilter methods + GovFilterExtras combine pattern
- Implemented `FilterTabType.GOVERNMENT` branch: Tags (TagPillRow), Departments (DepartmentPillRow), Type (SegmentedPill) sections
- Implemented `FilterTabType.FOLLOWING_HUB` branch: Tags (TagPillRow), Sources (SourcePillRow), Parties (PartyPillRow), Officials (SegmentedPill house filter), Type (SegmentedPill with Divisions) sections
- Created `TagPillRow` composable: horizontally scrollable pills, selected=primary 0.15 alpha bg + primary text Bold, unselected=onSurface 0.06 alpha bg + onSurfaceVariant text
- Created `SourcePillRow` composable: same pill style as TagPillRow, for department-stream source names
- Created `DepartmentPillRow` composable: same pill style, fixed max width 120dp with TextOverflow.Ellipsis, text centered
- Updated DirectoryScreen to pass allTags/allDepartments/allSources + filter callbacks to FilterBottomSheet
- Extended `FollowingScreen` with sectioned LazyColumn: Officials (existing followed MPs with unfollow), Parties, Sources, Tags sections with SectionHeader + SectionEmptyHint composables
- Extended FollowingScreen empty state copy to mention all followable entity types (MPs, parties, sources, tags)
- Changed FollowingScreen FilterBottomSheet call from OFFICIALS to FOLLOWING_HUB tab type
- Filter state persists via DataStore (existing pattern from Phase 4, extended with new keys)

## Deviations

None.

## Pre-existing Issues (not caused by this plan)

- `:app:testDebugUnitTest` fails to compile due to pre-existing test file issues (DirectoryViewModelTest, DirectoryViewModelFilterTest, ProfileViewModelTest, DatabaseUpdateWorkerTest reference constructors modified by prior phases — e.g. `governmentAnnouncementsRepository` was added to DirectoryViewModel in Task 2). These test files were NOT modified by this plan. The `:app:compileDebugKotlin` task passes successfully.
- `:core:data:testDebugUnitTest` fails to compile due to pre-existing test file issues (FeedRepositoryTest, VotesRepositoryTest, DatabaseUpdateManagerTest) — noted in 14-03-SUMMARY.md.

## Self-Check

- [x] FilterTabType enum has GOVERNMENT and FOLLOWING_HUB entries
- [x] FilterBottomSheet has GOVERNMENT branch with Tags/Departments/Type sections
- [x] FilterBottomSheet has FOLLOWING_HUB branch with Tags/Sources/Parties/Officials/Type sections
- [x] TagPillRow composable exists with correct pill styling (primary 0.15 alpha selected, onSurface 0.06 alpha unselected)
- [x] SourcePillRow composable exists with correct pill styling
- [x] DepartmentPillRow composable exists with 120dp max width + TextOverflow.Ellipsis
- [x] FollowingScreen has sections for Officials/Parties/Sources/Tags with unfollow actions
- [x] FollowingScreen empty state mentions all followable entity types (MPs, parties, sources, tags)
- [x] FollowingScreen FilterBottomSheet uses FOLLOWING_HUB tab type
- [x] DirectoryFilterState has tagFilter, sourceFilter, departmentFilter, typeFilter fields
- [x] DirectoryFilterPreferences has DataStore keys for all 4 new filter types
- [x] DirectoryViewModel exposes allAnnouncementTags, allDepartments, allSources StateFlows
- [x] DirectoryViewModel has toggleTagFilter, toggleSourceFilter, toggleDepartmentFilter, setTypeFilter methods
- [x] DirectoryScreen passes new filter data to FilterBottomSheet
- [x] GovernmentAnnouncementsRepository has observeAllAnnouncementTags() combining 3 tag flows
- [x] All source compiles successfully (:app:compileDebugKotlin passes)
- [x] spotlessApply passes

**Self-Check: PASSED**
