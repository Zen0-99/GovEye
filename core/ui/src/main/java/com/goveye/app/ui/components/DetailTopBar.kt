package com.goveye.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Miko-style shared detail top bar — replaces the floating search bar on
 * detail screens (profile, bill, etc.). A transparent row with back button
 * and optional action icons. No title — the title lives in the content
 * below (e.g. beside the avatar on profile screens).
 *
 * The bar is transparent so the profile's party gradient can extend from
 * the top of the screen, behind this bar. Icon tints can be customized
 * via [DetailTopBarConfig.iconTint] to ensure readability over the gradient.
 */
@Composable
fun DetailTopBar(config: DetailTopBarConfig, modifier: Modifier = Modifier) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val iconTint = config.iconTint ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = config.onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = iconTint
            )
        }

        if (config.title.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = config.title,
                style = MaterialTheme.typography.titleMedium,
                color = iconTint,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        } else {
            // No title — spacer pushes actions to the right
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.weight(1f)
            )
        }

        config.actions.forEach { action ->
            IconButton(
                onClick = action.onClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = action.tint ?: iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
