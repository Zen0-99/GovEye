package com.goveye.app.ui.screens.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.FollowedMpWithDetail
import com.goveye.app.data.local.entity.MemberRecentVote
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.ui.screens.directory.DirectoryFilterState
import com.goveye.app.ui.screens.directory.PartyFilterState
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
    val house: Int,
    val isActive: Boolean,
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
    val filterState: DirectoryFilterState = DirectoryFilterState(),
    val distinctParties: List<String> = emptyList(),
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

    private val _filterState = MutableStateFlow(DirectoryFilterState())
    val filterState: StateFlow<DirectoryFilterState> = _filterState.asStateFlow()

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
            _filterState,
            _isLoading,
        ) { followed, query, filter, loading ->
            // Extract distinct parties from the full list (before filtering)
            val distinctParties = followed
                .map { it.partyName }
                .distinct()
                .sorted()

            // Apply search filter
            val searchFiltered = if (query.isBlank()) {
                followed
            } else {
                followed.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                        it.constituencyName.contains(query, ignoreCase = true) ||
                        it.partyName.contains(query, ignoreCase = true)
                }
            }

            // Apply directory-style filters (party, house, status)
            val filterFiltered = searchFiltered.filter { mp ->
                (filter.houseFilter == 0 || mp.house == filter.houseFilter) &&
                    (!filter.currentOnly || mp.isActive) &&
                    (filter.includedParties.isEmpty() || mp.partyName in filter.includedParties) &&
                    (mp.partyName !in filter.excludedParties)
            }

            FollowingUiState(
                followedMps = filterFiltered,
                searchQuery = query,
                filterState = filter,
                distinctParties = distinctParties,
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

    // --- Filter operations ---

    fun togglePartyFilter(party: String) {
        val current = _filterState.value
        val state = current.partyState(party)
        val newIncluded = current.includedParties.toMutableSet()
        val newExcluded = current.excludedParties.toMutableSet()
        when (state) {
            PartyFilterState.DISABLED -> newIncluded.add(party)
            PartyFilterState.INCLUDED -> {
                newIncluded.remove(party)
                newExcluded.add(party)
            }
            PartyFilterState.EXCLUDED -> newExcluded.remove(party)
        }
        _filterState.value = current.copy(
            includedParties = newIncluded,
            excludedParties = newExcluded,
        )
    }

    fun setHouseFilter(house: Int) {
        _filterState.value = _filterState.value.copy(houseFilter = house)
    }

    fun setCurrentOnly(currentOnly: Boolean) {
        _filterState.value = _filterState.value.copy(currentOnly = currentOnly)
    }

    fun clearFilters() {
        _filterState.value = DirectoryFilterState()
    }

    // --- Follow operations ---

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
            house = house,
            isActive = true, // Followed MPs are always active (we only follow active MPs)
            isMuted = isMuted,
            recentVoteType = recentVote?.vote,
            recentDivisionTitle = recentVote?.title,
            recentDivisionId = recentVote?.divisionId,
            recentDivisionHouse = recentVote?.house,
            recentVoteDate = recentVote?.date,
        )
}
