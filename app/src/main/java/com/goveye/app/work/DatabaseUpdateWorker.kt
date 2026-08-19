package com.goveye.app.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily background worker that checks for DB updates (DATA-03).
 *
 * - If a patch is available (5-50KB), applies it silently in the background (D-05).
 * - If a full DB download is needed (~160MB), does NOT download — defers to the
 *   foreground where the user sees progress (Pitfall 4). The next app launch
 *   via [com.goveye.app.MainActivity]'s LaunchedEffect handles the full download.
 * - Returns [Result.retry] on transient network failures.
 *
 * Follows the [VotePollingWorker] @HiltWorker pattern.
 */
@HiltWorker
class DatabaseUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val databaseUpdateManager: DatabaseUpdateManager,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DatabaseUpdateWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            when (val state = databaseUpdateManager.checkForUpdates()) {
                is DatabaseUpdateState.NeedsPatch -> {
                    Log.i(TAG, "Patch available, applying in background")
                    val result = databaseUpdateManager.applyPatch(state.manifest)
                    when (result) {
                        is DatabaseUpdateState.UpToDate ->
                            Log.i(TAG, "Patch applied successfully")
                        is DatabaseUpdateState.Failed -> {
                            Log.w(TAG, "Patch application failed: ${result.message}")
                            return Result.retry()
                        }
                        else -> { /* unexpected */ }
                    }
                    Result.success()
                }
                is DatabaseUpdateState.NeedsFullDownload -> {
                    // Full DB downloads are deferred to foreground (Pitfall 4)
                    Log.i(TAG, "Full DB download needed — deferring to foreground")
                    Result.success()
                }
                is DatabaseUpdateState.UpToDate -> {
                    Log.i(TAG, "Database is up to date")
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
}
