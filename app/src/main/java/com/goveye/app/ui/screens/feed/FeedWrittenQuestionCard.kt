package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.ExpandableContent
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.cardSurfaceColor
import com.goveye.app.ui.components.rememberExpandState

/**
 * **Written question card — a followed MP's written question to a government department.**
 *
 * Shows the question heading (topic), the question text (3 lines collapsed,
 * full text expanded), and a tinted attribution bar with the MP's avatar,
 * name, answering body, and date. Tapping the card toggles expansion.
 *
 * @param showMember When false the attribution bar drops the MP avatar and
 *   name (used on the MP's own profile, where the identity is redundant).
 * @param answerText Government answer, when known. The feed does not carry
 *   answer data, so it defaults to null and the block is omitted; the MP
 *   profile's Activity tab supplies it.
 */
@Composable
fun FeedWrittenQuestionCard(
    item: FeedItem.WrittenQuestionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null,
    showMember: Boolean = true,
    answerText: String? = null,
    answerIsHolding: Boolean = false,
    dateAnswered: String? = null,
    isWithdrawn: Boolean = false
) {
    val expandState = rememberExpandState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = { expandState.toggle() }),
        shape = RoundedCornerShape(16.dp),
        color = cardSurfaceColor(MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Question icon + heading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.heading.ifBlank { "Written Question" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Question text — 3 lines collapsed, full text expanded
            if (item.questionText.isNotBlank()) {
                Text(
                    text = item.questionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expandState.expanded) 50 else 3,
                    overflow = if (expandState.expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // Answer status — only rendered when answer data is supplied.
            when {
                !answerText.isNullOrBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = if (answerIsHolding) "Holding Answer" else "Answer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = answerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expandState.expanded) 50 else 3,
                                overflow = if (expandState.expanded) {
                                    TextOverflow.Visible
                                } else {
                                    TextOverflow.Ellipsis
                                }
                            )
                            if (!dateAnswered.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Answered ${formatDivisionDate(dateAnswered)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                isWithdrawn -> {
                    Text(
                        text = "Withdrawn",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            ExpandableContent(state = expandState) {
                if (item.answeringBodyName.isNotBlank()) {
                    Text(
                        text = "Answering body: ${item.answeringBodyName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attribution bar — tinted strip matching speech/statement cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
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
                            text = item.answeringBodyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = item.answeringBodyName.ifBlank { "Written Question" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatDivisionDate(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
