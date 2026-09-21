package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.CareerCategory
import com.goveye.app.domain.model.CareerEvent
import com.goveye.app.ui.theme.padding

enum class CareerViewMode { TIMELINE, TABLE }

@Composable
fun CareerTimelineSection(
    experiences: List<BiographyExperience>,
    careerEvents: List<CareerEvent> = emptyList(),
    modifier: Modifier = Modifier
) {
    val unifiedTimeline = remember(experiences, careerEvents) {
        buildUnifiedTimeline(experiences, careerEvents)
    }

    if (unifiedTimeline.isEmpty()) return

    var viewMode by remember { mutableStateOf(CareerViewMode.TIMELINE) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.medium)
    ) {
        when (viewMode) {
            CareerViewMode.TIMELINE -> ConnectedTimelineView(unifiedTimeline)
            CareerViewMode.TABLE -> TableView(unifiedTimeline)
        }
    }
}

// --- Unified timeline item ---

private data class UnifiedTimelineItem(
    val dateLabel: String,
    val title: String,
    val subtitle: String?,
    val category: CareerCategory,
    val link: String?,
    val isCurrent: Boolean
)

private fun buildUnifiedTimeline(
    experiences: List<BiographyExperience>,
    careerEvents: List<CareerEvent>
): List<UnifiedTimelineItem> {
    val items = mutableListOf<UnifiedTimelineItem>()

    for (event in careerEvents) {
        items.add(
            UnifiedTimelineItem(
                dateLabel = event.formattedDateRange,
                title = event.name,
                subtitle = event.additionalInfo,
                category = event.category,
                link = event.additionalInfoLink,
                isCurrent = event.isCurrent
            )
        )
    }

    for (exp in experiences) {
        items.add(
            UnifiedTimelineItem(
                dateLabel = exp.dateRangeText,
                title = exp.title ?: exp.type ?: "Unknown role",
                subtitle = exp.organisation,
                category = CareerCategory.EXPERIENCE,
                link = null,
                isCurrent = exp.endYear == null
            )
        )
    }

    return items.sortedByDescending { item ->
        Regex("(\\d{4})").find(item.dateLabel)?.value?.toIntOrNull() ?: 0
    }
}

// --- Category visual properties ---

@Composable
private fun CareerCategory.color(): Color = MaterialTheme.colorScheme.primary

private fun CareerCategory.icon(): ImageVector = when (this) {
    CareerCategory.GOVERNMENT_POST -> Icons.Outlined.AccountBalance
    CareerCategory.OPPOSITION_POST -> Icons.Outlined.AccountBalance
    CareerCategory.OTHER_POST -> Icons.Outlined.Groups
    CareerCategory.COMMITTEE -> Icons.Outlined.Groups
    CareerCategory.REPRESENTATION -> Icons.Outlined.HowToVote
    CareerCategory.PARTY_AFFILIATION -> Icons.Outlined.Groups
    CareerCategory.HOUSE_MEMBERSHIP -> Icons.Outlined.AccountBalance
    CareerCategory.EDUCATION -> Icons.Outlined.School
    CareerCategory.OCCUPATION -> Icons.Outlined.Work
    CareerCategory.EXPERIENCE -> Icons.Outlined.BusinessCenter
}

private fun CareerCategory.badgeLabel(): String = when (this) {
    CareerCategory.GOVERNMENT_POST -> "Government"
    CareerCategory.OPPOSITION_POST -> "Opposition"
    CareerCategory.OTHER_POST -> "Party Role"
    CareerCategory.COMMITTEE -> "Committee"
    CareerCategory.REPRESENTATION -> "Elected"
    CareerCategory.PARTY_AFFILIATION -> "Party"
    CareerCategory.HOUSE_MEMBERSHIP -> "Parliament"
    CareerCategory.EDUCATION -> "Education"
    CareerCategory.OCCUPATION -> "Career"
    CareerCategory.EXPERIENCE -> "Experience"
}

// --- Connected Timeline View ---

@Composable
private fun ConnectedTimelineView(items: List<UnifiedTimelineItem>) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    val currentItems = items.filter { it.isCurrent }
    val pastItems = items.filter { !it.isCurrent }

    Column(
        modifier = Modifier
            .padding(top = MaterialTheme.padding.medium)
            .fillMaxWidth()
    ) {
        if (currentItems.isNotEmpty()) {
            Text(
                text = "Current",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
            )
            TimelineItems(currentItems, lineColor)
        }

        if (pastItems.isNotEmpty()) {
            if (currentItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
            }
            Text(
                text = "Past",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
            )
            TimelineItems(pastItems, lineColor)
        }
    }
}

@Composable
private fun TimelineItems(items: List<UnifiedTimelineItem>, lineColor: Color) {
    items.forEachIndexed { index, item ->
        val isFirst = index == 0
        val isLast = index == items.lastIndex
        val dotColor = item.category.color()
        val dotRadius = 7.dp
        val dotTopPadding = 8.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            // Left column: vertical line + dot
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val centerX = size.width / 2
                        val dotCenterY = dotTopPadding.toPx() + dotRadius.toPx()
                        val dotDiameter = dotRadius.toPx() * 2

                        // Draw connecting line
                        // Top segment: from top to dot center (skip for first item)
                        if (!isFirst) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, dotCenterY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        // Bottom segment: from dot center to bottom (skip for last item)
                        if (!isLast) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, dotCenterY),
                                end = Offset(centerX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Draw dot
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius.toPx(),
                            center = Offset(centerX, dotCenterY)
                        )
                        // Inner white circle for ring effect
                        drawCircle(
                            color = Color.White,
                            radius = dotRadius.toPx() * 0.4f,
                            center = Offset(centerX, dotCenterY)
                        )
                    }
            )

            // Right column: date title + info card
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Date title — only show when a date is available
                if (item.dateLabel.isNotBlank()) {
                    Text(
                        text = item.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Title
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Subtitle (department, "Elected N times", etc.) + category badge on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            item.subtitle?.let { sub ->
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } ?: Spacer(modifier = Modifier.weight(1f))

                            // Category badge — bottom right
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = item.category.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = item.category.color()
                                )
                                Text(
                                    text = item.category.badgeLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = item.category.color(),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Gap before next item — this space is where the line
                // continues to the next dot
                if (!isLast) {
                    Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                }
            }
        }
    }
}

// --- Table View (compact) ---

@Composable
private fun TableView(items: List<UnifiedTimelineItem>) {
    Column(
        modifier = Modifier.padding(top = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    item.subtitle?.let { sub ->
                        if (sub.isNotBlank()) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = item.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}
