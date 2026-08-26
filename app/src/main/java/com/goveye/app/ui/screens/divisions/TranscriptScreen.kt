package com.goveye.app.ui.screens.divisions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.data.local.entity.HistoricalMemberEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.domain.model.DebateSpeech
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.components.search.HighlightUtils

/**
 * Three display modes for the transcript — mirrors the manifesto pattern:
 * - [Mode.FULL]: no search query — render the full transcript
 * - [Mode.RESULTS]: search active — render the list of snippet results
 * - [Mode.SECTION]: user tapped a result — render the full transcript
 *   scrolled to the matching speech, with search terms highlighted
 */
private enum class TranscriptMode { FULL, RESULTS, SECTION }

@Composable
fun TranscriptScreen(
    divisionId: Int,
    divisionTitle: String,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    initialSpeechGid: String = "",
    modifier: Modifier = Modifier,
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    LaunchedEffect(divisionId) {
        viewModel.load(divisionId, divisionTitle)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Microview dialog state — shown when clicking an MP in the transcript
    var microviewMemberId by remember { mutableStateOf<Int?>(null) }
    var microviewFallback by remember { mutableStateOf<MicroviewFallback?>(null) }

    // SECTION mode: user tapped a result. The search query is NOT cleared —
    // it stays in the search bar so the user can press back to return to
    // the results list. Reset to RESULTS whenever the query changes.
    var isNavigated by remember { mutableStateOf(false) }
    var pendingScrollGid by remember { mutableStateOf<String?>(null) }

    // If navigated from a speech card with a speechGid, scroll to that speech
    LaunchedEffect(initialSpeechGid, state.speeches) {
        if (initialSpeechGid.isNotBlank() && state.speeches.isNotEmpty() && pendingScrollGid == null) {
            pendingScrollGid = initialSpeechGid
            isNavigated = true
        }
    }

    LaunchedEffect(state.searchQuery) {
        if (isNavigated) {
            isNavigated = false
            pendingScrollGid = null
        }
    }
    BackHandler(enabled = isNavigated) {
        isNavigated = false
        pendingScrollGid = null
    }

    val mode = when {
        state.searchQuery.isBlank() -> TranscriptMode.FULL
        isNavigated -> TranscriptMode.SECTION
        else -> TranscriptMode.RESULTS
    }

    // Extract search terms for highlighting in SECTION mode.
    // Use the same raw terms as the search results — no extra filtering —
    // so the highlighted word in the results is also highlighted in the
    // full transcript view.
    val searchTerms = remember(state.searchQuery) {
        state.searchQuery.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Configure the global search bar with back button + in-content search.
    // In SECTION mode, the back button returns to RESULTS (not exit screen).
    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            query = state.searchQuery,
            placeholder = "Search transcript…",
            onQueryChange = { query -> viewModel.updateSearchQuery(query) },
            onBack = {
                if (isNavigated) {
                    isNavigated = false
                    pendingScrollGid = null
                } else {
                    onBack()
                }
            },
            isSearchActive = false,
            onSearchActiveChange = { },
            segments = emptyList()
        )
    )

    if (state.isLoading) {
        DelayedSpinner(modifier = modifier)
        return
    }

    if (state.speeches.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transcript available for this division.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
        },
        label = "transcriptView",
        modifier = modifier.fillMaxSize()
    ) { currentMode ->
        when (currentMode) {
            TranscriptMode.FULL -> TranscriptFullList(
                speeches = state.speeches,
                historicalMembers = state.historicalMembers,
                mpInfo = state.mpInfo,
                highlightTerms = emptyList(),
                scrollTargetGid = null,
                onOpenMicroview = { memberId, name, partyName, partyColour, constituency ->
                    microviewMemberId = memberId
                    microviewFallback = MicroviewFallback(name, partyName, partyColour, constituency)
                }
            )

            TranscriptMode.RESULTS -> TranscriptResultsList(
                results = state.searchResults,
                onResultClick = { result ->
                    pendingScrollGid = result.speechGid
                    isNavigated = true
                }
            )

            TranscriptMode.SECTION -> TranscriptFullList(
                speeches = state.speeches,
                historicalMembers = state.historicalMembers,
                mpInfo = state.mpInfo,
                highlightTerms = searchTerms,
                scrollTargetGid = pendingScrollGid,
                onOpenMicroview = { memberId, name, partyName, partyColour, constituency ->
                    microviewMemberId = memberId
                    microviewFallback = MicroviewFallback(name, partyName, partyColour, constituency)
                }
            )
        }
    }

    // MP microview dialog — shown when clicking an MP in the transcript
    microviewMemberId?.let { memberId ->
        val fb = microviewFallback
        MpMicroviewDialog(
            memberId = memberId,
            fallbackName = fb?.name ?: "",
            fallbackPartyName = fb?.partyName,
            fallbackPartyColour = fb?.partyColour,
            fallbackConstituency = fb?.constituency,
            onNavigateToFullProfile = { id ->
                microviewMemberId = null
                microviewFallback = null
                onNavigateToProfile(id)
            },
            onDismiss = {
                microviewMemberId = null
                microviewFallback = null
            }
        )
    }
}

