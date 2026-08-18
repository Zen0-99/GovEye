package com.goveye.app.ui.screens.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.FollowedMpWithDetail
import com.goveye.app.data.local.entity.MemberRecentVote
import com.goveye.app.data.repo.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI model for a followed MP in the roster list (D-03).
 */
data class FollowedMpUi(
    val memberId: Int,
    val displayName: String,
    val thumbnailUrl: String?,
    val partyName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val constituencyName: String,
    val isMuted: Boolean,
    val recentVoteType: String?,
    val recentDivisionTitle: String?,
    val recentDivisionId: Int?,
    val recentDivisionHouse: Int?,
    val recentVoteDate: String?,
)

data class FollowingUiState(
    val followedMps: List<FollowedMpUi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val followRepository: FollowRepository,
    private val divisionDao: DivisionDao,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)

    private val followedWithVotes = followRepository.observeFollowedMpsWithDetails()
        .mapLatest { followed ->
            followed.map { detail ->
                val recentVote = divisionDao.getRecentVoteForMember(detail.memberId)
                detail.toUi(recentVote)
            }
        }

    val uiState: StateFlow<FollowingUiState> =
        combine(
            followedWithVotes,
            _searchQuery,
            _isLoading,
        ) { followed, query, loading ->
            val filtered = if (query.isBlank()) {
                followed
            } else {
                followed.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                        it.constituencyName.contains(query, ignoreCase = true) ||
                        it.partyName.contains(query, ignoreCase = true)
                }
            }
            FollowingUiState(
                followedMps = filtered,
                searchQuery = query,
                isLoading = loading && followed.isEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FollowingUiState(),
        )

    init {
        viewModelScope.launch { _isLoading.value = false }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun unfollow(memberId: Int) {
        viewModelScope.launch { followRepository.unfollow(memberId) }
    }

    fun toggleMute(memberId: Int, currentlyMuted: Boolean) {
        viewModelScope.launch { followRepository.setMuted(memberId, !currentlyMuted) }
    }

    private fun FollowedMpWithDetail.toUi(recentVote: MemberRecentVote?): FollowedMpUi =
        FollowedMpUi(
            memberId = memberId,
            displayName = nameDisplayAs,
            thumbnailUrl = thumbnailUrl,
            partyName = partyName,
            partyAbbreviation = partyAbbreviation,
            partyBackgroundColour = partyBackgroundColour,
            constituencyName = constituencyName,
            isMuted = isMuted,
            recentVoteType = recentVote?.vote,
            recentDivisionTitle = recentVote?.title,
            recentDivisionId = recentVote?.divisionId,
            recentDivisionHouse = recentVote?.house,
            recentVoteDate = recentVote?.date,
        )
}
