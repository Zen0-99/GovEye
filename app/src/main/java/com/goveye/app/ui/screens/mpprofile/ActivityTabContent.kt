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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Per-MP recent activity timeline with per-event weight badges (D-08).
 *
 * Shows up to 20 most recent divisions for the MP, each with a numeric weight
 * badge (0-10 scale) computed by [DivisionWeightCalculator]. The badge is
 * color-coded green (>= 7.0), yellow (>= 4.0), or red (< 4.0), following the
 * FotMob "match rating" pattern.
 */
@Composable
fun ActivityTabContent(
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: RebellionStats?,
    @Suppress("UNUSED_PARAMETER") allVotesByDivision: Map<Int, List<com.goveye.app.domain.model.DivisionVote>>,
    @Suppress("UNUSED_PARAMETER") memberPartyName: String?,
    onNavigateToDivision: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (memberVotes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recent activity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val recentVotes = remember(memberVotes) { memberVotes.take(20) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recentVotes, key = { it.divisionId }) { vote ->
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
