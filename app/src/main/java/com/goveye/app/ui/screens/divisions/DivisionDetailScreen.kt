package com.goveye.app.ui.screens.divisions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Public
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
import com.goveye.app.ui.components.DelayedSpinner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Theme-aware vote colors
private val AyeColor @Composable get() = com.goveye.app.ui.components.VoteColors.aye
private val NoColor @Composable get() = com.goveye.app.ui.components.VoteColors.no

data class DivisionDetailState(
    val division: Division? = null,
    val votes: List<DivisionVote> = emptyList(),
    val partyBreakdown: List<PartyBreakdown> = emptyList(),
    val speechCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DivisionDetailViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
    private val tagDao: com.goveye.app.data.local.dao.TagDao
) : ViewModel() {

    private val _state = MutableStateFlow(DivisionDetailState(isLoading = true))
    val state: StateFlow<DivisionDetailState> = _state.asStateFlow()

    private var loadedDivisionId: Int? = null

    fun load(divisionId: Int, house: Int) {
        if (loadedDivisionId == divisionId) return
        loadedDivisionId = divisionId

        // Phase 1: Load initial data synchronously using first() to avoid
        // the brief loading spinner. The bundled DB is local, so this is
        // fast (single Room query on a background thread).
        viewModelScope.launch {
            val divisionResult = votesRepository.observeDivision(divisionId).first()
            val division = divisionResult.data
            if (division == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val votes = votesRepository.getVotesForDivision(divisionId)
            val breakdown = votesRepository.getPartyBreakdown(divisionId)
            val speechCount = votesRepository.countSpeechesForDivision(divisionId)
            val tags = tagDao.getTagsForDivision(divisionId)
            _state.value = DivisionDetailState(
                division = division,
                votes = votes,
                partyBreakdown = breakdown,
                speechCount = speechCount,
                tags = tags,
                isLoading = false
            )
        }

        // Phase 2: Observe for ongoing updates (patches, sync, etc.)
        // Skip the first emission since we already loaded it above.
        viewModelScope.launch {
            votesRepository.observeDivision(divisionId).drop(1).collect { divisionResult ->
                val division = divisionResult.data ?: return@collect
                val votes = votesRepository.getVotesForDivision(divisionId)
                val breakdown = votesRepository.getPartyBreakdown(divisionId)
                val speechCount = votesRepository.countSpeechesForDivision(divisionId)
                val tags = tagDao.getTagsForDivision(divisionId)
                _state.value = _state.value.copy(
                    division = division,
                    votes = votes,
                    partyBreakdown = breakdown,
                    speechCount = speechCount,
                    tags = tags
                )
            }
        }

        // Separately observe votes as a flow - re-emits when votes are
        // upserted (even if the division entity itself doesn't change).
        viewModelScope.launch {
            votesRepository.observeVotesForDivision(divisionId).collect {
                val current = _state.value
                if (current.division != null && !current.isLoading) {
                    val votes = votesRepository.getVotesForDivision(divisionId)
                    val breakdown = votesRepository.getPartyBreakdown(divisionId)
                    _state.value = current.copy(
                        votes = votes,
                        partyBreakdown = breakdown
                    )
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
    onNavigateToTranscript: (divisionId: Int, divisionTitle: String, speechGid: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: DivisionDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(divisionId) {
        viewModel.load(divisionId, house)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var voteFilter by remember { mutableStateOf(VoteFilter.ALL) }
    var microviewMemberId by remember { mutableStateOf<Int?>(null) }
    var microviewFallback by remember {
        mutableStateOf<MicroviewFallback?>(null)
    }

    val ayeCount = state.votes.count { it.vote == VoteType.AYE }
    val noCount = state.votes.count { it.vote == VoteType.NO }

    // Configure the global search bar for division detail:
    // back button on the left, segmented pill (All / Ayes / Noes) below
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = searchQuery,
            placeholder = "Search voters / constituency…",
            onQueryChange = { searchQuery = it },
            onBack = {
                isSearchActive = false
                searchQuery = ""
                onBack()
            },
            isSearchActive = isSearchActive,
            onSearchActiveChange = { active ->
                isSearchActive = active
                if (!active) searchQuery = ""
            },
            segments = listOf(
                com.goveye.app.ui.components.SearchSegment(
                    label = "All (${ayeCount + noCount})",
                    isSelected = voteFilter == VoteFilter.ALL,
                    onClick = { voteFilter = VoteFilter.ALL }
                ),
                com.goveye.app.ui.components.SearchSegment(
                    label = "Ayes ($ayeCount)",
                    isSelected = voteFilter == VoteFilter.AYE,
                    onClick = { voteFilter = VoteFilter.AYE }
                ),
                com.goveye.app.ui.components.SearchSegment(
                    label = "Noes ($noCount)",
                    isSelected = voteFilter == VoteFilter.NO,
                    onClick = { voteFilter = VoteFilter.NO }
                )
            )
        )
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        if (state.isLoading) {
            DelayedSpinner(modifier = Modifier.padding(innerPadding))
        } else {
            val division = state.division
            if (division == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Division not found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                DivisionDetailContent(
                    division = division,
                    state = state,
                    onNavigateToProfile = { vote ->
                        microviewMemberId = vote.memberId
                        microviewFallback = MicroviewFallback(
                            name = vote.memberName,
                            partyName = vote.partyName,
                            partyColour = vote.partyColour,
                            constituency = vote.constituencyName
                        )
                    },
                    onNavigateToTranscript = onNavigateToTranscript,
                    searchQuery = searchQuery,
                    voteFilter = voteFilter,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
        }
    }

    // MP microview dialog — shown when clicking an MP in the voter list
    microviewMemberId?.let { memberId ->
        val fb = microviewFallback
        MpMicroviewDialog(
            memberId = memberId,
            fallbackName = fb?.name ?: "",
            fallbackPartyName = fb?.partyName,
            fallbackPartyColour = fb?.partyColour,
            fallbackConstituency = fb?.constituency,
            onNavigateToFullProfile = { id ->
                microviewMemberId = null
                microviewFallback = null
                onNavigateToProfile(id)
            },
            onDismiss = {
                microviewMemberId = null
                microviewFallback = null
            }
        )
    }
}

/** Fallback data for the microview dialog (from DivisionVote or transcript speech). */
internal data class MicroviewFallback(
    val name: String,
    val partyName: String?,
    val partyColour: String?,
    val constituency: String?
)

@Composable
private fun DivisionDetailContent(
    division: Division,
    state: DivisionDetailState,
    onNavigateToProfile: (DivisionVote) -> Unit,
    onNavigateToTranscript: (Int, String, String) -> Unit,
    searchQuery: String,
    voteFilter: VoteFilter,
    modifier: Modifier = Modifier
) {
    var breakdownExpanded by remember { mutableStateOf(true) }

    val ayes = state.votes.filter { it.vote == VoteType.AYE }
    val noes = state.votes.filter { it.vote == VoteType.NO }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card (always visible — contains the main result bar)
        item {
            DivisionHeaderCard(
                division = division,
                speechCount = state.speechCount,
                tags = state.tags,
                onNavigateToTranscript = {
                    onNavigateToTranscript(division.id, division.title, "")
                }
            )
        }

        // Party breakdown — collapsible. Hidden while searching so the user
        // only sees the people matching their query (the main result bar in
        // the header card remains visible).
        if (state.partyBreakdown.isNotEmpty() && searchQuery.isBlank()) {
            item {
                SectionHeader(
                    title = "Party Breakdown",
                    expanded = breakdownExpanded,
                    onToggle = { breakdownExpanded = !breakdownExpanded }
                )
            }
            if (breakdownExpanded) {
                items(
                    state.partyBreakdown.sortedByDescending { it.ayeCount + it.noCount },
                    key = { it.partyName }
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
                item(key = "header-$partyName-$voteFilter") {
                    PartyGroupHeader(
                        partyName = partyName,
                        count = partyVoters.size,
                        partyColour = partyVoters.firstOrNull()?.partyColour
                    )
                }
                items(partyVoters, key = { "$voteFilter-${it.memberId}" }) { vote ->
                    VoterRow(vote = vote, onClick = { onNavigateToProfile(vote) })
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No voters match \"$searchQuery\""
                        } else {
                            "No voters in this category"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

enum class VoteFilter { ALL, AYE, NO }

@Composable
private fun DivisionHeaderCard(
    division: Division,
    speechCount: Int = 0,
    tags: List<String> = emptyList(),
    onNavigateToTranscript: () -> Unit = {}
) {
    // TWFY debate URL — stored in the DB at build time (scraped from the
    // TWFY division page). Falls back to the TWFY division page URL if not
    // available (e.g. older DB without the column populated).
    val context = androidx.compose.ui.platform.LocalContext.current
    val twfyUrl = division.twfyDebateUrl?.takeIf { it.isNotBlank() } ?: run {
        val dateOnly = division.date.substringBefore('T')
        if (division.number != null) {
            val houseName = if (division.house == 2) "lords" else "commons"
            "https://www.theyworkforyou.com/divisions/pw-$dateOnly-${division.number}-$houseName"
        } else {
            "https://www.theyworkforyou.com/search/?s=" +
                java.net.URLEncoder.encode(division.title, "UTF-8")
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = division.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Globe icon — opens the TWFY debate page (with speeches)
                IconButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(twfyUrl)
                        )
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = "View debate on TheyWorkForYou",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDivisionDate(division.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (division.house == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Result bar with numbers inside
            val total = division.ayeCount + division.noCount
            if (total > 0) {
                ResultBarWithNumbers(
                    ayeCount = division.ayeCount,
                    noCount = division.noCount,
                    barHeight = 32.dp
                )
            }
            // View Transcript button — only shown if debate speeches exist
            // in the bundled DB for this division.
            if (speechCount > 0) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = onNavigateToTranscript,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "View Transcript ($speechCount speeches)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Tag pills — horizontally scrollable row under the transcript button.
            // Same style as feed cards and directory debate cards.
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        val shape = RoundedCornerShape(20.dp)
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBarWithNumbers(ayeCount: Int, noCount: Int, barHeight: androidx.compose.ui.unit.Dp) {
    val total = ayeCount + noCount
    if (total == 0) return
    val ayeFraction = ayeCount.toFloat() / total
    val noFraction = noCount.toFloat() / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (ayeFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(ayeFraction)
                    .background(AyeColor)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ayeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (noFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(noFraction)
                    .background(NoColor)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = noCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Party name + total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = party.partyName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Bar with numbers inside
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            if (ayeFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(ayeFraction)
                        .background(AyeColor)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (ayeFraction > 0.15f) {
                        Text(
                            text = party.ayeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
                    contentAlignment = Alignment.Center
                ) {
                    if (noFraction > 0.15f) {
                        Text(
                            text = party.noCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyGroupHeader(partyName: String, count: Int, partyColour: String?) {
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = "$partyName ($count)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vote indicator — rounded square showing Aye/No
        VoteBadge(vote = vote.vote)

        Text(
            text = vote.memberName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val constituency = vote.constituencyName
        if (!constituency.isNullOrBlank()) {
            Text(
                text = constituency,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Rounded-square vote badge. Shows "Aye" on a green background or "No" on a
 * red background so the user can see at a glance how a member voted. Sits to
 * the left of the member name in [VoterRow]. Matches the rounded-square tile
 * style used in the VoteMapGrid (official votes view).
 */
@Composable
private fun VoteBadge(vote: VoteType) {
    val (label, color) = when (vote) {
        VoteType.AYE -> "Aye" to AyeColor
        VoteType.NO -> "No" to NoColor
        VoteType.NO_VOTE_RECORDED -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
