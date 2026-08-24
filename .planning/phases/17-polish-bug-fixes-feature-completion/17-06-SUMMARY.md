---
plan: 17-06
title: Searchbar animation + light mode card color
status: complete
tasks_total: 2
tasks_completed: 2
---

# Plan 17-06 Summary: Searchbar Animation + Light Mode Card Color

## What Was Built

Fixed two visual polish issues from UAT: (1) the searchbar right-side transition jump when navigating back from profile to directory — the filter button now fades in/out via `AnimatedContent` with animated width, and a committed-navigation guard prevents the searchbar config from updating during predictive back preview. The searchbar color also now uses a direct `MaterialTheme.colorScheme` reference instead of `animateColorAsState`, eliminating the light/dark mode toggle lag. (2) Darkened the light mode `surfaceContainer` and cascading surface tones in `SkyColorScheme` from `#F0F0F0` to `#E8E8E8` so cards are visibly distinct from the background.

## Tasks Completed

- [x] 17-06-01: Animate searchbar right-side transition and guard predictive back config updates
- [x] 17-06-02: Darken light mode surface container colors for card readability

## Files Modified

- `core/ui/src/main/java/com/goveye/app/ui/components/FloatingSearchBar.kt`: Replaced instant filter button show/hide with `AnimatedContent` (fadeIn/fadeOut) wrapped in an `animateDpAsState`-driven width container. Removed `animateColorAsState` on the searchbar background — now uses a direct `MaterialTheme.colorScheme.surfaceContainer` reference so theme changes propagate synchronously.
- `app/src/main/java/com/goveye/app/ui/GovEyeApp.kt`: Added `committedSearchConfig` snapshot updated via `LaunchedEffect(currentRoute)` — the FloatingSearchBar now reads placeholder, onFilterClick, and hasActiveFilters from this committed snapshot instead of the live config, preventing premature updates during predictive back preview.
- `core/ui/src/main/java/com/goveye/app/ui/theme/colorscheme/SkyColorScheme.kt`: Updated light mode surface containers — `surfaceContainerLow` `#F5F5F5`→`#F0F0F0`, `surfaceContainer` `#F0F0F0`→`#E8E8E8`, `surfaceContainerHigh` `#EBEBEB`→`#E2E2E2`, `surfaceContainerHighest` `#E5E5E5`→`#DCDCDC`. Dark mode values unchanged.

## Decisions Made

- Used `committedSearchConfig` snapshot for visual fields (placeholder, filter button, hasActiveFilters) while keeping live `searchConfig` for interactive fields (query, onQueryChange, filterChips, segments, isSearchActive) — this preserves real-time search typing while preventing the predictive back preview from changing the searchbar appearance prematurely.
- Removed `animateColorAsState` entirely from the searchbar background rather than trying to selectively animate only accent transitions — the direct color reference is simpler and fully resolves the theme toggle lag. The accent color blend (25% party color over surfaceContainer) still applies instantly, with the `AnimatedVisibility` on action/back buttons providing visual transition smoothness.

## Issues Encountered

- Missing `togetherWith` import in `FloatingSearchBar.kt` — the original file only used the `+` operator for combining enter/exit transitions, so `togetherWith` (needed for `AnimatedContent`'s `transitionSpec`) was not imported. Added the import.
- ktlint max-line-length (120) violation on the filter button tint line — split the inline `if/else` into a block expression to stay under the limit.
