package com.goveye.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.goveye.app.data.preference.DownloadPreferences
import com.goveye.app.notifications.NotificationHelper
import com.goveye.app.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class GovEyeApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var downloadPreferences: DownloadPreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()

        // Read the WiFi-only preference synchronously so the periodic
        // update worker is scheduled with the correct network constraint
        // from the very first launch.
        val wifiOnly = runBlocking { downloadPreferences.wifiOnly.first() }
        WorkScheduler.scheduleDatabaseUpdateCheck(this, wifiOnly = wifiOnly)
    }
}
