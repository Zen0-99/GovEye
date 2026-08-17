package com.goveye.app.ui.screens.directory

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
 * Combined filter state for the directory.
 * Defaults: no party filter, Commons (house=1), current only.
 */
data class DirectoryFilterState(
    val selectedParties: Set<String> = emptySet(),
    val houseFilter: Int = 1,  // 0 = all, 1 = Commons, 2 = Lords
    val currentOnly: Boolean = true,
) {
    val hasActiveFilters: Boolean
        get() = selectedParties.isNotEmpty() || houseFilter != 1 || !currentOnly
}
