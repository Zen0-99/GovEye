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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goveye.app.ui.components.ExpandableContent
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.cardSurfaceColor
import com.goveye.app.ui.components.rememberExpandState

/**
 * **Speech combo — multiple speech segments from the same MP on the same date.**
 *
 * When an MP speaks multiple times in a debate (each segment ends when
 * another MP starts talking), the segments are grouped into this combo card
 * instead of showing 5 separate cards. Speech texts are joined with "[...]"
 * separators.
 *
 * Closed: max 3 lines. Open: max 18 lines (excluding [...] separators)
 * before truncating with "View full transcript".
 *
 * Layout mirrors [FeedSpeechCard]: quote glyph, italic text, attribution bar
 * with avatar + name + debate title + date.
 */
@Composable
fun FeedSpeechComboCard(
    item: FeedItem.SpeechComboItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTranscript: ((Int, String, String) -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val expandState = rememberExpandState()

    // Join speech texts with [...] separators
    val combinedText = item.speeches.joinToString("\n\n[...]\n\n") { it.speechText }

    // First speech for metadata
    val firstSpeech = item.speeches.first()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = { expandState.toggle() }),
        shape = RoundedCornerShape(16.dp),
        color = cardSurfaceColor(MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier
                    .padding(start = 14.dp, top = 10.dp)
                    .size(30.dp)
            )

            Text(
                text = combinedText,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                lineHeight = 25.sp,
                maxLines = if (expandState.expanded) 18 else 3,
                overflow = if (expandState.expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 2.dp)
            )

            ExpandableContent(state = expandState) {
                if (onNavigateToTranscript != null) {
                    TextButton(
                        onClick = {
                            onNavigateToTranscript(
                                firstSpeech.divisionId,
                                firstSpeech.divisionTitle,
                                firstSpeech.speechGid
                            )
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).height(16.dp)
                        )
                        Text("View full transcript", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attribution bar — tinted strip matching the statement card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "${item.speeches.size} speeches · ${firstSpeech.divisionTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
