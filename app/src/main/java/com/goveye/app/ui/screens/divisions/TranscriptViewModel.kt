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

            // Validate twfyPersonId matches: cross-reference the historical
            // member's displayName with the speech's speakerName. If they
            // don't match (e.g. "Baroness Stedman-Scott" vs "Deborah
            // Stedman-Scott"), try a name-based search. If a better match
            // is found, replace the entry. If no match, remove the entry
            // so the SpeechCard shows the speaker without a clickable
            // profile link (instead of navigating to the wrong profile).
            val validatedMembers = historicalMembers.toMutableMap()
            for (speech in speeches) {
                if (speech.twfyPersonId <= 0) continue
                val matched = validatedMembers[speech.twfyPersonId] ?: continue
                if (namesMatch(speech.speakerName, matched.displayName)) continue

                // Names don't match — try name-based search
                val strippedName = titlePattern.replace(speech.speakerName, "").trim()
                if (strippedName.isNotBlank()) {
                    val candidates = historicalMemberRepository.searchByDisplayName(strippedName)
                    val betterMatch = candidates.firstOrNull { namesMatch(speech.speakerName, it.displayName) }
                    if (betterMatch != null) {
                        validatedMembers[speech.twfyPersonId] = betterMatch
                    } else {
                        // No better match — remove so no wrong-profile navigation
                        validatedMembers.remove(speech.twfyPersonId)
                    }
                } else {
                    validatedMembers.remove(speech.twfyPersonId)
                }
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
                historicalMembers = validatedMembers,
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

    companion object {
        private val titlePattern = Regex(
            "^(Baroness|Lord|Mr|Ms|Mrs|Dr|Sir|Dame|Lady|Earl|Viscount|Rt|Hon|Right|Rev|Reverend|Father|Fr)\\.?\\s+",
            RegexOption.IGNORE_CASE
        )

        /**
         * Check whether a TWFY speaker name and a historical member display
         * name refer to the same person. Strips titles from both, then
         * compares case-insensitively. Also matches on surname-only when
         * the speaker name is a titled form (e.g. "Baroness Stedman-Scott"
         * vs "Deborah Stedman-Scott").
         */
        fun namesMatch(speakerName: String, dbDisplayName: String): Boolean {
            if (speakerName.isBlank() || dbDisplayName.isBlank()) return false
            val s = titlePattern.replace(speakerName, "").trim().lowercase()
            val d = titlePattern.replace(dbDisplayName, "").trim().lowercase()
            if (s.isBlank() || d.isBlank()) return false
            if (s == d) return true
            // Surname match: "Stedman-Scott" vs "Deborah Stedman-Scott"
            val sParts = s.split(" ")
            val dParts = d.split(" ")
            if (sParts.isNotEmpty() && dParts.size >= 2) {
                if (sParts.last() == dParts.last()) {
                    // Same surname — if speaker name is surname-only, accept
                    if (sParts.size == 1) return true
                    // Same surname + same first initial
                    if (sParts[0][0] == dParts[0][0]) return true
                }
            }
            return false
        }
    }
}
