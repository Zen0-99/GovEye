package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.Committee
import com.goveye.app.ui.components.rememberExpandState
import com.goveye.app.ui.theme.padding

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitteeChipsSection(
    committees: List<Committee>,
    onCommitteeClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (committees.isEmpty()) return

    val activeCommittees = committees.filter { it.isActive }
    val inactiveCommittees = committees.filter { !it.isActive }
    val expandState = rememberExpandState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Committees",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeCommittees.size} active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val visibleCommittees = if (expandState.expanded) committees else activeCommittees.take(3)
            FlowRow(
                modifier = Modifier.padding(top = MaterialTheme.padding.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
            ) {
                visibleCommittees.forEach { committee ->
                    AssistChip(
                        onClick = { onCommitteeClick(committee.id) },
                        label = { Text(committee.name, maxLines = 1) }
                    )
                }
            }

            if (!expandState.expanded && activeCommittees.size > 3) {
                TextButton(
                    onClick = { expandState.expand() },
                    modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall)
                ) {
                    Text("Show all ${committees.size}")
                }
            } else if (expandState.expanded) {
                TextButton(
                    onClick = { expandState.collapse() },
                    modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall)
                ) {
                    Text("Show less")
                }
            }
        }
    }
}
