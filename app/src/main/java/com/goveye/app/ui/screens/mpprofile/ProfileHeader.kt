package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parseMutedPartyColor

@Composable
fun ProfileHeader(
    mp: Mp,
    modifier: Modifier = Modifier,
) {
    val partyColor = parseMutedPartyColor(mp.party?.backgroundColour)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(partyColor),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(MaterialTheme.padding.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            MpAvatar(
                thumbnailUrl = mp.thumbnailUrl,
                displayName = mp.nameDisplayAs,
                partyColorHex = mp.party?.backgroundColour,
                size = 96.dp,
            )
            Text(
                text = mp.nameDisplayAs,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = mp.party?.name ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = mp.constituency?.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            mp.membershipStartDate?.let { startDate ->
                val tenureText = "Since ${startDate.take(4)}"
                Text(
                    text = tenureText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}
