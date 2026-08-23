package com.goveye.app.ui.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Step 3 — Sources: "Choose your sources".
 *
 * Recommended and all sources displayed as rounded rectangle cards
 * (matching the TagsStep card design). Tapping a department card
 * toggles all 3 streams at once.
 *
 * Per UI-SPEC Section 3, Step 3.
 */
@Composable
fun SourcesStep(
    selectedSources: Set<String>,
    selectedTags: Set<String>,
    recommendedDepartments: List<RecommendedDepartment>,
    allDepartments: List<DepartmentGroup>,
    onSourceToggle: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Choose your sources",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Recommended based on your topics. Tap a department to follow all its streams.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Content — LazyColumn with Recommended + All sources sections
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recommended section
            item {
                Text(
                    text = "Recommended",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (selectedTags.isEmpty()) {
                item {
                    Text(
                        text = "Select topics to see recommendations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else if (recommendedDepartments.isEmpty()) {
                item {
                    Text(
                        text = "No recommendations available yet — the database is still downloading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(
                    items = recommendedDepartments,
                    key = { "rec-${it.organisationSlug}" }
                ) { dept ->
                    SourceCard(
                        name = dept.organisationName,
                        streams = dept.streams,
                        selectedSources = selectedSources,
                        organisationSlug = dept.organisationSlug,
                        onSourceToggle = onSourceToggle
                    )
                }
            }

            // All sources section
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "All sources",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            allDepartments.forEach { dept ->
                item(key = "all-${dept.organisationSlug}") {
                    SourceCard(
                        name = dept.organisationName,
                        streams = dept.streams,
                        selectedSources = selectedSources,
                        organisationSlug = dept.organisationSlug,
                        onSourceToggle = onSourceToggle
                    )
                }
            }

            // Skip for now — at the bottom of the list
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip for now")
                    }
                }
            }
        }

        // Bottom buttons — Back (weight 1) + Continue (weight 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Back")
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * A source card — rounded rectangle matching the TagsStep card design.
 * Shows department name + stream chips. Tapping the card toggles all 3 streams.
 * Selected state (all streams checked) shows primary border + checkmark.
 */
@Composable
private fun SourceCard(
    name: String,
    streams: List<StreamState>,
    selectedSources: Set<String>,
    organisationSlug: String,
    onSourceToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = streams.all { stream ->
        "$organisationSlug:${stream.streamType.name}" in selectedSources
    }
    val anySelected = streams.any { stream ->
        "$organisationSlug:${stream.streamType.name}" in selectedSources
    }
    val borderColor = if (allSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (allSelected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (allSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                // Toggle all 3 streams at once
                streams.forEach { stream ->
                    onSourceToggle("$organisationSlug:${stream.streamType.name}")
                }
            }
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // Stream chips — show which streams are followed
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    streams.forEach { stream ->
                        val sourceKey = "$organisationSlug:${stream.streamType.name}"
                        val isChecked = sourceKey in selectedSources
                        StreamChip(
                            label = stream.streamType.displayName,
                            isChecked = isChecked || allSelected
                        )
                    }
                }
            }

            // Checkmark — top right corner
            if (allSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * A small chip showing a stream name with a check indicator.
 */
@Composable
private fun StreamChip(label: String, isChecked: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isChecked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
