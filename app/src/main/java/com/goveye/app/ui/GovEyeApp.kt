package com.goveye.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.goveye.app.domain.AppTheme
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.navigation.DirectoryRoute
import com.goveye.app.ui.navigation.FeedRoute
import com.goveye.app.ui.navigation.FollowingRoute
import com.goveye.app.ui.navigation.SettingsRoute
import com.goveye.app.ui.screens.DirectoryScreen
import com.goveye.app.ui.screens.FeedScreen
import com.goveye.app.ui.screens.FollowingScreen
import com.goveye.app.ui.screens.SettingsScreen
import com.goveye.app.ui.theme.GovEyeTheme
import com.goveye.app.ui.theme.ThemeViewModel

/**
 * Root composable for the GovEye app shell (D-14, D-20).
 *
 * Hosts the M3 theme wrapper, bottom navigation bar with 4 tabs, and
 * Nav3 [NavDisplay] with per-tab back stack preservation.
 *
 * Theme state is collected from [ThemeViewModel] (scoped to the Activity)
 * and passed to [GovEyeTheme]. The same ViewModel instance is shared with
 * [SettingsScreen] so theme changes propagate live.
 */
@Composable
fun GovEyeApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()

    GovEyeTheme(
        appTheme = appTheme,
        themeMode = themeMode,
        isAmoled = isAmoled
    ) {
        GovEyeAppContent(themeViewModel)
    }
}

@Composable
private fun GovEyeAppContent(themeViewModel: ThemeViewModel) {
    // Per-tab back stacks (D-20)
    val feedBackStack = rememberNavBackStack(FeedRoute)
    val directoryBackStack = rememberNavBackStack(DirectoryRoute)
    val followingBackStack = rememberNavBackStack(FollowingRoute)
    val settingsBackStack = rememberNavBackStack(SettingsRoute)

    // Current tab — Feed is default (D-15)
    var currentTab by rememberSaveable { mutableStateOf<NavKey>(FeedRoute) }

    val currentBackStack =
        when (currentTab) {
            FeedRoute -> feedBackStack
            DirectoryRoute -> directoryBackStack
            FollowingRoute -> followingBackStack
            SettingsRoute -> settingsBackStack
            else -> feedBackStack
        }

    val decorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>()
        )

    val entries =
        rememberDecoratedNavEntries(currentBackStack, decorators) { key ->
            when (key) {
                is FeedRoute ->
                    NavEntry(key) {
                        FeedScreen(modifier = Modifier.fillMaxSize())
                    }

                is DirectoryRoute ->
                    NavEntry(key) {
                        DirectoryScreen(modifier = Modifier.fillMaxSize())
                    }

                is FollowingRoute ->
                    NavEntry(key) {
                        FollowingScreen(modifier = Modifier.fillMaxSize())
                    }

                is SettingsRoute ->
                    NavEntry(key) {
                        SettingsScreen(
                            themeViewModel = themeViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                else -> NavEntry(key) { Text("Unknown") }
            }
        }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == FeedRoute,
                    onClick = { currentTab = FeedRoute },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = currentTab == DirectoryRoute,
                    onClick = { currentTab = DirectoryRoute },
                    icon = { Icon(Icons.Outlined.Search, contentDescription = "Directory") },
                    label = { Text("Directory") }
                )
                NavigationBarItem(
                    selected = currentTab == FollowingRoute,
                    onClick = { currentTab = FollowingRoute },
                    icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = "Following") },
                    label = { Text("Following") }
                )
                NavigationBarItem(
                    selected = currentTab == SettingsRoute,
                    onClick = { currentTab = SettingsRoute },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            entries = entries,
            onBack = { currentBackStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}
