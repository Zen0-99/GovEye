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

data class TranscriptState(
    val speeches: List<DebateSpeech> = emptyList(),
    val historicalMembers: Map<Int, HistoricalMemberEntity> = emptyMap(),
    val mpInfo: Map<Int, MpEntity> = emptyMap(),
    val isLoading: Boolean = true,
    val divisionTitle: String = ""
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
            // Only needed for speeches where memberId > 0 (current sitting MPs)
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
}
