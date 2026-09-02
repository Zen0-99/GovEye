package com.goveye.app.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.goveye.app.MainActivity
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

        // Create the download notification channel (idempotent)
        notificationHelper.createChannels()

        // Promote this worker to a foreground service so the download
        // continues even when the app is minimized. On Android 12+ this
        // may throw ForegroundServiceStartNotAllowedException when the
        // app is backgrounded — setForegroundSafely() catches that.
        setForegroundSafely()

        // Acquire a partial wake lock so the CPU stays awake during the
        // download even when the screen is off. The foreground service
        // should handle this, but some OEMs have aggressive battery
        // management that can still throttle background work.
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        // 10 min max — safety cap so the lock is never held indefinitely.
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "GovEye:DatabaseDownload"
        ).apply { acquire(10 * 60 * 1000L) }

        try {
            // Report initial progress
            setProgress(workDataOf(KEY_PROGRESS to 0f))

            var lastNotificationUpdate = 0f

            val result = try {
                databaseUpdateManager.downloadSeedDb { progress ->
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.i(TAG, "Download cancelled by user")
                return Result.failure(workDataOf("reason" to "cancelled"))
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
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /**
     * Builds the [ForegroundInfo] for [setForeground] — promotes this worker
     * to a foreground service with a dataSync type so it runs even when the
     * app is in the background (Android 12+).
     *
     * The notification includes:
     * - A content intent that opens [MainActivity] when the notification bar
     *   is tapped (navigates the user back into the app).
     * - A "Cancel" action button that sends a broadcast to
     *   [CancelDownloadReceiver], which instantly cancels the download
     *   (no confirmation dialog — that's only in the in-app UI).
     */
    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        val percent = (progress * 100).toInt()
        val notification = buildDownloadNotification(percent, null)

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
            val notification = buildDownloadNotification(percent, text)

            val nm = applicationContext.getSystemService(android.app.NotificationManager::class.java)
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update notification: ${e.message}")
        }
    }

    /**
     * Builds the download notification with content intent (opens app) and
     * cancel action button (instant cancel via [CancelDownloadReceiver]).
     */
    private fun buildDownloadNotification(percent: Int, text: String?): android.app.Notification {
        // Content intent — opens MainActivity when the notification is tapped
        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel action — broadcasts to CancelDownloadReceiver
        val cancelIntent = Intent(applicationContext, CancelDownloadReceiver::class.java).apply {
            action = CancelDownloadReceiver.ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.DOWNLOAD_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("GovEye")
            .setContentText(text ?: "Downloading parliamentary data… $percent%")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
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
