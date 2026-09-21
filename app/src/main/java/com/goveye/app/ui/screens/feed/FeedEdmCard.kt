package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.cardSurfaceColor
import com.goveye.app.ui.components.rememberExpandState

/**
 * **Early Day Motion card — an EDM primary-sponsored by a followed MP (D-04).**
 *
 * Shows the EDM designation + UIN, the motion title, the motion text
 * (3 lines collapsed, full text expanded), the live signature count +
 * status, and a tinted attribution bar with the primary sponsor's avatar,
 * name, and tabling date. Tapping the card toggles expansion — there is no
 * EDM detail screen this phase.
 *
 * @param showMember When false the attribution bar drops the MP avatar and
 *   name (reserved for reuse on the MP's own profile, where the identity
 *   is redundant).
 */
@Composable
fun FeedEdmCard(
    item: FeedItem.EdmItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null,
    showMember: Boolean = true
) {
    val expandState = rememberExpandState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = { expandState.toggle() }),
        shape = RoundedCornerShape(16.dp),
        color = cardSurfaceColor(MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Flag icon + "Early Day Motion" label + UIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (item.uinDisplay.isNullOrBlank()) {
                        "Early Day Motion"
                    } else {
                        "Early Day Motion · EDM ${item.uinDisplay}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Motion title — some EDMs have terse titles; the motion text
            // is the substance, so the title caps at 2 lines.
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // Motion text — 3 lines collapsed, full text expanded (same
            // mechanics as the written-question card).
            if (item.motionText.isNotBlank()) {
                Text(
                    text = item.motionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expandState.expanded) 50 else 3,
                    overflow = if (expandState.expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // Sponsor line — live signature count + status.
            val sponsorLine = buildString {
                append(
                    if (item.sponsorsCount == 1) {
                        "Signed by 1 MP"
                    } else {
                        "Signed by ${item.sponsorsCount} MPs"
                    }
                )
                if (!item.status.isNullOrBlank()) {
                    append(" · ${item.status}")
                }
            }
            Text(
                text = sponsorLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Attribution bar — tinted strip matching WQ/speech cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showMember) {
                    MpAvatar(
                        thumbnailUrl = item.memberPhotoUrl,
                        displayName = item.memberName,
                        partyColorHex = item.memberPartyColorHex,
                        size = 28.dp,
                        borderWidth = 1.dp,
                        modifier = if (onProfileClick != null) {
                            Modifier.clickable { onProfileClick() }
                        } else {
                            Modifier
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.memberName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Primary sponsor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Early Day Motion",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatDivisionDate(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
