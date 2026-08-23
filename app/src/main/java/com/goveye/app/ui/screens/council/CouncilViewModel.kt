package com.goveye.app.ui.screens.council

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.goveye.app.data.local.dao.CouncilDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.CouncilEntity
import com.goveye.app.data.local.entity.MpEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CouncilUiState(
    val council: CouncilEntity? = null,
    val isLoading: Boolean = true,
    val matchingMps: List<MpEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CouncilViewModel @Inject constructor(private val councilDao: CouncilDao, private val mpDao: MpDao) : ViewModel() {

    private val _uiState = MutableStateFlow(CouncilUiState())
    val uiState = _uiState.asStateFlow()

    private var currentCouncilId: Int? = null

    fun loadCouncil(councilId: Int) {
        if (currentCouncilId == councilId) return
        currentCouncilId = councilId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val council = councilDao.getCouncil(councilId)

            // Find MPs whose constituency name overlaps with the council name.
            // We use a name-matching approach: strip common council suffixes
            // and search for constituencies containing the core name.
            val matchingMps = if (council != null) {
                findMpsForCouncil(council.name)
            } else {
                emptyList()
            }

            _uiState.value = CouncilUiState(
                council = council,
                isLoading = false,
                matchingMps = matchingMps
            )
        }
    }

    /**
     * Find MPs whose constituency overlaps with the given council.
     * Uses name-based matching: strips common council suffixes (e.g., "Borough Council",
     * "District Council") and searches for constituencies containing the core name.
     *
     * This is a heuristic — some constituencies won't match (e.g., "Aldershot" is in
     * "Rushmoor" council). A proper mapping would require ONS constituency-to-LAD
     * lookup data, which is not currently bundled in the seed DB.
     */
    private suspend fun findMpsForCouncil(councilName: String): List<MpEntity> {
        // Strip common suffixes to get the core council name
        val coreName = stripCouncilSuffix(councilName)
        if (coreName.isBlank()) return emptyList()

        // Search for MPs whose constituency name contains the core council name
        val allMps = mpDao.searchMpsLocal(coreName)
        // Filter to active MPs only and those whose constituency actually matches
        return allMps.filter { mp ->
            mp.isActive && (
                mp.constituencyName.contains(coreName, ignoreCase = true) ||
                    coreName.contains(mp.constituencyName, ignoreCase = true)
                )
        }
    }

    private fun stripCouncilSuffix(name: String): String {
        val suffixes = listOf(
            " Metropolitan Borough Council",
            " Borough Council",
            " District Council",
            " City Council",
            " County Council",
            " Council",
            " London Borough",
            " Metropolitan Borough",
            " Borough",
            " District",
            " City",
            " County"
        )
        var result = name
        for (suffix in suffixes) {
            if (result.endsWith(suffix, ignoreCase = true)) {
                result = result.dropLast(suffix.length)
                break
            }
        }
        return result.trim()
    }
}
