package com.goveye.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.goveye.app.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GovEyeApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        com.goveye.app.work.WorkScheduler.scheduleVotePolling(this)
    }
}
