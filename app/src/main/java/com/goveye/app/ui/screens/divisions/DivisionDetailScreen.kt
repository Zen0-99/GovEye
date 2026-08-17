package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    fun load(divisionId: Int, house: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // Observe division
            votesRepository.observeDivision(divisionId).collect { result ->
                val division = result.data
                if (division != null) {
                    // Fetch votes if not cached
                    val voteResult = votesRepository.observeVotesForDivision(divisionId)
                    voteResult.collect { voteRes ->
                        if (voteRes.data.isEmpty()) {
                            // Fetch full division detail with voter lists
                            votesRepository.refreshDivisionDetail(divisionId, house)
                        }
                        val breakdown = votesRepository.getPartyBreakdown(divisionId)
                        _state.value = DivisionDetailState(
                            division = division,
                            votes = voteRes.data,
                            partyBreakdown = breakdown,
                            isLoading = false,
                        )
                    }
                    return@collect
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Division",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
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
            val division = state.division ?: return@Scaffold
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                item {
                    DivisionHeader(division = division)
                }

                // Party breakdown
                if (state.partyBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = "Party Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(state.partyBreakdown, key = { it.partyName }) { party ->
                        PartyBreakdownRow(party = party)
                    }
                }

                // Ayes list
                if (state.votes.any { it.vote == VoteType.AYE }) {
                    item {
                        Text(
                            text = "Ayes (${state.votes.count { it.vote == VoteType.AYE }})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(
                        state.votes.filter { it.vote == VoteType.AYE },
                        key = { "aye-${it.memberId}" },
                    ) { vote ->
                        VoterRow(vote = vote, onClick = { onNavigateToProfile(vote.memberId) })
                    }
                }

                // Noes list
                if (state.votes.any { it.vote == VoteType.NO }) {
                    item {
                        Text(
                            text = "Noes (${state.votes.count { it.vote == VoteType.NO }})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    items(
                        state.votes.filter { it.vote == VoteType.NO },
                        key = { "no-${it.memberId}" },
                    ) { vote ->
                        VoterRow(vote = vote, onClick = { onNavigateToProfile(vote.memberId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DivisionHeader(division: Division) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = division.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDivisionDate(division.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (division.house == 2) "Lords" else "Commons",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Result bar
        val total = division.ayeCount + division.noCount
        if (total > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(6.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(division.ayeCount.toFloat() / total)
                            .background(MaterialTheme.colorScheme.primary)
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = division.ayeCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(division.noCount.toFloat() / total)
                            .background(MaterialTheme.colorScheme.error)
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = division.noCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyBreakdownRow(party: PartyBreakdown) {
    val total = party.ayeCount + party.noCount
    if (total == 0) return
    val ayeFraction = party.ayeCount.toFloat() / total
    val fallbackColor = MaterialTheme.colorScheme.primary
    val partyColor = remember(party.partyColour) {
        try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${party.partyColour}")) }
        catch (e: Exception) { fallbackColor }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = party.partyName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.3f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .weight(0.6f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        ) {
            Row(
                modifier = Modifier
                    .weight(ayeFraction)
                    .background(partyColor)
                    .fillMaxSize(),
            ) {}
            Row(
                modifier = Modifier
                    .weight(1f - ayeFraction)
                    .background(partyColor.copy(alpha = 0.4f))
                    .fillMaxSize(),
            ) {}
        }
        Text(
            text = "${party.ayeCount}-${party.noCount}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.1f),
        )
    }
}

@Composable
private fun VoterRow(vote: DivisionVote, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
        val partyName = vote.partyName
        if (!partyName.isNullOrBlank()) {
            Text(
                text = partyName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
