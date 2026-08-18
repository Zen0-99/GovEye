package com.goveye.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.preference.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val votesEnabled: Boolean = true,
    val speechesEnabled: Boolean = false,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationPreferences: NotificationPreferences,
) : ViewModel() {

    val uiState: StateFlow<NotificationSettingsUiState> =
        combine(
            notificationPreferences.votesEnabled,
            notificationPreferences.speechesEnabled,
        ) { votes, speeches ->
            NotificationSettingsUiState(votesEnabled = votes, speechesEnabled = speeches)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationSettingsUiState(),
        )

    fun setVotesEnabled(enabled: Boolean) {
        viewModelScope.launch { notificationPreferences.setVotesEnabled(enabled) }
    }

    fun setSpeechesEnabled(enabled: Boolean) {
        viewModelScope.launch { notificationPreferences.setSpeechesEnabled(enabled) }
    }
}
