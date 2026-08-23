package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.preference.DirectoryViewMode

/**
 * Which tab is active — determines which filter sections to show.
 */
enum class FilterTabType {
    OFFICIALS,
    DIVISIONS,
    FEED,
    GOVERNMENT,
    FOLLOWING_HUB,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    distinctParties: List<String>,
    filterState: DirectoryFilterState,
    tabType: FilterTabType,
    viewMode: DirectoryViewMode,
    onPartyToggle: (String) -> Unit,
    onHouseChange: (Int) -> Unit,
    onCurrentOnlyChange: (Boolean) -> Unit,
    onFollowingOnlyChange: (Boolean) -> Unit = {},
    onViewModeChange: (DirectoryViewMode) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            ) {
                when (tabType) {
                    FilterTabType.OFFICIALS -> {
                        // Party — horizontally scrollable pills
                        item {
                            PartyPillRow(
                                parties = distinctParties,
                                filterState = filterState,
                                onPartyToggle = onPartyToggle
                            )
                        }

                        // House
                        item {
                            SectionLabel("House")
                            SegmentedPill(
                                options = listOf(
                                    "All" to 0,
                                    "Commons" to 1,
                                    "Lords" to 2
                                ),
                                selectedValue = filterState.houseFilter,
                                onValueChange = onHouseChange
                            )
                        }

                        // Status
                        item {
                            SectionLabel("Status")
                            SegmentedPill(
                                options = listOf(
                                    "All" to false,
                                    "Current" to true
                                ),
                                selectedValue = filterState.currentOnly,
                                onValueChange = onCurrentOnlyChange
                            )
                        }

                        // View mode — List / Grid (Officials only)
                        item {
                            SectionLabel("View")
                            SegmentedPill(
                                options = listOf(
                                    "List" to DirectoryViewMode.LIST,
                                    "Grid" to DirectoryViewMode.GRID
                                ),
                                selectedValue = viewMode,
                                onValueChange = onViewModeChange
                            )
                        }
                    }

                    FilterTabType.DIVISIONS -> {
                        // House — All / Commons / Lords
                        item {
                            SectionLabel("House")
                            SegmentedPill(
                                options = listOf(
                                    "All" to 0,
                                    "Commons" to 1,
                                    "Lords" to 2
                                ),
                                selectedValue = filterState.houseFilter,
                                onValueChange = onHouseChange
                            )
                        }
                    }

                    FilterTabType.FEED -> {
                        // Following only — All / Following
                        item {
                            SectionLabel("Following")
                            SegmentedPill(
                                options = listOf(
                                    "All" to false,
                                    "Following" to true
                                ),
                                selectedValue = filterState.followingOnly,
                                onValueChange = onFollowingOnlyChange
                            )
                        }
                        // House — All / Commons / Lords (same as DIVISIONS)
                        item {
                            SectionLabel("House")
                            SegmentedPill(
                                options = listOf(
                                    "All" to 0,
                                    "Commons" to 1,
                                    "Lords" to 2
                                ),
                                selectedValue = filterState.houseFilter,
                                onValueChange = onHouseChange
                            )
                        }
                    }

                    FilterTabType.OTHER -> {
                        item {
                            Text(
                                text = "No filters available for this tab",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilterTabType.GOVERNMENT -> {
                        // Tags, Departments, Type sections — fully implemented in Task 4
                        item {
                            SectionLabel("Type")
                            SegmentedPill(
                                options = listOf(
                                    "All" to 0,
                                    "Publications" to 1,
                                    "Statements" to 2,
                                    "Legislation" to 3
                                ),
                                selectedValue = 0,
                                onValueChange = {}
                            )
                        }
                    }

                    FilterTabType.FOLLOWING_HUB -> {
                        // Tags, Sources, Parties, Officials, Type sections — fully implemented in Task 4
                        item {
                            SectionLabel("Type")
                            SegmentedPill(
                                options = listOf(
                                    "All" to 0,
                                    "Publications" to 1,
                                    "Statements" to 2,
                                    "Legislation" to 3,
                                    "Divisions" to 4
                                ),
                                selectedValue = 0,
                                onValueChange = {}
                            )
                        }
                    }
                }
            }

            // Clear button only — filters apply live, no Apply button needed.
            OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(text = "Clear filters")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Combined segmented pill — a single rounded container with N segments.
 */
@Composable
private fun <T> SegmentedPill(
    options: List<Pair<String, T>>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(999.dp)
    val segmentShape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = selectedValue == value
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onValueChange(value) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Horizontally scrollable row of party filter pills.
 */
@Composable
private fun PartyPillRow(parties: List<String>, filterState: DirectoryFilterState, onPartyToggle: (String) -> Unit) {
    if (parties.isEmpty()) {
        Text(
            text = "No parties available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parties.forEach { party ->
            val state = filterState.partyState(party)
            val shape = RoundedCornerShape(20.dp)

            val containerColor = when (state) {
                PartyFilterState.INCLUDED ->
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

                PartyFilterState.EXCLUDED ->
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)

                PartyFilterState.DISABLED ->
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            }
            val contentColor = when (state) {
                PartyFilterState.INCLUDED -> MaterialTheme.colorScheme.primary
                PartyFilterState.EXCLUDED -> MaterialTheme.colorScheme.error
                PartyFilterState.DISABLED -> MaterialTheme.colorScheme.onSurface
            }
            val fontWeight = if (state != PartyFilterState.DISABLED) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }

            Text(
                text = party,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = fontWeight,
                maxLines = 1,
                modifier = Modifier
                    .clip(shape)
                    .background(containerColor)
                    .clickable { onPartyToggle(party) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
