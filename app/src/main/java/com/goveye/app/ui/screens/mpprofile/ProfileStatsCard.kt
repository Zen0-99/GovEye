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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.OfficerIdentity
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.utils.formatDob
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@Composable
fun ProfileStatsCard(
    mp: Mp,
    bioData: BioDataEntity? = null,
    officerIdentity: OfficerIdentity? = null,
    edmSponsoredCount: Int = 0,
    edmSignedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val yearsInParliament = mp.membershipStartDate?.let { startDate ->
        try {
            val start = LocalDate.parse(startDate.take(10))
            Period.between(start, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    val maidenSpeechFormatted = bioData?.maidenSpeechDate?.let { date ->
        try {
            val parsed = LocalDate.parse(date.take(10))
            parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        } catch (e: Exception) {
            null
        }
    }

    val birthDateFormatted = formatDob(bioData?.dateOfBirth)

    // Stats: label on top, value below. Order: Year of Birth, Maiden Speech,
    // Years in Parliament.
    val stats = buildList {
        birthDateFormatted?.let { add("Year of\nBirth" to it) }
        maidenSpeechFormatted?.let { add("Maiden\nSpeech" to it) }
        yearsInParliament?.let { add("Years in\nParliament" to "$it years") }
        officerIdentity?.nationality?.takeIf { it.isNotBlank() }?.let { add("Nationality" to it) }
        officerIdentity?.countryOfResidence?.takeIf { it.isNotBlank() }?.let { add("Residence" to it) }
        if (edmSponsoredCount > 0) add("EDMs\nSponsored" to "$edmSponsoredCount")
        if (edmSignedCount > 0) add("EDMs\nSigned" to "$edmSignedCount")
    }

    // Wider than just stats: an MP whose only CH fact is a disqualification
    // must still render the card so the flag isn't swallowed.
    if (stats.isEmpty() && officerIdentity?.isDisqualified != true) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = com.goveye.app.ui.components.cardSurfaceColor()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // ≤4 stats per row — Nationality/Residence push past the
            // original 3, so a second row appears only when needed.
            stats.chunked(4).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowStats.forEach { (label, value) ->
                        StatItem(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (officerIdentity?.isDisqualified == true) {
                Text(
                    text = "Barred from acting as a company director",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
