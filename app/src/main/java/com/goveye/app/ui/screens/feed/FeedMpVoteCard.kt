package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.cardClickable

/**
 * Feed card showing a followed MP's vote (Aye/No) on a division.
 *
 * Layout matches the financial card convention:
 * 1. Top row: MP avatar (32dp) inline with division title (bodyLarge, Bold)
 * 2. Below title: "Aye" / "No" vote badge (colored pill)
 * 3. Bottom row: Date (left) + Commons/Lords (right)
 *
 * Tapping the card navigates to the division detail.
 */
@Composable
fun FeedMpVoteCard(item: FeedItem.MpVoteItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ayeColor = VoteColors.aye
    val noColor = VoteColors.no
    val voteUpper = item.vote.uppercase()
    val isAye = voteUpper == "AYE"
    val isNoVote = voteUpper == "NO VOTE RECORDED" || voteUpper == "NOVOTERECORDED" || voteUpper.isBlank()
    val voteColor = when {
        isAye -> ayeColor
        isNoVote -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> noColor
    }
    val voteText = when {
        isAye -> "Aye"
        isNoVote -> "No vote recorded"
        else -> "No"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Avatar + division title (inline, same as financial card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MpAvatar(
                    thumbnailUrl = item.memberPhotoUrl,
                    displayName = item.memberName,
                    partyColorHex = item.memberPartyColorHex,
                    size = 32.dp,
                    borderWidth = 1.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.divisionTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Vote badge — "Aye" / "No" pill in the vote color
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = voteColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = voteText,
                        style = MaterialTheme.typography.labelSmall,
                        color = voteColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.memberName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Bottom row: Date (left) + Commons/Lords (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatVoteDate(item.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (item.divisionHouse == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatVoteDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
