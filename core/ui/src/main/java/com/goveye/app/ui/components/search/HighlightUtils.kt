package com.goveye.app.ui.components.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Highlights search terms in text using Compose AnnotatedString + SpanStyle.
 *
 * Port of Odysseus Vault `_hlSearch()` pattern — splits on whitespace,
 * longest-first, wraps each match with a background SpanStyle.
 */
object HighlightUtils {
    fun highlightSearchTerms(text: String, query: String, highlightColor: Color): AnnotatedString {
        val terms = query.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .sortedByDescending { it.length }
            .map { Regex.escape(it) }
        if (terms.isEmpty()) return AnnotatedString(text)
        val regex = Regex("(${terms.joinToString("|")})", RegexOption.IGNORE_CASE)
        return buildAnnotatedString {
            var lastEnd = 0
            for (match in regex.findAll(text)) {
                append(text.substring(lastEnd, match.range.first))
                withStyle(SpanStyle(background = highlightColor)) {
                    append(match.value)
                }
                lastEnd = match.range.last + 1
            }
            append(text.substring(lastEnd))
        }
    }
}
