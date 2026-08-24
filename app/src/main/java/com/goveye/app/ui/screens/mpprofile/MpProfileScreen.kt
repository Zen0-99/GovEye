package com.goveye.app.ui.screens.mpprofile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.VoteMapCalculator
import com.goveye.app.domain.stats.VotingStatsCalculator
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DetailTopBarAction
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.charts.AttendanceLineChart
import com.goveye.app.ui.components.charts.RebellionLineChart
import com.goveye.app.ui.components.charts.VotingBarChart
import com.goveye.app.ui.theme.LocalPartyAccent
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor
import com.goveye.app.ui.theme.partyAccentColorScheme

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
    STATS("Stats"),
    ACTIVITY("Activity"),
    INTERESTS("Finances")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToInterestBucket: (Int, String) -> Unit = { _, _ -> },
    onNavigateToExpenseBucket: (Int, String) -> Unit = { _, _ -> },
    onNavigateToParty: (Int) -> Unit = {},
    onNavigateToCommittee: (Int) -> Unit = {},
    onNavigateToVotingRecord: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNotificationSheet by remember { mutableStateOf(false) }

    // POST_NOTIFICATIONS permission launcher (Android 13+) — requested when
    // user enables any notification type
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If permission denied, the prefs are still saved but notifications
        // won't show. The NotificationHelper silently skips on SecurityException.
    }

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    var showInterestFilterSheet by remember { mutableStateOf(false) }
    var showActivityFilterSheet by remember { mutableStateOf(false) }

    // Track current tab index for search bar config and filter sheet
    var currentPage by remember { mutableIntStateOf(0) }

    // Lifted interest date filter state — needed at the screen level so the
    // global floating search bar's filter icon can trigger the sheet and
    // report hasActiveFilters.
    var interestFromDate by remember { mutableStateOf<String?>(null) }
    var interestToDate by remember { mutableStateOf<String?>(null) }
    val interestHasActiveFilter = interestFromDate != null || interestToDate != null

    // Configure the shell's detail top bar (Miko-style shared toolbar).
    // No title — the name lives in the content header beside the avatar.
    // Icons are white to be readable over the party gradient that extends
    // from the top of the screen behind the top bar.
    val mp = uiState.mp
    val partyColor = remember(mp) { parsePartyColor(mp?.party?.backgroundColour) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)
    val isInterestsTab = currentPage == ProfileTab.INTERESTS.ordinal
    val isActivityTab = currentPage == ProfileTab.ACTIVITY.ordinal

    // Wire the global floating search bar — the filter icon and search field
    // are built into the FloatingSearchBar at the shell level. We configure
    // it here so:
    // - On the Interests tab: the filter icon triggers the date filter sheet,
    //   and hasActiveFilters reflects whether a date range is set.
    // - On the Activity tab: the filter icon triggers the activity type filter
    //   sheet (D-05). No search on the mixed feed — search is on VotingRecordScreen.
    // - On other tabs: the search bar is visible but inactive (no filter).
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = "",
            placeholder = when {
                isActivityTab -> "Activity feed"
                isInterestsTab -> "Search entries…"
                else -> "Search…"
            },
            onQueryChange = { },
            onFilterClick = when {
                isInterestsTab -> {
                    { showInterestFilterSheet = true }
                }

                isActivityTab -> {
                    { showActivityFilterSheet = true }
                }

                else -> null
            },
            hasActiveFilters = (isActivityTab && uiState.activityEnabledTypes.size < 6) ||
                (isInterestsTab && interestHasActiveFilter)
        )
    )

    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = "",
            onBack = onBack,
            accentColor = if (mp != null) partyColor else null,
            iconTint = if (mp != null) headerIconTint else null,
            actions = if (mp != null) {
                buildList {
                    // Filter and search are handled by the floating search bar —
                    // no redundant icons here. Only notification and follow toggles.
                    add(
                        DetailTopBarAction(
                            icon = if (uiState.notificationsEnabled) {
                                Icons.Outlined.NotificationsActive
                            } else {
                                Icons.Outlined.Notifications
                            },
                            contentDescription = "Notification settings",
                            onClick = { showNotificationSheet = true },
                            tint = headerIconTint
                        )
                    )
                    add(
                        DetailTopBarAction(
                            icon = if (uiState.isFollowing) {
                                Icons.Outlined.PersonRemove
                            } else {
                                Icons.Outlined.PersonAdd
                            },
                            contentDescription = if (uiState.isFollowing) "Unfollow" else "Follow",
                            onClick = { viewModel.toggleFollow(memberId) },
                            tint = headerIconTint
                        )
                    )
                }
            } else {
                emptyList()
            }
        )
    )

    if (uiState.isLoading && mp == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (mp == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("MP not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val partyScheme = partyAccentColorScheme(partyColor, isDark)

        CompositionLocalProvider(LocalPartyAccent provides partyColor) {
            MaterialTheme(colorScheme = partyScheme) {
                Column(
                    modifier = modifier.fillMaxSize()
                ) {
                    // Scrolling content header — gradient + avatar + name + party pill.
                    // The gradient extends from the top of the screen (behind the
                    // transparent detail top bar) downwards. No back/action buttons
                    // (those are in the shell's detail top bar).
                    ProfileContentHeader(
                        mp = mp,
                        isDark = isDark,
                        contentTopPadding = contentTopPadding,
                        onNavigateToParty = onNavigateToParty
                    )

                    // Sub-tab pager — shared component for all tabbed screens.
                    // Handles instant jump, only-compose-current-page, and
                    // pre-loaded data pattern.
                    com.goveye.app.ui.components.SubTabPager(
                        tabs = ProfileTab.entries.map { com.goveye.app.ui.components.SubTab(label = it.title) },
                        onPageChange = { currentPage = it },
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        when (ProfileTab.entries[page]) {
                            ProfileTab.PROFILE -> ProfileTabContent(
                                mp = mp,
                                synopsis = uiState.synopsis,
                                contacts = uiState.contacts,
                                samePartyMps = uiState.samePartyMps,
                                committeePeerMps = uiState.committeePeerMps,
                                bioData = uiState.bioData,
                                mpLinks = uiState.mpLinks,
                                traitBars = uiState.traitBars,
                                onNavigateToProfile = onNavigateToProfile
                            )

                            ProfileTab.CAREER -> CareerTabContent(
                                experiences = uiState.experiences
                            )

                            ProfileTab.COMMITTEES -> CommitteesTabContent(
                                committees = uiState.committees,
                                onCommitteeClick = onNavigateToCommittee
                            )

                            ProfileTab.STATS -> ProfileStatsTabContent(
                                memberId = memberId,
                                memberVotes = uiState.memberVotes,
                                rebellionStats = uiState.rebellionStats,
                                allDivisionDates = uiState.allDivisionDates,
                                activityScore = uiState.activityScore,
                                traitBars = uiState.traitBars,
                                onNavigateToDivision = { divisionId, house ->
                                    onNavigateToDivision(divisionId, house)
                                },
                                onNavigateToVotingRecord = { onNavigateToVotingRecord(memberId) }
                            )

                            ProfileTab.ACTIVITY -> ActivityTabContent(
                                activityEntries = uiState.activityEntries,
                                enabledTypes = uiState.activityEnabledTypes,
                                totalCount = uiState.activityTotalCount,
                                onFilterClick = { showActivityFilterSheet = true },
                                onNavigateToDivision = { divisionId, house ->
                                    onNavigateToDivision(divisionId, house)
                                }
                            )

                            ProfileTab.INTERESTS -> InterestsTabContent(
                                memberId = memberId,
                                interests = uiState.interests,
                                expenseBucketTotals = uiState.expenseBucketTotals,
                                onNavigateToBucketDetail = { bucketLabel ->
                                    onNavigateToInterestBucket(memberId, bucketLabel)
                                },
                                onNavigateToExpenseBucket = { bucketLabel ->
                                    onNavigateToExpenseBucket(memberId, bucketLabel)
                                },
                                showFilterSheet = showInterestFilterSheet,
                                onFilterSheetDismiss = { showInterestFilterSheet = false },
                                fromDate = interestFromDate,
                                toDate = interestToDate,
                                onFromDateChange = { interestFromDate = it },
                                onToDateChange = { interestToDate = it }
                            )
                        }
                    }
                }
            }
        }

        // Notification settings bottom sheet (FotMob-style)
        if (showNotificationSheet) {
            NotificationSettingsBottomSheet(
                notificationsEnabled = uiState.notificationsEnabled,
                votesEnabled = uiState.votesNotificationsEnabled,
                speechesEnabled = uiState.speechesNotificationsEnabled,
                incomeEnabled = uiState.incomeEnabled,
                expensesEnabled = uiState.expensesEnabled,
                onMasterToggle = { enabled ->
                    // Request POST_NOTIFICATIONS when turning on (Android 13+)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setNotificationsEnabled(memberId, enabled)
                },
                onVotesToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setVotesNotificationsEnabled(memberId, enabled)
                },
                onSpeechesToggle = { enabled ->
                    viewModel.setSpeechesNotificationsEnabled(memberId, enabled)
                },
                onIncomeToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setIncomeNotificationsEnabled(memberId, enabled)
                },
                onExpensesToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setExpensesNotificationsEnabled(memberId, enabled)
                },
                onDismiss = { showNotificationSheet = false }
            )
        }

        // Activity type filter bottom sheet (D-05, D-06)
        if (showActivityFilterSheet) {
            ActivityFilterBottomSheet(
                enabledTypes = uiState.activityEnabledTypes,
                onTypeToggle = { type ->
                    viewModel.toggleActivityFilter(memberId, type)
                },
                onClearFilters = {
                    viewModel.clearActivityFilter(memberId)
                },
                onDismiss = { showActivityFilterSheet = false }
            )
        }
    }
}

