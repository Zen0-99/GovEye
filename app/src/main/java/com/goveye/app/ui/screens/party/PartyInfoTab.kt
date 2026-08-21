package com.goveye.app.ui.screens.party

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PartyInfoTab(party: PartySummary?, stats: PartyStatsEntity?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        if (party != null) {
            InfoCard("Party Name", party.partyName)
            InfoCard("Abbreviation", party.partyAbbreviation)
            InfoCard("Seats", "${party.seats} MPs")
        }

        stats?.description?.let {
            InfoCard("Description", it)
        }
        stats?.foundedYear?.let {
            InfoCard("Founded", it)
        }
        stats?.leaderName?.let {
            InfoCard("Leader", it)
        }
        stats?.lastElectionYear?.let {
            InfoCard("Last Election", it.toString())
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
