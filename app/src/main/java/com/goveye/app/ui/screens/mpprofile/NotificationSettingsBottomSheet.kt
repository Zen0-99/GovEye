package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * FotMob-style notification settings bottom sheet (D-04 revised).
 *
 * - Master toggle: "Notifications enabled" — derived from the checkboxes
 * - Below: checkboxes for each notification type (Votes, Speeches) with icons
 *
 * Logic:
 * - Master ON + some checkboxes ON → receive those notification types
 * - Master OFF → no notifications
 * - If master OFF and user checks a checkbox → master turns ON (only that type)
 * - If all checkboxes unchecked → master turns OFF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsBottomSheet(
    notificationsEnabled: Boolean,
    votesEnabled: Boolean,
    speechesEnabled: Boolean,
    onMasterToggle: (Boolean) -> Unit,
    onVotesToggle: (Boolean) -> Unit,
    onSpeechesToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Title
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Master toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Notifications enabled",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onMasterToggle,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Type checkboxes
            NotificationTypeRow(
                icon = Icons.Outlined.HowToVote,
                label = "Votes",
                checked = votesEnabled,
                onCheckedChange = onVotesToggle,
            )

            NotificationTypeRow(
                icon = Icons.Outlined.RecordVoiceOver,
                label = "Speeches",
                checked = speechesEnabled,
                onCheckedChange = onSpeechesToggle,
            )

            // Bottom spacing
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun NotificationTypeRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
