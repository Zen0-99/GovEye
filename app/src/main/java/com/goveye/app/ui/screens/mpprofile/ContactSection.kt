package com.goveye.app.ui.screens.mpprofile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Contact
import com.goveye.app.ui.theme.padding

@Composable
fun ContactSection(
    contacts: List<Contact>,
    modifier: Modifier = Modifier,
) {
    if (contacts.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = "Contact",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            contacts.forEach { contact ->
                ContactRow(contact)
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
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            val typeText = contact.type
            if (typeText != null) {
                Text(
                    text = typeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
