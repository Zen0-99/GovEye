package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.mapper.DivisionMapper
import com.goveye.app.domain.model.Division
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TagMicroviewUiState(
    val tag: String = "",
    val description: String = "",
    val divisionCount: Int = 0,
    val billCount: Int = 0,
    val divisions: List<Division> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TagMicroviewViewModel @Inject constructor(private val tagDao: TagDao, private val divisionDao: DivisionDao) :
    ViewModel() {

    private val _uiState = MutableStateFlow(TagMicroviewUiState())
    val uiState: StateFlow<TagMicroviewUiState> = _uiState.asStateFlow()

    fun load(tag: String) {
        viewModelScope.launch {
            _uiState.value = TagMicroviewUiState(tag = tag, isLoading = true)

            // Load metadata (description + counts)
            val metadata = tagDao.getTagMetadata(tag)

            // Load divisions for this tag
            val divisionIds = tagDao.getDivisionIdsForTag(tag)
            val divisions = if (divisionIds.isNotEmpty()) {
                divisionDao.getDivisionsByIds(divisionIds).map { it.toDomain() }
            } else {
                emptyList()
            }

            _uiState.value = TagMicroviewUiState(
                tag = tag,
                description = metadata?.description ?: "",
                divisionCount = metadata?.divisionCount ?: divisions.size,
                billCount = metadata?.billCount ?: 0,
                divisions = divisions.sortedByDescending { it.date },
                isLoading = false
            )
        }
    }

    private fun DivisionEntity.toDomain(): Division = Division(
        id = id,
        title = title,
        date = date,
        number = number,
        ayeCount = ayeCount,
        noCount = noCount,
        isDeferred = isDeferred,
        house = house,
        twfyDebateUrl = twfyDebateUrl
    )
}
