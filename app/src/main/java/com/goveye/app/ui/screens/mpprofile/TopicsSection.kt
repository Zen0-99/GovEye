package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.theme.padding

/**
 * "Topics" section for the MP profile Profile tab.
 *
 * Shows the MP's top tags (from the mp_tags table, ranked by frequency +
 * recency weighted hitCount) as clickable chips. Clicking a chip navigates
 * to [MpTagBrowseScreen] showing other MPs who share that tag.
 *
 * Hidden if the MP has no tags (empty list).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicsSection(tags: List<String>, onTagClick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = 8.dp)
    ) {
        Text(
            text = "Topics",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tags.forEach { tag ->
                val shape = RoundedCornerShape(20.dp)
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { onTagClick(tag) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
