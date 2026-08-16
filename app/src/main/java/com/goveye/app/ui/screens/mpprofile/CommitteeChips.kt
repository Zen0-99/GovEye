package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.goveye.app.domain.model.Committee
import com.goveye.app.ui.theme.padding

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitteeChipsSection(
    committees: List<Committee>,
    modifier: Modifier = Modifier,
) {
    if (committees.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.medium),
    ) {
        Text(
            text = "Committees",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            modifier = Modifier.padding(top = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            committees.forEach { committee ->
                AssistChip(
                    onClick = { /* Future: committee detail screen */ },
                    label = { Text(committee.name) },
                )
            }
        }
    }
}
