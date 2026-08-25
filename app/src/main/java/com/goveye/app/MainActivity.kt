package com.goveye.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.goveye.app.data.preference.DownloadPreferences
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.GovEyeApp
import com.goveye.app.ui.navigation.DeepLinkHandler
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.screens.DatabaseLoadingScreen
import com.goveye.app.ui.screens.OnboardingScreen
import com.goveye.app.ui.theme.GovEyeTheme
import com.goveye.app.ui.theme.ThemeViewModel
import com.goveye.app.work.DatabaseDownloadWorker
import com.goveye.app.work.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deepLinkNavigator: DeepLinkNavigator

    @Inject
    lateinit var databaseUpdateManager: DatabaseUpdateManager

    @Inject
    lateinit var downloadPreferences: DownloadPreferences

    @Inject
    lateinit var onboardingPreferences: com.goveye.app.data.preference.OnboardingPreferences

    @Volatile
    private var splashVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { splashVisible }
        MainScope().launch(Dispatchers.Main) {
            delay(600)
            splashVisible = false
        }

        super.onCreate(savedInstanceState)

        // Initial edge-to-edge — will be refined inside setContent with proper styles
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()

            // Permission launcher for POST_NOTIFICATIONS (Android 13+).
            // Used on first launch so the download foreground notification
            // is visible to the user.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                // Result is ignored — download proceeds regardless.
                // The notification just won't show if denied.
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            // Status bar: use isDark directly to pick icon style.
            // darkStyle = light (white) icons on transparent background
            // lightStyle = dark icons on transparent background
            LaunchedEffect(isDark) {
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) darkStyle else lightStyle,
                    navigationBarStyle = if (isDark) darkStyle else lightStyle
                )
            }

            // Wrap everything in GovEyeTheme so both DatabaseLoadingScreen
            // and GovEyeApp use the correct colors.
            GovEyeTheme(
                appTheme = appTheme,
                themeMode = themeMode,
                isAmoled = isAmoled
            ) {
                // Onboarding state — shown before the download flow on
                // first launch. Wakely-style fade transitions between
                // Welcome and Government selection steps.
                var showOnboarding by remember { mutableStateOf(false) }
                var onboardingTestMode by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val completed = onboardingPreferences.onboardingCompleted.first()
                    if (!completed) {
                        showOnboarding = true
                    }
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        testMode = onboardingTestMode,
                        onGovernmentSelected = {
                            // Start the download + request notification permission
                            // immediately when the user picks their country (step 1),
                            // so the seed DB downloads in the background while the
                            // user completes the remaining onboarding steps (tags,
                            // sources, parties, MPs).
                            MainScope().launch {
                                onboardingPreferences.setSelectedGovernment("UK")

                                // Request POST_NOTIFICATIONS on Android 13+ so the
                                // download foreground notification is visible.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }

                                // Enqueue the download worker immediately
                                val wifiOnly = downloadPreferences.wifiOnly.first()
                                WorkScheduler.enqueueDatabaseDownload(
                                    this@MainActivity,
                                    wifiOnly = wifiOnly
                                )
                                Log.i(TAG, "Download started during onboarding (government selected)")
                            }
                        },
                        onComplete = { selectedGov ->
                            if (!onboardingTestMode) {
                                // Onboarding complete — mark completed. The
                                // download was already started when the user
                                // selected their government (onGovernmentSelected).
                                MainScope().launch {
                                    onboardingPreferences.setOnboardingCompleted(true)
                                }
                            }
                            showOnboarding = false
                            onboardingTestMode = false
                        }
                    )
                    return@GovEyeTheme
                }

                // Database update state — drives whether to show the loading screen
                // or the main app (D-04, D-05, D-10a, DATA-03).
                // On first launch (no DB file yet), start with Checking to show
                // the loading screen. On subsequent launches, start with UpToDate
                // so the app shows immediately — update checks happen in the
                // background without any loading screen.
                val dbFileExists = remember {
                    databaseUpdateManager.databaseFileExists()
                }
                var dbState by remember {
                    mutableStateOf<DatabaseUpdateState>(
                        if (dbFileExists) {
                            DatabaseUpdateState.UpToDate
                        } else {
                            DatabaseUpdateState.Checking
                        }
                    )
                }
                var retryCount by remember { mutableIntStateOf(0) }

                LaunchedEffect(retryCount) {
                    // Wait for a government to be selected before doing anything.
                    // The download is started in onGovernmentSelected (during
                    // onboarding step 1), not here. This LaunchedEffect observes
                    // the worker progress and updates dbState accordingly.
                    val selectedGov = onboardingPreferences.selectedGovernment
                        .first { it != null }
                    if (selectedGov == null) {
                        Log.i(TAG, "No government selected — not starting download")
                        return@LaunchedEffect
                    }

                    val isFirstLaunch = databaseUpdateManager.isFirstLaunch()
                    if (isFirstLaunch) {
                        Log.i(TAG, "First launch (government=$selectedGov) — observing download worker")
                        dbState = DatabaseUpdateState.Downloading(0f, true)

                        // Re-enqueue the download worker. On a true first
                        // launch, the worker was already enqueued in
                        // onGovernmentSelected and KEEP policy prevents
                        // re-enqueueing. On a re-download (seed version
                        // bump), the previous worker is in SUCCEEDED state,
                        // so KEEP enqueues a fresh one.
                        val wifiOnly = downloadPreferences.wifiOnly.first()
                        WorkScheduler.enqueueDatabaseDownload(
                            this@MainActivity,
                            wifiOnly = wifiOnly
                        )

                        // Observe the worker's progress until it reaches a
                        // terminal state. The download was already enqueued
                        // in onGovernmentSelected — this just picks up the
                        // state for the UI.
                        val workInfoFlow = WorkManager.getInstance(this@MainActivity)
                            .getWorkInfosForUniqueWorkFlow(DatabaseDownloadWorker.WORK_NAME)

                        workInfoFlow.collect { workInfos ->
                            val workInfo = workInfos.firstOrNull() ?: return@collect
                            when (workInfo.state) {
                                WorkInfo.State.ENQUEUED -> {
                                    dbState = DatabaseUpdateState.Downloading(0f, true)
                                }

                                WorkInfo.State.RUNNING -> {
                                    val progress = workInfo.progress
                                        .getFloat(DatabaseDownloadWorker.KEY_PROGRESS, 0f)
                                    dbState = DatabaseUpdateState.Downloading(progress, true)
                                }

                                WorkInfo.State.SUCCEEDED -> {
                                    Log.i(TAG, "Download worker succeeded — needs restart")
                                    dbState = DatabaseUpdateState.NeedsRestart
                                }

                                WorkInfo.State.FAILED -> {
                                    val reason = workInfo.outputData.getString("reason")
                                    Log.w(TAG, "Download worker failed: reason=$reason")
                                    dbState = when (reason) {
                                        "needs_wifi" -> DatabaseUpdateState.NeedsWifi

                                        "cancelled" -> DatabaseUpdateState.Canceled

                                        else -> DatabaseUpdateState.Failed(
                                            workInfo.outputData.getString("message")
                                                ?: "Download failed"
                                        )
                                    }
                                }

                                WorkInfo.State.CANCELLED -> {
                                    dbState = DatabaseUpdateState.Canceled
                                }

                                else -> { /* BLOCKED — ignore */ }
                            }
                        }
                    } else {
                        // Subsequent launch — check for updates silently in the
                        // background. The app is already visible (UpToDate), so
                        // the user doesn't see any loading screen.
                        val updateState = databaseUpdateManager.checkForUpdates()
                        Log.i(TAG, "Update check result: $updateState")
                        when (updateState) {
                            is DatabaseUpdateState.NeedsPatches -> {
                                val patches = (updateState as DatabaseUpdateState.NeedsPatches).patches
                                Log.i(
                                    TAG,
                                    "Applying ${patches.size} patches: ${patches.joinToString {
                                        it.streamName
                                    }}"
                                )
                                val result = databaseUpdateManager.applyPatches(patches)
                                Log.i(TAG, "Patch application result: $result")
                            }

                            is DatabaseUpdateState.NeedsFullDownload -> {
                                Log.i(TAG, "Full download needed (stream multiple behind)")
                                val wifiOnly = downloadPreferences.wifiOnly.first()
                                WorkScheduler.enqueueDatabaseDownload(this@MainActivity, wifiOnly = wifiOnly)
                            }

                            is DatabaseUpdateState.Failed -> {
                                Log.w(TAG, "Update check failed: ${updateState.message}")
                            }

                            else -> { /* UpToDate — nothing to do */ }
                        }
                    }
                }

                // Show loading screen only on first launch (download/apply states).
                // On subsequent launches, dbState starts as UpToDate so the app
                // shows immediately. Failed state shows the loading screen with
                // error/retry so the user sees the error instead of an empty app.
                when (dbState) {
                    is DatabaseUpdateState.NeedsFullDownload,
                    is DatabaseUpdateState.Downloading,
                    is DatabaseUpdateState.Applying,
                    is DatabaseUpdateState.NeedsWifi,
                    is DatabaseUpdateState.NeedsPatches,
                    is DatabaseUpdateState.Checking,
                    is DatabaseUpdateState.Failed,
                    is DatabaseUpdateState.NeedsRestart,
                    is DatabaseUpdateState.Canceled -> {
                        DatabaseLoadingScreen(
                            state = dbState,
                            onRetry = {
                                dbState = DatabaseUpdateState.Checking
                                retryCount++
                            },
                            onRestart = {
                                // Kill the app process so the user gets a
                                // fresh start with clean Room connections.
                                // The InvalidationTracker was broken by
                                // database.close() during the download —
                                // only a process restart fixes it.
                                // The user taps the app icon to relaunch.
                                android.os.Process.killProcess(
                                    android.os.Process.myPid()
                                )
                            },
                            onCancel = {
                                // Cancel the WorkManager download worker.
                                // The worker's coroutine will throw
                                // CancellationException, clean up the temp
                                // file, and return Result.failure with
                                // reason="cancelled". The WorkInfo observer
                                // above will set dbState = Canceled.
                                WorkManager.getInstance(this@MainActivity)
                                    .cancelUniqueWork(DatabaseDownloadWorker.WORK_NAME)
                            },
                            onRedownload = {
                                // Re-enqueue the download worker. The previous
                                // work is in FAILED state, so KEEP policy
                                // enqueues a fresh one.
                                retryCount++
                            }
                        )
                    }

                    else -> {
                        // UpToDate or Idle — show the app
                        GovEyeApp(
                            deepLinkNavigator = deepLinkNavigator,
                            onTestOnboarding = {
                                showOnboarding = true
                                onboardingTestMode = true
                            }
                        )
                    }
                }
            }
        }

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent) {
        val route = DeepLinkHandler.parseDeepLink(intent)
        if (route != null) {
            MainScope().launch {
                deepLinkNavigator.emit(route)
            }
        }
    }

    companion object {
        private const val TAG = "GovEye/MainActivity"
    }
}
