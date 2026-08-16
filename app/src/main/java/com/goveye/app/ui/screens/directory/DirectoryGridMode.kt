package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parseMutedPartyColor

@Composable
fun MpGridCard(
    mp: Mp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partyColor = parseMutedPartyColor(mp.party?.backgroundColour)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(partyColor),
        )
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MpAvatar(
                thumbnailUrl = mp.thumbnailUrl,
                displayName = mp.nameDisplayAs,
                partyColorHex = mp.party?.backgroundColour,
                size = 72.dp,
            )
            Text(
                text = mp.nameDisplayAs,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = MaterialTheme.padding.small),
            )
            Text(
                text = mp.party?.abbreviation ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
