package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.ui.components.StickyInfoCard
import com.goveye.app.ui.components.SyncStatusBanner
import com.goveye.app.ui.theme.padding

// Theme-aware vote colors — teal for Aye, orange for No
private val AyeColor @Composable get() = com.goveye.app.ui.components.VoteColors.aye
private val NoColor @Composable get() = com.goveye.app.ui.components.VoteColors.no

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DivisionsTabContent(
    onNavigateToDivision: (Int, Int) -> Unit,
    houseFilter: Int = 0,
    searchQuery: String = "",
    showInfoCards: Boolean = true,
    modifier: Modifier = Modifier,
    state: DivisionBrowseState = DivisionBrowseState(),
    onSearchQueryChange: (String) -> Unit = {},
    onHouseFilterChange: (Int) -> Unit = {},
    onTagClick: (String) -> Unit = {}
) {
    // Apply external house filter from the filter bottom sheet
    androidx.compose.runtime.LaunchedEffect(houseFilter) {
        onHouseFilterChange(houseFilter)
    }

    // Apply search query from the global search bar
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        onSearchQueryChange(searchQuery)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.syncStatus != SyncStatus.FRESH && state.divisions.isNotEmpty()) {
            SyncStatusBanner(status = state.syncStatus)
        }

        if (state.isLoading && state.divisions.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
        } else if (state.divisions.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No divisions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = MaterialTheme.padding.small
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showInfoCards) {
                    stickyHeader(key = "tab-info") {
                        StickyInfoCard(
                            title = "Debates",
                            subtitle = "Browse every division and vote in the Commons and Lords."
                        )
                    }
                }
                items(state.divisions, key = { it.id }, contentType = { "division_card" }) { division ->
                    val tags = state.divisionTags[division.id] ?: emptyList()
                    com.goveye.app.ui.screens.feed.FeedDivisionCard(
                        division = division,
                        hasFollowedVotes = false,
                        tags = tags,
                        onClick = { onNavigateToDivision(division.id, division.house) },
                        onTagClick = onTagClick
                    )
                }
            }
        }
    }
}

@Composable
fun DivisionResultBar(ayeCount: Int, noCount: Int) {
    val total = ayeCount + noCount
    if (total == 0) return
    val ayeFraction = ayeCount.toFloat() / total
    val noFraction = 1f - ayeFraction
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .height(6.dp)
    ) {
        if (ayeFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(ayeFraction)
                    .background(AyeColor)
                    .fillMaxSize()
            )
        }
        if (noFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(noFraction)
                    .background(NoColor)
                    .fillMaxSize()
            )
        }
    }
}

private fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
