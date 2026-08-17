package com.goveye.app.ui.screens.mpprofile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.VotingStatsCalculator
import com.goveye.app.ui.components.charts.AttendanceLineChart
import com.goveye.app.ui.components.charts.RebellionLineChart
import com.goveye.app.ui.components.charts.VotingBarChart
import com.goveye.app.ui.theme.LocalPartyAccent
import com.goveye.app.ui.theme.partyAccentColorScheme
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
    VOTES("Votes"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { ProfileTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (uiState.isLoading && uiState.mp == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.mp == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("MP not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val mp = uiState.mp!!
            val partyColor = remember(mp) { parsePartyColor(mp.party?.backgroundColour) }
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val partyScheme = partyAccentColorScheme(partyColor, isDark)

            CompositionLocalProvider(LocalPartyAccent provides partyColor) {
                MaterialTheme(colorScheme = partyScheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Fixed header — does not scroll
                            ProfileHeader(
                                mp = mp,
                                onBack = onBack,
                            )

                            // Fixed tabs — indicator and selected text use party color
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                ProfileTab.entries.forEachIndexed { index, tab ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(tab.title) },
                            )
                        }
                    }

                    // Pager — each page has its own scrollable LazyColumn
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { page ->
                        when (ProfileTab.entries[page]) {
                            ProfileTab.PROFILE -> ProfileTabContent(
                                mp = mp,
                                synopsis = uiState.synopsis,
                                contacts = uiState.contacts,
                                samePartyMps = uiState.samePartyMps,
                                committeePeerMps = uiState.committeePeerMps,
                                activityScore = uiState.activityScore,
                                traitBars = uiState.traitBars,
                                onNavigateToProfile = onNavigateToProfile,
                            )
                            ProfileTab.CAREER -> CareerTabContent(
                                experiences = uiState.experiences,
                            )
                            ProfileTab.COMMITTEES -> CommitteesTabContent(
                                committees = uiState.committees,
                            )
                            ProfileTab.VOTES -> VotesTabContent(
                                memberVotes = uiState.memberVotes,
                                rebellionStats = uiState.rebellionStats,
                                onNavigateToDivision = { divisionId, house ->
                                    onNavigateToDivision(divisionId, house)
                                },
                            )
                        }
                    }
                }

                // Animated collapsed header — fades in when scrolled
                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp,
                    ) {
                        TopAppBar(
                            title = { Text(mp.nameDisplayAs) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                }
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
    activityScore: com.goveye.app.domain.stats.ActivityScore?,
    traitBars: List<com.goveye.app.domain.stats.TraitBar>,
    onNavigateToProfile: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item {
            ProfileStatsCard(
                mp = mp,
            )
        }
        // Activity score strip
        if (activityScore != null) {
            item {
                com.goveye.app.ui.components.stats.ActivityScoreStrip(
                    score = activityScore,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        // Trait bars
        if (traitBars.isNotEmpty()) {
            item {
                com.goveye.app.ui.components.stats.TraitBarsSection(
                    traitBars = traitBars,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item { BioSection(synopsis = synopsis) }
        item { ContactSection(contacts = contacts) }
        item {
            RelatedMpsSection(
                samePartyMps = samePartyMps,
                committeePeerMps = committeePeerMps,
                onNavigateToProfile = onNavigateToProfile,
            )
        }
    }
}

@Composable
private fun CareerTabContent(
    experiences: List<com.goveye.app.domain.model.BiographyExperience>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item { CareerTimelineSection(experiences = experiences) }
    }
}

@Composable
private fun CommitteesTabContent(
    committees: List<com.goveye.app.domain.model.Committee>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item { CommitteeChipsSection(committees = committees) }
    }
}

@Composable
private fun VotesTabContent(
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: RebellionStats?,
    onNavigateToDivision: (Int, Int) -> Unit,
) {
    // Compute chart data
    val monthlyVoting = remember(memberVotes) { VotingStatsCalculator.computeMonthlyVoting(memberVotes) }
    val attendanceTrend = remember(memberVotes) { VotingStatsCalculator.computeAttendanceTrend(memberVotes) }
    val rebellionTrend = remember(rebellionStats, memberVotes) {
        rebellionStats?.let { VotingStatsCalculator.computeRebellionTrend(it.rebellionInstances, memberVotes) } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary header
        if (memberVotes.isNotEmpty()) {
            item {
                VotesSummaryCard(
                    totalVotes = memberVotes.size,
                    rebellionStats = rebellionStats,
                )
            }
        }

        // Charts section
        if (monthlyVoting.isNotEmpty()) {
            item {
                ChartSectionLabel("Voting Pattern")
                VotingBarChart(data = monthlyVoting)
            }
        }

        if (attendanceTrend.isNotEmpty()) {
            item {
                ChartSectionLabel("Attendance Rate")
                AttendanceLineChart(data = attendanceTrend)
            }
        }

        if (rebellionTrend.isNotEmpty()) {
            item {
                ChartSectionLabel("Rebellion Trend")
                RebellionLineChart(data = rebellionTrend)
            }
        }

        if (memberVotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No voting data available yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(memberVotes, key = { it.divisionId }) { vote ->
                VoteRecordRow(
                    vote = vote,
                    isRebel = rebellionStats?.rebellionInstances?.any { it.divisionId == vote.divisionId } == true,
                    onClick = { onNavigateToDivision(vote.divisionId, vote.house) },
                )
            }
        }
    }
}

@Composable
private fun VotesSummaryCard(
    totalVotes: Int,
    rebellionStats: RebellionStats?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Voting Record",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Total Votes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = totalVotes.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (rebellionStats != null) {
                Column {
                    Text(
                        text = "Rebellion Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(rebellionStats.rebellionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rebellionStats.rebellionRate > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                Column {
                    Text(
                        text = "Rebellions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${rebellionStats.rebellionCount}/${rebellionStats.totalDivisionsVoted}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteRecordRow(
    vote: MemberVoteWithDivision,
    isRebel: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vote badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (vote.vote == VoteType.AYE) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (vote.vote == VoteType.AYE) "Aye" else "No",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vote.divisionTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDivisionDate(vote.divisionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isRebel) {
            Text(
                text = "Rebel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChartSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

private fun formatDivisionDate(dateString: String): String {
    return try {
        val parts = dateString.split("T").first().split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        dateString
    }
}
