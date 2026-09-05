package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.partyColorForId
import com.goveye.app.ui.utils.partyLogoResId

@Composable
fun PartiesTabContent(
    parties: List<PartySummary>,
    onNavigateToParty: (Int) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    rulingPartyId: Int? = null
) {
    val filtered = remember(parties, searchQuery) {
        if (searchQuery.isBlank()) {
            parties
        } else {
            parties.filter { it.partyName.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No parties found for \"$searchQuery\""
                } else {
                    "No parties available"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Split into Government (ruling party) and Other Parties.
    // When searching, skip the section split — just show a flat grid.
    val showSections = searchQuery.isBlank() && rulingPartyId != null
    val rulingParty = if (showSections) filtered.find { it.partyId == rulingPartyId } else null
    val otherParties = if (showSections) filtered.filter { it.partyId != rulingPartyId } else filtered

    if (!showSections || rulingParty == null) {
        // Flat 2-column grid (search results or no ruling party identified)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.medium
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            items(filtered, key = { it.partyId }) { party ->
                PartyCard(
                    party = party,
                    onClick = { onNavigateToParty(party.partyId) }
                )
            }
        }
        return
    }

    // Sectioned layout: Government (full-width) + Other Parties (2-col grid)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        // Section header: Government
        item(key = "header_government") {
            SectionHeader("Government")
        }
        // Ruling party — full-width card
        item(key = "ruling_${rulingParty.partyId}") {
            PartyCard(
                party = rulingParty,
                onClick = { onNavigateToParty(rulingParty.partyId) }
            )
        }
        // Section header: Other Parties
        item(key = "header_other") {
            SectionHeader("Other Parties")
        }
        // Other parties — 2-column grid rendered as chunked rows
        val chunked = otherParties.chunked(2)
        items(chunked, key = { row -> row.joinToString(",") { it.partyId.toString() } }) { rowParties ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowParties.forEach { party ->
                    PartyCard(
                        party = party,
                        onClick = { onNavigateToParty(party.partyId) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill the remaining slot if the row has only 1 party
                if (rowParties.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
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
private fun PartyCard(party: PartySummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PartyCardShared(
        partyId = party.partyId,
        partyName = party.partyName,
        partyBackgroundColour = party.partyBackgroundColour,
        seats = party.seats,
        selected = false,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Shared party card composable — used by both the Directory's Parties tab
 * and the onboarding Parties step. 110dp height, party color gradient
 * background, party logo at left, name + MP count at right.
 * When [selected] is true, a 2dp partyColor border is shown.
 */
@Composable
fun PartyCardShared(
    partyId: Int,
    partyName: String,
    partyBackgroundColour: String,
    seats: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partyColor = partyColorForId(partyId, partyBackgroundColour)
    val borderColor = if (selected) partyColor else Color.Transparent
    val borderWidth = if (selected) 2.dp else 0.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = partyColor.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            partyColor.copy(alpha = 0.6f),
                            partyColor.copy(alpha = 0.25f)
                        )
                    )
                )
                .padding(MaterialTheme.padding.medium)
        ) {
            // Logo — positioned at the left side.
            partyLogoResId(partyId)?.let { resId ->
                Image(
                    painter = painterResource(resId),
                    contentDescription = partyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(56.dp)
                )
            }

            // Party name + MP count, aligned to the right of the logo.
            Column(
                modifier = Modifier
                    .padding(start = if (partyLogoResId(partyId) != null) 12.dp else 0.dp)
            ) {
                Text(
                    text = partyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$seats MP${if (seats != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
