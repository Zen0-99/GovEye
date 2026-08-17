package com.goveye.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Data for a single filter chip displayed in the search bar's chip row.
 */
data class SearchFilterChip(
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    val leadingDotColor: androidx.compose.ui.graphics.Color? = null,
)

/**
 * Miko-style floating search bar — rounded pill shape with solid
 * surfaceContainer background (same as the nav bar, no shadow).
 *
 * Layout: [search icon] [text field / hint] [clear button] [filter icon]
 *
 * Below the search bar, an optional row of [FilterChip]s can be shown
 * (inspired by Miko's GlobalSearchToolbar). Pass [filterChips] to display
 * inline filter pills without any expand/collapse button.
 *
 * Layout: [search icon] [text field / hint] [clear button] [filter icon]
 *         [chip] [chip] [chip] ...  (horizontally scrollable)
 */
@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: (() -> Unit)? = null,
    hasActiveFilters: Boolean = false,
    placeholder: String = "Search…",
    filterChips: List<SearchFilterChip> = emptyList(),
    onBack: (() -> Unit)? = null,
    segments: List<SearchSegment> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Optional back button (shrinks the search bar)
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape),
                shape = shape,
                color = colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: search icon
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp).size(22.dp),
                    )

                    // Center: text field or hint
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Clear button (visible when query is non-empty)
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Filter icon — changes color when filters are active
                    if (onFilterClick != null) {
                        IconButton(
                            onClick = onFilterClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = "Filter",
                                tint = if (hasActiveFilters) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        // Segmented control (Miko BrowseSearchPill style) — double pill
        if (segments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                segments.forEach { segment ->
                    val isSelected = segment.isSelected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(colorScheme.primary.copy(alpha = 0.15f))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { segment.onClick() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = segment.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Inline filter chips row (Miko GlobalSearchToolbar style)
        if (filterChips.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filterChips.forEach { chip ->
                    FilterChip(
                        selected = chip.isSelected,
                        onClick = chip.onClick,
                        label = { Text(chip.label) },
                        leadingIcon = if (chip.leadingDotColor != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(chip.leadingDotColor),
                                )
                            }
                        } else null,
                        colors = if (chip.leadingDotColor != null) {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chip.leadingDotColor.copy(alpha = 0.2f),
                                selectedLabelColor = colorScheme.onSurface,
                            )
                        } else FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }
    }
}
