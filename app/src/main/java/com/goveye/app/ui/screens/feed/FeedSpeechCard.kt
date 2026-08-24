package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar

/**
 * Feed speech card — renders a followed MP's debate speech with the UI-SPEC
 * Section 3 LOCKED layout:
 *
 * 1. Profile icon (MpAvatar, 32dp, party-colored border) + speech text
 *    (bodyMedium, maxLines=3, Ellipsis) in a single Row.
 * 2. Tags inherited from the parent division (TagPillRow, only if non-empty).
 *
 * Clicking the card navigates to the transcript/division detail screen.
 */
@Composable
fun FeedSpeechCard(
    item: FeedItem.SpeechItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile icon + speech text (3 lines, truncated)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
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
                    text = item.speechText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tags inherited from the parent division
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TagPillRow(
                    tags = item.tags,
                    onTagClick = onTagClick
                )
            }
        }
    }
}
