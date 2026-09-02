package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpTagDao
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.domain.model.Mp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MpTagBrowseUiState(
    val tag: String = "",
    val description: String = "",
    val mps: List<Mp> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for [MpTagBrowseScreen] — shows all MPs who have a given tag,
 * ranked by frequency + recency weighted hitCount.
 */
@HiltViewModel
class MpTagBrowseViewModel @Inject constructor(
    private val mpTagDao: MpTagDao,
    private val mpDao: MpDao,
    private val tagDao: TagDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MpTagBrowseUiState())
    val uiState: StateFlow<MpTagBrowseUiState> = _uiState.asStateFlow()

    fun load(tag: String) {
        viewModelScope.launch {
            _uiState.value = MpTagBrowseUiState(tag = tag, isLoading = true)

            // Get metadata (description + counts)
            val metadata = runCatching { tagDao.getTagMetadata(tag) }.getOrNull()

            // Get MP IDs for this tag, ranked by hitCount descending
            val memberIds = runCatching { mpTagDao.getMpsForTag(tag) }.getOrDefault(emptyList())

            // Resolve to MP domain objects
            val mps = if (memberIds.isNotEmpty()) {
                val mpEntities = mpDao.getMpsByIds(memberIds)
                val mpById = mpEntities.associateBy { it.id }
                // Preserve the ranked order from memberIds
                memberIds.mapNotNull { id -> mpById[id]?.toDomain() }
            } else {
                emptyList()
            }

            _uiState.value = MpTagBrowseUiState(
                tag = tag,
                description = metadata?.description ?: "",
                mps = mps,
                isLoading = false
            )
        }
    }

    private fun com.goveye.app.data.local.entity.MpEntity.toDomain(): Mp = Mp(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = nameFullTitle,
        gender = gender,
        party = com.goveye.app.domain.model.Party(
            id = partyId,
            name = partyName,
            abbreviation = partyAbbreviation,
            backgroundColour = partyBackgroundColour,
            foregroundColour = partyForegroundColour
        ),
        constituency = com.goveye.app.domain.model.Constituency(
            id = constituencyId,
            name = constituencyName
        ),
        house = house,
        membershipStartDate = membershipStartDate,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl
    )
}
