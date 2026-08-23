---
phase: 14-government-announcements
plan: 05
subsystem: android-onboarding
tags: [compose, onboarding, tag-personalization, source-recommendation, mp-curation, datastore, hilt]
key-files:
  - app/src/main/java/com/goveye/app/ui/screens/OnboardingScreen.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/TagsStep.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/SourcesStep.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/PartiesStep.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/MPsStep.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/OnboardingViewModel.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/SourceRecommendationHelper.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/MpCurationHelper.kt
  - app/src/main/java/com/goveye/app/ui/screens/onboarding/OnboardingDataModels.kt
  - core/data/src/main/java/com/goveye/app/data/preference/OnboardingPreferences.kt
  - core/data/src/main/java/com/goveye/app/data/local/dao/MpTagDao.kt
  - core/data/src/main/java/com/goveye/app/data/repo/GovernmentAnnouncementsRepository.kt
metrics:
  step_composables_created: 4
  viewmodels_created: 1
  helper_classes_created: 2
  data_models_created: 7
  datastore_keys_added: 3
  dao_queries_added: 2
  files_created: 9
  files_modified: 3
---

# Plan 14-05 Summary — Onboarding Redesign

## Objective

Redesign onboarding from 2 steps to 5 steps (Government → Tags → Sources → Parties → MPs) with tag-based personalization, source recommendation logic, MP curation by tag match, and seed download running in background throughout. Creates 4 new step composables, an OnboardingViewModel managing all selection state, and 2 helper classes for source recommendation and MP curation logic.

## Commits

| Commit | Task | Description |
|--------|------|-------------|
| `0097e0a` | Tasks 1-3 (tracer + auto + auto) | 5-step onboarding redesign with tag-based personalization (D-02, D-05, D-06, D-07, D-08, D-09) |

## What was built

### New step composables (4)

- **TagsStep.kt** — Step 2: LazyVerticalGrid Fixed(2) of 26 tag cells, selection counter ("N selected"), checkmark on selected, "Skip for now" + "Continue to sources" CTAs. TAG_DESCRIPTIONS map for all 26 tags.
- **SourcesStep.kt** — Step 3: LazyColumn with Recommended section (department cards with 3 pre-checked stream checkboxes per D-05) + All sources section (75 department-stream combinations grouped by department, D-04). "Select topics to see recommendations" hint when no tags selected. "Continue to parties" CTA.
- **PartiesStep.kt** — Step 4: LazyVerticalGrid Fixed(2) of 17 party cards with partyColor tint (0.08 alpha bg), abbreviation badge (32dp circle), seat count, 2dp partyColor border + checkmark when selected. "Continue to MPs" CTA.
- **MPsStep.kt** — Step 5: 3 sections — Party leaders (horizontal scroll, D-07), Recommended for you (tag-matched MPs, D-08), All MPs (paged list). Follow toggle IconButton (PersonAdd→Check). Paging load/error states at bottom. "Finish setup" CTA.

### OnboardingViewModel.kt (new)

@HiltViewModel injecting GovernmentAnnouncementsRepository, FollowRepository, OnboardingPreferences, TagDao, MpDao. Exposes:
- Selection StateFlows: selectedTags, selectedSources, selectedParties, followedMpIds
- Data Flows: availableTags, recommendedDepartments, allDepartments, parties, partyLeaderInfos, recommendedMpDetails, pagedMps
- Toggle functions for each selection type
- `persistSelections()` writes to DataStore before onComplete fires

### Helper classes (2)

- **SourceRecommendationHelper.kt** — Maps selected tags to recommended departments using source_recommendations table (D-06). `getAllSources()` returns 25 departments × 3 streams = 75 combinations (D-04). Fallback hardcoded department list when no tags selected.
- **MpCurationHelper.kt** — Ranks MPs by recency-weighted tag hits from mp_tags table (D-08). Party leaders always included in Recommended (D-07). No-tag MPs excluded from Recommended, appear in All only (D-09). Sorted by totalScore descending.

### Data models (7)

OnboardingDataModels.kt: StreamType enum, StreamState, RecommendedDepartment, DepartmentGroup, PartyInfo, RecommendedMp, PartyLeaderInfo.

### Extended files (3)

- **OnboardingScreen.kt** — Extended from 2 steps to 6 steps (0-5) with AnimatedContent fade transitions preserved. ViewModel integration via hiltViewModel(). Step 1 Continue advances to step 2 (Tags). Step 5 onFinish triggers persistSelections() + fade-out + onComplete.
- **OnboardingPreferences.kt** — Added selectedTags, selectedSources, selectedParties DataStore keys (stringSetPreferencesKey) + Flow properties + suspend set functions. Parties stored as String set, converted to Int.
- **MpTagDao.kt** — Added `getMpTagsForTags(tags)` returning List<MpTagEntity> and `observeAllMpTagRows()` returning Flow<List<MpTagEntity>> for MpCurationHelper.
- **GovernmentAnnouncementsRepository.kt** — Added `getMpTagsForTags()` and `observeAllMpTagRows()` methods + MpTagEntity.toDomain() mapper.

## Decisions implemented

| Decision | Implementation |
|----------|----------------|
| D-02 | Seed download runs in background throughout all 5 steps (existing gating on selectedGovernment unchanged) |
| D-04 | Source = department × data stream, 25 depts × 3 streams = 75 combinations in SourceRecommendationHelper.getAllSources() |
| D-05 | Recommended departments with 3 stream checkboxes pre-checked in SourcesStep |
| D-06 | Hybrid tag→department mapping in SourceRecommendationHelper (source_recommendations table + hardcoded fallback) |
| D-07 | Party leaders from precomputed party_leaders table, always included in MPsStep Recommended section |
| D-08 | Recency-weighted MP ranking in MpCurationHelper using mp_tags hit counts |
| D-09 | No-tag MPs excluded from Recommended, appear in All MPs list only |

## Deviations

1. **MainActivity.kt NOT modified** — The existing onComplete callback is sufficient because the ViewModel's persistSelections() writes tag/source/party selections to DataStore before onComplete fires. The seed download gating (waiting for selectedGovernment) is unchanged. No functional gap — the plan anticipated a possible MainActivity edit but the existing wiring handles it.

## Verification

- `:app:compileDebugKotlin` — PASSED (only pre-existing warnings)
- `spotlessApply` — PASSED
- `:app:testDebugUnitTest` — Pre-existing test compilation failures (ProfileViewModelTest, DatabaseUpdateWorkerTest — NOT caused by this plan, noted in 14-04-SUMMARY.md)
- Human checkpoint (Task 3): User approved visual verification of all 5 onboarding steps

## Self-Check: PASSED

All 4 implementation tasks completed. All 7 key decisions (D-02, D-04 through D-09) implemented. 9 new files + 3 modified files. Build compiles. Human checkpoint approved.
