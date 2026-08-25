---
plan: 18-03
subsystem: ui
tags: compose, financial-card, structured-fields, feed, activity
key-files:
  - FinancialEntry.kt (16 new fields)
  - ActivityEntry.kt (16 new fields)
  - InterestStructuredFields.kt (new — formatInterestStructuredDetail, interestDescriptionLine)
  - FinancialBucketDetailScreen.kt (FinancialEntryCard updated)
  - FeedViewModel.kt (income mapping updated)
  - FeedFinancialCard.kt (structuredDetail for expandable)
  - MpProfileViewModel.kt (loadIncomeEntries updated)
  - ActivityTabContent.kt (INCOME card updated)
---

# Plan 18-03 Summary: Card UI integration — structured fields in all 3 rendering paths

## What was done

- Added 16 structured fields to `FinancialEntry` and `ActivityEntry` domain models
- Created `InterestStructuredFields.kt` with two helpers:
  - `formatInterestStructuredDetail()` — builds expandable "Label: Value" lines from secondary fields
  - `interestDescriptionLine()` — picks best description (paymentDescription > visitPurpose > organisationDescription)
- Updated all 3 card rendering paths:
  1. **FinancialBucketDetailScreen** — `Interest.toFinancialEntry()` passes 16 fields; `FinancialEntryCard` uses `donorName` for "by X", `interestDescriptionLine()` for description, `formatInterestStructuredDetail()` for expandable content (with fallbacks to summary)
  2. **Feed** — `FeedViewModel` income mapping uses structured fields; `FeedFinancialCard` uses `structuredDetail` for expandable
  3. **Activity tab** — `MpProfileViewModel.loadIncomeEntries()` passes 16 fields; `ActivityTabContent` INCOME card uses structured fields

## Fallback behavior (D-05)

When structured fields are NULL (unparseable entries):
- "by X" falls back to first line of summary (take 80 chars)
- Description line falls back to empty string
- Expandable content falls back to full summary (when >80 chars)

## Verification

- `spotlessApply` passes
- `:app:compileDebugKotlin` passes
- App installed on Pixel 8 Pro
