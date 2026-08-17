package com.goveye.app.ui.components.stats

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.VoteMapResult
import com.goveye.app.domain.stats.VoteMapTile
import kotlin.comparisons.reverseOrder

/**
 * Vote map grid — FotMob shot map style grid of colored tiles.
 * Green = with party, Red = rebel, Gray = no vote recorded.
 *
 * When there are multiple years of data, a year selector with left/right
 * arrows appears at the bottom. Only the selected year's tiles are shown,
 * retaining the shrink-to-fit sizing logic.
 */
@Composable
fun VoteMapGrid(
    tiles: List<VoteMapTile>,
    onTileClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return

    // Extract year from each tile's division date (assumes YYYY-MM-DD format)
    val yearToTiles = remember(tiles) {
        tiles.groupBy { it.divisionDate.take(4) }
            .toSortedMap(reverseOrder()) // most recent year first
    }
    val years = yearToTiles.keys.toList()

    var selectedYearIndex by remember { mutableStateOf(0) }
    val selectedYear = years.getOrElse(selectedYearIndex) { years.first() }
    val yearTiles = yearToTiles[selectedYear] ?: emptyList()

    // Shrink tile size when there are many tiles so all fit without scrolling.
    val tileSize = when {
        yearTiles.size <= 36 -> 32.dp
        yearTiles.size <= 64 -> 24.dp
        yearTiles.size <= 100 -> 18.dp
        yearTiles.size <= 200 -> 12.dp
        else -> 8.dp
    }
    val gridSpacing = if (tileSize <= 18.dp) 2.dp else 3.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary stats for the selected year
        VoteMapSummary(tiles = yearTiles)

        // Grid of tiles — height adapts to tile count so all are visible
        val rows = (yearTiles.size + 7) / 8
        val gridHeight = if (yearTiles.size <= 36) 200.dp else (rows * (tileSize.value + gridSpacing.value)).dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(tileSize),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
        ) {
            items(yearTiles, key = { it.divisionId }) { tile ->
                VoteMapTileView(
                    tile = tile,
                    onClick = { onTileClick(tile.divisionId, 1) },
                    size = tileSize,
                )
            }
        }

        // Year selector with left/right arrows
        if (years.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (selectedYearIndex < years.size - 1) selectedYearIndex++
                    },
                    enabled = selectedYearIndex < years.size - 1,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous year",
                        tint = if (selectedYearIndex < years.size - 1)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
                Text(
                    text = selectedYear,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                IconButton(
                    onClick = {
                        if (selectedYearIndex > 0) selectedYearIndex--
                    },
                    enabled = selectedYearIndex > 0,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next year",
                        tint = if (selectedYearIndex > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteMapSummary(tiles: List<VoteMapTile>) {
    val total = tiles.size
    val withParty = tiles.count { it.voteResult == VoteMapResult.WITH_PARTY }
    val rebels = tiles.count { it.voteResult == VoteMapResult.REBEL }
    val noVote = tiles.count { it.voteResult == VoteMapResult.NO_VOTE }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryStat("Total", total, MaterialTheme.colorScheme.onSurface)
        SummaryStat("With party", withParty, com.goveye.app.ui.components.VoteColors.aye)
        SummaryStat("Rebels", rebels, com.goveye.app.ui.components.VoteColors.no)
        SummaryStat("No vote", noVote, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VoteMapTileView(
    tile: VoteMapTile,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
) {
    val color = when (tile.voteResult) {
        VoteMapResult.WITH_PARTY -> com.goveye.app.ui.components.VoteColors.aye
        VoteMapResult.REBEL -> com.goveye.app.ui.components.VoteColors.no
        VoteMapResult.NO_VOTE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size <= 18.dp) 2.dp else 4.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {}
}
