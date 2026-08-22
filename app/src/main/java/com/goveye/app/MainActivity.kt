package com.goveye.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.GovEyeApp
import com.goveye.app.ui.navigation.DeepLinkHandler
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.screens.DatabaseLoadingScreen
import com.goveye.app.ui.theme.GovEyeTheme
import com.goveye.app.ui.theme.ThemeViewModel
import com.goveye.app.work.DatabaseDownloadWorker
import com.goveye.app.work.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deepLinkNavigator: DeepLinkNavigator

    @Inject
    lateinit var databaseUpdateManager: DatabaseUpdateManager

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
                    val isFirstLaunch = databaseUpdateManager.isFirstLaunch()
                    if (isFirstLaunch) {
                        Log.i(TAG, "First launch — enqueuing DatabaseDownloadWorker")
                        dbState = DatabaseUpdateState.Downloading(0f, true)

                        // Enqueue the foreground-service worker. KEEP policy
                        // means if the work is already running (e.g. Activity
                        // recreated after minimization), the in-progress
                        // download is not cancelled.
                        WorkScheduler.enqueueDatabaseDownload(this@MainActivity)

                        // Observe the worker's progress until it reaches a
                        // terminal state. The download runs in a foreground
                        // service, so it continues even if the app is
                        // minimized — this observation just picks up the
                        // current state when the UI is visible.
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
                                    Log.i(TAG, "Download worker succeeded")
                                    dbState = DatabaseUpdateState.UpToDate
                                }

                                WorkInfo.State.FAILED -> {
                                    val reason = workInfo.outputData.getString("reason")
                                    Log.w(TAG, "Download worker failed: reason=$reason")
                                    dbState = when (reason) {
                                        "needs_wifi" -> DatabaseUpdateState.NeedsWifi

                                        else -> DatabaseUpdateState.Failed(
                                            workInfo.outputData.getString("message")
                                                ?: "Download failed"
                                        )
                                    }
                                }

                                WorkInfo.State.CANCELLED -> {
                                    dbState = DatabaseUpdateState.Failed("Download cancelled")
                                }

                                else -> { /* BLOCKED — ignore */ }
                            }
                        }
                    } else {
                        // Subsequent launch — check for updates silently in the
                        // background. The app is already visible (UpToDate), so
                        // the user doesn't see any loading screen.
                        val updateState = databaseUpdateManager.checkForUpdates()
                        when (updateState) {
                            is DatabaseUpdateState.NeedsPatches -> {
                                val patches = (updateState as DatabaseUpdateState.NeedsPatches).patches
                                databaseUpdateManager.applyPatches(patches)
                            }

                            is DatabaseUpdateState.NeedsFullDownload -> {
                                Log.i(TAG, "Full download needed (stream multiple behind)")
                                WorkScheduler.enqueueDatabaseDownload(this@MainActivity)
                            }

                            else -> { /* UpToDate or Failed — nothing to do */ }
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
                    is DatabaseUpdateState.Failed -> {
                        DatabaseLoadingScreen(
                            state = dbState,
                            onRetry = {
                                dbState = DatabaseUpdateState.Checking
                                retryCount++
                            }
                        )
                    }

                    else -> {
                        // UpToDate or Idle — show the app
                        GovEyeApp(deepLinkNavigator = deepLinkNavigator)
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
