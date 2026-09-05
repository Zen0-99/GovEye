package com.goveye.app.ui.components.stats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.VoteMapResult
import com.goveye.app.domain.stats.VoteMapTile
import kotlin.comparisons.reverseOrder

/**
 * Vote map grid — FotMob shot map style grid of colored tiles.
 * Green = with party, Red = rebel, Gray = no vote recorded.
 *
 * Tiles are a fixed small size (14dp) regardless of vote count.
 * Long-press a tile to scale it up as visual feedback; release to
 * navigate to the division. This makes small tiles usable despite
 * their size.
 *
 * When there are multiple years of data, a year selector with left/right
 * arrows appears at the bottom. Only the selected year's tiles are shown.
 */
@Composable
fun VoteMapGrid(tiles: List<VoteMapTile>, onTileClick: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
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

    // Fixed small tile size — consistent regardless of vote count
    val tileSize = 14.dp
    val gridSpacing = 3.dp

    // Calculate how many columns fit on screen
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val columns = ((screenWidth - 24.dp) / (tileSize + gridSpacing)).toInt().coerceAtLeast(1)

    // Track which tile is currently being long-pressed (for scale animation)
    var pressedTileIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary stats for the selected year
        VoteMapSummary(tiles = yearTiles)

        // Grid of tiles — built as Rows in a Column for accurate height.
        // No fixed height needed; the grid is exactly as tall as its content.
        val rows = (yearTiles.size + columns - 1) / columns
        Column(verticalArrangement = Arrangement.spacedBy(gridSpacing)) {
            for (rowIndex in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
                    for (colIndex in 0 until columns) {
                        val index = rowIndex * columns + colIndex
                        if (index < yearTiles.size) {
                            val tile = yearTiles[index]
                            VoteMapTileView(
                                tile = tile,
                                isPressed = pressedTileIndex == index,
                                onPress = { pressedTileIndex = index },
                                onRelease = {
                                    pressedTileIndex = null
                                    onTileClick(tile.divisionId, 1)
                                },
                                size = tileSize
                            )
                        } else {
                            // Empty spacer to keep row alignment
                            Box(modifier = Modifier.size(tileSize))
                        }
                    }
                }
            }
        }

        // Year selector with left/right arrows
        if (years.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedYearIndex < years.size - 1) selectedYearIndex++
                    },
                    enabled = selectedYearIndex < years.size - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous year",
                        tint = if (selectedYearIndex < years.size - 1) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                }
                Text(
                    text = selectedYear,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(
                    onClick = {
                        if (selectedYearIndex > 0) selectedYearIndex--
                    },
                    enabled = selectedYearIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next year",
                        tint = if (selectedYearIndex > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
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
        horizontalArrangement = Arrangement.SpaceBetween
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
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoteMapTileView(
    tile: VoteMapTile,
    isPressed: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val color = when (tile.voteResult) {
        VoteMapResult.WITH_PARTY -> com.goveye.app.ui.components.VoteColors.aye
        VoteMapResult.REBEL -> com.goveye.app.ui.components.VoteColors.no
        VoteMapResult.NO_VOTE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    // Scale up when pressed — visual feedback that the tile is being held
    val animatedSize by animateDpAsState(
        targetValue = if (isPressed) size * 2.5f else size,
        animationSpec = tween(durationMillis = 150),
        label = "tileScale"
    )

    Box(
        modifier = Modifier
            .size(size) // keep layout slot fixed at original size
            .pointerInput(tile.divisionId) {
                detectTapGestures(
                    onLongPress = {
                        onPress()
                        // Navigate on release of the long press
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(animatedSize)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}