@Composable
private fun TranscriptFullList(
    speeches: List<DebateSpeech>,
    historicalMembers: Map<Int, HistoricalMemberEntity>,
    mpInfo: Map<Int, MpEntity>,
    highlightTerms: List<String>,
    scrollTargetGid: String?,
    onOpenMicroview: (
        memberId: Int,
        name: String,
        partyName: String?,
        partyColour: String?,
        constituency: String?
    ) -> Unit
) {
    val listState = rememberLazyListState()
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    // Scroll to the target speech when entering SECTION mode
    LaunchedEffect(scrollTargetGid) {
        if (scrollTargetGid != null) {
            val targetIndex = speeches.indexOfFirst { it.speechGid == scrollTargetGid }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(speeches, key = { it.speechGid }) { speech ->
            SpeechCard(
                speech = speech,
                historicalMember = historicalMembers[speech.twfyPersonId],
                mpInfo = if (speech.memberId > 0) mpInfo[speech.memberId] else null,
                highlightTerms = highlightTerms,
                highlightColor = highlightColor,
                onOpenMicroview = onOpenMicroview
            )
        }
    }
}

@Composable
private fun TranscriptResultsList(results: List<SpeechSearchResult>, onResultClick: (SpeechSearchResult) -> Unit) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No matches found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { it.speechGid }) { result ->
            SpeechSearchResultItem(
                result = result,
                highlightColor = highlightColor,
                onClick = { onResultClick(result) }
            )
        }
    }
}

