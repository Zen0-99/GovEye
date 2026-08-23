package com.goveye.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Step 3 — Sources: "Choose your sources".
 *
 * LazyColumn with two sections:
 * - "Recommended" — departments matching user tags with 3 stream checkboxes
 *   pre-checked per D-05.
 * - "All sources" — all 75 department-stream combinations grouped by department.
 *
 * If no tags selected, Recommended section shows a hint.
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
        Spacer(Modifier.height(60.dp))

        // Title
        Text(
            text = "Choose your sources",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Recommended based on your topics. Follow departments to see their " +
                "publications, statements, and legislation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

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
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(
                    items = recommendedDepartments,
                    key = { it.organisationSlug }
                ) { dept ->
                    RecommendedDepartmentCard(
                        department = dept,
                        selectedSources = selectedSources,
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
                item(key = "header-${dept.organisationSlug}") {
                    Text(
                        text = dept.organisationName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(
                    items = dept.streams,
                    key = { "${dept.organisationSlug}-${it.streamType.name}" }
                ) { stream ->
                    val sourceKey = "${dept.organisationSlug}:${stream.streamType.name}"
                    val isChecked = sourceKey in selectedSources
                    StreamCheckboxRow(
                        label = stream.streamType.displayName,
                        isChecked = isChecked,
                        onToggle = { onSourceToggle(sourceKey) }
                    )
                }
            }
        }

        // Skip for now — centered above Back/Continue row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip for now")
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
                Text("Continue to parties")
            }
        }
    }
}

@Composable
private fun RecommendedDepartmentCard(
    department: RecommendedDepartment,
    selectedSources: Set<String>,
    onSourceToggle: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = department.organisationName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                department.streams.forEach { stream ->
                    val sourceKey = "${department.organisationSlug}:${stream.streamType.name}"
                    val isChecked = sourceKey in selectedSources
                    StreamCheckboxRow(
                        label = stream.streamType.displayName,
                        isChecked = isChecked,
                        onToggle = { onSourceToggle(sourceKey) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamCheckboxRow(label: String, isChecked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
