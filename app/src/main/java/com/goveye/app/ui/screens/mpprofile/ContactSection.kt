package com.goveye.app.ui.screens.mpprofile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.domain.model.Contact
import com.goveye.app.ui.theme.padding

/**
 * Merges contacts that share the same email into a single entry.
 * The merged entry gets a combined type label (e.g., "Parliamentary and
 * Constituency office") and keeps the first non-null phone/address.
 * Contacts without an email are passed through unchanged.
 */
private fun deduplicateContacts(contacts: List<Contact>): List<Contact> {
    val byEmail = contacts.filter { !it.email.isNullOrBlank() }.groupBy { it.email }
    val withoutEmail = contacts.filter { it.email.isNullOrBlank() }
    val merged = byEmail.map { (email, group) ->
        if (group.size == 1) {
            group[0]
        } else {
            val combinedType = group.mapNotNull { it.type }.distinct().joinToString(" and ")
            Contact(
                type = combinedType,
                isPreferred = group.any { it.isPreferred == true },
                isWebAddress = false,
                line1 = group.firstNotNullOfOrNull { it.line1 },
                line2 = group.firstNotNullOfOrNull { it.line2 },
                line3 = group.firstNotNullOfOrNull { it.line3 },
                line4 = group.firstNotNullOfOrNull { it.line4 },
                line5 = group.firstNotNullOfOrNull { it.line5 },
                postcode = group.firstNotNullOfOrNull { it.postcode },
                phone = group.firstNotNullOfOrNull { it.phone },
                email = email,
                website = null,
                openingHours = group.firstNotNullOfOrNull { it.openingHours }
            )
        }
    }
    // Preserve original ordering: merged contacts first (in original order), then non-email contacts
    val mergedEmails = byEmail.keys
    val orderedMerged = contacts.filter { it.email in mergedEmails }.map { c ->
        merged.first { it.email == c.email }
    }.distinctBy { it.email }
    return orderedMerged + withoutEmail
}

@Composable
fun ContactSection(contacts: List<Contact>, socialLinks: MpLinkEntity? = null, modifier: Modifier = Modifier) {
    val deduplicated = remember(contacts) { deduplicateContacts(contacts) }
    if (deduplicated.isEmpty() && socialLinks == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            deduplicated.firstNotNullOfOrNull { it.openingHours?.takeIf(String::isNotBlank) }?.let { hours ->
                Text(
                    text = hours,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            deduplicated.forEach { contact ->
                ContactRow(contact)
            }
            // Social links row — inline at the bottom of the contact card
            if (socialLinks != null) {
                SocialLinksRow(
                    links = socialLinks,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact) {
    val context = LocalContext.current
    val icon = when {
        contact.isWebAddress == true -> Icons.Outlined.Public
        contact.email != null -> Icons.Outlined.Email
        contact.phone != null -> Icons.Outlined.Phone
        else -> Icons.Outlined.LocationOn
    }
    val primaryText: String = when {
        contact.isWebAddress == true -> contact.website ?: ""
        contact.email != null -> contact.email!!
        contact.phone != null -> contact.phone!!
        else -> contact.formattedAddress
    }
    if (primaryText.isBlank()) return

    val clickAction: (() -> Unit)? = when {
        contact.isWebAddress == true && contact.website != null -> {
            { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(contact.website))) }
        }

        contact.email != null -> {
            { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))) }
        }

        contact.phone != null -> {
            { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) }
        }

        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
            val typeText = contact.type
            if (typeText != null) {
                Text(
                    text = typeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
