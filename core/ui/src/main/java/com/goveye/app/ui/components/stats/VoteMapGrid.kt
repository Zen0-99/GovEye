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
 */
@Composable
fun VoteMapGrid(
    tiles: List<VoteMapTile>,
    onTileClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary stats
        VoteMapSummary(tiles = tiles)

        // Grid of tiles
        LazyVerticalGrid(
            columns = GridCells.Adaptive(36.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(tiles, key = { it.divisionId }) { tile ->
                VoteMapTileView(
                    tile = tile,
                    onClick = { onTileClick(tile.divisionId, 1) },
                )
            }
        }

        // Legend
        VoteMapLegend()
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
        SummaryStat("With party", withParty, androidx.compose.ui.graphics.Color(0xFF00796B))
        SummaryStat("Rebels", rebels, androidx.compose.ui.graphics.Color(0xFFE65100))
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
) {
    var showTooltip by remember { mutableStateOf(false) }
    val color = when (tile.voteResult) {
        VoteMapResult.WITH_PARTY -> androidx.compose.ui.graphics.Color(0xFF00796B) // teal
        VoteMapResult.REBEL -> androidx.compose.ui.graphics.Color(0xFFE65100) // orange
        VoteMapResult.NO_VOTE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
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
        LegendItem("With party", androidx.compose.ui.graphics.Color(0xFF00796B))
        LegendItem("Rebel", androidx.compose.ui.graphics.Color(0xFFE65100))
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
