package com.goveye.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * One-shot worker that detects bill stage changes after a bills DB patch and
 * dispatches notifications for followed bills (D-09, BILLS-04).
 *
 * Triggered by [DatabaseUpdateWorker] after a bills patch is applied.
 * No periodic scheduling — runs on-demand only.
 *
 * Flow:
 * 1. Get followed bill IDs from BillFollowRepository
 * 2. Read last known bill stages from DatabasePreferences (JSON map)
 * 3. Read current bill stages from BundledDatabase via BillDao
 * 4. Compare old vs new currentStageDescription for each followed bill
 * 5. Dispatch notifications for changed stages (max 5, then summary)
 * 6. Update lastNotifiedBillStages in preferences
 */
@HiltWorker
class BillPollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val billDao: BillDao,
    private val billFollowRepository: BillFollowRepository,
    private val notificationHelper: NotificationHelper,
    private val databasePreferences: DatabasePreferences,
    private val json: Json,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val MAX_NOTIFICATIONS_PER_CYCLE = 5
    }

    private val stagesSerializer = MapSerializer(Int.serializer(), String.serializer())

    override suspend fun doWork(): Result {
        return try {
            // 1. Get followed bill IDs
            val followedBillIds = billFollowRepository.getFollowedBillIds()
            if (followedBillIds.isEmpty()) {
                return Result.success()
            }

            // 2. Read last known bill stages from preferences (JSON map: billId → currentStageDescription)
            val lastStagesJson = databasePreferences.lastNotifiedBillStages.first()
            val lastStages: Map<Int, String> = if (lastStagesJson != null) {
                json.decodeFromString(stagesSerializer, lastStagesJson)
            } else {
                emptyMap()
            }

            // 3. Read current bill stages from DB
            val currentBills = billDao.getBillsByIds(followedBillIds)
            val currentStages = currentBills.associate { it.id to (it.currentStageDescription ?: "") }

            // 4. Detect stage changes for followed bills
            val stageChanges = mutableListOf<NotificationHelper.BillNotificationData>()
            for (bill in currentBills) {
                val oldStage = lastStages[bill.id]
                val newStage = bill.currentStageDescription ?: ""
                // Only notify if we had a previous stage and it changed
                if (oldStage != null && oldStage != newStage && newStage.isNotEmpty()) {
                    stageChanges.add(
                        NotificationHelper.BillNotificationData(
                            billId = bill.id,
                            billTitle = bill.shortTitle,
                            newStage = newStage,
                        ),
                    )
                }
            }

            // 5. Dispatch notifications (max 5, then summary)
            if (stageChanges.isNotEmpty()) {
                if (stageChanges.size <= MAX_NOTIFICATIONS_PER_CYCLE) {
                    for (change in stageChanges) {
                        notificationHelper.showBillStageNotification(change)
                    }
                } else {
                    notificationHelper.showBillSummaryNotification(stageChanges.size)
                }
            }

            // 6. Update lastNotifiedBillStages with current stages
            val stagesJson = json.encodeToString(stagesSerializer, currentStages)
            databasePreferences.setLastNotifiedBillStages(stagesJson)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
