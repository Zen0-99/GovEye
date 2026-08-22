package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.DivisionWeightCalculator
import com.goveye.app.domain.stats.RebellionStats

/**
 * Per-MP activity timeline with search and pagination.
 *
 * Shows the member's voting activity with per-event weight badges (D-08).
 * Votes are loaded in pages (30 at a time) via [onLoadMore] for infinite scroll.
 * A search bar filters by division title via [onSearchQueryChange].
 */
@Composable
fun ActivityTabContent(
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: RebellionStats?,
    @Suppress("UNUSED_PARAMETER") allVotesByDivision: Map<Int, List<com.goveye.app.domain.model.DivisionVote>>,
    @Suppress("UNUSED_PARAMETER") memberPartyName: String?,
    searchQuery: String,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    totalCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search votes…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Result count
        if (totalCount > 0) {
            item {
                Text(
                    text = "$totalCount vote${if (totalCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (memberVotes.isEmpty() && !isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No recent activity"
                        } else {
                            "No votes found for \"$searchQuery\""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(memberVotes, key = { it.divisionId }) { vote ->
                val isRebellion = isRebellionDivision(vote.divisionId, rebellionStats)
                val total = vote.ayeCount + vote.noCount
                val closeness = if (total > 0) {
                    1.0 - kotlin.math.abs(vote.ayeCount - vote.noCount).toDouble() / total
                } else {
                    0.0
                }
                val weight = DivisionWeightCalculator.compute(
                    mpVote = vote.vote,
                    isRebellion = isRebellion,
                    divisionCloseness = closeness
                )
                ActivityRow(
                    vote = vote,
                    isRebellion = isRebellion,
                    score = weight.score,
                    onClick = { onNavigateToDivision(vote.divisionId, vote.house) }
                )
            }

            // Load more trigger + loading indicator
            if (hasMore) {
                item {
                    LaunchedEffect(memberVotes.size) {
                        onLoadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (memberVotes.isNotEmpty()) {
                item {
                    Text(
                        text = "End of activity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(vote: MemberVoteWithDivision, isRebellion: Boolean, score: Double, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vote badge — Aye (teal) / No (orange), No-vote-recorded shows "—"
        val ayeColor = com.goveye.app.ui.components.VoteColors.aye
        val noColor = com.goveye.app.ui.components.VoteColors.no
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (vote.vote) {
                        VoteType.AYE -> ayeColor
                        VoteType.NO -> noColor
                        VoteType.NO_VOTE_RECORDED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (vote.vote) {
                    VoteType.AYE -> "Aye"
                    VoteType.NO -> "No"
                    VoteType.NO_VOTE_RECORDED -> "—"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vote.divisionTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatActivityDate(vote.divisionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (vote.house == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (isRebellion) {
                    Text(
                        text = "Rebel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        WeightBadge(score = score)
    }
}

/**
 * Numeric weight badge — color-coded green/yellow/red per D-08.
 */
@Composable
private fun WeightBadge(score: Double, modifier: Modifier = Modifier) {
    val color = when {
        score >= 7.0 -> Color(0xFF2E7D32)
        score >= 4.0 -> Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = String.format("%.1f", score),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun isRebellionDivision(divisionId: Int, rebellionStats: RebellionStats?): Boolean =
    rebellionStats?.rebellionInstances?.any { it.divisionId == divisionId } == true

private fun formatActivityDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
