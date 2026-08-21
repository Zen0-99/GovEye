package com.goveye.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.domain.model.NewVote
import com.goveye.app.domain.model.VoteType
import com.goveye.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * One-shot worker that detects new divisions after a votes DB patch and
 * dispatches vote notifications for MPs with votesEnabled=true (D-09).
 *
 * Triggered by [DatabaseUpdateWorker] after a votes patch is applied.
 * No periodic scheduling — runs on-demand only.
 *
 * Flow:
 * 1. Read lastNotifiedDivisionId from DatabasePreferences
 * 2. Query divisions WHERE id > lastNotifiedDivisionId
 * 3. For each new division, check if any notification-enabled MP voted
 * 4. Dispatch notifications (max 5 per cycle, then summary)
 * 5. Update lastNotifiedDivisionId to current max
 */
@HiltWorker
class VotePollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val divisionDao: DivisionDao,
    private val mpDao: MpDao,
    private val notificationHelper: NotificationHelper,
    private val notificationPrefRepository: NotificationPreferenceRepository,
    private val databasePreferences: DatabasePreferences,
    private val json: Json
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val MAX_NOTIFICATIONS_PER_CYCLE = 5
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Get the last notified division ID from preferences
            val lastNotifiedId = databasePreferences.lastNotifiedDivisionId.first() ?: 0

            // 2. Get current max division ID from DB
            val currentMaxId = divisionDao.getMaxDivisionId() ?: return Result.success()

            // 3. If no new divisions, return
            if (currentMaxId <= lastNotifiedId) {
                return Result.success()
            }

            // 4. Get new divisions (id > lastNotifiedId)
            val newDivisions = divisionDao.getDivisionsAfterId(lastNotifiedId)
            if (newDivisions.isEmpty()) {
                databasePreferences.setLastNotifiedDivisionId(currentMaxId)
                return Result.success()
            }

            // 5. Get MP IDs with vote notifications enabled
            val memberIds = notificationPrefRepository.getMemberIdsWithVotesEnabled()
            if (memberIds.isEmpty()) {
                databasePreferences.setLastNotifiedDivisionId(currentMaxId)
                return Result.success()
            }

            // 6. Get all votes for the new divisions
            val newDivisionIds = newDivisions.map { it.id }
            val votes = divisionDao.getVotesForDivisions(newDivisionIds)

            // 7. Build NewVote list for each enabled MP who voted in a new division
            val memberIdSet = memberIds.toSet()
            val allNewVotes = mutableListOf<NewVote>()
            for (division in newDivisions) {
                val divisionVotes = votes.filter { it.divisionId == division.id }
                for (vote in divisionVotes) {
                    if (vote.memberId in memberIdSet) {
                        val mp = mpDao.getMp(vote.memberId) ?: continue
                        val voteType = runCatching { VoteType.valueOf(vote.vote) }.getOrNull() ?: continue
                        val isRebel = checkIfRebel(division.id, mp.partyName, voteType)
                        allNewVotes.add(
                            NewVote(
                                memberId = vote.memberId,
                                memberName = mp.nameDisplayAs,
                                thumbnailUrl = mp.thumbnailUrl,
                                partyName = mp.partyName,
                                divisionId = division.id,
                                house = division.house,
                                divisionTitle = division.title,
                                voteType = voteType,
                                isRebel = isRebel
                            )
                        )
                    }
                }
            }

            // 8. Dispatch notifications (max 5, then summary)
            if (allNewVotes.isNotEmpty()) {
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
                                isRebel = vote.isRebel
                            )
                        )
                    }
                } else {
                    notificationHelper.showSummaryNotification(allNewVotes.size)
                }
            }

            // 9. Update lastNotifiedDivisionId
            databasePreferences.setLastNotifiedDivisionId(currentMaxId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Check if the MP's vote is a rebellion (against party majority).
     * Copied from VotesRepository.checkIfRebel() — the worker needs its own
     * copy since it no longer depends on VotesRepository (D-09).
     */
    private suspend fun checkIfRebel(divisionId: Int, partyName: String, mpVote: VoteType): Boolean {
        if (mpVote == VoteType.NO_VOTE_RECORDED) return false
        val votes = divisionDao.getVotesForDivision(divisionId)
        val partyVotes = votes.filter { it.partyName.equals(partyName, ignoreCase = true) }
        if (partyVotes.isEmpty()) return false
        val ayes = partyVotes.count { it.vote == "AYE" }
        val noes = partyVotes.count { it.vote == "NO" }
        if (ayes == noes) return false
        val partyMajority = if (ayes > noes) VoteType.AYE else VoteType.NO
        return mpVote != partyMajority
    }

    private fun VoteType.label(): String = when (this) {
        VoteType.AYE -> "Aye"
        VoteType.NO -> "No"
        VoteType.NO_VOTE_RECORDED -> "No vote recorded"
    }

    private fun NewVote.voteLabel(): String = voteType.label()
}
