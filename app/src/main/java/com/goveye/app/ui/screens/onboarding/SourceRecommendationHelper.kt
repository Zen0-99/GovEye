package com.goveye.app.ui.screens.onboarding

import com.goveye.app.domain.model.SourceRecommendation

/**
 * Maps selected tags to recommended departments using the
 * source_recommendations table (precomputed by 14-02 build_source_recs.py
 * per D-06 hybrid tag→department mapping).
 *
 * Per D-05: recommended departments have all 3 streams pre-checked.
 * Per D-04: source = department × data stream (~75 combinations).
 */
object SourceRecommendationHelper {

    /**
     * 25 major UK government departments with their GOV.UK slugs and names.
     * Used as a fallback when the BundledDatabase is not yet populated
     * (seed download runs in background throughout onboarding — D-02).
     */
    private val FALLBACK_DEPARTMENTS = listOf(
        "attorney-generals-office" to "Attorney General's Office",
        "cabinet-office" to "Cabinet Office",
        "department-for-business-and-trade" to "Department for Business and Trade",
        "department-for-culture-media-and-sport" to "Department for Culture, Media and Sport",
        "department-for-digital-culture-media-and-sport" to "Department for Science, Innovation and Technology",
        "department-for-education" to "Department for Education",
        "department-for-energy-security-and-net-zero" to "Department for Energy Security and Net Zero",
        "department-for-environment-food-rural-affairs" to "Department for Environment, Food & Rural Affairs",
        "department-for-levelling-up-housing-and-communities" to "Department for Levelling Up, Housing and Communities",
        "department-for-science-innovation-and-technology" to "Department for Science, Innovation and Technology",
        "department-for-transport" to "Department for Transport",
        "department-for-work-and-pensions" to "Department for Work and Pensions",
        "department-of-health-and-social-care" to "Department of Health and Social Care",
        "foreign-commonwealth-development-office" to "Foreign, Commonwealth & Development Office",
        "government-equalities-office" to "Government Equalities Office",
        "hm-revenue-customs" to "HM Revenue & Customs",
        "hm-treasury" to "HM Treasury",
        "home-office" to "Home Office",
        "ministry-of-defence" to "Ministry of Defence",
        "ministry-of-justice" to "Ministry of Justice",
        "northern-ireland-office" to "Northern Ireland Office",
        "office-of-the-advocate-general-for-scotland" to "Office of the Advocate General for Scotland",
        "office-of-the-leader-of-the-house-of-commons" to "Office of the Leader of the House of Commons",
        "scotland-office" to "Scotland Office",
        "wales-office" to "Wales Office"
    )

    /**
     * Fallback tag→department mapping used when the source_recommendations
     * table is empty (first launch before seed download completes).
     * Maps each of the 26 tags to the most relevant 1-3 departments.
     * Mirrors the hybrid mapping in build_source_recs.py (D-06).
     */
    private val FALLBACK_TAG_DEPARTMENTS: Map<String, List<Pair<String, String>>> = mapOf(
        "Universal Credit" to listOf("department-for-work-and-pensions" to "Department for Work and Pensions"),
        "PIP & Disability Benefits" to listOf("department-for-work-and-pensions" to "Department for Work and Pensions"),
        "Disability" to listOf("department-for-work-and-pensions" to "Department for Work and Pensions"),
        "Welfare & Social Security" to listOf("department-for-work-and-pensions" to "Department for Work and Pensions"),
        "Immigration & Asylum" to listOf("home-office" to "Home Office"),
        "Budget & Fiscal" to listOf("hm-treasury" to "HM Treasury"),
        "Taxation" to listOf("hm-revenue-customs" to "HM Revenue & Customs", "hm-treasury" to "HM Treasury"),
        "NHS" to listOf("department-of-health-and-social-care" to "Department of Health and Social Care"),
        "Social Care" to listOf("department-of-health-and-social-care" to "Department of Health and Social Care"),
        "Mental Health" to listOf("department-of-health-and-social-care" to "Department of Health and Social Care"),
        "Education" to listOf("department-for-education" to "Department for Education"),
        "Children & Families" to
            listOf(
                "department-for-education" to "Department for Education",
                "department-for-work-and-pensions" to "Department for Work and Pensions"
            ),
        "Climate & Environment" to
            listOf(
                "department-for-energy-security-and-net-zero" to "Department for Energy Security and Net Zero",
                "department-for-environment-food-rural-affairs" to "Department for Environment, Food & Rural Affairs"
            ),
        "Justice & Crime" to listOf("ministry-of-justice" to "Ministry of Justice", "home-office" to "Home Office"),
        "Human Rights" to
            listOf(
                "ministry-of-justice" to "Ministry of Justice",
                "attorney-generals-office" to "Attorney General's Office"
            ),
        "Defence" to listOf("ministry-of-defence" to "Ministry of Defence"),
        "Housing" to
            listOf(
                "department-for-levelling-up-housing-and-communities" to
                    "Department for Levelling Up, Housing and Communities"
            ),
        "Transport" to listOf("department-for-transport" to "Department for Transport"),
        "Brexit & EU" to
            listOf(
                "foreign-commonwealth-development-office" to "Foreign, Commonwealth & Development Office",
                "cabinet-office" to "Cabinet Office"
            ),
        "Foreign Policy" to
            listOf("foreign-commonwealth-development-office" to "Foreign, Commonwealth & Development Office"),
        "Employment & Workers" to
            listOf(
                "department-for-business-and-trade" to "Department for Business and Trade",
                "department-for-work-and-pensions" to "Department for Work and Pensions"
            ),
        "Business & Enterprise" to listOf("department-for-business-and-trade" to "Department for Business and Trade"),
        "Energy" to
            listOf("department-for-energy-security-and-net-zero" to "Department for Energy Security and Net Zero"),
        "Constitutional & Devolution" to
            listOf("cabinet-office" to "Cabinet Office", "ministry-of-justice" to "Ministry of Justice"),
        "Technology & Digital" to
            listOf(
                "department-for-science-innovation-and-technology" to
                    "Department for Science, Innovation and Technology"
            ),
        "Agriculture & Farming" to
            listOf(
                "department-for-environment-food-rural-affairs" to "Department for Environment, Food & Rural Affairs"
            )
    )

