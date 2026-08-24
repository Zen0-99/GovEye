---
plan: 17-05
title: Follow reactivity + Following sub-tabs
status: complete
tasks_total: 8
tasks_completed: 8
---

# Plan 17-05 Summary: Follow Reactivity + Following Sub-tabs

## What Was Built

Fixed three UAT issues: (1) follow/notification icons now update optimistically on tap — the
`MpProfileViewModel` flips `_uiState` before the repository call and reverts on failure, so the
follow bell and notification checkboxes respond instantly without a profile reload. (2) Added
Income and Expenses notification types end-to-end — new `incomeEnabled`/`expensesEnabled` columns
on `MpNotificationPreferenceEntity` (LocalDatabase v2→v3 additive migration), matching DAO +
repository methods, two new notification channels in `NotificationHelper`, and two new checkboxes
in the `NotificationSettingsBottomSheet`. (3) Redesigned the Following screen from a section-based
`LazyColumn` into a `SubTabPager` with four tabs (Officials, Parties, Sources, Tags) and replaced
the followed-MP card's three-dot overflow menu with a swipe-to-dismiss unfollow gesture.

## Tasks Completed

- [x] 17-05-01: Optimistic UI updates in MpProfileViewModel (toggleFollow, setNotificationsEnabled,
      setVotesNotificationsEnabled, setSpeechesNotificationsEnabled) + new
      setIncomeNotificationsEnabled/setExpensesNotificationsEnabled + incomeEnabled/expensesEnabled
      fields in ProfileUiState
- [x] 17-05-02: Added incomeEnabled and expensesEnabled fields to MpNotificationPreferenceEntity
      and extended the notificationsEnabled derived property
- [x] 17-05-03: Added DAO methods (setIncomeEnabled, setExpensesEnabled,
      getMemberIdsWithIncomeEnabled, getMemberIdsWithExpensesEnabled) and repository methods;
      updated setNotificationsEnabled to clear income/expense on OFF
- [x] 17-05-04: Bumped LocalDatabase to v3 and added LOCAL_MIGRATION_2_3 (additive ALTER TABLE
      for incomeEnabled and expensesEnabled columns)
- [x] 17-05-05: Added INCOME_CHANNEL_ID/EXPENSE_CHANNEL_ID constants, channel creation, and
      showIncomeNotification/showExpenseNotification methods to NotificationHelper
- [x] 17-05-06: Added Income and Expenses NotificationTypeRow entries to
      NotificationSettingsBottomSheet and wired the new parameters at the MpProfileScreen call site
- [x] 17-05-07: Redesigned FollowingScreen with SubTabPager (Officials, Parties, Sources, Tags),
      per-tab empty states, and removed SectionHeader/SectionEmptyHint
- [x] 17-05-08: Removed the three-dot overflow icon from FollowedMpCard and replaced the unfollow
      action with a SwipeToDismissBox (red "Unfollow" background revealed on swipe)

## Files Modified

- `app/.../mpprofile/MpProfileViewModel.kt`: optimistic UI updates in toggleFollow and the three
  notification setters; new setIncomeNotificationsEnabled/setExpensesNotificationsEnabled;
  incomeEnabled/expensesEnabled fields in ProfileUiState; loadProfile populates the new fields
- `core/data/.../entity/MpNotificationPreferenceEntity.kt`: incomeEnabled and expensesEnabled
  fields; notificationsEnabled OR chain extended
- `core/data/.../dao/MpNotificationPreferenceDao.kt`: four new DAO methods for income/expense prefs
- `core/data/.../repo/NotificationPreferenceRepository.kt`: setIncomeEnabled/setExpensesEnabled,
  getMemberIdsWithIncomeEnabled/getMemberIdsWithExpensesEnabled; setNotificationsEnabled clears
  income/expense on OFF
- `core/data/.../local/LocalDatabase.kt`: version bumped 2 → 3
- `app/.../di/DatabaseModule.kt`: LOCAL_MIGRATION_2_3 added to the LocalDatabase migration chain
- `app/.../notifications/NotificationHelper.kt`: income/expense channel constants, channel
  creation, showIncomeNotification/showExpenseNotification methods
- `app/.../mpprofile/NotificationSettingsBottomSheet.kt`: incomeEnabled/expensesEnabled/
  onIncomeToggle/onExpensesToggle parameters; Income and Expenses NotificationTypeRow entries
- `app/.../mpprofile/MpProfileScreen.kt`: passes the new income/expense params to
  NotificationSettingsBottomSheet
- `app/.../screens/FollowingScreen.kt`: SubTabPager with four tabs replacing the section list;
  OfficialsTabContent + TabEmptyHint composables; FollowedMpCard three-dot menu replaced with
  SwipeToDismissBox; SectionHeader/SectionEmptyHint removed

## Decisions Made

- **Task execution order**: The plan listed 17-05-01 first, but it references
  `notificationPrefRepository.setIncomeEnabled`/`setExpensesEnabled` and `notifPref.incomeEnabled`/
  `expensesEnabled` which only exist after 17-05-02 (entity) and 17-05-03 (repo). To keep each
  commit compiling, the data-layer tasks (17-05-02, 17-05-03) were committed before 17-05-01, then
  17-05-04 (which requires both app and core:data to compile) after 17-05-01. All eight commits
  are present with their correct task IDs.
- **Field naming**: ProfileUiState already used `votesNotificationsEnabled`/
  `speechesNotificationsEnabled`; the new fields follow the acceptance-criteria-specified
  `incomeEnabled`/`expensesEnabled` names (shorter, matching the entity field names).
- **SwipeToDismissBox**: Used the M3 `SwipeToDismissBox` with a `confirmValueChange` that allows
  dismiss in either direction; a `LaunchedEffect` on `currentValue` fires `onUnfollow`. The
  `confirmValueChange` overload is deprecated but still compiles and functions correctly.

## Issues Encountered

- `rememberSwipeToDismissBoxState(confirmValueChange = ...)` emits a deprecation warning
  (confirmValueChange is deprecated without replacement in favour of dynamic anchors). It compiles
  and works; a future pass can migrate to the anchored-draggable API if desired.
- No build failures encountered — all eight tasks compiled cleanly on first attempt after the
  execution-order adjustment.
