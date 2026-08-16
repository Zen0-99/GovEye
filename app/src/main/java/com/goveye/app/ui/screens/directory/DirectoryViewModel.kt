package com.goveye.app.ui.screens.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.Mp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val searchDao: SearchDao,
    private val directoryPreferences: DirectoryPreferences,
) : ViewModel() {

    val viewMode: StateFlow<DirectoryViewMode> =
        directoryPreferences.viewMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryViewMode.LIST)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: Flow<List<Mp>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                val ftsQuery = "${query.trim()}*"
                searchDao.searchMps(ftsQuery)
                    .map { entities -> entities.map { it.toDomainMp() } }
            }
        }

    val pagedMps: Flow<PagingData<Mp>> = membersRepository.observePagedMps()
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setViewMode(mode: DirectoryViewMode) {
        viewModelScope.launch { directoryPreferences.setViewMode(mode) }
    }

    private fun MpEntity.toDomainMp(): Mp =
        Mp(
            id = id,
            nameListAs = nameListAs,
            nameDisplayAs = nameDisplayAs,
            nameFullTitle = nameFullTitle,
            gender = gender,
            party = com.goveye.app.domain.model.Party(
                partyId, partyName, partyAbbreviation,
                partyBackgroundColour, partyForegroundColour,
            ),
            constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
            house = house,
            membershipStartDate = membershipStartDate,
            isActive = isActive,
            thumbnailUrl = thumbnailUrl,
        )
}
