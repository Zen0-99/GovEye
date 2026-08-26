package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.ExpandableContent
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.rememberExpandState

/**
 * Feed speech card — renders a followed MP's debate speech.
 *
 * Layout:
 * 1. Profile icon (MpAvatar, 32dp) + speech text (3 lines truncated when collapsed,
 *    full text when expanded) in a Row.
 * 2. When expanded: "See full transcript" button navigates to the transcript screen.
 *
 * Tags are NOT shown in the feed UI (Issue 10) but are kept in the data model
 * for backend filtering logic.
 */
@Composable
fun FeedSpeechCard(
    item: FeedItem.SpeechItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTranscript: ((Int, String, String) -> Unit)? = null,
    onTagClick: (String) -> Unit = {}
) {
    val expandState = rememberExpandState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = { expandState.toggle() }),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // MP name + division title header
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.memberName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }

            // Speech text — 3 lines when collapsed, full when expanded
            Text(
                text = item.speechText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expandState.expanded) Int.MAX_VALUE else 3,
                overflow = if (expandState.expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )

            // When expanded, show "See full transcript" button
            ExpandableContent(state = expandState) {
                if (onNavigateToTranscript != null) {
                    TextButton(
                        onClick = { onNavigateToTranscript(item.divisionId, item.divisionTitle, item.speechGid) }
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
        }
    }
}
