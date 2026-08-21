package com.goveye.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.goveye.app.ui.components.DetailTopBar
import com.goveye.app.ui.components.FloatingSearchBar
import com.goveye.app.ui.components.LocalSearchBarState
import com.goveye.app.ui.components.SearchBarStateHolder
import com.goveye.app.ui.navigation.BillDetailRoute
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.navigation.DirectoryRoute
import com.goveye.app.ui.navigation.DivisionDetailRoute
import com.goveye.app.ui.navigation.FeedRoute
import com.goveye.app.ui.navigation.FollowingRoute
import com.goveye.app.ui.navigation.InterestBucketDetailRoute
import com.goveye.app.ui.navigation.ProfileRoute
import com.goveye.app.ui.navigation.SettingsRoute
import com.goveye.app.ui.screens.FeedScreen
import com.goveye.app.ui.screens.FollowingScreen
import com.goveye.app.ui.screens.SettingsScreen
import com.goveye.app.ui.screens.bills.BillDetailScreen
import com.goveye.app.ui.screens.directory.DirectoryScreen
import com.goveye.app.ui.screens.divisions.DivisionDetailScreen
import com.goveye.app.ui.screens.mpprofile.InterestBucketDetailScreen
import com.goveye.app.ui.screens.mpprofile.ProfileScreen
import com.goveye.app.ui.theme.ThemeViewModel

/**
 * Root composable for the GovEye app shell (D-14, D-20).
 *
 * Hosts the M3 theme wrapper, shared top bar, and Nav3 [NavDisplay] with
 * per-tab back stack preservation. The bottom navigation bar lives inside
 * each tab root via [TabScreenScaffold], not in this root Scaffold — so
 * detail screens render without it and the bar leaves as part of the
 * NavDisplay transition, not as a competing overlay.
 *
 * Theme state is collected from [ThemeViewModel] (scoped to the Activity)
 * and passed to [GovEyeTheme]. The same ViewModel instance is shared with
 * [SettingsScreen] so theme changes propagate live.
 */
@Composable
fun GovEyeApp(deepLinkNavigator: DeepLinkNavigator) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val searchStateHolder = remember { SearchBarStateHolder() }
    CompositionLocalProvider(LocalSearchBarState provides searchStateHolder) {
        GovEyeAppContent(themeViewModel, deepLinkNavigator, searchStateHolder)
    }
}

