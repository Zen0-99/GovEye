package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DivisionBrowseState(
    val divisions: List<Division> = emptyList(),
    val divisionTags: Map<Int, List<String>> = emptyMap(),
    val isLoading: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val searchQuery: String = "",
    val houseFilter: Int = 0,
    val hasMore: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DivisionBrowseViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
    private val tagDao: TagDao
) : ViewModel() {

    private val searchQueryState = MutableStateFlow("")
    private val houseFilterState = MutableStateFlow(0)
    private val divisionsLimit = MutableStateFlow(50)

    val state: StateFlow<DivisionBrowseState> =
        combine(
            searchQueryState,
            houseFilterState,
            divisionsLimit
        ) { query, house, limit ->
            Triple(query, house, limit)
        }.flatMapLatest { (query, house, limit) ->
            val resultFlow = if (query.isNotBlank()) {
                votesRepository.searchDivisions(query, house)
            } else {
                votesRepository.observeDivisionsByHouse(house, limit)
            }
            // Combine divisions with all tag rows so cards can show tags
            combine(resultFlow, tagDao.observeAllDivisionTagRows()) { result, tagRows ->
                val divisionTags = tagRows
                    .groupBy { it.divisionId }
                    .mapValues { (_, rows) -> rows.map { it.tag } }
                DivisionBrowseState(
                    divisions = result.data,
                    divisionTags = divisionTags,
                    isLoading = false,
                    syncStatus = result.status,
                    searchQuery = query,
                    houseFilter = house,
                    hasMore = query.isBlank() && result.data.size >= limit
                )
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DivisionBrowseState())

    fun updateSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun setSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun setHouseFilter(house: Int) {
        houseFilterState.value = house
    }

    fun loadMore() {
        divisionsLimit.value += 50
    }
}
