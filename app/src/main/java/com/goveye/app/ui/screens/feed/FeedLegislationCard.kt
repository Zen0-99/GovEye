package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Legislation

/**
 * Feed legislation card — new SI/Act.
 *
 * Layout: Title + type (SI/Act) + date + type badge "Legislation" +
 * sub-type as secondary labelSmall + tag pills.
 *
 * Icon badge (D-11): [Icons.Outlined.Gavel] in a 32dp circle at TopEnd.
 * Type badge: "Legislation" in a labelSmall chip. Sub-type (SI/Act) shown
 * as secondary labelSmall text.
 *
 * Per UI-SPEC lines 314-318.
 */
@Composable
fun FeedLegislationCard(
    legislation: Legislation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList(),
    onTagClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Icon badge at TopEnd (D-11)
            CardIconBadge(
                icon = Icons.Outlined.Gavel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = legislation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sub-type (SI/Act) as secondary labelSmall
                    Text(
                        text = legislation.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatLegislationDate(legislation.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Type badge (D-11)
                    TypeBadge(text = "Legislation")
                }

                // Tag pills
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    TagPillRow(
                        tags = tags,
                        onTagClick = onTagClick
                    )
                }
            }
        }
    }
}

private fun formatLegislationDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
