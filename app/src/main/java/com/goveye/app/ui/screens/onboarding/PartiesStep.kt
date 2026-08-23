package com.goveye.app.ui.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.theme.parsePartyColor

/**
 * Step 4 — Parties: "Follow parties".
 *
 * LazyVerticalGrid(GridCells.Fixed(2)) of party cards with party color
 * tint, abbreviation badge, seat count. Selected state shows 2dp partyColor
 * border + checkmark.
 *
 * Falls back to a static list of major UK parties when the DB is empty
 * (first launch before seed download completes).
 *
 * Per UI-SPEC Section 3, Step 4.
 */
@Composable
fun PartiesStep(
    selectedParties: Set<Int>,
    parties: List<PartyInfo>,
    onPartyToggle: (Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val partiesToShow = if (parties.isEmpty()) FALLBACK_PARTIES else parties

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Follow parties",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Select parties to track in your feed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Party grid — 2 columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = partiesToShow,
                key = { it.partyId }
            ) { party ->
                PartyCard(
                    party = party,
                    selected = party.partyId in selectedParties,
                    onClick = { onPartyToggle(party.partyId) }
                )
            }

            // Skip for now — at the bottom of the grid, full width
            item(span = { GridItemSpan(maxLineSpan) }, key = "skip") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip for now")
                    }
                }
            }
        }

        // Bottom buttons — Back (weight 1) + Continue (weight 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Back")
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PartyCard(party: PartyInfo, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val partyColor = remember(party.partyBackgroundColour) {
        parsePartyColor(party.partyBackgroundColour)
    }
    val borderColor = if (selected) partyColor else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            partyColor.copy(alpha = 0.12f)
        } else {
            partyColor.copy(alpha = 0.08f)
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Party abbreviation badge — circle, partyColor bg, white text, 32dp
                Surface(
                    shape = CircleShape,
                    color = partyColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = party.partyAbbreviation.take(3),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = party.partyName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${party.seatCount} MPs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Checkmark — top right corner
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = partyColor,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fallback party list used when the DB is empty (first launch before
 * seed download completes). Major UK parties with approximate seat counts.
 */
private val FALLBACK_PARTIES = listOf(
    PartyInfo(0, "Labour", "Lab", "#E4003B", 411),
    PartyInfo(1, "Conservative", "Con", "#0A3B7C", 121),
    PartyInfo(2, "Liberal Democrats", "LD", "#FAA61A", 72),
    PartyInfo(3, "Scottish National Party", "SNP", "#FDF38E", 9),
    PartyInfo(4, "Plaid Cymru", "PC", "#005B54", 4),
    PartyInfo(5, "Green Party", "Green", "#6AB023", 4),
    PartyInfo(6, "Reform UK", "Reform", "#12B6CF", 5),
    PartyInfo(7, "Democratic Unionist Party", "DUP", "#D01906", 5),
    PartyInfo(8, "Sinn Féin", "SF", "#326738", 7),
    PartyInfo(9, "Independent", "Ind", "#808080", 4)
)
