package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor

@Composable
fun ProfileHeader(
    mp: Mp,
    modifier: Modifier = Modifier,
) {
    val partyColor = parsePartyColor(mp.party?.backgroundColour)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        partyColor.copy(alpha = 0.85f),
                        partyColor.copy(alpha = 0.3f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(MaterialTheme.padding.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            MpAvatar(
                thumbnailUrl = mp.thumbnailUrl,
                displayName = mp.nameDisplayAs,
                partyColorHex = mp.party?.backgroundColour,
                size = 72.dp,
                borderWidth = 2.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = mp.nameDisplayAs,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = mp.party?.name ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilledIconButton(
                    onClick = { /* Phase 6: notification toggle */ },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", modifier = Modifier.size(18.dp))
                }
                FilledIconButton(
                    onClick = { /* Phase 6: follow */ },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = "Follow", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
