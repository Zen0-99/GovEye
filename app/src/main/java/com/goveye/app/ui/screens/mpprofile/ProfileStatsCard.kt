package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.theme.padding
import java.time.LocalDate
import java.time.Period

@Composable
fun ProfileStatsCard(
    mp: Mp,
    modifier: Modifier = Modifier,
) {
    val yearsInParliament = mp.membershipStartDate?.let { startDate ->
        try {
            val start = LocalDate.parse(startDate.take(10))
            Period.between(start, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    val stats = buildList {
        yearsInParliament?.let { add("Years in\nParliament" to "$it") }
        mp.constituency?.name?.let { add("Constituency" to it) }
    }

    if (stats.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            stats.forEach { (label, value) ->
                StatItem(
                    label = label,
                    value = value,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
