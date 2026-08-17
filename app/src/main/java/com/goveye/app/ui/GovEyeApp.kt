package com.goveye.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.runtime.LaunchedEffect
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
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.navigation.DirectoryRoute
import com.goveye.app.ui.navigation.FeedRoute
import com.goveye.app.ui.navigation.FollowingRoute
import com.goveye.app.ui.navigation.ProfileRoute
import com.goveye.app.ui.navigation.SettingsRoute
import com.goveye.app.ui.navigation.DivisionDetailRoute
import com.goveye.app.ui.screens.FeedScreen
import com.goveye.app.ui.screens.FollowingScreen
import com.goveye.app.ui.screens.SettingsScreen
import com.goveye.app.ui.screens.directory.DirectoryScreen
import com.goveye.app.ui.screens.divisions.DivisionDetailScreen
import com.goveye.app.ui.screens.mpprofile.ProfileScreen
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
fun GovEyeApp(deepLinkNavigator: DeepLinkNavigator) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()

    GovEyeTheme(
        appTheme = appTheme,
        themeMode = themeMode,
        isAmoled = isAmoled
    ) {
        GovEyeAppContent(themeViewModel, deepLinkNavigator)
    }
}

@Composable
private fun GovEyeAppContent(
    themeViewModel: ThemeViewModel,
    deepLinkNavigator: DeepLinkNavigator,
) {
    // Per-tab back stacks (D-20)
    val feedBackStack = rememberNavBackStack(FeedRoute)
    val directoryBackStack = rememberNavBackStack(DirectoryRoute)
    val followingBackStack = rememberNavBackStack(FollowingRoute)
    val settingsBackStack = rememberNavBackStack(SettingsRoute)

    // Current tab — Feed is default (D-15).
    // Stored as an ordinal string because NavKey data objects cannot be saved to a Bundle
    // by rememberSaveable's default Saver (IllegalArgumentException at runtime).
    var currentTabIndex by rememberSaveable { mutableStateOf(0) }
    val currentTab: NavKey =
        when (currentTabIndex) {
            0 -> FeedRoute
            1 -> DirectoryRoute
            2 -> FollowingRoute
            3 -> SettingsRoute
            else -> FeedRoute
        }

    val currentBackStack =
        when (currentTab) {
            FeedRoute -> feedBackStack
            DirectoryRoute -> directoryBackStack
            FollowingRoute -> followingBackStack
            SettingsRoute -> settingsBackStack
            else -> feedBackStack
        }

    LaunchedEffect(Unit) {
        deepLinkNavigator.deepLinkEvents.collect { route ->
            when (route) {
                is ProfileRoute -> {
                    currentTabIndex = 1
                    directoryBackStack.add(route)
                }
                is DirectoryRoute -> {
                    currentTabIndex = 1
                }
                else -> { }
            }
        }
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
                        DirectoryScreen(
                            onNavigateToProfile = { memberId ->
                                currentBackStack.add(ProfileRoute(memberId))
                            },
                            onNavigateToDivision = { divisionId, house ->
                                currentBackStack.add(DivisionDetailRoute(divisionId, house))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                is ProfileRoute ->
                    NavEntry(key) {
                        ProfileScreen(
                            memberId = key.memberId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToProfile = { targetId ->
                                currentBackStack.add(ProfileRoute(targetId))
                            },
                            onNavigateToDivision = { divisionId, house ->
                                currentBackStack.add(DivisionDetailRoute(divisionId, house))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                is DivisionDetailRoute ->
                    NavEntry(key) {
                        DivisionDetailScreen(
                            divisionId = key.divisionId,
                            house = key.house,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToProfile = { targetId ->
                                currentBackStack.add(ProfileRoute(targetId))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
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

    val showBottomBar = currentBackStack.lastOrNull()?.let {
        it !is ProfileRoute && it !is DivisionDetailRoute
    } ?: true

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.clipToBounds(),
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTabIndex == 0,
                        onClick = { currentTabIndex = 0 },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Feed") },
                        label = { Text("Feed") }
                    )
                    NavigationBarItem(
                        selected = currentTabIndex == 1,
                        onClick = { currentTabIndex = 1 },
                        icon = { Icon(Icons.Outlined.Search, contentDescription = "Directory") },
                        label = { Text("Directory") }
                    )
                    NavigationBarItem(
                        selected = currentTabIndex == 2,
                        onClick = { currentTabIndex = 2 },
                        icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = "Following") },
                        label = { Text("Following") }
                    )
                    NavigationBarItem(
                        selected = currentTabIndex == 3,
                        onClick = { currentTabIndex = 3 },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
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
