# Phase 18: Interest Entry Structured Parsing — Context

**Gathered:** 2026-08-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Parse raw interest entry summary text into structured fields in `build_interests.py` (goveye-data repo) so the seed DB ships with fully structured data. Update Android entity/mapper/UI to read the new columns and render them in the unified financial card design.

**In scope:**
- Python parser in `build_interests.py` that extracts structured fields from both Format A (simple/legacy) and Format B (structured/modern) interest entries
- New DB columns on the `interests` table for the parsed fields
- Room migration on Android for the new columns
- Update `InterestEntity.kt`, `Interest.kt` domain model, and mapper to read new columns
- Update `UnifiedFinancialCard` and `FinancialBucketDetailScreen` to use structured fields for "by X" / "for X" and expandable content
- Rebuild the seed DB with the parsed data

**Out of scope:**
- Changes to expense (IPSA) parsing — expenses already have structured columns
- Changes to the card visual design (already done in the card redesign commit)
- Changes to the feed or activity tab card rendering logic (already done)
- New UI screens or navigation changes
</domain>

<decisions>
## Implementation Decisions

### Parser Location
- **D-01:** Parser is Python-side in `build_interests.py` (goveye-data repo). The build script parses raw summary text into structured columns at build time. The seed DB ships with fully structured data. Android just reads the columns — no runtime parsing. — **Reversibility:** reversible — Python code + DB schema.

### Structured Fields to Extract
- **D-02:** 16 structured fields extracted from the summary text:
  1. `donorName` — donor/payer name (from `Donor Name:`, `Payer Name:`, `Name of donor:`, or first line before `- £` for Format A)
  2. `paymentType` — `Monetary` / `In kind` / `Cash` (from `Payment Type:`)
  3. `paymentDescription` — description of the payment (from `Payment Description:`, `Purpose of visit:`, `Description:`)
  4. `donorStatus` — `Individual` / `Company` / `Trade Union` etc. (from `Donor Status:`)
  5. `donorAddress` — donor's public address (from `Donor Public Address:`, `Payer Public Address:`)
  6. `donorCompanyIdentifier` — Companies House ID (from `Donor Company Identifier:`)
  7. `destination` — for visits (from `Destination of visit:` or extracted from `International visit to X`)
  8. `visitPurpose` — for visits (from `Purpose of visit:`)
  9. `organisationName` — for shareholdings (from `Organisation Name:` or `Shares in X`)
  10. `organisationDescription` — for shareholdings (from `Organisation Description:`)
  11. `propertyLocation` — for land/property (from `Location:` or extracted from `Residential in X`)
  12. `propertyType` — for land/property (from `Property Type:` or `Residential`/`Commercial`)
  13. `hoursWorked` — for employment (from `Hours Worked:`)
  14. `familyMemberName` — for family categories (from `Name:`)
  15. `familyMemberRelationship` — for family categories (from `Relationship:`)
  16. `familyMemberRole` — for family categories (from `Role:`)
- **Reversibility:** reversible — DB columns can be added/removed via migration.

### Parser Strategy
- **D-03:** Two-stage parsing:
  1. **Format B detection:** Check if summary contains `\n` and any field label pattern (`Donor Name:`, `Payer Name:`, etc.). If yes, split by newlines, extract `FieldName: Value` pairs into a dict, map to structured columns.
  2. **Format A parsing:** Use category-specific regex patterns:
     - Cat 2/3/5: `^(.+?) - £([\d,]+\.?\d*)` → donorName, amount
     - Cat 4: `^International visit to (.+?) between (.+?) and (.+)$` → destination, dates
     - Cat 6: `^(Residential|Commercial) in (.+)$` → propertyType, propertyLocation
     - Cat 7: `^Shares in (.+)$` → organisationName
     - Cat 8: Use full text as description
     - Cat 1: Use full text as job title / description
  3. **Fallback:** If no pattern matches, leave structured fields NULL, keep summary as-is.
- **Reversibility:** reversible — parser logic can be adjusted.

### Android Integration
- **D-04:** New columns added to `InterestEntity.kt` and `Interest.kt` domain model. Room migration (v23 → v24) adds the columns to the bundled DB. The `UnifiedFinancialCard` uses `donorName` for "by X" (income) and `paymentDescription` for the description line. The expandable content shows the full structured detail (all non-null fields formatted as a readable list).
- **Reversibility:** reversible — migration + entity changes.

### Card Rendering with Structured Fields
- **D-05:** When structured fields are available:
  - "by X" shows `donorName` (income) or `paymentDescription` (expense — already structured)
  - Description line shows `paymentDescription` or `visitPurpose` or `organisationDescription`
  - Expandable content shows all remaining structured fields formatted as `Field: Value` lines
  - When structured fields are NULL (unparseable entries), fall back to current behavior (first line of summary as "by X", full summary as expandable content)
- **Reversibility:** reversible — UI code.
</decisions>

<scope_fences>
## Out of Scope

- Expense (IPSA) parsing changes — expenses already have structured columns
- Card visual design changes — already done in the card redesign commit
- Feed or activity tab rendering logic changes — already done
- New UI screens or navigation
- Changes to `build_expenses.py` or the `expenses` table
</scope_fences>
