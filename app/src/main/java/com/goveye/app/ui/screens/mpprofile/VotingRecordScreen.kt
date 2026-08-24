package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.DivisionWeightCalculator
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.SearchBarConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for [VotingRecordScreen] — a standalone screen showing an MP's
 * full voting record with weight scores, rebellion indicators, search, and
 * infinite scroll.
 *
 * This is a direct relocation of the former Activity tab content (D-07) —
 * not a redesign. All weight score and rebellion logic is preserved.
 */
data class VotingRecordUiState(
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    val searchQuery: String = "",
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val totalCount: Int = 0
)

/**
 * ViewModel for [VotingRecordScreen].
 *
 * Loads an MP's voting record with pagination and search, plus rebellion
 * stats. Has its own back stack entry (standalone screen, not a tab), so
 * it cannot share [ProfileViewModel].
 */
@HiltViewModel
class VotingRecordViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
    private val membersRepository: MembersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingRecordUiState())
    val uiState: StateFlow<VotingRecordUiState> = _uiState.asStateFlow()

    private val pageSize = 30

    /**
     * Load the first page of votes + rebellion stats for the given MP.
     * Called when the screen is first shown.
     */
    fun loadVotingRecord(memberId: Int) {
        viewModelScope.launch {
            // Load first page of votes
            val query = _uiState.value.searchQuery
            val (votes, total) = if (query.isBlank()) {
                votesRepository.getPagedMemberVoting(memberId, pageSize, 0) to
                    votesRepository.countVotesForMember(memberId)
            } else {
                votesRepository.searchPagedMemberVoting(memberId, query, pageSize, 0) to
                    votesRepository.countSearchVotesForMember(memberId, query)
            }
            _uiState.value = _uiState.value.copy(
                memberVotes = votes,
                totalCount = total,
                hasMore = votes.size < total,
                isLoadingMore = false
            )

            // Load rebellion stats in parallel — depends on party name
            launch {
                val rebellionStats = runCatching {
                    val mpResult = membersRepository.observeMp(memberId).first()
                    val partyName = mpResult.data?.party?.name ?: return@launch
                    val memberVotes = votesRepository.getMemberVotes(memberId)
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    val partyVoteCounts = votesRepository.getPartyVoteCounts(divisionIds, partyName)
                    RebellionCalculator.computeAggregated(memberVotes, partyVoteCounts)
                }.getOrNull()
                _uiState.value = _uiState.value.copy(rebellionStats = rebellionStats)
            }
        }
    }

    /**
     * Load the next page of votes (infinite scroll).
     */
    fun loadMore(memberId: Int) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            val offset = state.memberVotes.size
            val query = state.searchQuery
            val more = if (query.isBlank()) {
                votesRepository.getPagedMemberVoting(memberId, pageSize, offset)
            } else {
                votesRepository.searchPagedMemberVoting(memberId, query, pageSize, offset)
            }
            _uiState.value = _uiState.value.copy(
                memberVotes = state.memberVotes + more,
                hasMore = (offset + more.size) < state.totalCount,
                isLoadingMore = false
            )
        }
    }

    /**
     * Update the search query and reload from page 0.
     */
    fun updateSearchQuery(memberId: Int, query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadVotingRecord(memberId)
    }
}

/**
 * Full voting record screen — direct relocation of the former Activity tab
 * content (D-07). Shows votes with weight score badges, rebellion indicators,
 * search, and infinite scroll.
 *
 * Accessible from the Stats tab "Recent votes" summary via "See all".
 */
@Composable
fun VotingRecordScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VotingRecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadVotingRecord(memberId)
    }

    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = "Voting Record",
            onBack = onBack
        )
    )

    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            query = uiState.searchQuery,
            placeholder = "Search votes...",
            onQueryChange = { viewModel.updateSearchQuery(memberId, it) }
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Result count
        if (uiState.totalCount > 0) {
            item {
                Text(
                    text = "${uiState.totalCount} vote${if (uiState.totalCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (uiState.memberVotes.isEmpty() && !uiState.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isBlank()) {
                            "No votes found"
                        } else {
                            "No votes found for \"${uiState.searchQuery}\""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.memberVotes, key = { it.divisionId }) { vote ->
                val isRebellion = isRebellionDivision(vote.divisionId, uiState.rebellionStats)
                val total = vote.ayeCount + vote.noCount
                val closeness = if (total > 0) {
                    1.0 - kotlin.math.abs(vote.ayeCount - vote.noCount).toDouble() / total
                } else {
                    0.0
                }
                val weight = DivisionWeightCalculator.compute(
                    mpVote = vote.vote,
                    isRebellion = isRebellion,
                    divisionCloseness = closeness
                )
                ActivityRow(
                    vote = vote,
                    isRebellion = isRebellion,
                    score = weight.score,
                    onClick = { onNavigateToDivision(vote.divisionId, vote.house) }
                )
            }

            // Load more trigger + loading indicator
            if (uiState.hasMore) {
                item {
                    LaunchedEffect(uiState.memberVotes.size) {
                        viewModel.loadMore(memberId)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (uiState.memberVotes.isNotEmpty()) {
                item {
                    Text(
                        text = "End of voting record",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(vote: MemberVoteWithDivision, isRebellion: Boolean, score: Double, onClick: () -> Unit) {
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
        // Vote badge — Aye (teal) / No (orange), No-vote-recorded shows "—"
        val ayeColor = com.goveye.app.ui.components.VoteColors.aye
        val noColor = com.goveye.app.ui.components.VoteColors.no
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
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
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatActivityDate(vote.divisionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (vote.house == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (isRebellion) {
                    Text(
                        text = "Rebel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        WeightBadge(score = score)
    }
}

/**
 * Numeric weight badge — color-coded green/yellow/red per D-08.
 */
@Composable
private fun WeightBadge(score: Double, modifier: Modifier = Modifier) {
    val color = when {
        score >= 7.0 -> Color(0xFF2E7D32)
        score >= 4.0 -> Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = String.format("%.1f", score),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun isRebellionDivision(divisionId: Int, rebellionStats: RebellionStats?): Boolean =
    rebellionStats?.rebellionInstances?.any { it.divisionId == divisionId } == true

private fun formatActivityDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
