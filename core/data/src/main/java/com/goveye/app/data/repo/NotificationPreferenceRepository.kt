package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-MP notification preferences, decoupled from follows (D-04 revised).
 *
 * The "notifications enabled" master toggle is derived from the individual
 * type toggles — see [MpNotificationPreferenceEntity.notificationsEnabled].
 */
@Singleton
class NotificationPreferenceRepository @Inject constructor(private val dao: MpNotificationPreferenceDao) {
    fun observe(memberId: Int): Flow<MpNotificationPreferenceEntity> =
        dao.observe(memberId).map { it ?: MpNotificationPreferenceEntity(memberId) }

    suspend fun get(memberId: Int): MpNotificationPreferenceEntity =
        dao.get(memberId) ?: MpNotificationPreferenceEntity(memberId)

    suspend fun getMemberIdsWithVotesEnabled(): List<Int> = dao.getMemberIdsWithVotesEnabled()

    /**
     * Set the master toggle. When turning ON, enable all types.
     * When turning OFF, disable all types.
     */
    suspend fun setNotificationsEnabled(memberId: Int, enabled: Boolean) {
        val current = dao.get(memberId) ?: MpNotificationPreferenceEntity(memberId)
        if (enabled) {
            // Turn on: if nothing was enabled, enable votes by default
            if (!current.notificationsEnabled) {
                dao.upsert(current.copy(votesEnabled = true, speechesEnabled = false))
            }
        } else {
            // Turn off: disable everything
            dao.upsert(current.copy(votesEnabled = false, speechesEnabled = false))
        }
    }

    /** Toggle a single notification type. Auto-creates the row if needed. */
    suspend fun setVotesEnabled(memberId: Int, enabled: Boolean) {
        val current = dao.get(memberId) ?: MpNotificationPreferenceEntity(memberId)
        dao.upsert(current.copy(votesEnabled = enabled))
    }

    suspend fun setSpeechesEnabled(memberId: Int, enabled: Boolean) {
        val current = dao.get(memberId) ?: MpNotificationPreferenceEntity(memberId)
        dao.upsert(current.copy(speechesEnabled = enabled))
    }

    suspend fun delete(memberId: Int) = dao.delete(memberId)
}
