# Phase 18: Interest Entry Structured Parsing — Research

**Researched:** 2026-08-25
**Status:** Complete

## Pattern Analysis Results

### Overall Stats
- **Total entries:** 45,359
- **Entries with structured fields (Donor Name:/Payer Name:/Name of donor:):** 18,817 (41%)
- **Entries with simple format (single line, no newlines):** 30,541 (67%)
- **Entries with NULL parsedAmountPence:** 14,286 (31%) — all have no amount text either

### Two Format Generations

**Format A — Simple/Legacy (67% of entries):** Single-line text, no field labels.
- Cat 1: `Doctor` / `Writing articles for News UK and Ireland - News UK and Ireland`
- Cat 2: `Guildford Labour Party - £3,675.00` (donor name - amount)
- Cat 4: `International visit to China between 05 January 2025 and 10 January 2025`
- Cat 6: `Residential in Greater London`
- Cat 7: `Shares in Endava Ltd`
- Cat 8: `Since December 2015, a trustee of the Diane Abbott Foundation...`

**Format B — Structured/Modern (33% of entries):** Multi-line with field labels.
- Uses `FieldName: Value` format, newline-separated
- Has `Registration Date:` and `Published Date:` at the end
- Some have `Parent interest details:` section appended

### Per-Category Patterns

| Cat | Name | Count | Format A pattern | Format B fields |
|-----|------|-------|-----------------|-----------------|
| 1 | Employment | 10,469 | Job title only | (rare) |
| 1.1 | Ad hoc payments | 2,133 | `Payment received on DATE - £AMOUNT` | `Payment Type:`, `Value:`, `Hours Worked:`, `Hours Details:`, `Payer Name:`, `Payer Public Address:`, `Payer Nature Of Business:` |
| 1.2 | Ongoing employment | 542 | `Agreement - £AMOUNT` | `Regularity Of Payment:`, `Start Date:`, `Payment Type:`, `Payment Description:`, `Value:`, `Hours Worked:`, `Payer Name:`, `Payer Public Address:`, `Payer Nature Of Business:`, `Has Sought Acoba Advice:` |
| 2 | Donations | 14,898 | `Donor Name - £AMOUNT` | `Donation Source:`, `Payment Type:`, `Payment Description:`, `Value:`, `Is Sole Beneficiary:`, `Donor Name:`, `Donor Public Address:`, `Donor Status:`, `Donor Company Name:`, `Donor Company Identifier:` |
| 3 | Gifts UK | 5,621 | `Donor - £AMOUNT` | `Amount of donation:`, `Donor status:`, `Date of receipt:` |
| 4 | Visits outside UK | 5,205 | `International visit to DESTINATION between DATE and DATE` | `Purpose of visit:`, `Destination of visit:` |
| 5 | Overseas gifts | 231 | `Donor - £AMOUNT` | `Name of donor:`, `Date of receipt of donation:` |
| 6 | Land/Property | 1,172 | `Residential in LOCATION` | (rare — mostly simple format) |
| 7 | Shareholdings | 804 | `Shares in COMPANY` | `Shareholding Threshold:`, `Organisation Name:`, `Organisation Description:`, `Registrable Date:`, `End Date:` |
| 8 | Miscellaneous | 4,020 | Free text description | `Description:`, `Miscellaneous Interest Type:` |
| 9 | Family employed | 177 | `Name employed` | `Name:`, `Relationship:`, `Role:`, `Working pattern:` |
| 10 | Family lobbying | 87 | `Name employed as Lobbyist` | `Name:`, `Relationship:`, `Role:`, `Name of employer:`, `End date:` |

### Cross-Category Patterns

**Universal fields (appear across categories):**
- `Registration Date:` / `Published Date:` — in all Format B entries
- Amount — in `parsedAmountPence` for Format B; embedded in text for Format A

**Donor/Payer name field variants:**
- Cat 1.1/1.2: `Payer Name:`
- Cat 2: `Donor Name:`
- Cat 3/5: `Name of donor:` (older format) or `Donor Name:` (newer)
- Cat 9/10: `Name:` (family member, not donor)

**Payment Type field:** appears in Cat 1.1, 1.2, 2 — values: `Monetary`, `In kind`, `Cash`

### Suggested Unified Fields for Parser

The parser should extract these fields from the `summary` text and store as new DB columns:

1. **donorName** — donor/payer name (from `Donor Name:`, `Payer Name:`, `Name of donor:`, or first line before `- £` for Format A)
2. **paymentType** — `Monetary` / `In kind` / `Cash` (from `Payment Type:`)
3. **paymentDescription** — description of the payment (from `Payment Description:`, `Purpose of visit:`, `Description:`)
4. **donorStatus** — `Individual` / `Company` / `Trade Union` etc. (from `Donor Status:`)
5. **donorAddress** — donor's public address (from `Donor Public Address:`, `Payer Public Address:`)
6. **donorCompanyIdentifier** — Companies House ID (from `Donor Company Identifier:`)
7. **destination** — for visits (from `Destination of visit:` or extracted from `International visit to X`)
8. **visitPurpose** — for visits (from `Purpose of visit:`)
9. **organisationName** — for shareholdings (from `Organisation Name:` or `Shares in X`)
10. **organisationDescription** — for shareholdings (from `Organisation Description:`)
11. **propertyLocation** — for land/property (from `Location:` or extracted from `Residential in X`)
12. **propertyType** — for land/property (from `Property Type:` or `Residential`/`Commercial`)
13. **hoursWorked** — for employment (from `Hours Worked:`)
14. **familyMemberName** — for family categories (from `Name:`)
15. **familyMemberRelationship** — for family categories (from `Relationship:`)
16. **familyMemberRole** — for family categories (from `Role:`)

### Parser Strategy

1. **Format B detection:** Check if summary contains `\n` and any field label pattern (`Donor Name:`, `Payer Name:`, etc.)
2. **Format B parsing:** Split by newlines, extract `FieldName: Value` pairs into a dict, map to structured columns
3. **Format A parsing:** Use category-specific regex patterns:
   - Cat 2/3/5: `^(.+?) - £([\d,]+\.?\d*)` → donorName, amount
   - Cat 4: `^International visit to (.+?) between (.+?) and (.+)$` → destination, dates
   - Cat 6: `^(Residential|Commercial) in (.+)$` → propertyType, propertyLocation
   - Cat 7: `^Shares in (.+)$` → organisationName
   - Cat 8: Use full text as description
   - Cat 1: Use full text as job title / description
4. **Fallback:** If no pattern matches, leave structured fields NULL, keep summary as-is

### Implementation Location

**Python-side in `build_interests.py`** (goveye-data repo) — per user decision. The build script will:
1. Parse each interest entry's summary text after fetching from the API
2. Store structured fields as new columns in the `interests` table
3. The seed DB ships with fully structured data — Android just reads the columns

### Android-Side Changes

1. Add new columns to `InterestEntity.kt` and `Interest.kt` domain model
2. Update `InterestDao` mapper to read new columns
3. Update `UnifiedFinancialCard` to use structured fields for "by X" / "for X" and expandable content
4. Room migration for the new columns
