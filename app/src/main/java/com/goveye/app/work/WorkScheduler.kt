package com.goveye.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the [VotePollingWorker] as unique periodic work (D-01, FOLLOW-04).
 *
 * Uses [ExistingPeriodicWorkPolicy.UPDATE] so re-scheduling doesn't cancel
 * a running worker. The initial interval is 30 minutes (sitting day default);
 * the worker uses [androidx.work.WorkRequest.setNextScheduleTimeOverride] to
 * adaptively adjust the next run based on sitting-day detection.
 */
object WorkScheduler {
    const val VOTE_POLLING_WORK_NAME = "vote-polling"
    const val BILL_POLLING_WORK_NAME = "bill-polling"

    /**
     * Schedule the vote polling worker if not already scheduled.
     * Safe to call multiple times — uses unique work policy.
     */
    fun scheduleVotePolling(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<VotePollingWorker>(
            30,
            TimeUnit.MINUTES,
        )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(VOTE_POLLING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            VOTE_POLLING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Cancel the vote polling worker (e.g., when user unfollows their last MP).
     */
    fun cancelVotePolling(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(VOTE_POLLING_WORK_NAME)
    }

    /**
     * Schedule the bill polling worker if not already scheduled.
     * Runs every 4 hours (bills change less frequently than votes).
     */
    fun scheduleBillPolling(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BillPollingWorker>(
            4,
            TimeUnit.HOURS,
        )
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(BILL_POLLING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BILL_POLLING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Cancel the bill polling worker (e.g., when user unfollows their last bill).
     */
    fun cancelBillPolling(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BILL_POLLING_WORK_NAME)
    }
}
