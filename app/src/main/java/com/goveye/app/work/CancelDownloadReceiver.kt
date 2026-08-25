package com.goveye.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * BroadcastReceiver that cancels the seed download worker when the user
 * taps the "Cancel" action button on the download notification.
 *
 * Registered in AndroidManifest.xml with the action
 * `com.goveye.app.ACTION_CANCEL_DOWNLOAD`. The notification's cancel
 * action uses a [PendingIntent]getBroadcast with this action.
 *
 * This is an instant cancel — no confirmation dialog. The confirmation
 * dialog is only shown in the in-app [DatabaseLoadingScreen].
 */
class CancelDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_DOWNLOAD) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(DatabaseDownloadWorker.WORK_NAME)
        }
    }

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.goveye.app.ACTION_CANCEL_DOWNLOAD"
    }
}
