package com.goveye.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker that polls followed bills for stage changes and dispatches
 * notifications (BILLS-04, D-05, D-06).
 *
 * Runs every 4 hours (bills change less frequently than votes).
 *
 * The worker:
 * 1. Gets followed bill IDs from BillFollowRepository
 * 2. For each bill, reads cached currentStageDescription
 * 3. Refreshes from API (refreshBillDetail)
 * 4. Compares new currentStageDescription with cached value
 * 5. If changed → dispatches notification (max 5 per cycle, then summary)
 */
@HiltWorker
class BillPollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val billsRepository: BillsRepository,
    private val billFollowRepository: BillFollowRepository,
    private val billDao: BillDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val MAX_NOTIFICATIONS_PER_CYCLE = 5
    }

    override suspend fun doWork(): Result {
        try {
            val followedBillIds = billFollowRepository.getFollowedBillIds()
            if (followedBillIds.isEmpty()) {
                return Result.success()
            }

            val stageChanges = mutableListOf<NotificationHelper.BillNotificationData>()

            for (billId in followedBillIds) {
                try {
                    // Read cached stage before refresh
                    val cachedBill = billDao.getBill(billId)
                    val oldStage = cachedBill?.currentStageDescription

                    // Refresh from API
                    billsRepository.refreshBillDetail(billId)

                    // Read new stage after refresh
                    val refreshedBill = billDao.getBill(billId)
                    val newStage = refreshedBill?.currentStageDescription

                    // If stage changed (and we had a previous stage), notify
                    if (oldStage != null && newStage != null && oldStage != newStage) {
                        stageChanges.add(
                            NotificationHelper.BillNotificationData(
                                billId = billId,
                                billTitle = refreshedBill.shortTitle,
                                newStage = newStage,
                            ),
                        )
                    }
                } catch (e: Exception) {
                    // Continue to next bill on error
                }
            }

            if (stageChanges.isEmpty()) {
                return Result.success()
            }

            if (stageChanges.size <= MAX_NOTIFICATIONS_PER_CYCLE) {
                for (change in stageChanges) {
                    notificationHelper.showBillStageNotification(change)
                }
            } else {
                notificationHelper.showBillSummaryNotification(stageChanges.size)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
