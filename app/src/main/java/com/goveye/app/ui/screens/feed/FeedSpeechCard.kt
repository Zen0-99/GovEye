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
import com.goveye.app.ui.components.rememberExpandState

/**
 * **Speech — inverted pull-quote.**
 *
 * The words lead. A large quiet quote glyph opens the card, the speech runs
 * at reading size in italic with generous leading, and the speaker's portrait
 * and name arrive *underneath* as an attribution — the opposite order to
 * every other card in the feed, which all announce their subject first.
 *
 * Tapping expands to the full text plus a transcript link.
 */
@Composable
fun FeedSpeechCard(
    item: FeedItem.SpeechItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTranscript: ((Int, String, String) -> Unit)? = null,
    onTagClick: (String) -> Unit = {},
    onProfileClick: (() -> Unit)? = null
) {
    val expandState = rememberExpandState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = { expandState.toggle() }),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
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
                text = item.speechText,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                lineHeight = 25.sp,
                maxLines = if (expandState.expanded) Int.MAX_VALUE else 4,
                overflow = if (expandState.expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 2.dp)
            )

            ExpandableContent(state = expandState) {
                if (onNavigateToTranscript != null) {
                    TextButton(
                        onClick = { onNavigateToTranscript(item.divisionId, item.divisionTitle, item.speechGid) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).height(16.dp)
                        )
                        Text("See full transcript", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attribution bar — a tinted strip closing the card, carrying the
            // speaker, the debate and the date together.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
                        text = item.divisionTitle,
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
