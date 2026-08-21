package com.goveye.app.ui.screens.party

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.ui.theme.padding

@Composable
fun PartyStatsTab(party: PartySummary?, stats: PartyStatsEntity?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        party?.let {
            StatCard("Seats in Parliament", "${it.seats} / 650")
            val seatProgress = it.seats.toFloat() / 650f
            LinearProgressIndicator(
                progress = { seatProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        stats?.lastElectionVoteShare?.let { voteShare ->
            StatCard("Vote Share at Last Election", "${String.format("%.1f", voteShare * 100)}%")
            LinearProgressIndicator(
                progress = { voteShare },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        stats?.lastElectionSeats?.let {
            StatCard("Seats Won at Last Election", "$it")
        }

        stats?.foundedYear?.let {
            StatCard("Founded", it)
        }

        stats?.leaderName?.let {
            StatCard("Current Leader", it)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
