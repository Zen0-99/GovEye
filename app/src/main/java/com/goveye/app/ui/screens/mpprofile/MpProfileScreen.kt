package com.goveye.app.ui.screens.mpprofile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.data.local.entity.MpTagEntity
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.VoteMapCalculator
import com.goveye.app.domain.stats.VotingStatsCalculator
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DelayedSpinner
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
    onNavigateToProfile: (Int, com.goveye.app.ui.navigation.MpHeaderFallback?, Int) -> Unit = { _, _, _ -> },
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToInterestBucket: (Int, String) -> Unit = { _, _ -> },
    onNavigateToExpenseBucket: (Int, String) -> Unit = { _, _ -> },
    onNavigateToParty: (Int) -> Unit = {},
    onNavigateToCommittee: (Int) -> Unit = {},
    onNavigateToVotingRecord: (Int) -> Unit = {},
    onNavigateToMpTagBrowse: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
    // Optimistic header fallback data — used to render the header instantly
    // before Stage 1 DB load completes. null when navigating from places
    // that don't have this data (e.g. deep links).
    fallbackName: String? = null,
    fallbackPartyName: String? = null,
    fallbackPartyColor: String? = null,
    fallbackThumbnailUrl: String? = null,
    fallbackConstituency: String? = null,
    fallbackActivityScore: Float? = null,
    fallbackDateOfBirth: String? = null,
    initialTab: Int = 0,
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

    // Track current tab index for search bar config and filter sheet.
    // Initialized from initialTab so the search bar config matches the
    // visible tab on first frame (e.g. Interests tab when expanding from
    // the finances microview).
    var currentPage by remember { mutableIntStateOf(initialTab) }

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
    // Use displayMp for party color so the gradient renders from the
    // optimistic fallback data before the real mp loads.
    val partyColor = remember(mp, fallbackPartyColor) {
        parsePartyColor(mp?.party?.backgroundColour ?: fallbackPartyColor)
    }

    // Optimistic header: if we have fallback data from the source screen,
    // build a temporary Mp so the header (gradient, avatar, name, party pill,
    // activity score) renders instantly. When Stage 1 completes, the real mp
    // replaces this.
    val optimisticMp = if (mp == null && fallbackName != null) {
        com.goveye.app.domain.model.Mp(
            id = memberId,
            nameListAs = fallbackName,
            nameDisplayAs = fallbackName,
            nameFullTitle = null,
            gender = null,
            party = fallbackPartyName?.let {
                com.goveye.app.domain.model.Party(0, it, "", fallbackPartyColor ?: "", "")
            },
            constituency = fallbackConstituency?.let {
                com.goveye.app.domain.model.Constituency(0, it)
            },
            house = 1,
            membershipStartDate = null,
            isActive = true,
            thumbnailUrl = fallbackThumbnailUrl
        )
    } else {
        null
    }

    // Use the real mp if available, otherwise the optimistic fallback.
    val displayMp = mp ?: optimisticMp
    // Compute age from bioData.dateOfBirth (Stage 1) or fallback DOB
    // (optimistic header) so age appears instantly with the MP image.
    val bioDataAge = (uiState.bioData?.dateOfBirth ?: fallbackDateOfBirth)?.let { dob ->
        try {
            val birth = java.time.LocalDate.parse(dob.take(10))
            java.time.Period.between(birth, java.time.LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }
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
            accentColor = if (displayMp != null) partyColor else null,
            iconTint = if (displayMp != null) headerIconTint else null,
            actions = if (displayMp != null) {
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

    // Staggered domino fade-in for tab page content. Each section fades in
    // with a slight delay after the previous one (top-to-bottom cascade).
    // The tab bar itself (text + UI) is part of the optimistic header and
    // shows instantly. Header elements (name, party, photo, gradient,
    // activity score, age) all appear instantly from fallback data.
    val isOptimistic = optimisticMp != null && mp == null
    val contentAlpha by animateFloatAsState(
        targetValue = if (isOptimistic) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "contentFade"
    )

    if (uiState.isLoading && displayMp == null) {
        DelayedSpinner(modifier = modifier)
    } else if (displayMp == null) {
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
                        mp = displayMp,
                        isDark = isDark,
                        age = bioDataAge,
                        // Use real activity score if loaded, otherwise the
                        // fallback from the source screen so the score pill
                        // appears at the same time as the MP image.
                        activityScore = uiState.activityScore
                            ?: fallbackActivityScore?.let {
                                com.goveye.app.domain.stats.ActivityScore(
                                    score = it,
                                    breakdown = com.goveye.app.domain.stats.ScoreBreakdown(
                                        voteParticipationContribution = 0f,
                                        questionsContribution = 0f,
                                        speechesContribution = 0f,
                                        committeesContribution = 0f
                                    )
                                )
                            },
                        contentTopPadding = contentTopPadding,
                        onNavigateToParty = onNavigateToParty
                    )

                    // Sub-tab pager — shared component for all tabbed screens.
                    // Handles instant jump, only-compose-current-page, and
                    // pre-loaded data pattern. The tab bar (text + UI) is
                    // part of the optimistic header and shows instantly.
                    // Only the page content fades in with contentAlpha.
                    com.goveye.app.ui.components.SubTabPager(
                        tabs = ProfileTab.entries.map { com.goveye.app.ui.components.SubTab(label = it.title) },
                        onPageChange = { currentPage = it },
                        initialPage = initialTab,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        Box(modifier = Modifier.graphicsLayer { alpha = contentAlpha }) {
                            when (ProfileTab.entries[page]) {
                                ProfileTab.PROFILE -> ProfileTabContent(
                                    mp = displayMp,
                                    synopsis = uiState.synopsis,
                                    contacts = uiState.contacts,
                                    samePartyMps = uiState.samePartyMps,
                                    committeePeerMps = uiState.committeePeerMps,
                                    bioData = uiState.bioData,
                                    mpLinks = uiState.mpLinks,
                                    mpTags = uiState.mpTags,
                                    traitBars = uiState.traitBars,
                                    onNavigateToProfile = onNavigateToProfile,
                                    onNavigateToMpTagBrowse = onNavigateToMpTagBrowse,
                                    contentVisible = !isOptimistic
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
                                    },
                                    partyColorHex = displayMp.party?.backgroundColour
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
                                    onToDateChange = { interestToDate = it },
                                    partyColorHex = displayMp.party?.backgroundColour
                                )
                            }
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
                partyColor = partyColor,
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
    age: Int? = null,
    activityScore: com.goveye.app.domain.stats.ActivityScore? = null,
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
            // Avatar with optional activity score pill (FotMob-style)
            Box {
                MpAvatar(
                    thumbnailUrl = mp.thumbnailUrl,
                    displayName = mp.nameDisplayAs,
                    partyColorHex = mp.party?.backgroundColour,
                    size = 60.dp,
                    borderWidth = 2.dp
                )
                if (activityScore != null) {
                    // Score pill — party-colored, top-right of avatar
                    val scorePillColor = if (isDark) {
                        partyColor.copy(alpha = 0.95f)
                    } else {
                        Color(
                            red = partyColor.red * 0.8f,
                            green = partyColor.green * 0.8f,
                            blue = partyColor.blue * 0.8f,
                            alpha = 0.95f
                        )
                    }
                    val scorePillTextColor = if (scorePillColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scorePillColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = String.format("%.1f", activityScore.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = scorePillTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = mp.nameDisplayAs,
                        style = MaterialTheme.typography.titleLarge,
                        color = headerTextColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (age != null) {
                        Text(
                            text = "($age)",
                            style = MaterialTheme.typography.titleMedium,
                            color = headerTextColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                // Party pill + constituency inline
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillColor,
                        modifier = Modifier.clickable {
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
                    mp.constituency?.name?.let { constituencyName ->
                        Text(
                            text = constituencyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = headerTextColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    mpTags: List<MpTagEntity> = emptyList(),
    @Suppress("UNUSED_PARAMETER") traitBars: List<com.goveye.app.domain.stats.TraitBar> = emptyList(),
    onNavigateToProfile: (Int, com.goveye.app.ui.navigation.MpHeaderFallback?, Int) -> Unit,
    onNavigateToMpTagBrowse: (String) -> Unit = {},
    contentVisible: Boolean = true
) {
    // Staggered domino alpha: each section gets a 60ms delay after the
    // previous one, creating a top-to-bottom cascade when content loads.
    val target = if (contentVisible) 1f else 0f
    val alpha0 by animateFloatAsState(target, tween(250, delayMillis = 0), label = "s0")
    val alpha1 by animateFloatAsState(target, tween(250, delayMillis = 50), label = "s1")
    val alpha2 by animateFloatAsState(target, tween(250, delayMillis = 100), label = "s2")
    val alpha3 by animateFloatAsState(target, tween(250, delayMillis = 150), label = "s3")
    val alpha4 by animateFloatAsState(target, tween(250, delayMillis = 200), label = "s4")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp)
    ) {
        item {
            Box(modifier = Modifier.graphicsLayer { alpha = alpha0 }) {
                TopicsSection(
                    tags = mpTags,
                    onTagClick = onNavigateToMpTagBrowse
                )
            }
        }
        item {
            Box(modifier = Modifier.graphicsLayer { alpha = alpha1 }) {
                Column {
                    SectionHeader("History")
                    ProfileStatsCard(
                        mp = mp,
                        bioData = bioData
                    )
                }
            }
        }
        item {
            Box(modifier = Modifier.graphicsLayer { alpha = alpha2 }) {
                Column {
                    if (!synopsis.isNullOrBlank()) {
                        SectionHeader("Biography")
                    }
                    BioSection(synopsis = synopsis)
                }
            }
        }
        item {
            Box(modifier = Modifier.graphicsLayer { alpha = alpha3 }) {
                Column {
                    SectionHeader("Contact")
                    ContactSection(contacts = contacts, socialLinks = mpLinks)
                }
            }
        }
        item {
            Box(modifier = Modifier.graphicsLayer { alpha = alpha4 }) {
                RelatedMpsSection(
                    samePartyMps = samePartyMps,
                    committeePeerMps = committeePeerMps,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.padding.large,
            vertical = MaterialTheme.padding.small
        )
    )
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
