package com.goveye.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.GovEyeApp
import com.goveye.app.ui.navigation.DeepLinkHandler
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.screens.DatabaseLoadingScreen
import com.goveye.app.ui.theme.ThemeViewModel
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
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            // Port Miko's approach: set transparent system bars with proper
            // light/dark icon styles based on the current theme's surface luminance.
            // This makes the status bar transparent so content draws behind it,
            // with icons that are visible against the background.
            val surfaceColor = MaterialTheme.colorScheme.surface
            LaunchedEffect(isDark, surfaceColor) {
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (surfaceColor.luminance() > 0.5f) lightStyle else darkStyle,
                    navigationBarStyle = if (isDark) darkStyle else lightStyle
                )
            }

            // Database update state — drives whether to show the loading screen
            // or the main app (D-04, D-05, D-10a, DATA-03).
            var dbState by remember { mutableStateOf<DatabaseUpdateState>(DatabaseUpdateState.Idle) }

            LaunchedEffect(Unit) {
                if (databaseUpdateManager.isFirstLaunch()) {
                    // First launch — download all 7 per-API DBs and merge on-device
                    dbState = DatabaseUpdateState.NeedsFullDownload(null)
                    dbState = DatabaseUpdateState.Downloading(0f, true)
                    val downloadResult = databaseUpdateManager.downloadAndMergePerApiDbs { progress ->
                        dbState = DatabaseUpdateState.Downloading(progress, true)
                    }
                    dbState = downloadResult
                } else {
                    // Subsequent launch — check for updates silently
                    dbState = databaseUpdateManager.checkForUpdates()
                    when (dbState) {
                        is DatabaseUpdateState.NeedsPatches -> {
                            val patches = (dbState as DatabaseUpdateState.NeedsPatches).patches
                            dbState = DatabaseUpdateState.Applying
                            dbState = databaseUpdateManager.applyPatches(patches)
                        }

                        is DatabaseUpdateState.NeedsFullDownload -> {
                            // Full DB download needed (stream multiple behind) — show loading screen
                            dbState = DatabaseUpdateState.Downloading(0f, true)
                            dbState = databaseUpdateManager.downloadAndMergePerApiDbs { progress ->
                                dbState = DatabaseUpdateState.Downloading(progress, true)
                            }
                        }

                        else -> { /* UpToDate or Failed — proceed to app */ }
                    }
                }
            }

            // Show loading screen during download/apply, otherwise show the app
            when (dbState) {
                is DatabaseUpdateState.NeedsFullDownload,
                is DatabaseUpdateState.Downloading,
                is DatabaseUpdateState.Applying,
                is DatabaseUpdateState.NeedsWifi,
                is DatabaseUpdateState.NeedsPatches,
                is DatabaseUpdateState.Checking -> {
                    DatabaseLoadingScreen(state = dbState)
                }

                else -> {
                    // UpToDate, Failed, or Idle — show the app
                    GovEyeApp(deepLinkNavigator = deepLinkNavigator)
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
}
