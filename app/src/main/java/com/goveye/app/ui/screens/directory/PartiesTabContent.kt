package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.utils.partyLogoResId

@Composable
fun PartiesTabContent(parties: List<PartySummary>, onNavigateToParty: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (parties.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No parties available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        items(parties, key = { it.partyId }) { party ->
            PartyCard(
                party = party,
                onClick = { onNavigateToParty(party.partyId) }
            )
        }
    }
}

@Composable
private fun PartyCard(party: PartySummary, onClick: () -> Unit) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(party.partyBackgroundColour))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val foregroundColor = try {
        Color(android.graphics.Color.parseColor(party.partyForegroundColour))
    } catch (e: Exception) {
        Color.White
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo or abbreviation fallback
            partyLogoResId(party.partyId)?.let { resId ->
                androidx.compose.foundation.Image(
                    painter = painterResource(resId),
                    contentDescription = party.partyName,
                    modifier = Modifier.height(32.dp)
                )
            } ?: Text(
                text = party.partyAbbreviation,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = foregroundColor
            )

            Text(
                text = party.partyName,
                style = MaterialTheme.typography.labelMedium,
                color = foregroundColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "${party.seats} MPs",
                style = MaterialTheme.typography.labelSmall,
                color = foregroundColor.copy(alpha = 0.8f)
            )
        }
    }
}
