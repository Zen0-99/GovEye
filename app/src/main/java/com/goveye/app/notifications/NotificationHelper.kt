package com.goveye.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.goveye.app.R
import com.goveye.app.ui.navigation.DivisionDetailRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels and dispatches vote notifications (D-02).
 *
 * FotMob-style: includes the MP's thumbnail as the large icon, vote direction,
 * division title, and rebellion context. Tap opens the division detail screen.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val VOTES_CHANNEL_ID = "votes"
        const val SPEECHES_CHANNEL_ID = "speeches"
        const val BILLS_CHANNEL_ID = "bills"
        const val VOTES_CHANNEL_NAME = "Vote Notifications"
        const val SPEECHES_CHANNEL_NAME = "Speech Notifications"
        const val BILLS_CHANNEL_NAME = "Bill Notifications"
        const val VOTES_CHANNEL_DESC = "Notifications when followed MPs vote"
        const val SPEECHES_CHANNEL_DESC = "Notifications when followed MPs speak (coming soon)"
        const val BILLS_CHANNEL_DESC = "Notifications when followed bills change stage"

        const val EXTRA_DIVISION_ID = "division_id"
        const val EXTRA_DIVISION_HOUSE = "division_house"
        const val EXTRA_MP_NAME = "mp_name"
        const val EXTRA_BILL_ID = "bill_id"

        private var nextNotificationId = 1000
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Create notification channels. Called once in GovEyeApplication.onCreate. */
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val votesChannel = NotificationChannel(
                VOTES_CHANNEL_ID,
                VOTES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = VOTES_CHANNEL_DESC
                enableVibration(true)
            }
            val speechesChannel = NotificationChannel(
                SPEECHES_CHANNEL_ID,
                SPEECHES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = SPEECHES_CHANNEL_DESC
                setShowBadge(false)
            }
            val billsChannel = NotificationChannel(
                BILLS_CHANNEL_ID,
                BILLS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = BILLS_CHANNEL_DESC
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(votesChannel)
            nm.createNotificationChannel(speechesChannel)
            nm.createNotificationChannel(billsChannel)
        }
    }

    data class VoteNotificationData(
        val mpName: String,
        val mpThumbnailUrl: String?,
        val divisionId: Int,
        val divisionHouse: Int,
        val divisionTitle: String,
        val voteLabel: String,
        val isRebel: Boolean,
    )

    suspend fun showVoteNotification(data: VoteNotificationData) {
        val bitmap = loadThumbnail(data.mpThumbnailUrl)
        val body = buildString {
            append("Voted ")
            append(data.voteLabel)
            append(" on ")
            append(data.divisionTitle)
            if (data.isRebel) {
                append(" — rebel")
            }
        }

        val pendingIntent = buildDivisionPendingIntent(
            data.divisionId,
            data.divisionHouse,
            data.mpName,
        )

        val builder = NotificationCompat.Builder(context, VOTES_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(data.mpName)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        try {
            notificationManager.notify(nextNotificationId++, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    fun showSummaryNotification(newVoteCount: Int) {
        val pendingIntent = buildDivisionPendingIntent(0, 1, "")
        val body = "$newVoteCount new votes from followed MPs"

        val notification = NotificationCompat.Builder(context, VOTES_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GovEye")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            notificationManager.notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    private suspend fun loadThumbnail(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildDivisionPendingIntent(
        divisionId: Int,
        house: Int,
        mpName: String,
    ): PendingIntent {
        val intent = Intent(context, NotificationDeepLinkActivity::class.java).apply {
            putExtra(EXTRA_DIVISION_ID, divisionId)
            putExtra(EXTRA_DIVISION_HOUSE, house)
            putExtra(EXTRA_MP_NAME, mpName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = (divisionId * 100 + house).coerceAtMost(Int.MAX_VALUE)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    data class BillNotificationData(
        val billId: Int,
        val billTitle: String,
        val newStage: String,
    )

    fun showBillStageNotification(data: BillNotificationData) {
        val body = "Moved to ${data.newStage}"
        val pendingIntent = buildBillPendingIntent(data.billId)

        val notification = NotificationCompat.Builder(context, BILLS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(data.billTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    fun showBillSummaryNotification(count: Int) {
        val body = "$count followed bills changed stage"
        val pendingIntent = buildBillPendingIntent(0)

        val notification = NotificationCompat.Builder(context, BILLS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GovEye")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    private fun buildBillPendingIntent(billId: Int): PendingIntent {
        val intent = Intent(context, NotificationDeepLinkActivity::class.java).apply {
            putExtra(EXTRA_BILL_ID, billId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            billId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
