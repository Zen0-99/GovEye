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
     * Maps selected tags to recommended departments using the
     * source_recommendations table (isRecommended=1 entries).
     *
     * For each selected tag, queries source_recommendations where
     * isRecommended=1. Groups by organisationSlug. For each department,
     * all 3 streams are pre-checked (isChecked=true) per D-05.
     * Deduplicates departments across tags.
     *
     * @return sorted by organisationName
     */
    fun getRecommendedDepartments(
        selectedTags: Set<String>,
        allRecommendations: List<SourceRecommendation>
    ): List<RecommendedDepartment> {
        if (selectedTags.isEmpty()) return emptyList()

        val recommendedRecs = allRecommendations
            .filter { it.tag in selectedTags && it.isRecommended }

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
