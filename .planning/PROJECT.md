# GovEye

## What This Is

GovEye is a free, open-source Android app that makes UK Parliament accessible and engaging — like FotMob for politics. It lets users follow their MPs, track bills, get notifications when representatives vote or speak, and see stats (voting record, political leanings, declared income, committee membership) in a polished, modern interface designed for audiences who find politics opaque but would engage if it felt like tracking a sport.

## Core Value

Make UK government activity as easy to follow as a football team — so anyone, especially younger voters, can see what their representatives are actually doing.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Browse and search all 650 MPs with rich profiles (party, voting record, committee membership, declared interests, political leanings)
- [ ] Follow MPs and receive notifications when they vote, speak, or take action in Parliament
- [ ] Track bills through Parliament with plain-English summaries and progress stages
- [ ] Stats and data visualizations — voting patterns, rebellion rate, attendance, income declarations
- [ ] News feed of parliamentary activity (debates, questions, statements)
- [ ] Find your MP by postcode/constituency
- [ ] Polished Material 3 design that feels like a 2025 app, not a government portal
- [ ] Open source under a permissive license (MIT or GPL)

### Out of Scope

- iOS — Android-first; iOS is a future consideration, not v1
- Web app — native Android only for v1; web may come later
- Paid tiers or monetization — pure open source, no premium features
- WriteToThem / contact-your-MP functionality — TheyWorkForYou already does this well
- Scottish Parliament / Senedd / Northern Ireland Assembly — v1 focuses on UK Parliament (Westminster); devolved legislatures are future scope
- AI-generated political stance classification — UK Polity does this for paid professional use; GovEye sticks to factual data from Parliament APIs
- User accounts / social features — v1 is a tracking/information app, not a social platform

## Context

**Inspiration:** Govvy (iOS app for US government representatives) and FotMob (football tracking app). The "FotMob for politics" framing drives the product — follow reps like players, get notifications, see stats, track activity.

**Market gap:** Existing UK politics tracking solutions are either:
- Web-only (YouBase, TrackPolitics, Parli.uk, TheyWorkForYou) — no native mobile experience
- Official but buggy and basic (CommonsVotes — iOS only, blank screen bugs, just votes)
- Paid professional tools (UK Polity — £350/month, for lobbyists)
- Legacy and dated (TheyWorkForYou — 20 years old, charity-run, functional but not designed for younger audiences)

No free, polished, native Android app exists that targets younger UK voters with a FotMob-style experience.

**Data sources:** UK Parliament provides extensive open data APIs under Open Parliament Licence:
- UK Parliament Developer Hub (developer.parliament.uk) — API directory
- Commons Votes API (commonsvotes-api.parliament.uk) — division/vote data
- api.parliament.uk — REST, OData, SPARQL, query endpoints
- Members API — MP data (recommended over legacy MNIS)
- TheyWorkForYou API — free for low-volume charitable use (Hansard, debates, voting records)

**Audience:** Primary focus is younger UK voters (Millennials, Gen Z, Gen Alpha) who find politics opaque but would engage with a well-designed tracking app. Not exclusive to this audience — the app should work for anyone who wants to follow UK politics.

## Constraints

- **Platform**: Android-first (native) — iOS and web are future scope
- **Data license**: Open Parliament Licence governs all parliamentary data usage
- **Open source**: Pure open source project, no monetization, volunteer-built
- **Design**: Material 3, polished native — must feel like a 2025 app, not a government portal
- **Data freshness**: Commons Votes API updates ~20 minutes after results announced in the Chamber; notifications should reflect near-real-time activity

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Android-first, not iOS | Inspiration (Govvy) is iOS-only for US; gap in UK market is Android. User preference. | — Pending |
| Pure open source (no monetization) | User preference — community project, volunteer-built | — Pending |
| UK Parliament (Westminster) only for v1 | Focus scope; devolved legislatures add complexity without core value | — Pending |
| FotMob-style engagement model | Differentiates from existing solutions which are informational, not engagement-driven | — Pending |
| Material 3 design language | User convention — polished native, modern, consistent with other projects | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-16 after initialization*
