package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding

@Composable
fun RelatedMpsSection(
    samePartyMps: List<Mp>,
    committeePeerMps: List<Mp>,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (samePartyMps.isEmpty() && committeePeerMps.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        if (samePartyMps.isNotEmpty()) {
            RelatedMpsRow(
                title = "Same Party Colleagues",
                mps = samePartyMps,
                onNavigateToProfile = onNavigateToProfile
            )
        }
        if (committeePeerMps.isNotEmpty()) {
            RelatedMpsRow(
                title = "Committee Peers",
                mps = committeePeerMps,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun RelatedMpsRow(title: String, mps: List<Mp>, onNavigateToProfile: (Int) -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.padding.large,
                vertical = MaterialTheme.padding.small
            )
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = MaterialTheme.padding.large),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            items(mps, key = { it.id }) { mp ->
                RelatedMpCard(
                    mp = mp,
                    onClick = { onNavigateToProfile(mp.id) }
                )
            }
        }
    }
}

@Composable
private fun RelatedMpCard(mp: Mp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MpAvatar(
            thumbnailUrl = mp.thumbnailUrl,
            displayName = mp.nameDisplayAs,
            partyColorHex = mp.party?.backgroundColour,
            size = 56.dp,
            borderWidth = 2.dp
        )
        Text(
            text = mp.nameDisplayAs,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall)
        )
    }
}
