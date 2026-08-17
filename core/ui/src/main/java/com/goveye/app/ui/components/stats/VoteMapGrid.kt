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

/**
 * Vote map grid — FotMob shot map style grid of colored tiles.
 * Green = with party, Red = rebel, Gray = no vote recorded.
 * When there are more than 36 tiles, blocks shrink to fit all without scrolling.
 */
@Composable
fun VoteMapGrid(
    tiles: List<VoteMapTile>,
    onTileClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return

    // Shrink tile size when there are many tiles so all fit without scrolling.
    // Base size is 32.dp; for >36 tiles, shrink proportionally down to 8.dp min.
    val tileSize = when {
        tiles.size <= 36 -> 32.dp
        tiles.size <= 64 -> 24.dp
        tiles.size <= 100 -> 18.dp
        tiles.size <= 200 -> 12.dp
        else -> 8.dp
    }
    val gridSpacing = if (tileSize <= 18.dp) 2.dp else 3.dp

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary stats
        VoteMapSummary(tiles = tiles)

        // Grid of tiles — height adapts to tile count so all are visible
        val columns = (tiles.size + 7) / 8 // estimate rows, 8 per row
        val gridHeight = if (tiles.size <= 36) 200.dp else (columns * (tileSize.value + gridSpacing.value)).dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(tileSize),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
        ) {
            items(tiles, key = { it.divisionId }) { tile ->
                VoteMapTileView(
                    tile = tile,
                    onClick = { onTileClick(tile.divisionId, 1) },
                    size = tileSize,
                )
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
    var showTooltip by remember { mutableStateOf(false) }
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
    ) {
        // Tooltip would be shown on long press — simplified for now
    }
}

@Composable
private fun VoteMapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem("With party", com.goveye.app.ui.components.VoteColors.aye)
        LegendItem("Rebel", com.goveye.app.ui.components.VoteColors.no)
        LegendItem("No vote", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
