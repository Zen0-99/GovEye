package com.goveye.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules background workers for GovEye (D-09).
 *
 * - [scheduleDatabaseUpdateCheck]: Periodic every 6h — checks 5 manifest tags
 *   for DB patches, applies them, and triggers one-shot polling workers.
 * - [enqueueVotePollingOneShot]: One-shot — triggered after a votes patch.
 * - [enqueueBillPollingOneShot]: One-shot — triggered after a bills patch.
 *
 * No periodic polling workers — all change detection is DB-patch-driven (D-09).
 */
object WorkScheduler {
    const val VOTE_POLLING_WORK_NAME = "vote-polling"
    const val BILL_POLLING_WORK_NAME = "bill-polling"
    const val DATABASE_UPDATE_WORK_NAME = "database-update"

    /**
     * Enqueue VotePollingWorker as one-shot work (triggered after a votes patch).
     * Uses [ExistingWorkPolicy.REPLACE] so if a previous one-shot is still queued,
     * it's replaced with the latest.
     */
    fun enqueueVotePollingOneShot(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<VotePollingWorker>()
            .setConstraints(constraints)
            .addTag(VOTE_POLLING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            VOTE_POLLING_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Enqueue BillPollingWorker as one-shot work (triggered after a bills patch).
     */
    fun enqueueBillPollingOneShot(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BillPollingWorker>()
            .setConstraints(constraints)
            .addTag(BILL_POLLING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            BILL_POLLING_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Schedule the database update check worker (DATA-03, D-09).
     *
     * Runs every 6 hours with a 2-hour flex period per D-09 (6h latency,
     * not daily). Uses [ExistingPeriodicWorkPolicy.KEEP] so re-scheduling
     * doesn't replace an already-scheduled worker. Initial delay is 15
     * minutes after app start (the startup check in MainActivity handles
     * the immediate check).
     */
    fun scheduleDatabaseUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DatabaseUpdateWorker>(
            6,
            TimeUnit.HOURS,
            2,
            TimeUnit.HOURS,
        )
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(DATABASE_UPDATE_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DATABASE_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
