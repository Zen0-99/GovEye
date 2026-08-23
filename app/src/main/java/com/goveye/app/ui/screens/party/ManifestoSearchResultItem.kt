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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.ui.theme.padding

@Composable
fun ManifestoSearchResultItem(
    result: ManifestoSearchResult,
    @Suppress("UNUSED_PARAMETER") query: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToSection: () -> Unit,
    @Suppress("UNUSED_PARAMETER") fullText: String?,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val annotatedSnippet = remember(result.snippetText, highlightColor) {
        parseBoldTags(result.snippetText, highlightColor)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onNavigateToSection)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            Text(
                text = annotatedSnippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Parse <b>...</b> tags in the snippet and convert to AnnotatedString with
 * a highlighted SpanStyle (background color). This renders the search
 * highlighting that the ManifestoRepository produces.
 */
private fun parseBoldTags(snippet: String, highlightColor: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < snippet.length) {
            val openTag = snippet.indexOf("<b>", i)
            if (openTag < 0) {
                append(snippet.substring(i))
                break
            }
            // Append text before the tag
            append(snippet.substring(i, openTag))
            val closeTag = snippet.indexOf("</b>", openTag + 3)
            if (closeTag < 0) {
                append(snippet.substring(openTag))
                break
            }
            val highlightedText = snippet.substring(openTag + 3, closeTag)
            pushStyle(
                SpanStyle(
                    background = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            )
            append(highlightedText)
            pop()
            i = closeTag + 4
        }
    }