    /**
     * Maps selected tags to recommended departments using the
     * source_recommendations table (isRecommended=1 entries).
     *
     * Falls back to [FALLBACK_TAG_DEPARTMENTS] when the DB table is empty
     * (first launch before seed download completes) so the user always
     * sees recommendations based on their selected tags.
     *
     * @return sorted by organisationName
     */
    fun getRecommendedDepartments(
        selectedTags: Set<String>,
        allRecommendations: List<SourceRecommendation>
    ): List<RecommendedDepartment> {
        if (selectedTags.isEmpty()) return emptyList()

        // Try DB-backed recommendations first
        val recommendedRecs = allRecommendations
            .filter { it.tag in selectedTags && it.isRecommended }

        if (recommendedRecs.isNotEmpty()) {
            val byDepartment = recommendedRecs
                .groupBy { it.organisationSlug }
                .mapValues { (_, recs) -> recs.first().organisationName }

            return byDepartment.entries
                .map { (slug, name) ->
                    RecommendedDepartment(
                        organisationSlug = slug,
                        organisationName = name,
                        streams = StreamType.ALL.map { StreamState(it, isChecked = true) }
                    )
                }
                .sortedBy { it.organisationName }
        }

        // Fallback: static tag→department mapping (DB not yet populated)
        val fallbackDepts = selectedTags
            .flatMap { tag -> FALLBACK_TAG_DEPARTMENTS[tag] ?: emptyList() }
            .distinctBy { it.first }

        return fallbackDepts
            .map { (slug, name) ->
                RecommendedDepartment(
                    organisationSlug = slug,
                    organisationName = name,
                    streams = StreamType.ALL.map { StreamState(it, isChecked = true) }
                )
            }
            .sortedBy { it.organisationName }
    }

    /**
     * Returns all department-stream combinations grouped by department
     * for the "All sources" section. Uses the fallback hardcoded list
     * of 25 departments × 3 streams = 75 combinations (D-04).
     */
    fun getAllSources(): List<DepartmentGroup> = FALLBACK_DEPARTMENTS
        .map { (slug, name) ->
            DepartmentGroup(
                organisationName = name,
                organisationSlug = slug,
                streams = StreamType.ALL.map { StreamState(it, isChecked = false) }
            )
        }
        .sortedBy { it.organisationName }

    /**
     * Returns all department-stream combinations from the database
     * (distinct organisationSlug/organisationName from government_publications).
     * Falls back to [getAllSources] if the list is empty (DB not yet populated).
     */
    fun getAllSourcesFromDb(orgs: List<Pair<String, String>>): List<DepartmentGroup> {
        if (orgs.isEmpty()) return getAllSources()
        return orgs
            .map { (slug, name) ->
                DepartmentGroup(
                    organisationName = name,
                    organisationSlug = slug,
                    streams = StreamType.ALL.map { StreamState(it, isChecked = false) }
                )
            }
            .sortedBy { it.organisationName }
    }
}
