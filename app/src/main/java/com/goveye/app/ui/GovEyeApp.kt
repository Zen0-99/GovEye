package com.goveye.app.ui

import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.goveye.app.ui.navigation.PartyRoute
import com.goveye.app.ui.navigation.ProfileRoute
import com.goveye.app.ui.navigation.SettingsRoute
import com.goveye.app.ui.navigation.TranscriptRoute
import com.goveye.app.ui.screens.FeedScreen
import com.goveye.app.ui.screens.FollowingScreen
import com.goveye.app.ui.screens.SettingsScreen
import com.goveye.app.ui.screens.bills.BillDetailScreen
import com.goveye.app.ui.screens.directory.DirectoryScreen
import com.goveye.app.ui.screens.divisions.DivisionDetailScreen
import com.goveye.app.ui.screens.divisions.TranscriptScreen
import com.goveye.app.ui.screens.mpprofile.InterestBucketDetailScreen
import com.goveye.app.ui.screens.mpprofile.ProfileScreen
import com.goveye.app.ui.screens.party.PartyScreen
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
    val showInfoCards by themeViewModel.showInfoCards.collectAsStateWithLifecycle()
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

    Log.i(
        "GovEye/Nav",
        "GovEyeApp compose — tab=$currentTabIndex route=${currentBackStack.lastOrNull()?.javaClass?.simpleName} backstackSize=${currentBackStack.size}"
    )

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
    // Initialize with status bar padding as a minimum so the first frame
    // doesn't render content behind the search bar. SideEffect updates
    // with the actual height after the first composition.
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var topBarHeight by remember { mutableStateOf(statusBarPadding) }
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    val entries =
        rememberDecoratedNavEntries(currentBackStack, decorators) { key ->
            Log.i("GovEye/Nav", "NavEntry created: ${key.javaClass.simpleName}")
            when (key) {
                is FeedRoute ->
                    NavEntry(key) {
                        FeedScreen(
                            onNavigateToDivision = { divisionId, house ->
                                currentBackStack.add(DivisionDetailRoute(divisionId, house))
                            },
                            showInfoCards = showInfoCards,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topBarHeight)
                                .padding(bottom = bottomBarHeight)
                        )
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
                            onNavigateToBill = { billId ->
                                currentBackStack.add(BillDetailRoute(billId))
                            },
                            onNavigateToParty = { partyId ->
                                currentBackStack.add(PartyRoute(partyId))
                            },
                            showInfoCards = showInfoCards,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topBarHeight)
                                .padding(bottom = bottomBarHeight)
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
                            onNavigateToInterestBucket = { targetMemberId, bucketLabel ->
                                currentBackStack.add(InterestBucketDetailRoute(targetMemberId, bucketLabel))
                            },
                            onNavigateToParty = { partyId ->
                                currentBackStack.add(PartyRoute(partyId))
                            },
                            contentTopPadding = topBarHeight,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                is PartyRoute ->
                    NavEntry(key) {
                        PartyScreen(
                            partyId = key.partyId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToProfile = { targetId ->
                                currentBackStack.add(ProfileRoute(targetId))
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
                            onNavigateToTranscript = { divId, divTitle ->
                                currentBackStack.add(TranscriptRoute(divId, divTitle))
                            },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is TranscriptRoute ->
                    NavEntry(key) {
                        TranscriptScreen(
                            divisionId = key.divisionId,
                            divisionTitle = key.divisionTitle,
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
                        FollowingScreen(
                            onNavigateToProfile = { memberId ->
                                currentBackStack.add(ProfileRoute(memberId))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topBarHeight)
                                .padding(bottom = bottomBarHeight)
                        )
                    }

                is SettingsRoute ->
                    NavEntry(key) {
                        SettingsScreen(
                            themeViewModel = themeViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topBarHeight)
                                .padding(bottom = bottomBarHeight)
                        )
                    }

                else -> NavEntry(key) { Text("Unknown") }
            }
        }

    // Determine which top bar mode to show:
    // - Profile and interest bucket screens use the detail top bar
    //   (Miko-style shared toolbar with title + back button)
    // - All other screens use the floating search bar
    val currentRoute = currentBackStack.lastOrNull()
    val isDetailTopBar =
        currentRoute is ProfileRoute || currentRoute is InterestBucketDetailRoute || currentRoute is PartyRoute

    // Bottom bar is only shown on tab root screens — not on detail screens
    // pushed onto a tab's back stack.
    val isTabRoot = currentRoute is FeedRoute ||
        currentRoute is DirectoryRoute ||
        currentRoute is FollowingRoute ||
        currentRoute is SettingsRoute

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            if (isTabRoot) {
                GovEyeBottomBar(currentTabIndex) {
                    Log.i("GovEye/Nav", "Tab selected: $it (was $currentTabIndex)")
                    currentTabIndex = it
                }
            }
        },
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
            bottomBarHeight = innerPadding.calculateBottomPadding()
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
            // Duration: 150ms push, 100ms pop — snappy, minimal overlap window.
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(150)) + slideInHorizontally(
                        animationSpec = tween(150),
                        initialOffsetX = { it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
                        animationSpec = tween(150),
                        targetOffsetX = { -it / 5 }
                    )
                    )
            },
            popTransitionSpec = {
                (
                    fadeIn(animationSpec = tween(100)) + slideInHorizontally(
                        animationSpec = tween(100),
                        initialOffsetX = { -it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(100)) + slideOutHorizontally(
                        animationSpec = tween(100),
                        targetOffsetX = { it / 5 }
                    )
                    )
            },
            predictivePopTransitionSpec = { _ ->
                (
                    fadeIn(animationSpec = tween(100)) + slideInHorizontally(
                        animationSpec = tween(100),
                        initialOffsetX = { -it / 5 }
                    )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(100)) + slideOutHorizontally(
                        animationSpec = tween(100),
                        targetOffsetX = { it / 5 }
                    )
                    )
            }
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
