package com.goveye.app.ui.screens.party

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.ui.theme.padding

/**
 * Three display modes for the manifesto tab:
 * - [Mode.FULL]: no search query — render the full manifesto
 * - [Mode.RESULTS]: search active — render the list of snippet results
 * - [Mode.SECTION]: user tapped a result — render the full manifesto
 *   scrolled to the matching block, with search terms highlighted
 */
private enum class ManifestoMode { FULL, RESULTS, SECTION }

@Composable
fun PartyManifestoTab(
    manifesto: PartyManifestoEntity?,
    searchQuery: String,
    searchResults: List<ManifestoSearchResult>,
    fullManifestoText: String?,
    modifier: Modifier = Modifier
) {
    if (manifesto == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No manifesto available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Parse the manifesto text into blocks once
    val blocks = remember(manifesto.manifestoText) {
        ManifestoParser.parse(manifesto.manifestoText)
    }

    // Extract search terms for highlighting (split on whitespace, strip
    // punctuation, filter short tokens). Used in SECTION mode.
    val searchTerms = remember(searchQuery) {
        searchQuery.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("^[^\\w]+|[^\\w]+$"), "") }
            .filter { it.length >= 2 }
            .distinct()
    }

    // SECTION mode: user tapped a result. The search query is NOT cleared —
    // it stays in the search bar so the user can press back to return to
    // the results list. Reset to RESULTS whenever the query changes.
    var isNavigated by remember { mutableStateOf(false) }
    var pendingScrollText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(searchQuery) {
        // Any query change (typing more, clearing via X) exits SECTION mode
        if (isNavigated) {
            isNavigated = false
            pendingScrollText = null
        }
    }

    // Intercept back press in SECTION mode → return to results, not away
    BackHandler(enabled = isNavigated) {
        isNavigated = false
        pendingScrollText = null
    }

    val mode = when {
        searchQuery.isBlank() -> ManifestoMode.FULL
        isNavigated -> ManifestoMode.SECTION
        else -> ManifestoMode.RESULTS
    }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
        },
        label = "manifestoView",
        modifier = modifier.fillMaxSize()
    ) { currentMode ->
        when (currentMode) {
            ManifestoMode.FULL -> ManifestoFullList(
                blocks = blocks,
                manifesto = manifesto,
                highlightTerms = emptyList()
            )

            ManifestoMode.RESULTS -> ManifestoResultsList(
                searchQuery = searchQuery,
                searchResults = searchResults,
                fullManifestoText = fullManifestoText,
                onResultClick = { result ->
                    // Extract a searchable substring from the snippet:
                    // strip <b> tags and ... markers, take a clean chunk
                    // for matching against block text.
                    val plainText = result.snippetText
                        .replace(Regex("</?b>"), "")
                        .replace("...", "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    pendingScrollText = if (plainText.length > 60) {
                        plainText.substring(0, 60)
                    } else {
                        plainText
                    }
                    isNavigated = true
                }
            )

            ManifestoMode.SECTION -> ManifestoFullList(
                blocks = blocks,
                manifesto = manifesto,
                highlightTerms = searchTerms,
                scrollToText = pendingScrollText
            )
        }
    }
}

@Composable
private fun ManifestoFullList(
    blocks: List<ManifestoBlock>,
    manifesto: PartyManifestoEntity,
    highlightTerms: List<String>,
    scrollToText: String? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    // Scroll to the block containing the target text when entering SECTION
    LaunchedEffect(scrollToText) {
        if (scrollToText != null) {
            val targetIndex = blocks.indexOfFirst { block ->
                when (block) {
                    is ManifestoBlock.Heading -> block.text.contains(scrollToText, ignoreCase = true)
                    is ManifestoBlock.Paragraph -> block.text.contains(scrollToText, ignoreCase = true)
                    is ManifestoBlock.Bullet -> block.text.contains(scrollToText, ignoreCase = true)
                    else -> false
                }
            }
            if (targetIndex >= 0) {
                // +1 to account for the title item at index 0
                listState.animateScrollToItem(targetIndex + 1)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        )
    ) {
        // Title
        item {
            Text(
                text = "${manifesto.manifestoYear} Manifesto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
            )
            Text(
                text = "${manifesto.wordCount} words",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.medium)
            )
        }

        // Render parsed blocks with optional highlighting
        items(blocks.size) { index ->
            ManifestoBlockRenderer(blocks[index], highlightTerms, highlightColor)
        }
    }
}

@Composable
private fun ManifestoResultsList(
    searchQuery: String,
    searchResults: List<ManifestoSearchResult>,
    fullManifestoText: String?,
    onResultClick: (ManifestoSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        )
    ) {
        if (searchResults.isEmpty()) {
            // Empty state
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.padding.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results for '$searchQuery' in this manifesto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Results count
            item {
                Text(
                    text = "${searchResults.size} results",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
                )
            }
            // Results list
            items(searchResults, key = { it.partyId * 100000 + it.snippetText.hashCode() }) { result ->
                ManifestoSearchResultItem(
                    result = result,
                    query = searchQuery,
                    isExpanded = false,
                    onToggleExpand = {},
                    onNavigateToSection = { onResultClick(result) },
                    fullText = fullManifestoText
                )
            }
        }
    }
}

@Composable
private fun ManifestoBlockRenderer(block: ManifestoBlock, highlightTerms: List<String>, highlightColor: Color) {
    when (block) {
        is ManifestoBlock.Heading -> {
            val text = highlightSearchTerms(block.text, highlightTerms, highlightColor)
            Text(
                text = text,
                style = if (block.level == 1) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    top = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small
                )
            )
        }

        is ManifestoBlock.Paragraph -> {
            val text = highlightSearchTerms(block.text, highlightTerms, highlightColor)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
            )
        }

        is ManifestoBlock.Bullet -> {
            val text = highlightSearchTerms("•  ${block.text}", highlightTerms, highlightColor)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small
                )
            )
        }

        is ManifestoBlock.PageBreak -> {
            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.padding.small
                ),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * Build an [AnnotatedString] with a background highlight on every
 * case-insensitive occurrence of any term in [terms].
 */
private fun highlightSearchTerms(text: String, terms: List<String>, highlightColor: Color): AnnotatedString =
    buildAnnotatedString {
        if (terms.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        // Build a single regex matching any term (longest first to avoid
        // partial overlaps when one term is a prefix of another).
        val sortedTerms = terms.sortedByDescending { it.length }
        val pattern = sortedTerms.joinToString("|") { Regex.escape(it) }
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        var lastEnd = 0
        for (match in regex.findAll(text)) {
            append(text.substring(lastEnd, match.range.first))
            pushStyle(
                SpanStyle(
                    background = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            )
            append(match.value)
            pop()
            lastEnd = match.range.last + 1
        }
        append(text.substring(lastEnd))
    }
