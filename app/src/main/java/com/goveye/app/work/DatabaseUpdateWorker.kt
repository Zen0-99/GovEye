package com.goveye.app.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.goveye.app.data.preference.DownloadPreferences
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Background worker that checks for DB updates every 6h (D-09, DATA-03).
 *
 * - If patches are available (up to 7 × 5-50KB = max 350KB), applies them
 *   in the background via [DatabaseUpdateManager.applyPatches] silently —
 *   no user notification.
 * - Promotes itself to a foreground service (Stonesync pattern) with a
 *   notification while applying patches, so the user can see that an
 *   update is in progress.
 * - If a full DB download is needed (stream multiple versions behind),
 *   enqueues [DatabaseDownloadWorker] to download the seed DB silently
 *   in the background. The download worker is a foreground service so
 *   it survives app minimization and screen-off.
 * - After applying patches, enqueues VotePollingWorker and/or
 *   BillPollingWorker as one-shot workers to detect new divisions or
 *   bill stage changes (D-09).
 * - Returns [Result.retry] on transient network failures.
 *
 * @see DatabaseUpdateManager.checkForUpdates
 * @see DatabaseUpdateManager.applyPatches
 */
@HiltWorker
class DatabaseUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val databaseUpdateManager: DatabaseUpdateManager,
    private val notificationHelper: NotificationHelper,
    private val downloadPreferences: DownloadPreferences
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DatabaseUpdateWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            when (val state = databaseUpdateManager.checkForUpdates()) {
                is DatabaseUpdateState.NeedsPatches -> {
                    Log.i(
                        TAG,
                        "Patches available for ${state.patches.size} streams: ${state.patches.joinToString {
                            it.streamName
                        }}"
                    )

                    // Promote to foreground service while applying patches
                    // (Stonesync pattern — visible notification during update)
                    setForegroundSafely("Applying updates…")

                    val result = databaseUpdateManager.applyPatches(state.patches)
                    when (result) {
                        is DatabaseUpdateState.UpToDate -> {
                            Log.i(TAG, "Patches applied successfully")

                            // Determine which streams were patched
                            val appliedStreams = state.patches.map { it.streamName }.toSet()

                            // Trigger polling workers for streams that had updates
                            if ("commons-votes" in appliedStreams || "lords-votes" in appliedStreams) {
                                Log.i(TAG, "Votes patch applied — enqueuing VotePollingWorker")
                                WorkScheduler.enqueueVotePollingOneShot(applicationContext)
                            }
                            if ("bills" in appliedStreams) {
                                Log.i(TAG, "Bills patch applied — enqueuing BillPollingWorker")
                                WorkScheduler.enqueueBillPollingOneShot(applicationContext)
                            }
                        }

                        is DatabaseUpdateState.Failed -> {
                            Log.w(TAG, "Patch application failed: ${result.message}")
                            return Result.retry()
                        }

                        else -> { /* unexpected */ }
                    }
                    Result.success()
                }

                is DatabaseUpdateState.NeedsFullDownload -> {
                    // Enqueue the download worker to download the seed DB
                    // silently in the background. The download worker is a
                    // foreground service so it survives app minimization
                    // and screen-off. The wifiOnly constraint respects the
                    // user's preference.
                    Log.i(TAG, "Full DB download needed — enqueuing download worker")
                    val wifiOnly = downloadPreferences.wifiOnly.first()
                    WorkScheduler.enqueueDatabaseDownload(applicationContext, wifiOnly = wifiOnly)
                    Result.success()
                }

                is DatabaseUpdateState.UpToDate -> {
                    Log.i(TAG, "All streams up to date")
                    Result.success()
                }

                is DatabaseUpdateState.Failed -> {
                    Log.w(TAG, "Update check failed: ${state.message}")
                    Result.retry()
                }

                else -> Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker error: ${e.message}")
            Result.retry()
        }
    }

    /**
     * Promotes the worker to a foreground service with a dataSync
     * notification (Stonesync pattern). Catches
     * ForegroundServiceStartNotAllowedException on Android 12+.
     */
    private suspend fun setForegroundSafely(contentText: String) {
        try {
            val notification = notificationHelper.buildUpdateForegroundNotification(
                contentText = contentText,
                indeterminate = true
            ).build()

            val foregroundInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NotificationHelper.UPDATE_FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(NotificationHelper.UPDATE_FOREGROUND_NOTIFICATION_ID, notification)
            }

            setForeground(foregroundInfo)
            delay(500)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set foreground: ${e.message}")
        }
    }
}
