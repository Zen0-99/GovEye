---
plan: 17-02
title: Income/expense card + feed integration
status: complete
tasks_total: 7
tasks_completed: 7
---

# Plan 17-02 Summary: Income/Expense Card + Feed Integration

## What Was Built

Created a single `UnifiedFinancialCard` composable implementing the UI-SPEC Section 2 LOCKED layout (amount as bold title, who/where subtext, party-colored "Income"/"Expense" label + icon top-right, 2-line description, category bottom-left, date bottom-right) with an optional `showProfileIcon` flag for the feed variant. It replaces the corrupt `ActivityIncomeCard`, `ActivityExpenseCard`, and `ExpenseBucketCard` across the MP activity tab and interests tab. Followed MP income, expenses, and debate speeches are now loaded into the feed as `FeedItem.FinancialItem` and `FeedItem.SpeechItem` cards, with dedicated `FeedFinancialCard` and `FeedSpeechCard` composables wired into `FeedScreen`.

## Tasks Completed

- [x] 17-02-01: Create UnifiedFinancialCard.kt composable
- [x] 17-02-02: Replace ActivityIncomeCard/ActivityExpenseCard with UnifiedFinancialCard
- [x] 17-02-03: Replace ExpenseBucketCard in InterestsTabContent with UnifiedFinancialCard
- [x] 17-02-04: Add FeedItem.FinancialItem and SpeechItem to FeedUiState
- [x] 17-02-05: Add DebateSpeechDao queries for speeches by member
- [x] 17-02-06: Update FeedViewModel to load followed MP financial+speeches
- [x] 17-02-07: Create FeedFinancialCard/FeedSpeechCard and wire in FeedScreen

## Files Modified

### GovEye (Android)
- `app/.../feed/UnifiedFinancialCard.kt` (new): Unified financial card composable with LOCKED layout and `showProfileIcon` parameter; uses `MpAvatar` (party-colored border) and `parsePartyColor` for the income/expense icon tint.
- `app/.../feed/FeedFinancialCard.kt` (new): Feed wrapper calling `UnifiedFinancialCard` with `showProfileIcon = true`, date formatted via `formatDivisionDate`.
- `app/.../feed/FeedSpeechCard.kt` (new): Feed speech card — `MpAvatar` (party-colored border) + 3-line speech text (`bodyMedium`, `maxLines=3`, `Ellipsis`) + `TagPillRow` with inherited division tags.
- `app/.../feed/FeedUiState.kt`: Added `FINANCIAL` and `SPEECH` to `CardType` enum; added `FeedItem.FinancialItem` and `FeedItem.SpeechItem` data classes.
- `app/.../feed/UnifiedFeedCard.kt`: Added `FinancialItem`/`SpeechItem` branches to `getCardTypeData` to keep the exhaustive `when` compiling (these types are rendered by their own cards, never `UnifiedFeedCard`).
- `app/.../feed/FeedViewModel.kt`: Injected `InterestsRepository`, `ExpensesRepository`, `MembersRepository`, `DebateSpeechDao`; loads followed MP income (interests), expenses, and speeches inside the feed combine block; merges into date groups; caps feed at 200 items; added `formatFeedPence` helper.
- `app/.../FeedScreen.kt`: Restructured `FeedItemCard` to dispatch `FeedFinancialCard`/`FeedSpeechCard` for the new item types (exhaustive `when` over `FeedItem`).
- `app/.../mpprofile/ActivityTabContent.kt`: Removed `ActivityIncomeCard` and `ActivityExpenseCard` composables; INCOME/EXPENSE branches now call `UnifiedFinancialCard`; added `partyColorHex` parameter.
- `app/.../mpprofile/InterestsTabContent.kt`: Removed `ExpenseBucketCard` composable and `EXPENSE_BUCKET_ICONS` map; both expense bucket call sites now use `UnifiedFinancialCard`; added `partyColorHex` parameter.
- `app/.../mpprofile/MpProfileScreen.kt`: Passes `mp.party?.backgroundColour` as `partyColorHex` to `ActivityTabContent` and `InterestsTabContent`.
- `core/data/.../dao/DebateSpeechDao.kt`: Added `SpeechWithDivision` data class; added `getSpeechesByMember` and `getSpeechesByMemberIds` suspend queries (join divisions for title/date).

### goveye-data (Python)
- No changes required — all data already exists in the bundled DB.

## Decisions Made

- `MpAvatar` signature differs from the plan's assumed `imageUrl`/`initials`/`borderColor` params; used the actual `thumbnailUrl`/`displayName`/`partyColorHex`/`borderWidth` signature (border color is computed internally from `partyColorHex` via `parsePartyColor`).
- `ExpenseBucketTotal` only has `bucket` and `totalPence` (no `claimCount`/`monthEndDate`), so the interests-tab expense cards use "Monthly total" as the description and an empty date string.
- Income (interests) loaded per-member via `observeInterestsForMember(memberId).first()` (no batch DAO query existed; stayed within plan scope rather than adding one). Expenses loaded via `ExpensesRepository.getExpenses(mpId)`. Speeches loaded in a single batch via `getSpeechesByMemberIds`.
- Feed capped at 200 items (sorted by date descending before capping) to avoid performance issues, per plan point 5.
- `getCardTypeData` in `UnifiedFeedCard.kt` gained `FinancialItem`/`SpeechItem` branches returning placeholder `CardTypeData` solely to satisfy the exhaustive `when`; these types are never rendered through `UnifiedFeedCard`.

## Issues Encountered

- Initial compile after adding `FeedItem.FinancialItem`/`SpeechItem` failed because two exhaustive `when` expressions (`getCardTypeData` in `UnifiedFeedCard.kt` and the `onClick` `when` in `FeedScreen.kt`) no longer covered all subtypes. Fixed by adding branches to both; the `FeedScreen` branches were later replaced by dedicated card dispatch in task 17-02-07.
- `RepositoryResult` field is `data`, not `items` — corrected in the interests loading call.
