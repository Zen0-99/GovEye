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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp

/**
 * Step 2 — Tags: "Pick your topics".
 *
 * LazyVerticalGrid(GridCells.Fixed(2)) of 26 tag cells with tag name +
 * description. Selected state shows primary border 2dp + primaryContainer
 * 0.3 alpha bg + checkmark. Selection counter below subtitle.
 *
 * Per UI-SPEC Section 3, Step 2.
 */
@Composable
fun TagsStep(
    selectedTags: Set<String>,
    availableTags: List<String>,
    onTagToggle: (String) -> Unit,
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
            text = "Pick your topics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Select the areas you care about. We'll use these to recommend sources and MPs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${selectedTags.size} selected",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Tag grid — 2 columns, 26 tag cells
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = availableTags,
                key = { tag -> tag }
            ) { tag ->
                TagCell(
                    tagName = tag,
                    description = TAG_DESCRIPTIONS[tag] ?: "",
                    selected = tag in selectedTags,
                    onClick = { onTagToggle(tag) }
                )
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
                enabled = selectedTags.isNotEmpty(),
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue to sources")
            }
        }
    }
}

/**
 * A selectable tag cell — tag name + description, with checkmark when selected.
 * Follows the GovernmentCard selection pattern (OnboardingScreen.kt lines 311-382).
 */
@Composable
private fun TagCell(
    tagName: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = tagName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }

            // Checkmark — top right corner (GovernmentCard pattern)
            if (selected) {
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
 * Short 1-line descriptions for all 26 tags from TAG_DICTIONARY.
 * Used in the Tags step grid cells.
 */
val TAG_DESCRIPTIONS: Map<String, String> = mapOf(
    "Universal Credit" to "Benefits and social security",
    "PIP & Disability Benefits" to "Disability benefits and carer's allowance",
    "Disability" to "Disability rights and accessibility",
    "Welfare & Social Security" to "Welfare policy and benefit caps",
    "Immigration & Asylum" to "Immigration, asylum, and border security",
    "Budget & Fiscal" to "Government budgets and public spending",
    "Taxation" to "Tax and revenue",
    "NHS" to "Health and healthcare",
    "Social Care" to "Adult and children's social care",
    "Mental Health" to "Mental health services and legislation",
    "Education" to "Schools and universities",
    "Children & Families" to "Childcare and family policy",
    "Climate & Environment" to "Net zero and environmental policy",
    "Justice & Crime" to "Policing, courts, and prisons",
    "Human Rights" to "Civil liberties and human rights law",
    "Defence" to "Military and national security",
    "Housing" to "Housing and homelessness",
    "Transport" to "Rail, roads, and infrastructure",
    "Brexit & EU" to "Brexit and UK-EU relations",
    "Foreign Policy" to "Foreign affairs and international aid",
    "Employment & Workers" to "Workers' rights and minimum wage",
    "Business & Enterprise" to "Small businesses and enterprise policy",
    "Energy" to "Energy policy and fuel poverty",
    "Constitutional & Devolution" to "Constitutional reform and devolution",
    "Technology & Digital" to "AI, digital policy, and online safety",
    "Agriculture & Farming" to "Farming and food security"
)
