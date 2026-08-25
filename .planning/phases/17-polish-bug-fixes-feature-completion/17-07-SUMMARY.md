---
plan: 17-07
title: Speaker matching fix + speech activity cards
status: complete
tasks_total: 4
tasks_completed: 4
---

# Plan 17-07 Summary: Speaker Matching + Speech Cards

## What Was Built

Fixed broken speaker matching in debate transcripts (issue #14) by adding a manual override table, name cross-referencing, and unmatched-speaker logging in the build script, plus a name-validation fallback in TranscriptViewModel that prevents wrong-profile navigation. Added SPEECH as a new activity entry type with an ActivitySpeechCard composable (3-line text + inherited division tags) in the MP activity tab. Feed speech cards (FeedItem.SpeechItem + FeedSpeechCard) were already implemented by plan 17-02 and verified complete with an added DAO alias.

## Tasks Completed

- [x] 17-07-01: Fix speaker matching in build_debates.py — added SPEAKER_OVERRIDES dict (with "Baroness Stedman-Scott" entry), name cross-referencing after twfyPersonId match, and unmatched_speakers.txt logging
- [x] 17-07-02: Fix speaker resolution fallback in TranscriptViewModel.kt — added name validation that removes mismatched historical members from the map, name-based search fallback via searchByDisplayName DAO query, and updated TranscriptScreen to use partyAbbreviation/partyColourHex from HistoricalMemberEntity instead of raw lowercase party strings
- [x] 17-07-03: Add SPEECH ActivityEntryType + ActivitySpeechCard — added SPEECH to enum, speechText/speechTags fields to ActivityEntry, getSpeechesByMember method to VotesRepository, loadSpeechEntries in ProfileViewModel, ActivitySpeechCard composable (bodyMedium maxLines=3 Ellipsis + TagPillRow), SPEECH branch in when block, and "Speeches" label in ActivityFilterBottomSheet
- [x] 17-07-04: Verify/complete feed speech cards — 17-02 already implemented FeedItem.SpeechItem, FeedSpeechCard, FeedViewModel speech loading, and FeedScreen wiring; added getSpeechesByFollowedMembers DAO alias to satisfy acceptance criteria

## Files Modified

### GovEye (Android)
- `core/domain/src/main/java/com/goveye/app/domain/model/ActivityEntry.kt`: Added SPEECH to ActivityEntryType enum, speechText and speechTags fields to ActivityEntry data class
- `core/data/src/main/java/com/goveye/app/data/local/dao/DebateSpeechDao.kt`: Added getSpeechesByFollowedMembers alias method
- `core/data/src/main/java/com/goveye/app/data/local/dao/HistoricalMemberDao.kt`: Added searchByDisplayName query for name-based fallback
- `core/data/src/main/java/com/goveye/app/data/repo/HistoricalMemberRepository.kt`: Added searchByDisplayName method
- `core/data/src/main/java/com/goveye/app/data/repo/VotesRepository.kt`: Added getSpeechesByMember method and SpeechWithDivision import
- `app/src/main/java/com/goveye/app/ui/screens/divisions/TranscriptViewModel.kt`: Added name validation loop in load() that cross-references historical member displayName with speech speakerName, removes mismatched entries, and falls back to name-based search; added namesMatch companion object helper
- `app/src/main/java/com/goveye/app/ui/screens/divisions/TranscriptScreen.kt`: Updated SpeechCard to use partyAbbreviation and partyColourHex from HistoricalMemberEntity instead of raw party string and partyNameToColorHex fallback
- `app/src/main/java/com/goveye/app/ui/screens/mpprofile/MpProfileViewModel.kt`: Injected TagDao, added loadSpeechEntries method, wired speech loading into loadActivityFeed and reloadFilteredEntries
- `app/src/main/java/com/goveye/app/ui/screens/mpprofile/ActivityTabContent.kt`: Added ActivitySpeechCard composable (bodyMedium maxLines=3 Ellipsis + TagPillRow with inherited division tags), SPEECH branch in when block, Surface/Spacer/height imports
- `app/src/main/java/com/goveye/app/ui/screens/mpprofile/ActivityFilterBottomSheet.kt`: Added SPEECH -> "Speeches" branch in displayName() when expression

### goveye-data (Python)
- `build_debates.py`: Added SPEAKER_OVERRIDES dict (with "Baroness Stedman-Scott": 4735), _names_match helper for cross-referencing speaker name with DB name, _log_unmatched_speaker for logging to unmatched_speakers.txt, updated build_twfy_id_lookup to return (parl_id, display_name) tuples, updated parse_debate_page to check overrides first, cross-reference names after twfyPersonId match, and log unmatched speakers

## Decisions Made

- **build_debates.py not build_hansard.py**: The plan referenced build_hansard.py but the actual speaker matching logic lives in build_debates.py. build_hansard.py only fetches contribution counts. Applied fixes to the correct file.
- **Map-based null instead of speakerMpId field**: The plan's acceptance criteria expected `speakerMpId = null` but TranscriptViewModel uses a Map<Int, HistoricalMemberEntity> keyed by twfyPersonId. The equivalent behavior is removing mismatched entries from the map, which prevents the SpeechCard from navigating to a wrong profile.
- **No DebateSpeechRepository created**: The plan mentioned creating a DebateSpeechRepository, but the existing architecture accesses DebateSpeechDao via VotesRepository. Added getSpeechesByMember to VotesRepository instead, consistent with existing patterns.
- **getSpeechesByFollowedMembers as alias**: 17-02 already implemented getSpeechesByMemberIds which serves the same purpose. Added getSpeechesByFollowedMembers as a delegating alias to satisfy the plan's acceptance criteria without duplicating query logic.

## Issues Encountered

- **Exhaustive when block**: Adding SPEECH to ActivityEntryType broke the exhaustive when in ActivityFilterBottomSheet.kt's displayName() function. Fixed by adding the SPEECH -> "Speeches" branch.
- **Missing imports**: ActivitySpeechCard used Surface, Spacer, and height without imports. Added the missing import statements.
- **Windows console encoding**: `python build_debates.py --help` fails with UnicodeEncodeError on Windows cp1252 console due to arrow characters in docstrings. Works correctly with PYTHONIOENCODING=utf-8. Pre-existing issue, not introduced by this plan.
