package com.goveye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.goveye.app.ui.GovEyeApp
import com.goveye.app.ui.navigation.DeepLinkHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Volatile
    private var splashVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate (D-04)
        val splashScreen = installSplashScreen()
        // Hold the splash briefly so it's perceptible on fast cold starts
        splashScreen.setKeepOnScreenCondition { splashVisible }
        MainScope().launch(Dispatchers.Main) {
            delay(600)
            splashVisible = false
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GovEyeApp()
        }

        // Handle deep link from initial intent (D-21)
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent) {
        DeepLinkHandler.parseDeepLink(intent) // Phase 1: logs the route
    }
}
