package com.goveye.app.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.goveye.app.R
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Foreground-service worker that downloads and merges the per-API databases
 * on first launch (or when a full re-download is needed).
 *
 * Replaces the old in-Activity [DatabaseUpdateManager.downloadAndMergePerApiDbs]
 * call so the download survives app minimization — the worker is promoted to
 * a foreground service via [setForeground], keeping the process alive even
 * when the Activity is stopped.
 *
 * Pattern adapted from Syncstone's SyncWorker:
 * - [setForeground] with a low-priority notification (dataSync type)
 * - Progress reported via [setProgress] so the UI can observe [WorkInfo]
 * - 500ms delay after setForeground to let the promotion take effect
 *
 * @see DatabaseUpdateManager.downloadSeedDb
 */
@HiltWorker
class DatabaseDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val databaseUpdateManager: DatabaseUpdateManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DatabaseDownloadWorker"

        /** WorkManager unique work name for the first-launch download. */
        const val WORK_NAME = "database-download"

        /** Progress key in WorkInfo.progress — Float 0f..1f. */
        const val KEY_PROGRESS = "progress"

        /** Notification ID for the foreground download notification. */
        private const val NOTIFICATION_ID = 1003

        /** Update the notification at most every N progress ticks to avoid flooding. */
        private const val NOTIFICATION_UPDATE_THRESHOLD = 0.05f
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "doWork started — promoting to foreground service")

        // Guard: if the seed was already downloaded (e.g. process was killed
        // after a successful download and WorkManager retried this worker),
        // return success immediately without re-downloading.
        if (!databaseUpdateManager.isFirstLaunch()) {
            Log.i(TAG, "Seed already installed — skipping download")
            return Result.success()
        }

        // Create the download notification channel (idempotent)
        notificationHelper.createChannels()

        // Promote this worker to a foreground service so the download
        // continues even when the app is minimized. On Android 12+ this
        // may throw ForegroundServiceStartNotAllowedException when the
        // app is backgrounded — setForegroundSafely() catches that.
        setForegroundSafely()

        // Report initial progress
        setProgress(workDataOf(KEY_PROGRESS to 0f))

        var lastNotificationUpdate = 0f

        val result = databaseUpdateManager.downloadSeedDb { progress ->
            Log.d(TAG, "Download progress: ${(progress * 100).toInt()}%")

            // Report progress to WorkInfo for UI observation.
            // Use the suspend setProgress (not setProgressAsync) so
            // updates are delivered reliably to the observer in MainActivity.
            setProgress(workDataOf(KEY_PROGRESS to progress))

            // Update the notification periodically (avoid flooding)
            if (progress - lastNotificationUpdate >= NOTIFICATION_UPDATE_THRESHOLD || progress >= 1f) {
                lastNotificationUpdate = progress
                updateForegroundNotification(progress)
            }
        }

        return when (result) {
            is DatabaseUpdateState.UpToDate -> {
                Log.i(TAG, "Download and merge complete — waiting for user to restart")
                updateForegroundNotification(1f, "Download complete")
                // Don't auto-relaunch the Activity. The UI observes
                // WorkInfo.State.SUCCEEDED and shows a "Restart" button.
                // The user taps it to recreate the Activity, which gives
                // all ViewModels fresh Room connections (InvalidationTracker
                // is broken from database.close() during the download).
                Result.success()
            }

            is DatabaseUpdateState.NeedsWifi -> {
                Log.w(TAG, "Download deferred — metered connection")
                Result.failure(workDataOf("reason" to "needs_wifi"))
            }

            is DatabaseUpdateState.Failed -> {
                Log.e(TAG, "Download failed: ${result.message}")
                Result.failure(workDataOf("reason" to "failed", "message" to result.message))
            }

            else -> {
                Log.w(TAG, "Unexpected state: $result")
                Result.success()
            }
        }
    }

    /**
     * Builds the [ForegroundInfo] for [setForeground] — promotes this worker
     * to a foreground service with a dataSync type so it runs even when the
     * app is in the background (Android 12+).
     */
    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        val percent = (progress * 100).toInt()
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.DOWNLOAD_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("GovEye")
            .setContentText("Downloading parliamentary data… $percent%")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /** Updates the foreground notification with the latest progress. */
    private fun updateForegroundNotification(progress: Float, text: String? = null) {
        try {
            val percent = (progress * 100).toInt()
            val notification = NotificationCompat.Builder(
                applicationContext,
                NotificationHelper.DOWNLOAD_CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("GovEye")
                .setContentText(text ?: "Downloading parliamentary data… $percent%")
                .setProgress(100, percent, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val nm = applicationContext.getSystemService(android.app.NotificationManager::class.java)
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update notification: ${e.message}")
        }
    }

    /**
     * Promotes the worker to a foreground service, catching the
     * ForegroundServiceStartNotAllowedException that Android 12+ throws
     * when the app is in the background.
     *
     * The 500ms delay after setForeground() lets the foreground service
     * promotion take effect before heavy work starts (Syncstone pattern).
     */
    private suspend fun setForegroundSafely() {
        try {
            setForeground(createForegroundInfo(0f))
            delay(500)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set foreground: ${e.message}")
        }
    }
}