@Composable
private fun GovEyeAppContent(
    themeViewModel: ThemeViewModel,
    deepLinkNavigator: DeepLinkNavigator,
    searchStateHolder: SearchBarStateHolder
) {
    val searchConfig by searchStateHolder.config
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

                is DivisionDetailRoute -> {
                    // Notification deep link — push onto directory back stack
                    // (the most natural entry point for division detail)
                    currentTabIndex = 1
                    directoryBackStack.add(route)
                }

                is BillDetailRoute -> {
                    // Notification deep link — push onto directory back stack
                    currentTabIndex = 1
                    directoryBackStack.add(route)
                }

                is InterestBucketDetailRoute -> {
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

    // Top bar height — captured from the root Scaffold's innerPadding via
    // SideEffect. Passed to tab screens (so content sits below the search
    // bar) and to the profile screen (so its header content is padded below
    // the transparent detail top bar while the gradient extends behind it).
    var topBarHeight by remember { mutableStateOf(0.dp) }

    val entries =
        rememberDecoratedNavEntries(currentBackStack, decorators) { key ->
            when (key) {
                is FeedRoute ->
                    NavEntry(key) {
                        TabScreenScaffold(
                            currentTabIndex = currentTabIndex,
                            onTabSelected = { currentTabIndex = it },
                            topPadding = topBarHeight
                        ) { contentModifier ->
                            FeedScreen(
                                onNavigateToDivision = { divisionId, house ->
                                    currentBackStack.add(DivisionDetailRoute(divisionId, house))
                                },
                                modifier = contentModifier
                            )
                        }
                    }

                is DirectoryRoute ->
                    NavEntry(key) {
                        TabScreenScaffold(
                            currentTabIndex = currentTabIndex,
                            onTabSelected = { currentTabIndex = it },
                            topPadding = topBarHeight
                        ) { contentModifier ->
                            DirectoryScreen(
                                onNavigateToProfile = { memberId ->
                                    currentBackStack.add(ProfileRoute(memberId))
                                },
                                onNavigateToDivision = { divisionId, house ->
                                    currentBackStack.add(DivisionDetailRoute(divisionId, house))
                                },
                                onNavigateToBill = { billId ->
                                    currentBackStack.add(BillDetailRoute(billId))
                                },
                                modifier = contentModifier
                            )
                        }
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
                            onNavigateToInterestBucket = { targetMemberId, bucketLabel ->
                                currentBackStack.add(InterestBucketDetailRoute(targetMemberId, bucketLabel))
                            },
                            contentTopPadding = topBarHeight,
                            modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is BillDetailRoute ->
                    NavEntry(key) {
                        BillDetailScreen(
                            billId = key.billId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is InterestBucketDetailRoute ->
                    NavEntry(key) {
                        InterestBucketDetailScreen(
                            memberId = key.memberId,
                            bucketLabel = key.bucketLabel,
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is FollowingRoute ->
                    NavEntry(key) {
                        TabScreenScaffold(
                            currentTabIndex = currentTabIndex,
                            onTabSelected = { currentTabIndex = it },
                            topPadding = topBarHeight
                        ) { contentModifier ->
                            FollowingScreen(
                                onNavigateToProfile = { memberId ->
                                    currentBackStack.add(ProfileRoute(memberId))
                                },
                                modifier = contentModifier
                            )
                        }
                    }

                is SettingsRoute ->
                    NavEntry(key) {
                        TabScreenScaffold(
                            currentTabIndex = currentTabIndex,
                            onTabSelected = { currentTabIndex = it },
                            topPadding = topBarHeight
                        ) { contentModifier ->
                            SettingsScreen(
                                themeViewModel = themeViewModel,
                                modifier = contentModifier
                            )
                        }
                    }

                else -> NavEntry(key) { Text("Unknown") }
            }
        }

    // Determine which top bar mode to show:
    // - Profile and interest bucket screens use the detail top bar
    //   (Miko-style shared toolbar with title + back button)
    // - All other screens use the floating search bar
    val isDetailTopBar = currentBackStack.lastOrNull()?.let {
        it is ProfileRoute || it is InterestBucketDetailRoute
    } ?: false

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
        topBar = {
            // Miko-style shared top bar — swaps between the floating search
            // bar (list screens) and the detail top bar (profile/bucket
            // screens). Instant swap (no animation) to avoid competing with
            // the NavDisplay content transition.
            androidx.compose.animation.AnimatedContent(
                targetState = isDetailTopBar,
                transitionSpec = {
                    fadeIn(animationSpec = tween(0)) togetherWith
                        fadeOut(animationSpec = tween(0))
                },
                label = "topBar"
            ) { showDetail ->
                if (showDetail) {
                    DetailTopBar(
                        config = searchStateHolder.detailConfig.value,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    FloatingSearchBar(
                        query = searchConfig.query,
                        onQueryChange = searchConfig.onQueryChange,
                        onFilterClick = searchConfig.onFilterClick,
                        hasActiveFilters = searchConfig.hasActiveFilters,
                        placeholder = searchConfig.placeholder,
                        filterChips = searchConfig.filterChips,
                        onBack = searchConfig.onBack,
                        segments = searchConfig.segments,
                        isSearchActive = searchConfig.isSearchActive,
                        onSearchActiveChange = searchConfig.onSearchActiveChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = statusBarPadding)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        // Capture the top bar height so we can pass it to tab screens (which
        // need to pad content below the search bar) and to the profile screen
        // (which needs to pad its header content below the transparent detail
        // top bar while letting the gradient extend behind it).
        SideEffect {
            topBarHeight = innerPadding.calculateTopPadding()
        }
        NavDisplay(
            entries = entries,
            onBack = { currentBackStack.removeLastOrNull() },
            // No top padding — each screen handles its own top padding via
            // topBarHeight. This lets the profile screen's gradient extend
            // behind the transparent detail top bar (Miko-style).
            modifier = Modifier.fillMaxSize(),
            // Miko-style transitions: crossfade + subtle 20% horizontal slide.
            // Push: new screen fades in from right 20%, old fades out sliding left 20%.
            // Pop: old screen fades out sliding right 20%, new fades in from left 20%.
            // Duration: 200ms push, 150ms pop — snappy like Miko.
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                        animationSpec = tween(200),
                        initialOffsetX = { it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        animationSpec = tween(200),
                        targetOffsetX = { -it / 5 }
                    )
                    )
            },
            popTransitionSpec = {
                (
                    fadeIn(animationSpec = tween(150)) + slideInHorizontally(
                        animationSpec = tween(150),
                        initialOffsetX = { -it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
                        animationSpec = tween(150),
                        targetOffsetX = { it / 5 }
                    )
                    )
            },
            predictivePopTransitionSpec = { _ ->
                (
                    fadeIn(animationSpec = tween(150)) + slideInHorizontally(
                        animationSpec = tween(150),
                        initialOffsetX = { -it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
                        animationSpec = tween(150),
                        targetOffsetX = { it / 5 }
                    )
                    )
            }
        )
    }
}

/**
 * Per-tab Scaffold that owns the bottom navigation bar.
 *
 * Each tab root screen (Feed, Directory, Following, Settings) is wrapped in
 * this Scaffold so the bottom bar is part of the tab root's composition.
 * When a detail screen is pushed onto the back stack, the tab root (with
 * this bar) exits via the NavDisplay transition — the bar leaves with the
 * screen, not as a separate overlay. This mirrors Miko's architecture where
 * the bottom bar lives inside the tab container, not the root shell.
 *
 * [topPadding] is the root Scaffold's top bar height (search bar). It's
 * applied to the content so the tab content sits below the search bar,
 * since the NavDisplay no longer applies top padding itself.
 */
@Composable
private fun TabScreenScaffold(
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    topPadding: Dp = 0.dp,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = { GovEyeBottomBar(currentTabIndex, onTabSelected) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        content(
            Modifier.fillMaxSize()
                .padding(top = topPadding)
                .padding(innerPadding)
        )
    }
}

@Composable
private fun GovEyeBottomBar(currentTabIndex: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTabIndex == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Feed") },
            label = { Text("Feed") }
        )
        NavigationBarItem(
            selected = currentTabIndex == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Outlined.Search, contentDescription = "Directory") },
            label = { Text("Directory") }
        )
        NavigationBarItem(
            selected = currentTabIndex == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = "Following") },
            label = { Text("Following") }
        )
        NavigationBarItem(
            selected = currentTabIndex == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}
