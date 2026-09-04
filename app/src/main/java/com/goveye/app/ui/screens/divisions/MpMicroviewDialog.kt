package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.VoteMapCalculator
import com.goveye.app.domain.stats.VotingStatsCalculator
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.charts.AttendanceLineChart
import com.goveye.app.ui.components.charts.ChartCard
import com.goveye.app.ui.components.charts.ChartHeaderWithLegend
import com.goveye.app.ui.components.charts.RebellionLineChart
import com.goveye.app.ui.components.charts.VotingBarChart
import com.goveye.app.ui.components.stats.VoteMapGrid
import com.goveye.app.ui.screens.mpprofile.InterestsTabContent
import com.goveye.app.ui.theme.parsePartyColor

/**
 * Which content to show in the microview dialog.
 * - [VOTES]: Voting charts + recent votes list (default — used from division detail)
 * - [FINANCES]: Interests + expense bucket totals (used from feed financial cards).
 *   Cards show totals but are not clickable — the user must open the full profile
 *   to drill into individual entries.
 */
enum class MpMicroviewMode { VOTES, FINANCES }

/**
 * MP microview dialog — shown when clicking an MP in the division detail
 * voter list. Shows a compact profile with gradient header, avatar, name,
 * party, follow button, and the votes tab content (charts + vote list).
 *
 * A "fullscreen" button navigates to the full profile screen.
 *
 * Uses [MpMicroviewViewModel] which shows fallback data from the DivisionVote
 * immediately (name, party, constituency) and loads the full profile + voting
 * record in the background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpMicroviewDialog(
    memberId: Int,
    fallbackName: String,
    fallbackPartyName: String?,
    fallbackPartyColour: String?,
    fallbackConstituency: String?,
    onNavigateToFullProfile: (Int) -> Unit,
    onDismiss: () -> Unit,
    mode: MpMicroviewMode = MpMicroviewMode.VOTES,
    viewModel: MpMicroviewViewModel = hiltViewModel()
) {
    LaunchedEffect(memberId) {
        viewModel.load(memberId, fallbackName, fallbackPartyName, fallbackPartyColour, fallbackConstituency)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Gradient header (compact version of ProfileHeader)
                val mp = uiState.mp
                if (mp != null) {
                    val partyColor = parsePartyColor(mp.party?.backgroundColour)
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)
                    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)

                    Box(
                        modifier = Modifier
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Controls row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Close",
                                        tint = headerIconTint
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Follow button
                                    IconButton(
                                        onClick = { viewModel.toggleFollow(memberId) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isFollowing) {
                                                Icons.Outlined.PersonRemove
                                            } else {
                                                Icons.Outlined.PersonAdd
                                            },
                                            contentDescription = if (uiState.isFollowing) "Unfollow" else "Follow",
                                            tint = headerIconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // Full profile button
                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToFullProfile(memberId)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Fullscreen,
                                            contentDescription = "View full profile",
                                            tint = headerIconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Identity row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MpAvatar(
                                    thumbnailUrl = mp.thumbnailUrl,
                                    displayName = mp.nameDisplayAs,
                                    partyColorHex = mp.party?.backgroundColour,
                                    size = 48.dp,
                                    borderWidth = 2.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mp.nameDisplayAs,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = headerTextColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!mp.party?.name.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isDark) {
                                                partyColor.copy(alpha = 0.9f)
                                            } else {
                                                Color(
                                                    red = partyColor.red * 0.8f,
                                                    green = partyColor.green * 0.8f,
                                                    blue = partyColor.blue * 0.8f,
                                                    alpha = 0.85f
                                                )
                                            },
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = mp.party?.name ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color.White else Color(0xFF1A1A1A),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Content — votes or finances depending on mode
                if (uiState.isLoading && uiState.mp == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (mode) {
                        MpMicroviewMode.VOTES -> VotesContent(
                            memberVotes = uiState.memberVotes,
                            rebellionStats = uiState.rebellionStats,
                            allDivisionDates = uiState.allDivisionDates,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        MpMicroviewMode.FINANCES -> FinancesContent(
                            interests = uiState.interests,
                            expenseBucketTotals = uiState.expenseBucketTotals,
                            partyColorHex = uiState.mp?.party?.backgroundColour,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Votes content for the microview — charts (voting bar, attendance, rebellion,
 * vote map) followed by the recent votes list. Reuses the same chart components
 * as the full profile's Votes tab.
 */
@Composable
private fun VotesContent(
    memberVotes: List<MemberVoteWithDivision>,
    rebellionStats: com.goveye.app.domain.stats.RebellionStats?,
    allDivisionDates: List<String>,
    modifier: Modifier = Modifier
) {
    if (memberVotes.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No voting record",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Compute chart data (same as VotesTabContent in MpProfileScreen)
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
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Charts section — same order as full profile Votes tab
        if (monthlyVoting.isNotEmpty()) {
            item { VotingBarChart(data = monthlyVoting) }
        }

        if (attendanceTrend.isNotEmpty()) {
            item { AttendanceLineChart(data = attendanceTrend) }
        }

        if (rebellionTrend.isNotEmpty()) {
            item { RebellionLineChart(data = rebellionTrend) }
        }

        if (voteMapTiles.isNotEmpty()) {
            item {
                ChartCard {
                    ChartHeaderWithLegend(
                        title = "Vote Map",
                        legendItems = listOf(
                            "With party" to VoteColors.aye,
                            "Rebel" to VoteColors.no,
                            "No vote" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    VoteMapGrid(
                        tiles = voteMapTiles,
                        onTileClick = { _, _ -> }
                    )
                }
            }
        }

        // Recent votes list
        items(memberVotes.take(20)) { voteRecord ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vote badge
                val (label, color) = when (voteRecord.vote) {
                    VoteType.AYE -> "Aye" to VoteColors.aye
                    VoteType.NO -> "No" to VoteColors.no
                    VoteType.NO_VOTE_RECORDED -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = voteRecord.divisionTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Finances content for the microview — shows the same dashboard as the
 * Finances tab on the full profile (income bucket summaries + expense bucket
 * totals) but with non-clickable cards. The user must open the full profile
 * to drill into individual entries.
 */
@Composable
private fun FinancesContent(
    interests: List<com.goveye.app.domain.model.Interest>,
    expenseBucketTotals: List<com.goveye.app.data.local.dao.ExpenseBucketTotal>,
    partyColorHex: String?,
    modifier: Modifier = Modifier
) {
    if (interests.isEmpty() && expenseBucketTotals.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No registered financial interests",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    InterestsTabContent(
        memberId = 0,
        interests = interests,
        expenseBucketTotals = expenseBucketTotals,
        onNavigateToBucketDetail = { },
        onNavigateToExpenseBucket = { },
        partyColorHex = partyColorHex,
        modifier = modifier
    )
}
