package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.ui.components.SyncStatusBanner
import com.goveye.app.ui.theme.padding

// Theme-aware vote colors — teal for Aye, orange for No
private val AyeColor @Composable get() = com.goveye.app.ui.components.VoteColors.aye
private val NoColor @Composable get() = com.goveye.app.ui.components.VoteColors.no

@Composable
fun DivisionsTabContent(
    onNavigateToDivision: (Int, Int) -> Unit,
    houseFilter: Int = 0,
    searchQuery: String = "",
    modifier: Modifier = Modifier,
    viewModel: DivisionBrowseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Apply external house filter from the filter bottom sheet
    androidx.compose.runtime.LaunchedEffect(houseFilter) {
        viewModel.setHouseFilter(houseFilter)
    }

    // Apply search query from the global search bar
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
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
                    horizontal = 12.dp,
                    vertical = MaterialTheme.padding.small
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.divisions, key = { it.id }) { division ->
                    DivisionCard(
                        division = division,
                        onClick = { onNavigateToDivision(division.id, division.house) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DivisionCard(division: Division, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = division.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Date + house badge inline, then result bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date + house on the left
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDivisionDate(division.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (division.house == 2) "Lords" else "Commons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Aye vs No counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${division.ayeCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AyeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${division.noCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NoColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Result bar
            DivisionResultBar(ayeCount = division.ayeCount, noCount = division.noCount)
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
