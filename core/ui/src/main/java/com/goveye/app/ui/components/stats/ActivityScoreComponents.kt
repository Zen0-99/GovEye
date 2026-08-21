package com.goveye.app.ui.components.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.ActivityScore
import com.goveye.app.domain.stats.TraitBar

/**
 * FotMob-style activity score display — large number with colored background.
 */
@Composable
fun ActivityScoreStrip(score: ActivityScore, modifier: Modifier = Modifier) {
    val backgroundColor = when {
        score.score <= 30 -> MaterialTheme.colorScheme.surfaceVariant
        score.score <= 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }
    val onColor = if (score.score <= 30) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = score.score.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
            Text(
                text = "Parliamentary Activity Score",
                style = MaterialTheme.typography.labelMedium,
                color = onColor.copy(alpha = 0.8f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ScoreBreakdownRow("Votes", score.breakdown.voteParticipationContribution, 40, onColor)
            ScoreBreakdownRow("Questions", score.breakdown.questionsContribution, 20, onColor)
            ScoreBreakdownRow("Speeches", score.breakdown.speechesContribution, 20, onColor)
            ScoreBreakdownRow("Committees", score.breakdown.committeesContribution, 20, onColor)
        }
    }
}

@Composable
private fun ScoreBreakdownRow(label: String, value: Int, max: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
        Text(
            text = "$value/$max",
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * FotMob-style trait bars — 5 horizontal bars showing percentile rankings.
 */
@Composable
fun TraitBarsSection(traitBars: List<TraitBar>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Traits vs Peers",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        traitBars.forEach { trait ->
            TraitBarRow(trait = trait)
        }
    }
}

@Composable
private fun TraitBarRow(trait: TraitBar) {
    val animatedPercentile by animateFloatAsState(
        targetValue = trait.percentile.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "trait_${trait.label}"
    )
    val barColor = if (trait.percentile >= 50) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = trait.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${ordinalSuffix(trait.percentile)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPercentile / 100f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

private fun ordinalSuffix(percentile: Int): String {
    if (percentile == 0) return "0th"
    val suffix = when (percentile % 10) {
        1 -> if (percentile % 100 == 11) "th" else "st"
        2 -> if (percentile % 100 == 12) "th" else "nd"
        3 -> if (percentile % 100 == 13) "th" else "rd"
        else -> "th"
    }
    return "$percentile$suffix"
}
