package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Per-MP notification preferences, decoupled from follows (D-04 revised).
 *
 * A user can enable notifications for an MP without following them, and
 * choose which notification types to receive (votes, speeches, etc.).
 *
 * The "notifications enabled" master toggle is derived: it's ON when any
 * type checkbox is ON, and OFF when all are OFF.
 *
 * Used by the NotificationSettingsBottomSheet (profile header bell icon)
 * and the VotePollingWorker (queries MPs with votesEnabled = true).
 */
@Serializable
@Entity(tableName = "mp_notification_prefs")
data class MpNotificationPreferenceEntity(
    @PrimaryKey val memberId: Int,
    val votesEnabled: Boolean = false,
    val speechesEnabled: Boolean = false,
    val incomeEnabled: Boolean = false,
    val expensesEnabled: Boolean = false
) {
    /** Master toggle — true when any notification type is enabled. */
    val notificationsEnabled: Boolean
        get() = votesEnabled || speechesEnabled || incomeEnabled || expensesEnabled
}
