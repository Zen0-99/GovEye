package com.goveye.app.ui.screens.divisions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.PartyBreakdown
import com.goveye.app.domain.model.VoteType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Theme-aware vote colors
private val AyeColor @Composable get() = com.goveye.app.ui.components.VoteColors.aye
private val NoColor @Composable get() = com.goveye.app.ui.components.VoteColors.no

data class DivisionDetailState(
    val division: Division? = null,
    val votes: List<DivisionVote> = emptyList(),
    val partyBreakdown: List<PartyBreakdown> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class DivisionDetailViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DivisionDetailState(isLoading = true))
    val state: StateFlow<DivisionDetailState> = _state.asStateFlow()

    private var loadedDivisionId: Int? = null

    fun load(divisionId: Int, house: Int) {
        if (loadedDivisionId == divisionId) return
        loadedDivisionId = divisionId

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                votesRepository.refreshDivisionDetail(divisionId, house)
            } catch (e: Exception) {
            }
        }

        viewModelScope.launch {
            votesRepository.observeDivision(divisionId).collect { divisionResult ->
                val division = divisionResult.data
                if (division == null) {
                    _state.value = _state.value.copy(isLoading = false)
                    return@collect
                }

                val votes = votesRepository.getVotesForDivision(divisionId)
                val breakdown = votesRepository.getPartyBreakdown(divisionId)
                _state.value = DivisionDetailState(
                    division = division,
                    votes = votes,
                    partyBreakdown = breakdown,
                    isLoading = false,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionDetailScreen(
    divisionId: Int,
    house: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DivisionDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(divisionId) {
        viewModel.load(divisionId, house)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var voteFilter by remember { mutableStateOf(VoteFilter.ALL) }

    val ayeCount = state.votes.count { it.vote == VoteType.AYE }
    val noCount = state.votes.count { it.vote == VoteType.NO }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                // Top row: back button + search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    com.goveye.app.ui.components.FloatingSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search voters / constituency…",
                        filterChips = listOf(
                            com.goveye.app.ui.components.SearchFilterChip(
                                label = "All (${ayeCount + noCount})",
                                isSelected = voteFilter == VoteFilter.ALL,
                                onClick = { voteFilter = VoteFilter.ALL },
                            ),
                            com.goveye.app.ui.components.SearchFilterChip(
                                label = "Ayes ($ayeCount)",
                                isSelected = voteFilter == VoteFilter.AYE,
                                onClick = { voteFilter = VoteFilter.AYE },
                                leadingDotColor = AyeColor,
                            ),
                            com.goveye.app.ui.components.SearchFilterChip(
                                label = "Noes ($noCount)",
                                isSelected = voteFilter == VoteFilter.NO,
                                onClick = { voteFilter = VoteFilter.NO },
                                leadingDotColor = NoColor,
                            ),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val division = state.division
            if (division == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Division not found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                DivisionDetailContent(
                    division = division,
                    state = state,
                    onNavigateToProfile = onNavigateToProfile,
                    searchQuery = searchQuery,
                    voteFilter = voteFilter,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun DivisionDetailContent(
    division: Division,
    state: DivisionDetailState,
    onNavigateToProfile: (Int) -> Unit,
    searchQuery: String,
    voteFilter: VoteFilter,
    modifier: Modifier = Modifier,
) {
    var breakdownExpanded by remember { mutableStateOf(true) }

    val ayes = state.votes.filter { it.vote == VoteType.AYE }
    val noes = state.votes.filter { it.vote == VoteType.NO }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header card
        item { DivisionHeaderCard(division = division) }

        // Party breakdown — collapsible
        if (state.partyBreakdown.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Party Breakdown",
                    expanded = breakdownExpanded,
                    onToggle = { breakdownExpanded = !breakdownExpanded },
                )
            }
            if (breakdownExpanded) {
                items(
                    state.partyBreakdown.sortedByDescending { it.ayeCount + it.noCount },
                    key = { it.partyName },
                ) { party ->
                    PartyBreakdownBar(party = party)
                }
            }
        }

        // Voter list — filtered by search query and vote filter from the top bar
        if (ayes.isNotEmpty() || noes.isNotEmpty()) {
            // Filtered voter list
            val voters = when (voteFilter) {
                VoteFilter.ALL -> ayes + noes
                VoteFilter.AYE -> ayes
                VoteFilter.NO -> noes
            }
            val filtered = if (searchQuery.isBlank()) {
                voters
            } else {
                voters.filter {
                    it.memberName.contains(searchQuery, ignoreCase = true) ||
                    it.constituencyName?.contains(searchQuery, ignoreCase = true) == true
                }
            }

            // Group by party
            val groupedByParty = filtered.groupBy { it.partyName ?: "Unknown" }

            groupedByParty.forEach { (partyName, partyVoters) ->
                item(key = "header-$partyName-${voteFilter}") {
                    PartyGroupHeader(
                        partyName = partyName,
                        count = partyVoters.size,
                        partyColour = partyVoters.firstOrNull()?.partyColour,
                    )
                }
                items(partyVoters, key = { "${voteFilter}-${it.memberId}" }) { vote ->
                    VoterRow(vote = vote, onClick = { onNavigateToProfile(vote.memberId) })
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank())
                            "No voters match \"$searchQuery\""
                        else
                            "No voters in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

enum class VoteFilter { ALL, AYE, NO }

@Composable
private fun DivisionHeaderCard(division: Division) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = division.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDivisionDate(division.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (division.house == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Result bar with numbers inside
            val total = division.ayeCount + division.noCount
            if (total > 0) {
                ResultBarWithNumbers(
                    ayeCount = division.ayeCount,
                    noCount = division.noCount,
                    barHeight = 32.dp,
                )
            }
        }
    }
}

@Composable
private fun ResultBarWithNumbers(
    ayeCount: Int,
    noCount: Int,
    barHeight: androidx.compose.ui.unit.Dp,
) {
    val total = ayeCount + noCount
    if (total == 0) return
    val ayeFraction = ayeCount.toFloat() / total
    val noFraction = noCount.toFloat() / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        if (ayeFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(ayeFraction)
                    .background(AyeColor)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ayeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (noFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(noFraction)
                    .background(NoColor)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = noCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Compact party breakdown bar — single row with party name, inline bar
 * with Aye/No numbers inside each segment, and total on the right.
 */
@Composable
private fun PartyBreakdownBar(party: PartyBreakdown) {
    val total = party.ayeCount + party.noCount
    if (total == 0) return
    val ayeFraction = party.ayeCount.toFloat() / total
    val noFraction = party.noCount.toFloat() / total

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Party name + total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = party.partyName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Bar with numbers inside
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            if (ayeFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(ayeFraction)
                        .background(AyeColor)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (ayeFraction > 0.15f) {
                        Text(
                            text = party.ayeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (noFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(noFraction)
                        .background(NoColor)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (noFraction > 0.15f) {
                        Text(
                            text = party.noCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyGroupHeader(
    partyName: String,
    count: Int,
    partyColour: String?,
) {
    val fallback = MaterialTheme.colorScheme.onSurfaceVariant
    val color = remember(partyColour) {
        try {
            partyColour?.let { Color(android.graphics.Color.parseColor("#$it")) } ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = "$partyName ($count)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun VoterRow(vote: DivisionVote, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = vote.memberName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val constituency = vote.constituencyName
        if (!constituency.isNullOrBlank()) {
            Text(
                text = constituency,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDivisionDate(dateString: String): String {
    return try {
        val parts = dateString.split("T").first().split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        dateString
    }
}
