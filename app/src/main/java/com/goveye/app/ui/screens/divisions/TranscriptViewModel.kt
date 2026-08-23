package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.entity.HistoricalMemberEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.repo.HistoricalMemberRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.DebateSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A search result for a speech — the speech index, a snippet with context
 * around the match, and the match positions for highlighting.
 */
data class SpeechSearchResult(
    val speechIndex: Int,
    val speechGid: String,
    val speakerName: String,
    val snippet: String,
    val matchPositions: List<IntRange>
)

data class TranscriptState(
    val speeches: List<DebateSpeech> = emptyList(),
    val historicalMembers: Map<Int, HistoricalMemberEntity> = emptyMap(),
    val mpInfo: Map<Int, MpEntity> = emptyMap(),
    val isLoading: Boolean = true,
    val divisionTitle: String = "",
    val searchQuery: String = "",
    val searchResults: List<SpeechSearchResult> = emptyList()
)

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
    private val membersRepository: MembersRepository,
    private val historicalMemberRepository: HistoricalMemberRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TranscriptState())
    val state: StateFlow<TranscriptState> = _state.asStateFlow()

    fun load(divisionId: Int, divisionTitle: String) {
        _state.value = _state.value.copy(divisionTitle = divisionTitle)

        viewModelScope.launch {
            val speeches = votesRepository.getSpeechesForDivision(divisionId)

            // Primary: resolve speakers via twfyPersonId → historical_members
            val twfyPersonIds = speeches.map { it.twfyPersonId }.filter { it > 0 }.distinct()
            val historicalMembers = if (twfyPersonIds.isNotEmpty()) {
                historicalMemberRepository.getByTwfyPersonIds(twfyPersonIds).associateBy { it.twfyPersonId }
            } else {
                emptyMap()
            }

            // Secondary: load MP info for current MPs (avatar, profile link)
            val memberIds = speeches.map { it.memberId }.filter { it > 0 }.distinct()
            val mpInfo = if (memberIds.isNotEmpty()) {
                membersRepository.getMpsByIds(memberIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            _state.value = TranscriptState(
                speeches = speeches,
                historicalMembers = historicalMembers,
                mpInfo = mpInfo,
                isLoading = false,
                divisionTitle = divisionTitle
            )
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _state.value
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            computeSearchResults(current.speeches, query)
        }
        _state.value = current.copy(searchQuery = query, searchResults = results)
    }

    /**
     * Compute search results by scanning each speech's text for matches.
     * Produces a snippet with ~80 chars of context around the first match,
     * plus all match positions for highlighting.
     */
    private fun computeSearchResults(speeches: List<DebateSpeech>, query: String): List<SpeechSearchResult> {
        val terms = query.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { Regex.escape(it) }
        if (terms.isEmpty()) return emptyList()
        val regex = Regex("(${terms.joinToString("|")})", RegexOption.IGNORE_CASE)

        return speeches.mapIndexedNotNull { index, speech ->
            val matches = regex.findAll(speech.speechText).toList()
            if (matches.isEmpty()) return@mapIndexedNotNull null

            val firstMatch = matches.first()
            val contextStart = (firstMatch.range.first - 80).coerceAtLeast(0)
            val contextEnd = (firstMatch.range.last + 80).coerceAtMost(speech.speechText.length)
            val prefix = if (contextStart > 0) "…" else ""
            val suffix = if (contextEnd < speech.speechText.length) "…" else ""
            val snippetText = speech.speechText.substring(contextStart, contextEnd)
            val snippet = prefix + snippetText + suffix

            // Only include matches that fall within the snippet's source range.
            // Adjust positions relative to the snippet (accounting for prefix length).
            val prefixLen = prefix.length
            val adjustedPositions = matches
                .filter { it.range.first >= contextStart && it.range.last < contextEnd }
                .map { match ->
                    (match.range.first - contextStart + prefixLen)..(match.range.last - contextStart + prefixLen)
                }

            SpeechSearchResult(
                speechIndex = index,
                speechGid = speech.speechGid,
                speakerName = speech.speakerName.ifBlank { "Procedural" },
                snippet = snippet,
                matchPositions = adjustedPositions
            )
        }
    }
}
