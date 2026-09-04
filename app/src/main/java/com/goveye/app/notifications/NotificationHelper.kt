package com.goveye.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        const val VOTES_CHANNEL_ID = "votes"
        const val SPEECHES_CHANNEL_ID = "speeches"
        const val BILLS_CHANNEL_ID = "bills"
        const val INCOME_CHANNEL_ID = "income"
        const val EXPENSE_CHANNEL_ID = "expenses"
        const val DOWNLOAD_CHANNEL_ID = "download"
        const val UPDATE_CHANNEL_ID = "updates"
        const val VOTES_CHANNEL_NAME = "Vote Notifications"
        const val SPEECHES_CHANNEL_NAME = "Speech Notifications"
        const val BILLS_CHANNEL_NAME = "Bill Notifications"
        const val INCOME_CHANNEL_NAME = "Income Notifications"
        const val EXPENSE_CHANNEL_NAME = "Expense Notifications"
        const val DOWNLOAD_CHANNEL_NAME = "Data Download"
        const val UPDATE_CHANNEL_NAME = "Data Updates"
        const val VOTES_CHANNEL_DESC = "Notifications when followed MPs vote"
        const val SPEECHES_CHANNEL_DESC = "Notifications when followed MPs speak (coming soon)"
        const val BILLS_CHANNEL_DESC = "Notifications when followed bills change stage"
        const val INCOME_CHANNEL_DESC = "Notifications when followed MPs declare new income"
        const val EXPENSE_CHANNEL_DESC = "Notifications when followed MPs submit expense claims"
        const val DOWNLOAD_CHANNEL_DESC = "Foreground service notification during database download"
        const val UPDATE_CHANNEL_DESC = "Notifications when parliamentary data has been updated"

        const val EXTRA_DIVISION_ID = "division_id"
        const val EXTRA_DIVISION_HOUSE = "division_house"
        const val EXTRA_MP_NAME = "mp_name"
        const val EXTRA_BILL_ID = "bill_id"

        /** Notification ID for the update worker foreground notification. */
        const val UPDATE_FOREGROUND_NOTIFICATION_ID = 1004

        /** Notification ID for update announcement notifications. */
        const val UPDATE_ANNOUNCEMENT_NOTIFICATION_ID = 1005

        private var nextNotificationId = 1000
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Create notification channels. Called once in GovEyeApplication.onCreate. */
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val votesChannel = NotificationChannel(
                VOTES_CHANNEL_ID,
                VOTES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = VOTES_CHANNEL_DESC
                enableVibration(true)
            }
            val speechesChannel = NotificationChannel(
                SPEECHES_CHANNEL_ID,
                SPEECHES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SPEECHES_CHANNEL_DESC
                setShowBadge(false)
            }
            val billsChannel = NotificationChannel(
                BILLS_CHANNEL_ID,
                BILLS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = BILLS_CHANNEL_DESC
                enableVibration(true)
            }
            val incomeChannel = NotificationChannel(
                INCOME_CHANNEL_ID,
                INCOME_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = INCOME_CHANNEL_DESC
            }
            val expenseChannel = NotificationChannel(
                EXPENSE_CHANNEL_ID,
                EXPENSE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = EXPENSE_CHANNEL_DESC
            }
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = DOWNLOAD_CHANNEL_DESC
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                UPDATE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = UPDATE_CHANNEL_DESC
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(votesChannel)
            nm.createNotificationChannel(speechesChannel)
            nm.createNotificationChannel(billsChannel)
            nm.createNotificationChannel(incomeChannel)
            nm.createNotificationChannel(expenseChannel)
            nm.createNotificationChannel(downloadChannel)
            nm.createNotificationChannel(updateChannel)
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
        val partyColor: Int? = null
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
            data.mpName
        )

        val notification = buildNotification(
            channelId = VOTES_CHANNEL_ID,
            title = data.mpName,
            body = body,
            pendingIntent = pendingIntent,
            priority = NotificationCompat.PRIORITY_HIGH,
            mpBitmap = bitmap,
            partyColor = data.partyColor
        )

        try {
            notificationManager.notify(nextNotificationId++, notification)
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

    /**
     * Shows a notification when a followed MP declares new income (issue #8).
     *
     * @param memberId The MP's member ID (used for the deep-link request code).
     * @param mpName The MP's display name (notification title).
     * @param amount The income amount (e.g. "£5,000").
     * @param source The payer or source of the income (notification body).
     * @param mpThumbnailUrl The MP's profile image URL (shown as large icon).
     * @param partyColor The MP's party color (ARGB int, shown as notification accent).
     */
    suspend fun showIncomeNotification(
        memberId: Int,
        mpName: String,
        amount: String,
        source: String,
        mpThumbnailUrl: String? = null,
        partyColor: Int? = null
    ) {
        val bitmap = loadThumbnail(mpThumbnailUrl)
        val body = "New income: $amount from $source"

        val notification = buildNotification(
            channelId = INCOME_CHANNEL_ID,
            title = mpName,
            body = body,
            pendingIntent = null,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            mpBitmap = bitmap,
            partyColor = partyColor
        )

        try {
            notificationManager.notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    /**
     * Shows a mock speech notification for testing (D-04 testing).
     */
    suspend fun showSpeechNotification(
        mpName: String,
        debateTitle: String,
        mpThumbnailUrl: String? = null,
        partyColor: Int? = null
    ) {
        val bitmap = loadThumbnail(mpThumbnailUrl)
        val body = "Spoke in $debateTitle"

        val notification = buildNotification(
            channelId = SPEECHES_CHANNEL_ID,
            title = mpName,
            body = body,
            pendingIntent = null,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            mpBitmap = bitmap,
            partyColor = partyColor
        )

        try {
            notificationManager.notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    /**
     * Shows a notification when a followed MP submits an expense claim (issue #8).
     *
     * @param memberId The MP's member ID (used for the deep-link request code).
     * @param mpName The MP's display name (notification title).
     * @param amount The expense amount (e.g. "£1,200").
     * @param category The expense category/bucket (notification body).
     * @param mpThumbnailUrl The MP's profile image URL (shown as large icon).
     * @param partyColor The MP's party color (ARGB int, shown as notification accent).
     */
    suspend fun showExpenseNotification(
        memberId: Int,
        mpName: String,
        amount: String,
        category: String,
        mpThumbnailUrl: String? = null,
        partyColor: Int? = null
    ) {
        val bitmap = loadThumbnail(mpThumbnailUrl)
        val body = "New expense: $amount — $category"

        val notification = buildNotification(
            channelId = EXPENSE_CHANNEL_ID,
            title = mpName,
            body = body,
            pendingIntent = null,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            mpBitmap = bitmap,
            partyColor = partyColor
        )

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
                // Force ARGB_8888 (software bitmap) — hardware bitmaps can't be
                // drawn onto a Canvas for compositing (party ring rendering).
                val hwBitmap = result.image.toBitmap()
                if (hwBitmap.config == Bitmap.Config.HARDWARE) {
                    hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    hwBitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Render a square bitmap as a circular avatar with a party-colored ring,
     * matching the directory's [MpAvatar] border style.
     *
     * The output is a square bitmap of [size]×[size] pixels containing:
     *   1. A party-colored ring of [borderPx] thickness around the edge
     *   2. The MP's photo clipped to a circle inside the ring
     *
     * @param src The source MP thumbnail bitmap
     * @param partyColor ARGB int for the ring color (null = no ring)
     * @param size Output bitmap dimension in px (default 128)
     * @param borderPx Ring thickness in px (default 8)
     */
    private fun renderCircularBitmapWithBorder(
        src: Bitmap,
        partyColor: Int?,
        size: Int = 128,
        borderPx: Int = 4
    ): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Draw party color ring (full circle, then photo on top leaves ring visible)
        if (partyColor != null) {
            paint.color = partyColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        }

        // 2. Draw the photo clipped to a circle inside the ring
        val innerRadius = (size / 2f) - borderPx
        val innerLeft = (size / 2f) - innerRadius
        val innerTop = (size / 2f) - innerRadius
        val innerDiameter = innerRadius * 2
        val innerRect = Rect(
            innerLeft.toInt(),
            innerTop.toInt(),
            (innerLeft + innerDiameter).toInt(),
            (innerTop + innerDiameter).toInt()
        )

        paint.reset()
        paint.isAntiAlias = true
        canvas.save()
        canvas.clipPath(
            android.graphics.Path().apply {
                addCircle(size / 2f, size / 2f, innerRadius, android.graphics.Path.Direction.CW)
            }
        )
        // Scale source bitmap to fit the inner circle
        val srcRect = Rect(0, 0, src.width, src.height)
        canvas.drawBitmap(src, srcRect, innerRect, paint)
        canvas.restore()

        return output
    }

    /**
     * Build a notification showing the MP's face as a circular avatar.
     *
     * Uses [NotificationCompat.MessagingStyle] with a [Person] whose icon is
     * the MP's thumbnail bitmap. This is the same approach FotMob and messaging
     * apps use — the person's avatar appears as a circular image in the
     * notification header, replacing the app icon position.
     *
     * The small icon stays as the app icon (required by Android to be a
     * monochrome resource). The party color is applied via [setColor].
     *
     * @param channelId Notification channel ID
     * @param title Notification title (MP name)
     * @param body Notification body text
     * @param pendingIntent Tap intent (null = no tap action)
     * @param priority Notification priority
     * @param mpBitmap MP's thumbnail bitmap (shown as person avatar)
     * @param partyColor Party color ARGB int (notification accent)
     */
    @android.annotation.SuppressLint("NotificationPermission")
    private fun buildNotification(
        channelId: String,
        title: String,
        body: String,
        pendingIntent: PendingIntent?,
        priority: Int,
        mpBitmap: Bitmap?,
        partyColor: Int?
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(context, channelId)
            .setAutoCancel(true)
            .setPriority(priority)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        if (partyColor != null) {
            builder.setColor(partyColor)
        }

        if (mpBitmap != null) {
            // FotMob-style: MP's face as a circular avatar with party-colored
            // ring on the right side of the notification (setLargeIcon).
            // The app icon stays as the small icon in the header — this is
            // the standard Android layout, same as FotMob.
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            val avatarBitmap = renderCircularBitmapWithBorder(mpBitmap, partyColor)
            builder.setLargeIcon(avatarBitmap)
        } else {
            // Fallback: no bitmap — use BigTextStyle with the MP name as title
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        return builder.build()
    }

    private fun buildDivisionPendingIntent(divisionId: Int, house: Int, mpName: String): PendingIntent {
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
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    data class BillNotificationData(val billId: Int, val billTitle: String, val newStage: String)

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
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Shows a notification announcing that parliamentary data has been
     * updated. Adapted from Miko's [ExtensionUpdateNotifier.promptUpdates]
     * pattern — informs the user that fresh data (votes, bills, etc.) has
     * been applied in the background.
     *
     * Uses the [UPDATE_CHANNEL_ID] (IMPORTANCE_LOW, silent) so it doesn't
     * disrupt the user. Auto-cancel on tap.
     *
     * @param streamNames The data streams that were updated (e.g. "commons-votes", "bills").
     */
    fun showDataUpdatedNotification(streamNames: List<String>) {
        val count = streamNames.size
        val title = if (count == 1) {
            "GovEye data updated"
        } else {
            "GovEye data updated"
        }
        val body = streamNames.joinToString(", ")

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(UPDATE_ANNOUNCEMENT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    /**
     * Shows a notification announcing that a major data update is available
     * and the user needs to open the app to download it. This is used when
     * the periodic update worker detects that a full re-download is needed
     * (e.g. a stream is multiple versions behind) but can't perform it in
     * the background.
     *
     * Uses the [UPDATE_CHANNEL_ID] (IMPORTANCE_LOW, silent). Auto-cancel on tap.
     */
    fun showMajorUpdateAvailableNotification() {
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Update available")
            .setContentText("New parliamentary data is available. Open GovEye to update.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(UPDATE_ANNOUNCEMENT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    /**
     * Builds a foreground notification for the update worker (Stonesync
     * pattern). Used by [com.goveye.app.work.DatabaseUpdateWorker] when
     * applying patches in the background.
     *
     * @param contentText The notification body text (e.g. "Applying updates…").
     * @param indeterminate When true, shows an indeterminate progress spinner.
     */
    fun buildUpdateForegroundNotification(
        contentText: String,
        indeterminate: Boolean = false
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("GovEye")
        .setContentText(contentText)
        .setProgress(0, 0, indeterminate)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
}
