package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.goveye.app.ui.theme.parseMutedPartyColor
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
    // Muted party color — the same desaturated variant used in gradients/pills
    val mutedColor = parseMutedPartyColor(party.partyBackgroundColour)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = mutedColor.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            mutedColor.copy(alpha = 0.25f),
                            mutedColor.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            // Gigantic logo — positioned at bottom-start, oversized so it clips
            // the left and bottom edges of the card. The right and top of the
            // logo are fully visible. Wakely puzzle-card style.
            partyLogoResId(party.partyId)?.let { resId ->
                Image(
                    painter = painterResource(resId),
                    contentDescription = party.partyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(130.dp)
                        .offset(
                            x = (-20).dp, // clip left edge
                            y = (-20).dp // clip bottom edge
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
                    text = party.partyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${party.seats} MP${if (party.seats != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
