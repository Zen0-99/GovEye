package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor
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
        contentPadding = PaddingValues(
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
    PartyCardShared(
        partyId = party.partyId,
        partyName = party.partyName,
        partyBackgroundColour = party.partyBackgroundColour,
        seats = party.seats,
        selected = false,
        onClick = onClick
    )
}

/**
 * Shared party card composable — used by both the Directory's Parties tab
 * and the onboarding Parties step. 140dp height, party color gradient
 * background, party logo at bottom-end, name + MP count at top-left.
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
    val partyColor = parsePartyColor(partyBackgroundColour)
    val borderColor = if (selected) partyColor else Color.Transparent
    val borderWidth = if (selected) 2.dp else 0.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = partyColor.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            partyColor.copy(alpha = 0.6f),
                            partyColor.copy(alpha = 0.25f)
                        )
                    )
                )
        ) {
            // Logo — positioned at bottom-end, fully visible (no right-edge clip).
            partyLogoResId(partyId)?.let { resId ->
                Image(
                    painter = painterResource(resId),
                    contentDescription = partyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(110.dp)
                        .offset(
                            x = (-5).dp,
                            y = 10.dp
                        )
                )
            }

            // Party name + MP count at the top-left, above the logo
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(MaterialTheme.padding.medium)
            ) {
                Text(
                    text = partyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$seats MP${if (seats != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
