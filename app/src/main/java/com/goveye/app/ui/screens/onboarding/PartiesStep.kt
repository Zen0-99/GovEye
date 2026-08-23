package com.goveye.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.screens.directory.PartyCardShared

/**
 * Step 3 — Parties: "Follow parties".
 *
 * Uses the same [PartyCardShared] composable as the Directory's Parties tab
 * — 140dp height, party color gradient background, party logo, name + MP
 * count. Selected state shows 2dp partyColor border.
 *
 * Falls back to a static list of major UK parties (with real MNIS party IDs
 * for logos) when the DB is empty.
 *
 * Per UI-SPEC Section 3, Step 3.
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

        // Party grid — 2 columns, same PartyCardShared as Directory
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
                PartyCardShared(
                    partyId = party.partyId,
                    partyName = party.partyName,
                    partyBackgroundColour = party.partyBackgroundColour,
                    seats = party.seatCount,
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

/**
 * Fallback party list using real MNIS party IDs (for logo mapping).
 * Used when the DB is empty (first launch before seed download).
 * Seat counts are approximate (post-2024 election).
 */
private val FALLBACK_PARTIES = listOf(
    PartyInfo(15, "Labour Party", "Lab", "#E4003B", 411),
    PartyInfo(4, "Conservative Party", "Con", "#0A3B7C", 121),
    PartyInfo(17, "Liberal Democrats", "LD", "#FAA61A", 72),
    PartyInfo(29, "Scottish National Party", "SNP", "#FDF38E", 9),
    PartyInfo(22, "Plaid Cymru", "PC", "#005B54", 4),
    PartyInfo(44, "Green Party", "Green", "#6AB023", 4),
    PartyInfo(1036, "Reform UK", "Reform", "#12B6CF", 5),
    PartyInfo(7, "Democratic Unionist Party", "DUP", "#D01906", 5),
    PartyInfo(31, "Social Democratic & Labour Party", "SDLP", "#326738", 0),
    PartyInfo(1, "Alliance Party", "APNI", "#F6CB2C", 0)
)
