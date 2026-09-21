package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.ConstituencyElection
import com.goveye.app.domain.model.ElectionCandidate
import com.goveye.app.domain.model.MpElectionResults
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.partyColorForId
import java.text.NumberFormat

/**
 * Career-tab "Election Results" section (D-05).
 *
 * Renders a headline card with the latest result for the MP's current
 * seat, then one card per election the MP personally contested
 * (candidate.memberId == memberId) with the full candidate table.
 *
 * Hidden entirely when the election tables hold nothing for this MP
 * (e.g. installs that haven't received the member-details patch yet).
 * Self-contained: candidate rows are text only — no MP navigation.
 */
@Composable
fun ElectionResultsSection(memberId: Int, results: MpElectionResults, modifier: Modifier = Modifier) {
    if (results.contested.isEmpty() && results.currentSeatLatest == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.medium)
    ) {
        Text(
            text = "Election Results",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Headline: latest result for the MP's current seat (context).
        // The same election may also appear in the contested list below —
        // kept deliberately (headline = compact summary, list = full table).
        results.currentSeatLatest?.let { latest ->
            Spacer(modifier = Modifier.height(MaterialTheme.padding.small))
            CurrentSeatCard(memberId = memberId, election = latest)
        }

        results.contested.forEach { election ->
            Spacer(modifier = Modifier.height(MaterialTheme.padding.small))
            ContestedElectionCard(memberId = memberId, election = election)
        }
    }
}

@Composable
private fun CurrentSeatCard(memberId: Int, election: ConstituencyElection) {
    val mpCandidate = election.candidates.firstOrNull { it.memberId == memberId }
    val mpWon = mpCandidate?.isWinner == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (mpWon) {
                            val seat = election.constituencyName ?: "the seat"
                            election.majority?.let { "Won $seat by ${formatCount(it)}" } ?: "Won $seat"
                        } else {
                            election.constituencyName ?: election.electionTitle ?: "Latest result"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtitle = listOfNotNull(
                        election.electionTitle,
                        election.formattedDate.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val result = election.result
                if (!result.isNullOrBlank()) {
                    ElectionChip(
                        text = result,
                        partyId = election.winningPartyId,
                        colourHex = election.winningPartyColour
                    )
                }
            }

            // Majority is already in the headline when the MP won — only
            // repeat it for seats won by someone else.
            val detail = listOfNotNull(
                if (!mpWon) election.majority?.let { "Majority ${formatCount(it)}" } else null,
                election.winningPartyName,
                election.turnout?.let { "Turnout ${formatCount(it)}" }
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (election.isNotional) {
                NotionalBadge()
            }
        }
    }
}

@Composable
private fun ContestedElectionCard(memberId: Int, election: ConstituencyElection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = election.electionTitle ?: "Election",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtitle = listOfNotNull(
                        election.constituencyName,
                        election.formattedDate.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val result = election.result
                if (!result.isNullOrBlank()) {
                    ElectionChip(
                        text = result,
                        partyId = election.winningPartyId,
                        colourHex = election.winningPartyColour
                    )
                }
            }

            if (election.isNotional) {
                NotionalBadge()
            }

            election.candidates.sortedBy { it.rankOrder }.forEach { candidate ->
                CandidateRow(candidate = candidate, isThisMp = candidate.memberId == memberId)
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: ElectionCandidate, isThisMp: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isThisMp) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val partyLabel = candidate.partyAbbreviation?.takeIf { it.isNotBlank() }
            ?: candidate.partyName?.takeIf { it.isNotBlank() }
        if (partyLabel != null) {
            ElectionChip(
                text = partyLabel,
                partyId = candidate.partyId,
                colourHex = candidate.partyColour
            )
        }

        Text(
            text = candidate.name ?: "Unknown candidate",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isThisMp) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (candidate.isWinner) {
            Text(
                text = "Won",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isThisMp) {
            Text(
                text = "This MP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        val trailing = listOfNotNull(
            candidate.resultChange?.takeIf { it.isNotBlank() },
            candidate.votes?.let { formatCount(it) }
        ).joinToString(" · ")
        if (trailing.isNotBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Party-pill style chip (result strings and party abbreviations). */
@Composable
private fun ElectionChip(text: String, partyId: Int?, colourHex: String?) {
    val fallback = MaterialTheme.colorScheme.surfaceVariant
    val colour = if (colourHex.isNullOrBlank()) {
        fallback
    } else {
        runCatching { partyColorForId(partyId, colourHex) }.getOrDefault(fallback)
    }
    val textColour = if (colour == fallback) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if (colour.luminance() > 0.5f) {
        Color(0xFF1A1A1A)
    } else {
        Color.White
    }
    Surface(shape = RoundedCornerShape(50), color = colour) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColour,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NotionalBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Notional — boundary change",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatCount(v: Int): String = NumberFormat.getIntegerInstance().format(v)
