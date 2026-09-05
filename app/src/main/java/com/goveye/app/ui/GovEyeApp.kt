package com.goveye.app.ui

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.goveye.app.ui.components.FloatingSearchBar
import com.goveye.app.ui.components.LocalSearchBarState
import com.goveye.app.ui.components.SearchBarStateHolder
import com.goveye.app.ui.navigation.BillDetailRoute
import com.goveye.app.ui.navigation.CommitteeRoute
import com.goveye.app.ui.navigation.CouncilRoute
import com.goveye.app.ui.navigation.DeepLinkNavigator
import com.goveye.app.ui.navigation.DirectoryRoute
import com.goveye.app.ui.navigation.DivisionDetailRoute
import com.goveye.app.ui.navigation.FeedRoute
import com.goveye.app.ui.navigation.FollowingRoute
import com.goveye.app.ui.navigation.InterestBucketDetailRoute
import com.goveye.app.ui.navigation.LegislationDetailRoute
import com.goveye.app.ui.navigation.MpTagBrowseRoute
import com.goveye.app.ui.navigation.PartyRoute
import com.goveye.app.ui.navigation.ProfileRoute
import com.goveye.app.ui.navigation.PublicationDetailRoute
import com.goveye.app.ui.navigation.SettingsRoute
import com.goveye.app.ui.navigation.StatementDetailRoute
import com.goveye.app.ui.navigation.TranscriptRoute
import com.goveye.app.ui.navigation.VotingRecordRoute
import com.goveye.app.ui.screens.FeedScreen
import com.goveye.app.ui.screens.FollowingScreen
import com.goveye.app.ui.screens.SettingsScreen
import com.goveye.app.ui.screens.bills.BillDetailScreen
import com.goveye.app.ui.screens.committee.CommitteeScreen
import com.goveye.app.ui.screens.council.CouncilScreen
import com.goveye.app.ui.screens.directory.DirectoryScreen
import com.goveye.app.ui.screens.divisions.DivisionDetailScreen
import com.goveye.app.ui.screens.divisions.TranscriptScreen
import com.goveye.app.ui.screens.feed.LegislationDetailScreen
import com.goveye.app.ui.screens.feed.PublicationDetailScreen
import com.goveye.app.ui.screens.feed.StatementDetailScreen
import com.goveye.app.ui.screens.mpprofile.FinancialBucketDetailScreen
import com.goveye.app.ui.screens.mpprofile.MpTagBrowseScreen
import com.goveye.app.ui.screens.mpprofile.ProfileScreen
import com.goveye.app.ui.screens.mpprofile.VotingRecordScreen
import com.goveye.app.ui.screens.party.PartyScreen
import com.goveye.app.ui.settings.UpdateCheckViewModel
import com.goveye.app.ui.settings.UpdateSnackbarMessage
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
fun GovEyeApp(deepLinkNavigator: DeepLinkNavigator, onTestOnboarding: () -> Unit = {}) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val searchStateHolder = remember { SearchBarStateHolder() }
    CompositionLocalProvider(LocalSearchBarState provides searchStateHolder) {
        GovEyeAppContent(themeViewModel, deepLinkNavigator, searchStateHolder, onTestOnboarding)
    }
}

