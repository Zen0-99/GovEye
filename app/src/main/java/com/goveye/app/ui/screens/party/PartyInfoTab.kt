package com.goveye.app.ui.screens.party

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.domain.model.PartyLeaderDetail
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding

@Composable
fun PartyInfoTab(
    party: PartySummary?,
    stats: PartyStatsEntity?,
    modifier: Modifier = Modifier,
    leader: PartyLeaderDetail? = null,
    onNavigateToProfile: (Int) -> Unit = {}
) {
    val partyId = party?.partyId
    val history = partyId?.let { getPartyHistory(it) }

    Column(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        // Party Leader section — shown above Description
        leader?.let {
            SectionTitle("Party Leader")
            InfoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile(it.memberId) }
                ) {
                    MpAvatar(
                        thumbnailUrl = it.thumbnailUrl,
                        displayName = it.name,
                        partyColorHex = it.partyBackgroundColour,
                        size = 56.dp,
                        borderWidth = 2.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val subtitleParts = mutableListOf<String>()
                        it.age?.let { age -> subtitleParts.add("$age years old") }
                        subtitleParts.add(it.constituency)
                        Text(
                            text = subtitleParts.joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        it.leaderSinceLabel?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Description section — title outside the card
        stats?.description?.let {
            SectionTitle("Description")
            InfoCard {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Key Facts section — title outside the card
        val hasKeyFacts = stats?.foundedYear != null || stats?.leaderName != null || stats?.lastElectionYear != null
        if (hasKeyFacts) {
            SectionTitle("Key Facts")
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.foundedYear?.let { FactRow("Founded", it) }
                    stats.leaderName?.let { FactRow("Leader", it) }
                    stats.lastElectionYear?.let {
                        val electionText = buildString {
                            append(it.toString())
                            stats.lastElectionSeats?.let { seats ->
                                append("  ·  $seats seats")
                            }
                            stats.lastElectionVoteShare?.let { share ->
                                val pct = (share * 100).toInt()
                                append("  ·  $pct% vote share")
                            }
                        }
                        FactRow("Last Election", electionText)
                    }
                }
            }
        }

        // History section — title outside the card
        history?.history?.let {
            SectionTitle("History")
            InfoCard {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Party Colour section — title outside the card
        history?.colourExplanation?.let {
            SectionTitle("Party Colour")
            InfoCard {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.padding.small,
            vertical = 4.dp
        )
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium)
        ) {
            content()
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f)
        )
    }
}
