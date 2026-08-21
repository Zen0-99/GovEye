package com.goveye.app.ui.screens.party

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.ui.components.search.HighlightUtils
import com.goveye.app.ui.theme.padding

@Composable
fun ManifestoSearchResultItem(
    result: ManifestoSearchResult,
    query: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    fullText: String?,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val cleanSnippet = result.snippetText
        .replace("<b>", "")
        .replace("</b>", "")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggleExpand)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            Text(
                text = HighlightUtils.highlightSearchTerms(cleanSnippet, query, highlightColor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (isExpanded && fullText != null) {
                val snippetIndex = fullText.indexOf(cleanSnippet)
                if (snippetIndex >= 0) {
                    val contextStart = (snippetIndex - 500).coerceAtLeast(0)
                    val contextEnd = (snippetIndex + cleanSnippet.length + 500).coerceAtMost(fullText.length)
                    val context = fullText.substring(contextStart, contextEnd)
                    Text(
                        text = HighlightUtils.highlightSearchTerms(context, query, highlightColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
