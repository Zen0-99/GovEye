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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.R
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.ui.theme.padding

@Composable
fun SocialLinksRow(links: MpLinkEntity?, modifier: Modifier = Modifier) {
    if (links == null) return

    val context = LocalContext.current
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    // Build list of (icon, subtext label, displayText, url) for all available links.
    // Social media shows handle/page name; website shows URL without protocol.
    // Subtext labels match the "Parliamentary Office" style from ContactRow.
    val rows = buildList {
        links.twitterHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.X,
                    subtext = "X (Twitter)",
                    displayText = "@$handle",
                    url = "https://x.com/$handle"
                )
            )
        }
        links.facebookUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.FACEBOOK,
                    subtext = "Facebook",
                    displayText = extractFacebookName(url),
                    url = url
                )
            )
        }
        links.instagramUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.INSTAGRAM,
                    subtext = "Instagram",
                    displayText = extractHandleFromUrl(url),
                    url = url
                )
            )
        }
        links.linkedinUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.LINKEDIN,
                    subtext = "LinkedIn",
                    displayText = extractHandleFromUrl(url),
                    url = url
                )
            )
        }
        links.wikipediaUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.WIKIPEDIA,
                    subtext = "Wikipedia",
                    displayText = stripProtocol(url),
                    url = url
                )
            )
        }
        links.personalWebsiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                SocialLinkEntry(
                    iconType = SocialIconType.WEBSITE,
                    subtext = "Website",
                    displayText = stripProtocol(url),
                    url = url
                )
            )
        }
    }

    if (rows.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        rows.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(context, entry.url) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
            ) {
                when (entry.iconType) {
                    SocialIconType.WEBSITE -> Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = "Website",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )

                    SocialIconType.X -> Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = "X (formerly Twitter)",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )

                    SocialIconType.FACEBOOK -> Icon(
                        painter = painterResource(R.drawable.ic_facebook),
                        contentDescription = "Facebook",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )

                    SocialIconType.INSTAGRAM -> Icon(
                        painter = painterResource(R.drawable.ic_instagram),
                        contentDescription = "Instagram",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )

                    SocialIconType.LINKEDIN -> Icon(
                        painter = painterResource(R.drawable.ic_linkedin),
                        contentDescription = "LinkedIn",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )

                    SocialIconType.WIKIPEDIA -> Icon(
                        painter = painterResource(R.drawable.ic_wikipedia),
                        contentDescription = "Wikipedia",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = entry.subtext,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class SocialLinkEntry(
    val iconType: SocialIconType,
    val subtext: String,
    val displayText: String,
    val url: String
)

private enum class SocialIconType {
    X,
    FACEBOOK,
    INSTAGRAM,
    LINKEDIN,
    WIKIPEDIA,
    WEBSITE
}

/**
 * Strip the protocol (http://, https://) and trailing slash from a URL,
 * leaving just the domain and path (e.g. "www.example.co.uk").
 */
private fun stripProtocol(url: String): String = url
    .removePrefix("https://")
    .removePrefix("http://")
    .removeSuffix("/")

/**
 * Extract a Facebook page name from a URL, stripping numeric IDs.
 * e.g. "https://www.facebook.com/JohnSmithMP-123456789" → "JohnSmithMP"
 *      "https://www.facebook.com/profile.php?id=123456" → "Facebook page"
 */
private fun extractFacebookName(url: String): String {
    val stripped = stripProtocol(url)
    // Handle profile.php?id=... URLs — just show "Facebook page"
    if (stripped.contains("profile.php")) return "Facebook page"
    // Handle group URLs
    if (stripped.contains("/groups/")) {
        val parts = stripped.split("/")
        val groupIdx = parts.indexOf("groups")
        if (groupIdx >= 0 && groupIdx + 1 < parts.size) {
            return parts[groupIdx + 1].substringBefore("-").substringBefore("?")
        }
    }
    // Standard page URL: facebook.com/PageName or facebook.com/PageName-123456789
    val parts = stripped.split("/")
    val pageSegment = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return stripped
    // Strip numeric IDs after a hyphen or dot: "PageName-123456" → "PageName"
    return pageSegment
        .substringBefore("-")
        .substringBefore("?")
        .substringBefore("&")
        .takeIf { it.isNotBlank() } ?: stripped
}

/**
 * Extract the last path segment from a URL as a handle.
 * e.g. "https://www.instagram.com/johnsmith" → "johnsmith"
 */
private fun extractHandleFromUrl(url: String): String {
    val stripped = stripProtocol(url)
    val parts = stripped.split("/")
    return parts.lastOrNull { it.isNotBlank() }?.substringBefore("?") ?: stripped
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Silently fail — broken URL or no browser
    }
}