@Composable
private fun SpeechSearchResultItem(
    result: SpeechSearchResult,
    highlightColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedSnippet = remember(result.snippet, result.matchPositions, highlightColor) {
        val snippet = result.snippet
        buildAnnotatedString {
            var lastEnd = 0
            for (range in result.matchPositions) {
                val start = range.first.coerceIn(0, snippet.length)
                val end = (range.last + 1).coerceIn(0, snippet.length)
                if (start <= lastEnd) continue // skip overlapping/invalid ranges
                if (start > lastEnd) {
                    append(snippet.substring(lastEnd, start))
                }
                withStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(snippet.substring(start, end))
                }
                lastEnd = end
            }
            if (lastEnd < snippet.length) {
                append(snippet.substring(lastEnd))
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = result.speakerName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = annotatedSnippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpeechCard(
    speech: DebateSpeech,
    historicalMember: HistoricalMemberEntity?,
    mpInfo: MpEntity?,
    highlightTerms: List<String> = emptyList(),
    highlightColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    onOpenMicroview: (
        memberId: Int,
        name: String,
        partyName: String?,
        partyColour: String?,
        constituency: String?
    ) -> Unit
) {
    // Procedural blocks (no speaker) — render as a centered, muted note
    if (speech.speakerName.isBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val text = if (highlightTerms.isNotEmpty()) {
                HighlightUtils.highlightSearchTerms(speech.speechText, highlightTerms.joinToString(" "), highlightColor)
            } else {
                AnnotatedString(speech.speechText)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    val isIntervention = speech.isIntervention
    val isCurrentMp = speech.memberId > 0 && mpInfo != null

    // Determine the best memberId, name, party, and constituency for microview
    val historicalPmId = historicalMember?.parliamentMemberId
    val microviewMemberId: Int = when {
        speech.memberId > 0 -> speech.memberId
        historicalPmId != null && historicalPmId > 0 -> historicalPmId
        else -> -1
    }
    val canOpenMicroview = microviewMemberId > 0
    val microviewName = speech.speakerName
    val microviewPartyName = when {
        mpInfo != null -> mpInfo.partyName

        !historicalMember?.partyAbbreviation.isNullOrBlank() -> historicalMember!!.partyAbbreviation!!

        !historicalMember?.party.isNullOrBlank() -> historicalMember!!.party!!.replace("-", " ").replaceFirstChar {
            it.uppercase()
        }

        else -> null
    }
    val microviewPartyColour = when {
        mpInfo != null -> mpInfo.partyBackgroundColour
        !historicalMember?.partyColourHex.isNullOrBlank() -> historicalMember!!.partyColourHex
        else -> null
    }
    val microviewConstituency = when {
        mpInfo != null -> mpInfo.constituencyName
        !historicalMember?.constituency.isNullOrBlank() -> historicalMember!!.constituency
        else -> null
    }

    val openMicroview = {
        if (canOpenMicroview) {
            onOpenMicroview(
                microviewMemberId,
                microviewName,
                microviewPartyName,
                microviewPartyColour,
                microviewConstituency
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isIntervention) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speaker row — avatar + name + party
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCurrentMp && mpInfo != null) {
                    MpAvatar(
                        thumbnailUrl = mpInfo.thumbnailUrl,
                        displayName = mpInfo.nameDisplayAs,
                        partyColorHex = mpInfo.partyBackgroundColour,
                        size = 36.dp,
                        borderWidth = 2.dp,
                        modifier = if (canOpenMicroview) Modifier.clickable { openMicroview() } else Modifier
                    )
                } else if (historicalMember != null && historicalMember.photo != null) {
                    MpAvatar(
                        photoBytes = historicalMember.photo,
                        displayName = speech.speakerName,
                        partyColorHex = historicalMember.partyColourHex
                            ?: com.goveye.app.ui.theme.partyNameToColorHex(historicalMember.party),
                        size = 36.dp,
                        borderWidth = 2.dp,
                        modifier = if (canOpenMicroview) Modifier.clickable { openMicroview() } else Modifier
                    )
                } else if (historicalMember != null && historicalMember.parliamentMemberId != null) {
                    MpAvatar(
                        thumbnailUrl = null,
                        displayName = speech.speakerName,
                        partyColorHex = historicalMember.partyColourHex
                            ?: com.goveye.app.ui.theme.partyNameToColorHex(historicalMember.party),
                        size = 36.dp,
                        borderWidth = 2.dp,
                        modifier = if (canOpenMicroview) Modifier.clickable { openMicroview() } else Modifier
                    )
                } else {
                    // Unmatched speaker — show initials avatar, no microview
                    MpAvatar(
                        thumbnailUrl = null,
                        displayName = speech.speakerName,
                        partyColorHex = null,
                        size = 36.dp,
                        borderWidth = 2.dp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = speech.speakerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = if (canOpenMicroview) Modifier.clickable { openMicroview() } else Modifier
                    )
                    if (microviewPartyName != null) {
                        Text(
                            text = microviewPartyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Speech text — with highlight if in SECTION mode
            val speechText = if (highlightTerms.isNotEmpty()) {
                HighlightUtils.highlightSearchTerms(
                    speech.speechText,
                    highlightTerms.joinToString(" "),
                    highlightColor
                )
            } else {
                AnnotatedString(speech.speechText)
            }
            Text(
                text = speechText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
