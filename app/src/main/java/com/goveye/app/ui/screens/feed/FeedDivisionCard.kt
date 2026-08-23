package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Division
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.divisions.DivisionResultBar

// Theme-aware vote colors — teal for Aye, orange for No
private val AyeColor @Composable get() = VoteColors.aye
private val NoColor @Composable get() = VoteColors.no

/**
 * Feed division card with optional followed-MP highlight tint and tag pills.
 *
 * When [hasFollowedVotes] is true, the card background uses primary at 0.05 alpha
 * (D-04 — subtle tint) and a 4dp vertical strip on the left edge signals the highlight.
 * Otherwise uses surfaceContainer (same as the existing DivisionCard).
 *
 * When [tags] is non-empty, a horizontally scrollable row of tag pills is shown
 * at the bottom of the card. Clicking a pill invokes [onTagClick].
 *
 * Icon badge (D-11): [Icons.Outlined.HowToVote] in a 32dp circle at TopEnd.
 * Type badge: "Division" in a labelSmall chip.
 */
@Composable
fun FeedDivisionCard(
    division: Division,
    hasFollowedVotes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList(),
    onTagClick: (String) -> Unit = {}
) {
    val cardColor = if (hasFollowedVotes) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Icon badge at TopEnd (D-11)
            CardIconBadge(
                icon = Icons.Outlined.HowToVote,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                // Left edge strip for followed-MP highlight
                if (hasFollowedVotes) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(72.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = division.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            // Type badge (D-11)
                            TypeBadge(text = "Division")
                        }
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
                                text = "-",
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
                    DivisionResultBar(ayeCount = division.ayeCount, noCount = division.noCount)

                    // Tag pills - extra spacing from the result bar
                    Spacer(Modifier.height(4.dp))

                    // Tag pills - horizontally scrollable, truncating at the right edge
                    if (tags.isNotEmpty()) {
                        TagPillRow(
                            tags = tags,
                            onTagClick = onTagClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icon badge for feed cards — 24dp icon inside a 32dp circle Surface.
 * Used by all card types (D-11).
 */
@Composable
fun CardIconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Type badge — labelSmall text in a RoundedCornerShape(4.dp) chip with
 * primaryContainer background and onPrimaryContainer text.
 */
@Composable
fun TypeBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
