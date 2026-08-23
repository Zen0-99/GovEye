package com.goveye.app.ui.screens.mpprofile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.goveye.app.R
import com.goveye.app.data.local.entity.MpLinkEntity

@Composable
fun SocialLinksRow(links: MpLinkEntity?, modifier: Modifier = Modifier) {
    if (links == null) return
    val hasAnyLink = listOf(
        links.twitterHandle,
        links.facebookUrl,
        links.instagramUrl,
        links.linkedinUrl,
        links.wikipediaUrl,
        links.personalWebsiteUrl
    ).any { !it.isNullOrBlank() }
    if (!hasAnyLink) return

    val context = LocalContext.current
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        links.twitterHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            IconButton(onClick = { openUrl(context, "https://x.com/$handle") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "X (formerly Twitter)",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        links.facebookUrl?.takeIf { it.isNotBlank() }?.let { url ->
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_facebook),
                    contentDescription = "Facebook",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        links.instagramUrl?.takeIf { it.isNotBlank() }?.let { url ->
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_instagram),
                    contentDescription = "Instagram",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        links.linkedinUrl?.takeIf { it.isNotBlank() }?.let { url ->
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_linkedin),
                    contentDescription = "LinkedIn",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        links.wikipediaUrl?.takeIf { it.isNotBlank() }?.let { url ->
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_wikipedia),
                    contentDescription = "Wikipedia",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        links.personalWebsiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
            IconButton(onClick = { openUrl(context, url) }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_compass),
                    contentDescription = "Website",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Silently fail — broken URL or no browser
    }
}
