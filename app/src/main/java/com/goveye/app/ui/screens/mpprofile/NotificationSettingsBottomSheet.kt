package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * FotMob-style notification settings bottom sheet (D-04 revised).
 *
 * - Master toggle (no box fill around it)
 * - Below: checkboxes for each notification type (Votes, Speeches, Income,
 *   Expenses) with icons
 *
 * Checkboxes and switch use the MP's party color (desaturated) — same
 * approach as party cards in the Directory's Parties tab.
 *
 * No bottom sheet handle — just small padding above the content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsBottomSheet(
    notificationsEnabled: Boolean,
    votesEnabled: Boolean,
    speechesEnabled: Boolean,
    incomeEnabled: Boolean,
    expensesEnabled: Boolean,
    onMasterToggle: (Boolean) -> Unit,
    onVotesToggle: (Boolean) -> Unit,
    onSpeechesToggle: (Boolean) -> Unit,
    onIncomeToggle: (Boolean) -> Unit,
    onExpensesToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    partyColor: Color = MaterialTheme.colorScheme.primary
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Smart party coloring — follows Miko's CheckboxItem pattern:
    // ON:  thumb = surface (white), track = party color, border = party color
    // OFF: thumb = party color (muted), track = party color (very faint),
    //      border = party color (faint) — so the party theme is always visible
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.surface,
        checkedTrackColor = partyColor,
        checkedBorderColor = partyColor,
        uncheckedThumbColor = partyColor.copy(alpha = 0.5f),
        uncheckedTrackColor = partyColor.copy(alpha = 0.12f),
        uncheckedBorderColor = partyColor.copy(alpha = 0.2f)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Master toggle — no box fill, just label + switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications enabled",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onMasterToggle,
                    colors = switchColors
                )
            }

            // Type checkboxes — tighter spacing
            NotificationTypeRow(
                icon = Icons.Outlined.HowToVote,
                label = "Votes",
                checked = votesEnabled,
                onCheckedChange = onVotesToggle,
                partyColor = partyColor
            )

            NotificationTypeRow(
                icon = Icons.Outlined.RecordVoiceOver,
                label = "Speeches",
                checked = speechesEnabled,
                onCheckedChange = onSpeechesToggle,
                partyColor = partyColor
            )

            NotificationTypeRow(
                icon = Icons.Outlined.ArrowDownward,
                label = "Income",
                checked = incomeEnabled,
                onCheckedChange = onIncomeToggle,
                partyColor = partyColor
            )

            NotificationTypeRow(
                icon = Icons.Outlined.ArrowUpward,
                label = "Expenses",
                checked = expensesEnabled,
                onCheckedChange = onExpensesToggle,
                partyColor = partyColor
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
    partyColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) partyColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = partyColor,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
