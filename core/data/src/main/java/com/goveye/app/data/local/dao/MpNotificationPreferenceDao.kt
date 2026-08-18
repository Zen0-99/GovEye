package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MpNotificationPreferenceDao {
    @Query("SELECT * FROM mp_notification_prefs WHERE memberId = :memberId")
    fun observe(memberId: Int): Flow<MpNotificationPreferenceEntity?>

    @Query("SELECT * FROM mp_notification_prefs WHERE memberId = :memberId")
    suspend fun get(memberId: Int): MpNotificationPreferenceEntity?

    @Query("SELECT memberId FROM mp_notification_prefs WHERE votesEnabled = 1")
    suspend fun getMemberIdsWithVotesEnabled(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: MpNotificationPreferenceEntity)

    @Query("UPDATE mp_notification_prefs SET votesEnabled = :enabled WHERE memberId = :memberId")
    suspend fun setVotesEnabled(memberId: Int, enabled: Boolean)

    @Query("UPDATE mp_notification_prefs SET speechesEnabled = :enabled WHERE memberId = :memberId")
    suspend fun setSpeechesEnabled(memberId: Int, enabled: Boolean)

    @Query("DELETE FROM mp_notification_prefs WHERE memberId = :memberId")
    suspend fun delete(memberId: Int)
}
