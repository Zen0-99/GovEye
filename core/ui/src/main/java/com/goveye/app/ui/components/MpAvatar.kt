package com.goveye.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.goveye.app.ui.theme.deriveInitials
import com.goveye.app.ui.theme.parseMutedPartyColor

@Composable
fun MpAvatar(
    thumbnailUrl: String?,
    displayName: String,
    partyColorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val partyColor = parseMutedPartyColor(partyColorHex)
    val initials = deriveInitials(displayName)

    if (thumbnailUrl != null) {
        SubcomposeAsyncImage(
            model = thumbnailUrl,
            contentDescription = displayName,
            modifier = modifier.size(size).clip(CircleShape),
            loading = {
                InitialsAvatar(initials, partyColor, Modifier.size(size))
            },
            error = {
                InitialsAvatar(initials, partyColor, Modifier.size(size))
            },
        )
    } else {
        InitialsAvatar(initials, partyColor, modifier.size(size))
    }
}

@Composable
private fun InitialsAvatar(
    initials: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(CircleShape).background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
