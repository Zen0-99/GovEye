package com.goveye.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.goveye.app.ui.theme.deriveInitials
import com.goveye.app.ui.theme.parseMutedPartyColor
import com.goveye.app.ui.theme.parsePartyColor

/**
 * MP avatar — circular image with initials fallback.
 *
 * Uses [AsyncImage] (not SubcomposeAsyncImage) for list performance.
 * SubcomposeAsyncImage uses subcomposition per item which is expensive
 * in scrolling lists — AsyncImage renders the placeholder directly and
 * swaps the image in when loaded, avoiding subcomposition overhead.
 */
@Composable
fun MpAvatar(
    thumbnailUrl: String?,
    displayName: String,
    partyColorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 0.dp,
) {
    val partyColor = remember(partyColorHex) { parseMutedPartyColor(partyColorHex) }
    val initials = remember(displayName) { deriveInitials(displayName) }
    val borderModifier = if (borderWidth > 0.dp) {
        val borderColor = remember(partyColorHex) { parsePartyColor(partyColorHex) }
        Modifier.border(borderWidth, borderColor, CircleShape)
    } else {
        Modifier
    }

    if (thumbnailUrl != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val request = remember(thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = displayName,
            modifier = modifier
                .then(borderModifier)
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(modifier = modifier.then(borderModifier).size(size).clip(CircleShape)) {
            InitialsAvatar(initials, partyColor, Modifier.size(size))
        }
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
