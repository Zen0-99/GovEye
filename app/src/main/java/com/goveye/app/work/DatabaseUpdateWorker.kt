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
 * Background worker that checks for DB updates every 6h (D-09, DATA-03).
 *
 * - If patches are available (up to 5 × 5-50KB = max 250KB), applies them
 *   silently in the background (D-05, D-10a) via DatabaseUpdateManager.applyPatches().
 * - After applying patches, enqueues VotePollingWorker and/or BillPollingWorker
 *   as one-shot workers to detect new divisions or bill stage changes (D-09).
 * - If a full DB download is needed (~160MB), does NOT download — defers to
 *   the foreground where the user sees progress (Pitfall 4). The next app
 *   launch via [com.goveye.app.MainActivity]'s LaunchedEffect handles the
 *   full download.
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
                is DatabaseUpdateState.NeedsPatches -> {
                    Log.i(TAG, "Patches available for ${state.patches.size} streams: ${state.patches.joinToString { it.streamName }}")
                    val result = databaseUpdateManager.applyPatches(state.patches)
                    when (result) {
                        is DatabaseUpdateState.UpToDate -> {
                            Log.i(TAG, "Patches applied successfully")

                            // Determine which streams were patched
                            val appliedStreams = state.patches.map { it.streamName }.toSet()

                            // Trigger polling workers for streams that had updates
                            if ("votes" in appliedStreams) {
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
                    // Full DB downloads are deferred to foreground (Pitfall 4)
                    Log.i(TAG, "Full DB download needed — deferring to foreground")
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
}
