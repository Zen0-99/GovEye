package com.goveye.app.ui.screens.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.data.repo.ManifestoRepository
import com.goveye.app.data.repo.PartyStatsRepository
import com.goveye.app.domain.model.PartyLeaderDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PartyUiState(
    val party: PartySummary? = null,
    val stats: PartyStatsEntity? = null,
    val manifesto: PartyManifestoEntity? = null,
    val leader: PartyLeaderDetail? = null,
    val isLoading: Boolean = true,
    val isFollowing: Boolean = false,
    val notificationsEnabled: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PartyViewModel @Inject constructor(
    private val mpDao: MpDao,
    private val partyStatsRepository: PartyStatsRepository,
    private val manifestoRepository: ManifestoRepository,
    private val governmentAnnouncementsRepository: GovernmentAnnouncementsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState = _uiState.asStateFlow()

    private var currentPartyId: Int? = null
    private var cachedPagedMps: Flow<PagingData<MpEntity>>? = null

    // Manifesto search state
    private val _manifestoSearchQuery = MutableStateFlow("")
    val manifestoSearchQuery = _manifestoSearchQuery.asStateFlow()

    val manifestoSearchResults: kotlinx.coroutines.flow.StateFlow<List<ManifestoSearchResult>> =
        _manifestoSearchQuery
            .debounce(300)
            .flatMapLatest { query ->
                val partyId = currentPartyId
                if (query.isBlank() || partyId == null) {
                    flowOf(emptyList())
                } else {
                    flowOf(manifestoRepository.searchManifesto(partyId, query))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateManifestoSearchQuery(query: String) {
        _manifestoSearchQuery.value = query
    }

    fun loadParty(partyId: Int) {
        if (currentPartyId == partyId) return
        currentPartyId = partyId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Fetch party summary from MpDao
            val parties = mpDao.getActiveParties()
            val party = parties.find { it.partyId == partyId }

            // Fetch stats
            val stats = try {
                partyStatsRepository.getPartyStats(partyId)
            } catch (e: Exception) {
                null
            }

            // Fetch manifesto
            val manifesto = try {
                manifestoRepository.getManifesto(partyId)
            } catch (e: Exception) {
                null
            }

            // Fetch party leader detail (joins party_leaders + mps + bio_data)
            val leader = try {
                governmentAnnouncementsRepository.getPartyLeaderDetail(partyId)
            } catch (e: Exception) {
                null
            }

            _uiState.value = PartyUiState(
                party = party,
                stats = stats,
                manifesto = manifesto,
                leader = leader,
                isLoading = false
            )
        }
    }

    fun getPagedMps(partyId: Int): Flow<PagingData<MpEntity>> = cachedPagedMps ?: run {
        val flow = Pager(
            config = PagingConfig(pageSize = 30, prefetchDistance = 10),
            pagingSourceFactory = { mpDao.pagingSourceByParty(partyId) }
        ).flow.cachedIn(viewModelScope)
        cachedPagedMps = flow
        flow
    }

    // TODO: Implement party follow/notifications persistence (no PartyFollowDao yet)
    fun toggleFollow(partyId: Int) {
        _uiState.value = _uiState.value.copy(isFollowing = !_uiState.value.isFollowing)
    }
}
