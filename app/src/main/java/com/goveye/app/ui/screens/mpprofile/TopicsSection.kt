package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.entity.MpTagEntity
import com.goveye.app.ui.theme.padding

/**
 * "Topics" section for the MP profile Profile tab.
 *
 * Shows the MP's top tags (from the mp_tags table, ranked by recency-weighted
 * hitCount) as clickable chips in a horizontal scrollable row. Each chip
 * shows the tag name and its hit count. Tags with hitCount < 5 are filtered
 * out by the DAO query. Clicking a chip navigates to [MpTagBrowseScreen]
 * showing other MPs who share that tag.
 *
 * Hidden if the MP has no tags (empty list).
 */
@Composable
fun TopicsSection(tags: List<MpTagEntity>, onTagClick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = MaterialTheme.padding.large
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tags, key = { it.tag }) { tagEntity ->
            TagChip(
                tag = tagEntity.tag,
                hitCount = tagEntity.hitCount,
                onClick = { onTagClick(tagEntity.tag) }
            )
        }
    }
}

@Composable
private fun TagChip(tag: String, hitCount: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = hitCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
