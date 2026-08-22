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
 * Activity score display — large centered number with breakdown columns.
 *
 * Layout:
 *   [Votes   /4.0]     [ 5.2 ]     [Questions /2.0]
 *   [Speeches/2.0]     [ score]     [Committees/2.0]
 *
 * Left column: Votes + Speeches (left-aligned)
 * Center: large score number + label
 * Right column: Questions + Committees (right-aligned)
 */
@Composable
fun ActivityScoreStrip(score: ActivityScore, modifier: Modifier = Modifier) {
    val onColor = MaterialTheme.colorScheme.onSurface
    val onColorVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column — Votes + Speeches (left-aligned)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ScoreBreakdownEntry("Votes", score.breakdown.voteParticipationContribution, 4.0f, onColor, onColorVariant)
            ScoreBreakdownEntry("Speeches", score.breakdown.speechesContribution, 2.0f, onColor, onColorVariant)
        }

        // Center — large score number + label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", score.score),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
            Text(
                text = "Activity Score",
                style = MaterialTheme.typography.labelSmall,
                color = onColorVariant
            )
        }

        // Right column — Questions + Committees (right-aligned)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            ScoreBreakdownEntry(
                "Questions",
                score.breakdown.questionsContribution,
                2.0f,
                onColor,
                onColorVariant,
                Alignment.End
            )
            ScoreBreakdownEntry(
                "Committees",
                score.breakdown.committeesContribution,
                2.0f,
                onColor,
                onColorVariant,
                Alignment.End
            )
        }
    }
}

@Composable
private fun ScoreBreakdownEntry(
    label: String,
    value: Float,
    max: Float,
    color: androidx.compose.ui.graphics.Color,
    variantColor: androidx.compose.ui.graphics.Color,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = variantColor,
            textAlign = when (alignment) {
                Alignment.End -> androidx.compose.ui.text.style.TextAlign.End
                else -> androidx.compose.ui.text.style.TextAlign.Start
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${String.format("%.1f", value)}/${String.format("%.1f", max)}",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = when (alignment) {
                Alignment.End -> androidx.compose.ui.text.style.TextAlign.End
                else -> androidx.compose.ui.text.style.TextAlign.Start
            },
            modifier = Modifier.fillMaxWidth()
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
