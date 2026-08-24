package com.goveye.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Data for a single filter chip displayed in the search bar's chip row.
 */
data class SearchFilterChip(
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    val leadingDotColor: androidx.compose.ui.graphics.Color? = null
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
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    actions: List<DetailTopBarAction> = emptyList(),
    iconTint: androidx.compose.ui.graphics.Color? = null,
    accentColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val focusManager = LocalFocusManager.current
    val backIconTint = iconTint ?: colorScheme.onSurface
    // Blend the party accent color into the search bar background when on
    // a detail screen (profile/party). Deeper tint — 25% party color over
    // surfaceContainer. Direct color reference (no animateColorAsState) so
    // it recomposes synchronously with theme changes — previously the
    // animated color caused the search bar to lag behind the rest of the
    // screen during light/dark mode toggle (issue #12.2).
    val searchBarColor = if (accentColor != null) {
        androidx.compose.ui.graphics.lerp(
            colorScheme.surfaceContainer,
            accentColor,
            0.25f
        )
    } else {
        colorScheme.surfaceContainer
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional back button — animated in/out so the search bar
            // morphs width smoothly. We capture the callback so it's
            // available during the exit animation even if onBack becomes
            // null on the next composition.
            // Padding is applied inside the AnimatedVisibility so it
            // animates away with the button — no gap jump when it exits.
            val backCallback = onBack
            AnimatedVisibility(
                visible = onBack != null,
                enter = expandHorizontally(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                Box(modifier = Modifier.padding(end = 4.dp)) {
                    IconButton(
                        onClick = backCallback ?: {},
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = backIconTint
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape),
                shape = shape,
                color = searchBarColor
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: search icon
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp).size(22.dp)
                        )

                        // Center: text field or hint
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            }
                            // Use a local TextFieldValue to preserve cursor
                            // position across recompositions. When the query
                            // is updated from external state (ViewModel →
                            // StateFlow → recomposition), a BasicTextField with
                            // String value resets the cursor to position 0.
                            // TextFieldValue carries the selection, so the
                            // cursor stays where the user put it.
                            var textFieldValue by remember {
                                mutableStateOf(
                                    TextFieldValue(
                                        text = query,
                                        selection = TextRange(query.length)
                                    )
                                )
                            }
                            // Sync external query changes (e.g., clear button,
                            // view model reset) to local state. When the user
                            // types, onValueChange updates textFieldValue first,
                            // then calls onQueryChange — so when the external
                            // query flows back, textFieldValue.text already
                            // matches and we skip the reset.
                            LaunchedEffect(query) {
                                if (textFieldValue.text != query) {
                                    textFieldValue = TextFieldValue(
                                        text = query,
                                        selection = TextRange(query.length)
                                    )
                                }
                            }
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    onQueryChange(newValue.text)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        onSearchActiveChange(state.isFocused)
                                    }
                            )
                        }

                        // Clear button — clears the query AND dismisses the
                        // search (collapses pills, hides keyboard, shows all
                        // bars again).
                        if (query.isNotEmpty() || isSearchActive) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Filter icon — changes color when filters are active.
                        // Wrapped in AnimatedContent with fadeIn/fadeOut and
                        // animated width so it transitions smoothly when
                        // navigating between list and detail screens (issue
                        // #12). Previously the filter button appeared/
                        // disappeared instantly, causing the right side of
                        // the search bar to jump.
                        val filterClick = onFilterClick
                        val filterButtonWidth by animateDpAsState(
                            targetValue = if (filterClick != null) 40.dp else 0.dp,
                            animationSpec = tween(durationMillis = 300),
                            label = "filterButtonWidth"
                        )
                        AnimatedContent(
                            targetState = filterClick,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                            },
                            label = "filterButton",
                            modifier = Modifier.size(filterButtonWidth)
                        ) { click ->
                            if (click != null) {
                                IconButton(
                                    onClick = click,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Filter",
                                        tint = if (hasActiveFilters) {
                                            colorScheme.primary
                                        } else {
                                            colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Segmented control (Miko BrowseSearchPill style) — nested
                    // inside the search bar surface. Expands when the search bar
                    // is focused (user taps it) and collapses when focus is lost
                    // (user presses X or navigates away).
                    if (segments.isNotEmpty()) {
                        AnimatedVisibility(
                            visible = isSearchActive,
                            enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                                animationSpec = tween(300),
                                expandFrom = Alignment.Top
                            ),
                            exit = fadeOut(animationSpec = tween(220)) + shrinkVertically(
                                animationSpec = tween(220),
                                shrinkTowards = Alignment.Top
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colorScheme.onSurface.copy(alpha = 0.06f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                                                }
                                            )
                                            .clickable { segment.onClick() }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) {
                                                colorScheme.primary
                                            } else {
                                                colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action icons (detail screens) — animated in/out as a group
            // so the search bar morphs width smoothly when entering/leaving
            // detail screens. Padding is inside AnimatedVisibility so it
            // animates away with the icons — no gap jump on exit.
            AnimatedVisibility(
                visible = actions.isNotEmpty(),
                enter = expandHorizontally(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        IconButton(
                            onClick = action.onClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = action.tint ?: iconTint ?: colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        .background(chip.leadingDotColor)
                                )
                            }
                        } else {
                            null
                        },
                        colors = if (chip.leadingDotColor != null) {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chip.leadingDotColor.copy(alpha = 0.2f),
                                selectedLabelColor = colorScheme.onSurface
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        }
                    )
                }
            }
        }
    }
}
