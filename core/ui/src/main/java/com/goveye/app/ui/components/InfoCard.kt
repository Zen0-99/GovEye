package com.goveye.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A rounded rectangle info card with a title and subtitle — used for
 * contextual information banners (e.g., "Parliament is in recess" on the
 * Feed, or tab descriptions in the Directory).
 *
 * @param title Bold first line (the headline)
 * @param subtitle Second line (the explanation/detail)
 * @param modifier Modifier for layout
 */
@Composable
fun InfoCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Left
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Info card wrapped for use as a sticky header. Adds top padding (so the
 * card isn't flush against the top of the viewport when stuck) and an
 * opaque background (so scrolling content doesn't show through the gaps
 * around the rounded corners).
 *
 * @param title Bold first line (the headline)
 * @param subtitle Second line (the explanation/detail)
 * @param topPaddingDp Top padding in dp (remains sticky)
 */
@Composable
fun StickyInfoCard(title: String, subtitle: String, topPaddingDp: Int = 8, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(
                top = topPaddingDp.dp,
                bottom = 8.dp
            )
        ) {
            InfoCard(title = title, subtitle = subtitle)
        }
    }
}
