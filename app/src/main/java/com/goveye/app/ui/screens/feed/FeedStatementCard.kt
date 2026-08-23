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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.WrittenStatement

/**
 * Feed statement card — written ministerial statement.
 *
 * Layout: Title + minister name (prefixed "by ") + answering body + date +
 * type badge "Statement" + tag pills (no image — statements don't have images).
 *
 * Icon badge (D-11): [Icons.Outlined.ChatBubbleOutline] in a 32dp circle at TopEnd.
 * Type badge: "Statement" in a labelSmall chip.
 *
 * Per UI-SPEC lines 307-313.
 */
@Composable
fun FeedStatementCard(
    statement: WrittenStatement,
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
                icon = Icons.Outlined.ChatBubbleOutline,
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
                    text = statement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Minister name prefixed "by " (bodySmall, onSurfaceVariant)
                Text(
                    text = "by ${statement.memberRole}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statement.answeringBodyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatStatementDate(statement.dateMade),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Type badge (D-11)
                    TypeBadge(text = "Statement")
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

private fun formatStatementDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
