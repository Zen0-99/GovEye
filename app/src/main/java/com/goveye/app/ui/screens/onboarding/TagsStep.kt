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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Step 2 — Tags: "Pick your topics".
 *
 * Tags grouped under category headings in a 2-column grid of rounded
 * rectangle cards (matching the GovernmentCard design). No sub-text —
 * the tag name alone is the label. Selected state shows primary border
 * 2dp + primaryContainer 0.3 alpha bg + checkmark.
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
    // Use the static tag list — the 26 tags are a fixed set defined in
    // TAG_DICTIONARY (build scripts). The DB query may return empty during
    // first launch before the seed download completes, but onboarding must
    // always show all 26 tags so the user can pick topics.
    val tagsToShow = if (availableTags.isEmpty()) ALL_TAG_NAMES else availableTags

    // Group tags into categories, preserving category order.
    val categorized = TAG_CATEGORIES.map { category ->
        category to category.tags.filter { it in tagsToShow }
    }.filter { (_, tags) -> tags.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Pick your topics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
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

        Spacer(Modifier.height(16.dp))

        // Tag grid — 2 columns with category headers spanning full width.
        // Skip for now is the last item inside the grid so it scrolls with
        // the content instead of being sticky.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categorized.forEach { (category, tags) ->
                // Category header — spans both columns
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${category.name}") {
                    CategoryHeader(title = category.name)
                }
                // Tag cards within this category
                items(
                    items = tags,
                    key = { tag -> tag }
                ) { tag ->
                    TagCell(
                        tagName = tag,
                        selected = tag in selectedTags,
                        onClick = { onTagToggle(tag) }
                    )
                }
            }

            // Skip for now — at the bottom of the list, full width
            item(span = { GridItemSpan(maxLineSpan) }, key = "skip") {
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
                enabled = selectedTags.isNotEmpty(),
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Category header — full-width label above a group of tag cards.
 */
@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/**
 * A selectable tag cell — rounded rectangle card with tag name only (no sub-text).
 * Matches the GovernmentCard selection pattern (OnboardingScreen.kt):
 *   - RoundedCornerShape(16.dp)
 *   - primary border 2dp + primaryContainer 0.3 alpha bg when selected
 *   - outlineVariant border 1dp + surface bg when unselected
 *   - Checkmark badge top-right when selected
 *
 * Fixed height + single line text so all cards are the same height
 * regardless of tag name length.
 */
@Composable
private fun TagCell(tagName: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .height(56.dp)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = tagName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart)
            )

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
 * Tag categories — 26 tags grouped into 9 thematic headings.
 * The tag names must match TAG_DICTIONARY keys in the build scripts.
 */
private data class TagCategory(val name: String, val tags: List<String>)

private val TAG_CATEGORIES: List<TagCategory> = listOf(
    TagCategory(
        "Benefits & Welfare",
        listOf(
            "Universal Credit",
            "PIP & Disability Benefits",
            "Disability",
            "Welfare & Social Security"
        )
    ),
    TagCategory(
        "Economy & Work",
        listOf(
            "Budget & Fiscal",
            "Taxation",
            "Employment & Workers",
            "Business & Enterprise"
        )
    ),
    TagCategory(
        "Health & Social Care",
        listOf("NHS", "Social Care", "Mental Health")
    ),
    TagCategory(
        "Education & Family",
        listOf("Education", "Children & Families")
    ),
    TagCategory(
        "Environment & Energy",
        listOf("Climate & Environment", "Energy", "Agriculture & Farming")
    ),
    TagCategory(
        "Justice, Rights & Immigration",
        listOf("Justice & Crime", "Human Rights", "Immigration & Asylum")
    ),
    TagCategory(
        "Housing & Transport",
        listOf("Housing", "Transport")
    ),
    TagCategory(
        "Defence & Foreign Affairs",
        listOf("Defence", "Brexit & EU", "Foreign Policy")
    ),
    TagCategory(
        "Constitutional & Technology",
        listOf("Constitutional & Devolution", "Technology & Digital")
    )
)

/**
 * All 26 tag names in a flat list — used as the fallback when the DB
 * has not been populated yet (first launch before seed download).
 */
val ALL_TAG_NAMES: List<String> = TAG_CATEGORIES.flatMap { it.tags }
