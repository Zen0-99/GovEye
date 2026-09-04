package com.goveye.app.ui.settings

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Check for updates" button in Settings.
 *
 * Exposes [isChecking] state and [snackbarMessages] for the UI to show
 * progress and result notifications. Uses a [Channel] for snackbar
 * messages (BUFFERED) so the SnackbarHost doesn't miss events if
 * multiple checks happen in quick succession.
 *
 * The actual update logic delegates to [DatabaseUpdateManager.checkForUpdates]
 * and [DatabaseUpdateManager.applyPatches] — same flow as the automatic
 * check on app launch, just triggered manually.
 */
@HiltViewModel
class UpdateCheckViewModel
@Inject
constructor(
    private val updateManager: DatabaseUpdateManager,
    private val notificationHelper: NotificationHelper,
    private val mpDao: MpDao
) : ViewModel() {

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    private val _snackbarMessages = Channel<UpdateSnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    /**
     * Manually triggers a check for database updates.
     * If patches are available, they are downloaded and applied immediately.
     * A snackbar message is emitted with the result.
     */
    fun checkForUpdates() {
        if (_isChecking.value) return
        viewModelScope.launch {
            _isChecking.value = true
            try {
                val state = updateManager.checkForUpdates()
                when (state) {
                    is DatabaseUpdateState.NeedsPatches -> {
                        val streamNames = state.patches.joinToString { it.streamName }
                        val result = updateManager.applyPatches(state.patches)
                        when (result) {
                            is DatabaseUpdateState.UpToDate -> {
                                _snackbarMessages.send(
                                    UpdateSnackbarMessage.Success(
                                        streamCount = state.patches.size,
                                        streamNames = streamNames
                                    )
                                )
                            }

                            is DatabaseUpdateState.Failed -> {
                                _snackbarMessages.send(
                                    UpdateSnackbarMessage.Error(result.message)
                                )
                            }

                            else -> {
                                _snackbarMessages.send(
                                    UpdateSnackbarMessage.Error("Unexpected state: $result")
                                )
                            }
                        }
                    }

                    is DatabaseUpdateState.UpToDate -> {
                        _snackbarMessages.send(UpdateSnackbarMessage.AlreadyUpToDate)
                    }

                    is DatabaseUpdateState.NeedsFullDownload -> {
                        _snackbarMessages.send(
                            UpdateSnackbarMessage.FullDownloadRequired
                        )
                    }

                    is DatabaseUpdateState.Failed -> {
                        _snackbarMessages.send(UpdateSnackbarMessage.Error(state.message))
                    }

                    else -> {
                        _snackbarMessages.send(
                            UpdateSnackbarMessage.Error("Unexpected state: $state")
                        )
                    }
                }
            } catch (e: Exception) {
                _snackbarMessages.send(UpdateSnackbarMessage.Error(e.message ?: "Unknown error"))
            } finally {
                _isChecking.value = false
            }
        }
    }

    // --- Mock notifications (testing) ---

    fun mockVoteNotification() {
        viewModelScope.launch {
            val mp = mpDao.getMp(4514) // Sir Keir Starmer
            notificationHelper.showVoteNotification(
                NotificationHelper.VoteNotificationData(
                    mpName = mp?.nameDisplayAs ?: "Sir Keir Starmer",
                    mpThumbnailUrl = mp?.thumbnailUrl,
                    divisionId = 12345,
                    divisionHouse = 1,
                    divisionTitle = "Second Reading of the Employment Rights Bill",
                    voteLabel = "Aye",
                    isRebel = false,
                    partyColor = mp?.partyBackgroundColour?.let { parsePartyColorInt(it) }
                )
            )
        }
    }

    fun mockSpeechNotification() {
        viewModelScope.launch {
            val mp = mpDao.getMp(4031) // Rachel Reeves
            notificationHelper.showSpeechNotification(
                mpName = mp?.nameDisplayAs ?: "Rachel Reeves",
                debateTitle = "Autumn Budget Statement",
                mpThumbnailUrl = mp?.thumbnailUrl,
                partyColor = mp?.partyBackgroundColour?.let { parsePartyColorInt(it) }
            )
        }
    }

    fun mockIncomeNotification() {
        viewModelScope.launch {
            val mp = mpDao.getMp(4483) // Rishi Sunak
            notificationHelper.showIncomeNotification(
                memberId = 4483,
                mpName = mp?.nameDisplayAs ?: "Rishi Sunak",
                amount = "£8,500",
                source = "Corwin Holdings Ltd",
                mpThumbnailUrl = mp?.thumbnailUrl,
                partyColor = mp?.partyBackgroundColour?.let { parsePartyColorInt(it) }
            )
        }
    }

    fun mockExpenseNotification() {
        viewModelScope.launch {
            val mp = mpDao.getMp(4483) // Rishi Sunak
            notificationHelper.showExpenseNotification(
                memberId = 4483,
                mpName = mp?.nameDisplayAs ?: "Rishi Sunak",
                amount = "£1,200",
                category = "Office Costs",
                mpThumbnailUrl = mp?.thumbnailUrl,
                partyColor = mp?.partyBackgroundColour?.let { parsePartyColorInt(it) }
            )
        }
    }

    /**
     * Parse a hex party color string (e.g. "d50000") to an ARGB int
     * for NotificationCompat.Builder.setColor().
     */
    private fun parsePartyColorInt(hex: String): Int = try {
        if (hex.startsWith("#")) {
            Color.parseColor(hex)
        } else {
            Color.parseColor("#$hex")
        }
    } catch (e: Exception) {
        0xFF6750A4.toInt() // fallback to Material purple
    }
}

/**
 * Sealed message type for update-check snackbars.
 * Allows the UI to format different message styles per result.
 */
sealed interface UpdateSnackbarMessage {
    /** Database is already up to date — no patches needed. */
    data object AlreadyUpToDate : UpdateSnackbarMessage

    /** Patches were downloaded and applied successfully. */
    data class Success(val streamCount: Int, val streamNames: String) : UpdateSnackbarMessage

    /** A full DB download is required (multiple versions behind). */
    data object FullDownloadRequired : UpdateSnackbarMessage

    /** An error occurred during the check or patch application. */
    data class Error(val message: String) : UpdateSnackbarMessage
}
