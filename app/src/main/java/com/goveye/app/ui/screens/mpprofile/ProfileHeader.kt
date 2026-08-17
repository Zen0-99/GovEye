package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partyColor = parsePartyColor(mp.party?.backgroundColour)
    // Derive dark/light from the actual theme surface, not isSystemInDarkTheme().
    // The app allows explicit LIGHT/DARK override via ThemeMode; isSystemInDarkTheme()
    // only reflects the system setting, causing black text/icons when the user
    // picks DARK while the system is in light mode (or vice versa).
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // The gradient's top color is partyColor at 85% alpha over the background.
    // In dark mode that's a dark navy → party color stays relatively dark → white text.
    // In light mode that's white → party color becomes lighter → need dark text.
    // Use the party color's luminance to pick readable text color.
    val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)

    // Party pill: use the actual party color with high opacity for more color
    // and darkness. In dark mode, use the raw party color; in light mode,
    // darken it slightly so it stands out against the light gradient.
    val pillColor = if (isDark) {
        partyColor.copy(alpha = 0.9f)
    } else {
        // Darken the party color by blending 20% toward black for more depth
        Color(
            red = partyColor.red * 0.8f,
            green = partyColor.green * 0.8f,
            blue = partyColor.blue * 0.8f,
            alpha = 0.85f,
        )
    }
    // Pill text: white if the pill color is dark enough, else dark
    val pillTextColor = if (pillColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White

    // Box fills the full area including behind the status bar with the gradient
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        partyColor.copy(alpha = if (isDark) 0.85f else 0.7f),
                        partyColor.copy(alpha = if (isDark) 0.3f else 0.2f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = 4.dp, bottom = MaterialTheme.padding.small),
        ) {
            // Controls row — back arrow left, follow/notification right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = headerIconTint,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { /* Phase 6: notification toggle */ },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = headerIconTint, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = { /* Phase 6: follow */ },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = "Follow", tint = headerIconTint, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Identity row — avatar, name, party pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            ) {
                MpAvatar(
                    thumbnailUrl = mp.thumbnailUrl,
                    displayName = mp.nameDisplayAs,
                    partyColorHex = mp.party?.backgroundColour,
                    size = 60.dp,
                    borderWidth = 2.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mp.nameDisplayAs,
                        style = MaterialTheme.typography.titleLarge,
                        color = headerTextColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillColor,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = mp.party?.name ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = pillTextColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
