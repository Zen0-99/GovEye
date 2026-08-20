package com.goveye.app.ui.screens.directory

import com.goveye.app.domain.model.Mp

/**
 * A single filter option (e.g., "Labour", "Commons", "Current only").
 * Adapted from Miko's FilterOptionData — simplified (no StringResource).
 */
data class FilterOptionData(
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

/**
 * A filter section (e.g., "Party", "House", "Status").
 * Adapted from Miko's FilterSectionData — simplified (no LibraryFilterId).
 */
data class FilterSectionData(
    val id: String,
    val title: String,
    val items: List<FilterOptionData>,
)

/**
 * Tri-state for party filtering, matching Miko's TriState cycle:
 * DISABLED → INCLUDE → EXCLUDE → DISABLED.
 */
enum class PartyFilterState {
    DISABLED,
    INCLUDED,
    EXCLUDED,
    ;

    fun next(): PartyFilterState = when (this) {
        DISABLED -> INCLUDED
        INCLUDED -> EXCLUDED
        EXCLUDED -> DISABLED
    }
}

/**
 * Combined filter state for the directory.
 * Defaults: no party filter, all houses (0), include former (currentOnly=false).
 * "No active filters" = the natural browsing state showing everything.
 */
data class DirectoryFilterState(
    val includedParties: Set<String> = emptySet(),
    val excludedParties: Set<String> = emptySet(),
    val houseFilter: Int = 0,  // 0 = all, 1 = Commons, 2 = Lords
    val currentOnly: Boolean = false,
    val followingOnly: Boolean = false,  // feed filter — Following only toggle
) {
    val hasActiveFilters: Boolean
        get() = includedParties.isNotEmpty() || excludedParties.isNotEmpty() ||
            houseFilter != 0 || currentOnly || followingOnly

    /**
     * Returns the [PartyFilterState] for [party] — DISABLED, INCLUDED, or EXCLUDED.
     */
    fun partyState(party: String): PartyFilterState = when {
        party in includedParties -> PartyFilterState.INCLUDED
        party in excludedParties -> PartyFilterState.EXCLUDED
        else -> PartyFilterState.DISABLED
    }

    /**
     * Returns true if [mp] passes all active filters.
     */
    fun matches(mp: Mp): Boolean =
        (houseFilter == 0 || mp.house == houseFilter) &&
            (!currentOnly || mp.isActive) &&
            (includedParties.isEmpty() || mp.party?.name in includedParties) &&
            (mp.party?.name !in excludedParties)
}