@Composable
private fun GovEyeAppContent(
    themeViewModel: ThemeViewModel,
    deepLinkNavigator: DeepLinkNavigator,
    searchStateHolder: SearchBarStateHolder,
    onTestOnboarding: () -> Unit = {}
) {
    val searchConfig by searchStateHolder.config

    // Snackbar host for update-check notifications (SyncStone convention:
    // in-app snackbar, no Toasts). The UpdateCheckViewModel is scoped to
    // the Activity so the snackbar host survives navigation between tabs.
    val updateCheckViewModel: UpdateCheckViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(updateCheckViewModel) {
        updateCheckViewModel.snackbarMessages.collect { message ->
            val (text, duration) = when (message) {
                is UpdateSnackbarMessage.AlreadyUpToDate ->
                    "Database is up to date" to SnackbarDuration.Short

                is UpdateSnackbarMessage.Success -> {
                    val plural = if (message.streamCount > 1) "s" else ""
                    "Updated ${message.streamCount} stream$plural: ${message.streamNames}" to
                        SnackbarDuration.Long
                }

                is UpdateSnackbarMessage.FullDownloadRequired ->
                    "A full database download is required — restart the app" to SnackbarDuration.Long

                is UpdateSnackbarMessage.Error ->
                    "Update failed: ${message.message}" to SnackbarDuration.Long
            }
            snackbarHostState.showSnackbar(text, duration = duration)
        }
    }
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

                is VotingRecordRoute -> {
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
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var topBarHeight by remember { mutableStateOf(statusBarPadding) }
    // Initialize to standard M3 NavigationBar height (80dp) + system inset
    // so tab root screens pad correctly on the first frame. Updated by
    // onSizeChanged after the bar is measured.
    var bottomBarHeight by remember { mutableStateOf(80.dp + navBarBottomInset) }

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
                            onNavigateToPublicationDetail = { publicationId ->
                                currentBackStack.add(PublicationDetailRoute(publicationId))
                            },
                            onNavigateToStatementDetail = { statementId ->
                                currentBackStack.add(StatementDetailRoute(statementId))
                            },
                            onNavigateToLegislationDetail = { legislationId ->
                                currentBackStack.add(LegislationDetailRoute(legislationId))
                            },
                            onNavigateToTranscript = { divId, divTitle, speechGid ->
                                currentBackStack.add(TranscriptRoute(divId, divTitle, speechGid))
                            },
                            onNavigateToProfile = { memberId, fallback, initialTab ->
                                currentBackStack.add(
                                    fallback?.toRoute(memberId, initialTab)
                                        ?: ProfileRoute(memberId, initialTab = initialTab)
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topBarHeight)
                                .padding(bottom = bottomBarHeight)
                        )
                    }

                is DirectoryRoute ->
                    NavEntry(key) {
                        DirectoryScreen(
                            onNavigateToProfile = { memberId, fallback ->
                                currentBackStack.add(
                                    fallback?.toRoute(memberId) ?: ProfileRoute(memberId, initialTab = 0)
                                )
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
                            onNavigateToCommittee = { committeeId ->
                                currentBackStack.add(CommitteeRoute(committeeId))
                            },
                            onNavigateToCouncil = { councilId ->
                                currentBackStack.add(CouncilRoute(councilId))
                            },
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
                            onNavigateToProfile = { targetId, fallback, initialTab ->
                                currentBackStack.add(
                                    fallback?.toRoute(targetId, initialTab)
                                        ?: ProfileRoute(targetId, initialTab = initialTab)
                                )
                            },
                            fallbackName = key.fallbackName,
                            fallbackPartyName = key.fallbackPartyName,
                            fallbackPartyColor = key.fallbackPartyColor,
                            fallbackThumbnailUrl = key.fallbackThumbnailUrl,
                            fallbackConstituency = key.fallbackConstituency,
                            fallbackActivityScore = key.fallbackActivityScore,
                            fallbackDateOfBirth = key.fallbackDateOfBirth,
                            initialTab = key.initialTab,
                            onNavigateToDivision = { divisionId, house ->
                                currentBackStack.add(DivisionDetailRoute(divisionId, house))
                            },
                            onNavigateToInterestBucket = { targetMemberId, bucketLabel ->
                                currentBackStack.add(InterestBucketDetailRoute(targetMemberId, bucketLabel))
                            },
                            onNavigateToExpenseBucket = { targetMemberId, bucketLabel ->
                                currentBackStack.add(InterestBucketDetailRoute(targetMemberId, bucketLabel, "EXPENSE"))
                            },
                            onNavigateToParty = { partyId ->
                                currentBackStack.add(PartyRoute(partyId))
                            },
                            onNavigateToCommittee = { committeeId ->
                                currentBackStack.add(CommitteeRoute(committeeId))
                            },
                            onNavigateToVotingRecord = { memberId ->
                                currentBackStack.add(VotingRecordRoute(memberId))
                            },
                            onNavigateToMpTagBrowse = { tag ->
                                currentBackStack.add(MpTagBrowseRoute(tag))
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

                is CommitteeRoute ->
                    NavEntry(key) {
                        CommitteeScreen(
                            committeeId = key.committeeId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToProfile = { targetId ->
                                currentBackStack.add(ProfileRoute(targetId))
                            },
                            contentTopPadding = topBarHeight,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                is CouncilRoute ->
                    NavEntry(key) {
                        CouncilScreen(
                            councilId = key.councilId,
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
                            onNavigateToProfile = { targetId, fallback, initialTab ->
                                currentBackStack.add(
                                    fallback?.toRoute(targetId, initialTab)
                                        ?: ProfileRoute(targetId, initialTab = initialTab)
                                )
                            },
                            onNavigateToTranscript = { divId, divTitle, speechGid ->
                                currentBackStack.add(TranscriptRoute(divId, divTitle, speechGid))
                            },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is TranscriptRoute ->
                    NavEntry(key) {
                        TranscriptScreen(
                            divisionId = key.divisionId,
                            divisionTitle = key.divisionTitle,
                            initialSpeechGid = key.speechGid,
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
                        FinancialBucketDetailScreen(
                            memberId = key.memberId,
                            bucketLabel = key.bucketLabel,
                            entryType = if (key.entryType == "EXPENSE") {
                                com.goveye.app.domain.model.FinancialEntryType.EXPENSE
                            } else {
                                com.goveye.app.domain.model.FinancialEntryType.INCOME
                            },
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is VotingRecordRoute ->
                    NavEntry(key) {
                        VotingRecordScreen(
                            memberId = key.memberId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToDivision = { divisionId, house ->
                                currentBackStack.add(DivisionDetailRoute(divisionId, house))
                            },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is PublicationDetailRoute ->
                    NavEntry(key) {
                        PublicationDetailScreen(
                            publicationId = key.publicationId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is StatementDetailRoute ->
                    NavEntry(key) {
                        StatementDetailScreen(
                            statementId = key.statementId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is LegislationDetailRoute ->
                    NavEntry(key) {
                        LegislationDetailScreen(
                            legislationId = key.legislationId,
                            onBack = { currentBackStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is MpTagBrowseRoute ->
                    NavEntry(key) {
                        MpTagBrowseScreen(
                            tag = key.tag,
                            onBack = { currentBackStack.removeLastOrNull() },
                            onNavigateToProfile = { targetId ->
                                currentBackStack.add(ProfileRoute(targetId))
                            },
                            contentTopPadding = topBarHeight,
                            modifier = Modifier.fillMaxSize().padding(top = topBarHeight)
                        )
                    }

                is FollowingRoute ->
                    NavEntry(key) {
                        FollowingScreen(
                            onNavigateToProfile = { memberId ->
                                currentBackStack.add(ProfileRoute(memberId))
                            },
                            onNavigateToParty = { partyId ->
                                currentBackStack.add(PartyRoute(partyId))
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
                            onTestOnboarding = onTestOnboarding,
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
    // - Detail screens (profile, party, interest bucket) show back button
    //   and action icons alongside the search bar
    // - All other screens show just the search bar (full width)
    val currentRoute = currentBackStack.lastOrNull()
    val isDetailTopBar =
        currentRoute is ProfileRoute || currentRoute is InterestBucketDetailRoute ||
            currentRoute is PartyRoute || currentRoute is CommitteeRoute ||
            currentRoute is CouncilRoute || currentRoute is VotingRecordRoute

    // Merge search config and detail config for the unified top bar.
    // On detail screens, the back button and action icons come from
    // detailConfig. On list screens, they come from searchConfig (if any).
    val detailConfig = searchStateHolder.detailConfig.value
    val topBarOnBack = if (isDetailTopBar) detailConfig.onBack else searchConfig.onBack
    val topBarActions = if (isDetailTopBar) detailConfig.actions else emptyList()
    val topBarIconTint = if (isDetailTopBar) detailConfig.iconTint else null
    val topBarAccentColor = if (isDetailTopBar) detailConfig.accentColor else null

    // Guard: snapshot the search config on committed navigation (route
    // change) so the search bar does not update during predictive back
    // preview. During predictive back, the previous screen's
    // ConfigureSearchBar fires prematurely (its DisposableEffect runs when
    // the entry is composed for the transition animation), which caused
    // the placeholder text and filter button to change before the back
    // gesture was committed (issue #12.1). By snapshotting only when the
    // route actually changes, the search bar retains the committed
    // screen's config until the back navigation is committed.
    var committedSearchConfig by remember { mutableStateOf(searchConfig) }
    LaunchedEffect(currentRoute) {
        // Wait one frame for the new screen to compose and set its config
        // via ConfigureSearchBar before snapshotting. Without this delay,
        // the snapshot captures the previous screen's config (e.g. filterClick=null
        // on profile's default tab) before the new screen (DirectoryScreen with
        // filterClick=non-null) has a chance to recompose.
        withFrameNanos { }
        committedSearchConfig = searchStateHolder.config.value
    }
    // Sync query live so user typing updates the search bar immediately,
    // without causing a full config swap during navigation transitions.
    LaunchedEffect(searchConfig.query) {
        committedSearchConfig = committedSearchConfig.copy(query = searchConfig.query)
    }

    // Bottom bar is only shown on tab root screens — not on detail screens
    // pushed onto a tab's back stack.
    val isTabRoot = currentRoute is FeedRoute ||
        currentRoute is DirectoryRoute ||
        currentRoute is FollowingRoute ||
        currentRoute is SettingsRoute

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
        // No bottomBar slot — the nav bar is an overlay in the content Box.
        // This is the Miko pattern: the nav bar is not part of the Scaffold
        // layout, so showing/hiding it doesn't resize the content area.
        // Detail screens slide in at full height and cover where the bar was.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Unified top bar — the FloatingSearchBar is always rendered
            // in the same tree position so it persists (not rebuilt) across
            // navigation. On detail screens, back button and action icons
            // appear alongside it, causing the search bar to morph width
            // via AnimatedVisibility expand/shrink.
            FloatingSearchBar(
                query = committedSearchConfig.query,
                onQueryChange = searchConfig.onQueryChange,
                onFilterClick = committedSearchConfig.onFilterClick,
                hasActiveFilters = committedSearchConfig.hasActiveFilters,
                placeholder = committedSearchConfig.placeholder,
                filterChips = committedSearchConfig.filterChips,
                onBack = topBarOnBack,
                segments = committedSearchConfig.segments,
                isSearchActive = searchConfig.isSearchActive,
                onSearchActiveChange = searchConfig.onSearchActiveChange,
                actions = topBarActions,
                iconTint = topBarIconTint,
                accentColor = topBarAccentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    ) { innerPadding ->
        // Capture the top bar height so we can pass it to tab screens.
        SideEffect {
            val newHeight = innerPadding.calculateTopPadding()
            if (newHeight != topBarHeight) {
                Log.i("GovEye/Layout", "topBarHeight changed: $topBarHeight -> $newHeight")
                topBarHeight = newHeight
            }
        }
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ── Shell-level gradient ───────────────────────────────────
            // Renders behind NavDisplay so it persists across navigation
            // transitions (like the search bar) instead of sliding with
            // the content. Fades in/out smoothly via animateColorAsState
            // when navigating to/from detail screens with a party accent.
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

            // Remember the last non-null accent color so the gradient
            // can fade OUT (alpha → 0) after the screen leaves, instead
            // of snapping to transparent instantly.
            var lastAccentColor by remember { mutableStateOf<Color?>(null) }
            LaunchedEffect(topBarAccentColor) {
                if (topBarAccentColor != null) {
                    lastAccentColor = topBarAccentColor
                }
            }

            // Animate the gradient alpha: 1 when on a detail screen with
            // an accent color, 0 otherwise. This gives a smooth fade
            // in/out regardless of navigation direction.
            val gradientAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (topBarAccentColor != null) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "gradientAlpha"
            )

            // Only render the gradient when there's something to show
            // (avoids unnecessary compositing when fully transparent).
            if (gradientAlpha > 0.01f && lastAccentColor != null) {
                val color = lastAccentColor!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    color.copy(
                                        alpha = (if (isDark) 0.85f else 0.7f) * gradientAlpha
                                    ),
                                    color.copy(
                                        alpha = (if (isDark) 0.3f else 0.2f) * gradientAlpha
                                    ),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // NavDisplay fills the full height — no bottom padding from
            // Scaffold. Detail screens cover the nav bar area entirely.
            // Tab root screens pad their own content by bottomBarHeight.
            NavDisplay(
                entries = entries,
                onBack = { currentBackStack.removeLastOrNull() },
                modifier = Modifier.fillMaxSize(),
                // Miko-style transitions: crossfade + subtle 20% horizontal slide.
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

            // Bottom bar — always composed as an overlay, but alpha-animated
            // so it fades out on detail screens and fades in on tab roots.
            // This avoids the blank area during predictive back (the bar is
            // always in the layout, just invisible) and the layout shift
            // (no height change). Miko uses AnimatedVisibility in a Scaffold
            // bottomBar slot, but GovEye's detail screens share the same
            // NavDisplay, so removing the bar from composition causes a
            // height change that pushes content. Alpha animation keeps the
            // layout stable while hiding the bar visually.
            val bottomBarAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isTabRoot) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "bottomBarAlpha"
            )
            GovEyeBottomBar(
                currentTabIndex = currentTabIndex,
                onTabSelected = {
                    Log.i("GovEye/Nav", "Tab selected: $it (was $currentTabIndex)")
                    currentTabIndex = it
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = bottomBarAlpha }
                    .onSizeChanged { size ->
                        bottomBarHeight = with(density) { size.height.toDp() }
                    }
            )
        }
    }
}

@Composable
private fun GovEyeBottomBar(currentTabIndex: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier
    ) {
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
