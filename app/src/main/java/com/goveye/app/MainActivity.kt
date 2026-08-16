package com.goveye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.goveye.app.ui.GovEyeApp
import com.goveye.app.ui.navigation.DeepLinkHandler
import com.goveye.app.ui.navigation.DeepLinkNavigator
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
        enableEdgeToEdge()
        setContent {
            GovEyeApp(deepLinkNavigator = deepLinkNavigator)
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
