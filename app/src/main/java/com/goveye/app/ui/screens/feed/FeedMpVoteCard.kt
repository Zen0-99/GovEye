package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.cardSurfaceColor

/**
 * **MP vote — verdict-led quote card.**
 *
 * The division title and the MP's vote (AYE/NO) take the main stage —
 * large, coloured, in caps, without a coloured box. Below them, an
 * attribution bar (matching the speech card) carries the MP's avatar,
 * name, house, and date.
 */
@Composable
fun FeedMpVoteCard(
    item: FeedItem.MpVoteItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null,
    showMember: Boolean = true
) {
    val voteUpper = item.vote.uppercase()
    val isAye = voteUpper == "AYE"
    val isNoVote = voteUpper == "NO VOTE RECORDED" || voteUpper == "NOVOTERECORDED" || voteUpper.isBlank()
    val voteColor = when {
        isAye -> VoteColors.aye
        isNoVote -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> VoteColors.no
    }
    val voteText = when {
        isAye -> "AYE"
        isNoVote -> "NO VOTE"
        else -> "NO"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardSurfaceColor(voteColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main stage — division title + vote verdict, large and coloured
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.divisionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = voteText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = voteColor
                )
            }

            // Attribution bar — tinted strip matching the speech card.
            // MP avatar + name on the left, house + date on the right.
            // On the MP's own profile [showMember] is false: the identity is
            // dropped (the whole page is about them) and the strip carries
            // just the house and date.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showMember) {
                    MpAvatar(
                        thumbnailUrl = item.memberPhotoUrl,
                        displayName = item.memberName,
                        partyColorHex = item.memberPartyColorHex,
                        size = 28.dp,
                        borderWidth = 1.dp,
                        modifier = if (onProfileClick != null) {
                            Modifier.clickable { onProfileClick() }
                        } else {
                            Modifier
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.memberName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (item.divisionHouse == 2) "Lords" else "Commons",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = if (item.divisionHouse == 2) "Lords" else "Commons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatVoteDate(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
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
