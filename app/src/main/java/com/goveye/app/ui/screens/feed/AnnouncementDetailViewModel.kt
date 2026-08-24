package com.goveye.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.domain.model.GovernmentPublication
import com.goveye.app.domain.model.Legislation
import com.goveye.app.domain.model.WrittenStatement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared UI state for the three announcement detail screens
 * (publication, statement, legislation). Only one of [publication],
 * [statement], or [legislation] is non-null at a time, depending on
 * which [load] method was called.
 */
data class AnnouncementDetailUiState(
    val publication: GovernmentPublication? = null,
    val statement: WrittenStatement? = null,
    val legislation: Legislation? = null,
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Shared ViewModel for [PublicationDetailScreen], [StatementDetailScreen],
 * and [LegislationDetailScreen]. Loads a single announcement by ID from
 * [GovernmentAnnouncementsRepository] along with its tags.
 */
@HiltViewModel
class AnnouncementDetailViewModel @Inject constructor(
    private val governmentAnnouncementsRepository: GovernmentAnnouncementsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementDetailUiState())
    val state: StateFlow<AnnouncementDetailUiState> = _state.asStateFlow()

    private var loadedKey: String? = null

    fun loadPublication(publicationId: Int) {
        val key = "publication-$publicationId"
        if (loadedKey == key) return
        loadedKey = key
        _state.value = AnnouncementDetailUiState(isLoading = true)
        viewModelScope.launch {
            val publication = governmentAnnouncementsRepository.getPublication(publicationId)
            val tags = if (publication != null) {
                governmentAnnouncementsRepository.getTagsForPublication(publicationId)
            } else {
                emptyList()
            }
            _state.value = AnnouncementDetailUiState(
                publication = publication,
                tags = tags,
                isLoading = false
            )
        }
    }

    fun loadStatement(statementId: Int) {
        val key = "statement-$statementId"
        if (loadedKey == key) return
        loadedKey = key
        _state.value = AnnouncementDetailUiState(isLoading = true)
        viewModelScope.launch {
            val statement = governmentAnnouncementsRepository.getStatement(statementId)
            val tags = if (statement != null) {
                governmentAnnouncementsRepository.getTagsForStatement(statementId)
            } else {
                emptyList()
            }
            _state.value = AnnouncementDetailUiState(
                statement = statement,
                tags = tags,
                isLoading = false
            )
        }
    }

    fun loadLegislation(legislationId: Int) {
        val key = "legislation-$legislationId"
        if (loadedKey == key) return
        loadedKey = key
        _state.value = AnnouncementDetailUiState(isLoading = true)
        viewModelScope.launch {
            val legislation = governmentAnnouncementsRepository.getLegislation(legislationId)
            val tags = if (legislation != null) {
                governmentAnnouncementsRepository.getTagsForLegislation(legislationId)
            } else {
                emptyList()
            }
            _state.value = AnnouncementDetailUiState(
                legislation = legislation,
                tags = tags,
                isLoading = false
            )
        }
    }
}
