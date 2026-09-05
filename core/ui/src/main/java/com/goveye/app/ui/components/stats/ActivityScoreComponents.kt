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
 *   [Votes   /2.5]     [ 5.2 ]     [Questions /2.0]
 *   [Speeches/2.0]     [ score]     [Committees/1.5]
 *   [Finance /2.0]
 *
 * Left column: Votes + Speeches + Finance (left-aligned)
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
        // Left column — Votes + Speeches + Finance (left-aligned)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ScoreBreakdownEntry("Votes", score.breakdown.voteParticipationContribution, 2.5f, onColor, onColorVariant)
            ScoreBreakdownEntry("Speeches", score.breakdown.speechesContribution, 2.0f, onColor, onColorVariant)
            ScoreBreakdownEntry("Finance", score.breakdown.financeContribution, 2.0f, onColor, onColorVariant)
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
                1.5f,
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
 * FotMob-style trait bars — 6 horizontal bars showing normalized scores.
 * Loyalty/Participation show the actual rate (0-100%).
 * Questions/Speeches/Finance show rate-vs-average (100% = average).
 * Committees show count-vs-ceiling (50% = 5 committees, 100% = 10+).
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
            text = "Performance Breakdown",
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
    val displayPercent = traitDisplayPercent(trait)
    val animatedPercent by animateFloatAsState(
        targetValue = displayPercent,
        animationSpec = tween(durationMillis = 600),
        label = "trait_${trait.label}"
    )
    val barColor = if (displayPercent >= 50) {
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${displayPercent.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                    .fillMaxWidth(animatedPercent / 100f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Returns the percentage value to display and use for bar/radar fills.
 *
 * Rate-based traits (Loyalty, Participation) use the actual mpValue (0-100).
 * Count-based traits (Questions, Speeches, Committees, Finance) use the normalized
 * rate-based score stored in [TraitBar.percentile] (0-100, clamped).
 *
 * Questions/Speeches/Finance: score = (mpRate / avgRate) * 100, where rate = count / yearsServed.
 * An MP at the average rate scores 100%.
 *
 * Committees: score = (mpCount / 10) * 100.
 * 5 committees = 50%, 10+ = 100%.
 */
fun traitDisplayPercent(trait: TraitBar): Float = when (trait.label) {
    "Loyalty", "Participation" -> trait.mpValue.coerceIn(0f, 100f)
    else -> trait.percentile.toFloat().coerceIn(0f, 100f)
}
