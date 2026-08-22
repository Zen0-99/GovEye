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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
    STATS("Stats"),
    ACTIVITY("Activity"),
    INTERESTS("Interests")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToInterestBucket: (Int, String) -> Unit = { _, _ -> },
    onNavigateToParty: (Int) -> Unit = {},
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

    val pagerState = rememberPagerState(pageCount = { ProfileTab.entries.size })
    val coroutineScope = rememberCoroutineScope()

    // Configure the shell's detail top bar (Miko-style shared toolbar).
    // No title — the name lives in the content header beside the avatar.
    // Icons are white to be readable over the party gradient that extends
    // from the top of the screen behind the top bar.
    val mp = uiState.mp
    val partyColor = remember(mp) { parsePartyColor(mp?.party?.backgroundColour) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)

    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = "",
            onBack = onBack,
            iconTint = if (mp != null) headerIconTint else null,
            actions = if (mp != null) {
                listOf(
                    DetailTopBarAction(
                        icon = if (uiState.notificationsEnabled) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.Notifications
                        },
                        contentDescription = "Notification settings",
                        onClick = { showNotificationSheet = true },
                        tint = headerIconTint
                    ),
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

                    // Scrollable tabs
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        edgePadding = 16.dp
                    ) {
                        ProfileTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = {
                                    Text(
                                        text = tab.title,
                                        color = if (pagerState.currentPage == index) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pager — each page has its own scrollable LazyColumn
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
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
                                activityScore = uiState.activityScore,
                                traitBars = uiState.traitBars,
                                onNavigateToProfile = onNavigateToProfile
                            )

                            ProfileTab.CAREER -> CareerTabContent(
                                experiences = uiState.experiences
                            )

                            ProfileTab.COMMITTEES -> CommitteesTabContent(
                                committees = uiState.committees
                            )

                            ProfileTab.STATS -> ProfileStatsTabContent(
                                memberVotes = uiState.memberVotes,
                                rebellionStats = uiState.rebellionStats,
                                allDivisionDates = uiState.allDivisionDates,
                                activityScore = uiState.activityScore,
                                traitBars = uiState.traitBars,
                                onNavigateToDivision = { divisionId, house ->
                                    onNavigateToDivision(divisionId, house)
                                }
                            )

                            ProfileTab.ACTIVITY -> ActivityTabContent(
                                memberVotes = uiState.activityVotes,
                                rebellionStats = uiState.rebellionStats,
                                allVotesByDivision = uiState.allVotesByDivision,
                                memberPartyName = uiState.memberPartyName,
                                searchQuery = uiState.activitySearchQuery,
                                isLoadingMore = uiState.activityIsLoadingMore,
                                hasMore = uiState.activityHasMore,
                                totalCount = uiState.activityTotalCount,
                                onSearchQueryChange = { query ->
                                    viewModel.updateActivitySearchQuery(memberId, query)
                                },
                                onLoadMore = {
                                    viewModel.loadMoreActivityVotes(memberId)
                                },
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
                                }
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
                onDismiss = { showNotificationSheet = false }
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        partyColor.copy(alpha = if (isDark) 0.85f else 0.7f),
                        partyColor.copy(alpha = if (isDark) 0.3f else 0.2f),
                        Color.Transparent
                    )
                )
            )
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
    activityScore: com.goveye.app.domain.stats.ActivityScore? = null,
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
        // Activity score strip (trait radar moved to Stats tab)
        if (activityScore != null) {
            item {
                com.goveye.app.ui.components.stats.ActivityScoreStrip(
                    score = activityScore
                )
            }
        }
        item { BioSection(synopsis = synopsis) }
        item { SocialLinksRow(links = mpLinks) }
        item { ContactSection(contacts = contacts) }
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
private fun CommitteesTabContent(committees: List<com.goveye.app.domain.model.Committee>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp)
    ) {
        item { CommitteeChipsSection(committees = committees) }
    }
}

@Composable
private fun ProfileStatsTabContent(
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: RebellionStats?,
    allDivisionDates: List<String>,
    activityScore: com.goveye.app.domain.stats.ActivityScore?,
    traitBars: List<com.goveye.app.domain.stats.TraitBar>,
    onNavigateToDivision: (Int, Int) -> Unit
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
