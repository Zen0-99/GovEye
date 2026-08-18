package com.goveye.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.preference.NotificationPreferences
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.SittingDayResolver
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Periodic worker that polls for new votes by followed MPs and dispatches
 * notifications (D-01, D-02, FOLLOW-02, FOLLOW-04).
 *
 * Adaptive scheduling (D-01):
 * - Sitting day → 30 min interval
 * - Non-sitting day → 4 hour interval
 *
 * The worker:
 * 1. Refreshes recess dates cache if stale (weekly)
 * 2. Checks if notifications are enabled
 * 3. Gets unmuted followed MP IDs
 * 4. For each MP, detects new votes (diff against cache)
 * 5. Dispatches notifications (max 5 per cycle, then summary)
 */
@HiltWorker
class VotePollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val followRepository: FollowRepository,
    private val votesRepository: VotesRepository,
    private val mpDao: MpDao,
    private val notificationHelper: NotificationHelper,
    private val notificationPreferences: NotificationPreferences,
    private val sittingDayResolver: SittingDayResolver,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val MAX_NOTIFICATIONS_PER_CYCLE = 5
        private const val SITTING_DAY_INTERVAL_MS = 30 * 60 * 1000L      // 30 min
        private const val NON_SITTING_DAY_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    override suspend fun doWork(): Result {
        try {
            // 1. Refresh recess dates cache if needed (weekly)
            sittingDayResolver.refreshRecessDatesIfNeeded()

            // 2. Check if vote notifications are enabled
            if (!notificationPreferences.getVotesEnabled()) {
                return Result.success()
            }

            // 3. Get unmuted followed MP IDs
            val followedIds = followRepository.getUnmutedMemberIds()
            if (followedIds.isEmpty()) {
                return Result.success()
            }

            // 4. Detect new votes for each followed MP
            val allNewVotes = mutableListOf<com.goveye.app.domain.model.NewVote>()
            for (memberId in followedIds) {
                try {
                    val mp = mpDao.getMp(memberId) ?: continue
                    val newVotes = votesRepository.detectNewVotesForMember(
                        memberId = memberId,
                        house = mp.house,
                        memberName = mp.nameDisplayAs,
                        thumbnailUrl = mp.thumbnailUrl,
                        partyName = mp.partyName,
                    )
                    allNewVotes.addAll(newVotes)
                } catch (e: Exception) {
                    // Continue to next MP on error
                }
            }

            // 5. Dispatch notifications (max 5, then summary)
            if (allNewVotes.isEmpty()) {
                return Result.success()
            }

            if (allNewVotes.size <= MAX_NOTIFICATIONS_PER_CYCLE) {
                for (vote in allNewVotes) {
                    notificationHelper.showVoteNotification(
                        NotificationHelper.VoteNotificationData(
                            mpName = vote.memberName,
                            mpThumbnailUrl = vote.thumbnailUrl,
                            divisionId = vote.divisionId,
                            divisionHouse = vote.house,
                            divisionTitle = vote.divisionTitle,
                            voteLabel = vote.voteLabel(),
                            isRebel = vote.isRebel,
                        ),
                    )
                }
            } else {
                notificationHelper.showSummaryNotification(allNewVotes.size)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun com.goveye.app.domain.model.VoteType.label(): String = when (this) {
        com.goveye.app.domain.model.VoteType.AYE -> "Aye"
        com.goveye.app.domain.model.VoteType.NO -> "No"
        com.goveye.app.domain.model.VoteType.NO_VOTE_RECORDED -> "No vote recorded"
    }

    private fun com.goveye.app.domain.model.NewVote.voteLabel(): String = voteType.label()
}
