package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.ui.theme.padding

enum class CareerViewMode { TIMELINE, TABLE }

@Composable
fun CareerTimelineSection(experiences: List<BiographyExperience>, modifier: Modifier = Modifier) {
    if (experiences.isEmpty()) return

    var viewMode by remember { mutableStateOf(CareerViewMode.TIMELINE) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Political Career",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                viewMode = if (viewMode == CareerViewMode.TIMELINE) {
                    CareerViewMode.TABLE
                } else {
                    CareerViewMode.TIMELINE
                }
            }) {
                Icon(
                    imageVector = if (viewMode == CareerViewMode.TIMELINE) {
                        Icons.Outlined.ViewList
                    } else {
                        Icons.Outlined.Timeline
                    },
                    contentDescription = if (viewMode == CareerViewMode.TIMELINE) {
                        "Table view"
                    } else {
                        "Timeline view"
                    }
                )
            }
        }

        when (viewMode) {
            CareerViewMode.TIMELINE -> TimelineView(experiences)
            CareerViewMode.TABLE -> TableView(experiences)
        }
    }
}

@Composable
private fun TimelineView(experiences: List<BiographyExperience>) {
    val dotColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier.padding(top = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        experiences.forEach { experience ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawCircle(dotColor)
                            drawLine(
                                color = lineColor,
                                start = Offset(size.width / 2, size.height),
                                end = Offset(size.width / 2, size.height + 40.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {}

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = experience.title ?: "Unknown role",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    experience.organisation?.let { org ->
                        Text(
                            text = org,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = experience.dateRangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TableView(experiences: List<BiographyExperience>) {
    Column(
        modifier = Modifier.padding(top = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        experiences.forEach { experience ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = experience.title ?: "Unknown role",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    experience.organisation?.let { org ->
                        Text(
                            text = org,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = experience.dateRangeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}