/**
 * Scrolling content header for the profile screen — gradient background with
 * avatar, name, and party pill. Unlike the old [ProfileHeader], this does NOT
 * include back/action buttons (those are in the shell's detail top bar).
 *
 * [contentTopPadding] is the height of the transparent detail top bar
 * (including status bar inset). The gradient fills from y=0 (behind the top
 * bar), but the content (avatar, name, pill) is padded down by this amount
 * so it doesn't overlap the back/action buttons — mirroring Miko's
 * AnimeInfoHeader where the cover-art backdrop fills from the top but the
 * titles are padded by appBarPadding.
 */
@Composable
private fun ProfileContentHeader(
    mp: com.goveye.app.domain.model.Mp,
    isDark: Boolean,
    contentTopPadding: Dp = 0.dp,
    onNavigateToParty: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val partyColor = parsePartyColor(mp.party?.backgroundColour)
    val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    val pillColor = if (isDark) {
        partyColor.copy(alpha = 0.9f)
    } else {
        Color(
            red = partyColor.red * 0.8f,
            green = partyColor.green * 0.8f,
            blue = partyColor.blue * 0.8f,
            alpha = 0.85f
        )
    }
    val pillTextColor = if (pillColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White

    // Gradient is rendered at the shell level (GovEyeApp) so it fades
    // in/out smoothly across navigation transitions. This Box is
    // transparent — the shell-level gradient shows through.
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = contentTopPadding + MaterialTheme.padding.small, bottom = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            MpAvatar(
                thumbnailUrl = mp.thumbnailUrl,
                displayName = mp.nameDisplayAs,
                partyColorHex = mp.party?.backgroundColour,
                size = 60.dp,
                borderWidth = 2.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mp.nameDisplayAs,
                    style = MaterialTheme.typography.titleLarge,
                    color = headerTextColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillColor,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            mp.party?.id?.let { onNavigateToParty(it) }
                        }
                ) {
                    Text(
                        text = mp.party?.name ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = pillTextColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    mp: com.goveye.app.domain.model.Mp,
    synopsis: String?,
    contacts: List<com.goveye.app.domain.model.Contact>,
    samePartyMps: List<com.goveye.app.domain.model.Mp>,
    committeePeerMps: List<com.goveye.app.domain.model.Mp>,
    bioData: com.goveye.app.data.local.entity.BioDataEntity? = null,
    mpLinks: com.goveye.app.data.local.entity.MpLinkEntity? = null,
    @Suppress("UNUSED_PARAMETER") traitBars: List<com.goveye.app.domain.stats.TraitBar> = emptyList(),
    onNavigateToProfile: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp)
    ) {
        item {
            ProfileStatsCard(
                mp = mp,
                bioData = bioData
            )
        }
        item { BioSection(synopsis = synopsis) }
        item { ContactSection(contacts = contacts, socialLinks = mpLinks) }
        item {
            RelatedMpsSection(
                samePartyMps = samePartyMps,
                committeePeerMps = committeePeerMps,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun CareerTabContent(experiences: List<com.goveye.app.domain.model.BiographyExperience>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp)
    ) {
        item { CareerTimelineSection(experiences = experiences) }
    }
}

@Composable
private fun CommitteesTabContent(
    committees: List<com.goveye.app.domain.model.Committee>,
    onCommitteeClick: (Int) -> Unit = {}
) {
    if (committees.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not currently serving on any committees",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp)
        ) {
            item { CommitteeChipsSection(committees = committees, onCommitteeClick = onCommitteeClick) }
        }
    }
}

@Composable
private fun ProfileStatsTabContent(
    memberId: Int,
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: RebellionStats?,
    allDivisionDates: List<String>,
    activityScore: com.goveye.app.domain.stats.ActivityScore?,
    traitBars: List<com.goveye.app.domain.stats.TraitBar>,
    onNavigateToDivision: (Int, Int) -> Unit,
    onNavigateToVotingRecord: (Int) -> Unit
) {
    // Compute chart data
    val monthlyVoting = remember(memberVotes) { VotingStatsCalculator.computeMonthlyVoting(memberVotes) }
    val attendanceTrend = remember(memberVotes, allDivisionDates) {
        VotingStatsCalculator.computeAttendanceTrend(memberVotes, allDivisionDates)
    }
    val rebellionTrend = remember(rebellionStats, memberVotes) {
        rebellionStats?.let { VotingStatsCalculator.computeRebellionTrend(it.rebellionInstances, memberVotes) }
            ?: emptyList()
    }
    val voteMapTiles = remember(rebellionStats, memberVotes) {
        rebellionStats?.let { VoteMapCalculator.compute(memberVotes, it.rebellionInstances) } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Activity score strip
        if (activityScore != null) {
            item {
                com.goveye.app.ui.components.stats.ActivityScoreStrip(
                    score = activityScore
                )
            }
        }

        // Trait radar chart
        if (traitBars.isNotEmpty()) {
            item {
                com.goveye.app.ui.components.stats.TraitRadarChart(
                    traitBars = traitBars
                )
            }
        }

        // Summary header
        if (memberVotes.isNotEmpty()) {
            item {
                VotesSummaryCard(
                    totalVotes = memberVotes.size,
                    rebellionStats = rebellionStats
                )
            }
        }

        // Charts section — each chart renders its own card with title
        if (monthlyVoting.isNotEmpty()) {
            item {
                VotingBarChart(data = monthlyVoting)
            }
        }

        if (attendanceTrend.isNotEmpty()) {
            item {
                AttendanceLineChart(data = attendanceTrend)
            }
        }

        if (rebellionTrend.isNotEmpty()) {
            item {
                RebellionLineChart(data = rebellionTrend)
            }
        }

        // Vote map — wrapped in a card
        if (voteMapTiles.isNotEmpty()) {
            item {
                com.goveye.app.ui.components.charts.ChartCard {
                    com.goveye.app.ui.components.charts.ChartHeaderWithLegend(
                        title = "Vote Map",
                        legendItems = listOf(
                            "With party" to com.goveye.app.ui.components.VoteColors.aye,
                            "Rebel" to com.goveye.app.ui.components.VoteColors.no,
                            "No vote" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    com.goveye.app.ui.components.stats.VoteMapGrid(
                        tiles = voteMapTiles,
                        onTileClick = onNavigateToDivision
                    )
                }
            }
        }

        // Recent votes summary — 5 compact rows + "See all" link (D-08)
        if (memberVotes.isNotEmpty()) {
            item {
                RecentVotesSummary(
                    recentVotes = memberVotes.take(5),
                    onSeeAll = { onNavigateToVotingRecord(memberId) },
                    onNavigateToDivision = onNavigateToDivision
                )
            }
        }

        if (memberVotes.isEmpty()) {
            item {
                com.goveye.app.ui.components.charts.ChartCard {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No voting data available yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // Voting history removed — see Activity tab for vote-by-vote list
    }
}

@Composable
private fun VotesSummaryCard(totalVotes: Int, rebellionStats: RebellionStats?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Voting Record",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Votes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalVotes.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (rebellionStats != null) {
                Column {
                    Text(
                        text = "Rebellion Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(rebellionStats.rebellionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rebellionStats.rebellionRate > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Column {
                    Text(
                        text = "Rebellions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${rebellionStats.rebellionCount}/${rebellionStats.totalDivisionsVoted}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteRecordRow(vote: MemberVoteWithDivision, isRebel: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vote badge — theme-aware Aye (teal) / No (orange) colors
        val ayeColor = com.goveye.app.ui.components.VoteColors.aye
        val noColor = com.goveye.app.ui.components.VoteColors.no
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (vote.vote == VoteType.AYE) ayeColor else noColor
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (vote.vote == VoteType.AYE) "Aye" else "No",
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vote.divisionTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDivisionDate(vote.divisionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isRebel) {
            Text(
                text = "Rebel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}

/**
 * Compact "Recent votes" summary for the Stats tab (D-08).
 *
 * Shows up to 5 recent votes as compact rows (division title, aye/no badge,
 * date) with a "See all" link that navigates to [VotingRecordScreen].
 */
@Composable
private fun RecentVotesSummary(
    recentVotes: List<MemberVoteWithDivision>,
    onSeeAll: () -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit
) {
    com.goveye.app.ui.components.charts.ChartCard {
        Text(
            text = "Recent votes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            recentVotes.forEach { vote ->
                RecentVoteRow(
                    vote = vote,
                    onClick = { onNavigateToDivision(vote.divisionId, vote.house) }
                )
            }
        }
        TextButton(
            onClick = onSeeAll,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecentVoteRow(vote: MemberVoteWithDivision, onClick: () -> Unit) {
    val ayeColor = com.goveye.app.ui.components.VoteColors.aye
    val noColor = com.goveye.app.ui.components.VoteColors.no
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact aye/no badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (vote.vote) {
                        VoteType.AYE -> ayeColor
                        VoteType.NO -> noColor
                        VoteType.NO_VOTE_RECORDED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (vote.vote) {
                    VoteType.AYE -> "Aye"
                    VoteType.NO -> "No"
                    VoteType.NO_VOTE_RECORDED -> "—"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vote.divisionTitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDivisionDate(vote.divisionDate),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
