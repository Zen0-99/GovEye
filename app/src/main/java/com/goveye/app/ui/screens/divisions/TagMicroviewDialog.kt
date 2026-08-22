package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Division
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.divisions.DivisionResultBar

private val AyeColor @Composable get() = VoteColors.aye
private val NoColor @Composable get() = VoteColors.no

/**
 * Tag microview dialog — shown when clicking a tag pill on a division card.
 * Shows the tag name, description, counts of divisions/bills, and a list of
 * all divisions that carry this tag.
 *
 * Mirrors the MpMicroviewDialog pattern: a Dialog with a gradient header
 * and a scrollable content area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagMicroviewDialog(
    tag: String,
    onNavigateToDivision: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: TagMicroviewViewModel = hiltViewModel()
) {
    LaunchedEffect(tag) {
        viewModel.load(tag)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Gradient header
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)
                val primaryColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = if (isDark) 0.85f else 0.7f),
                                    primaryColor.copy(alpha = if (isDark) 0.3f else 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Close",
                                    tint = headerTextColor
                                )
                            }
                        }

                        // Tag name
                        Text(
                            text = uiState.tag,
                            style = MaterialTheme.typography.headlineSmall,
                            color = headerTextColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Counts
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${uiState.divisionCount} division${if (uiState.divisionCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelLarge,
                                color = headerTextColor.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.billCount > 0) {
                                Text(
                                    text = "${uiState.billCount} bill${if (uiState.billCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = headerTextColor.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Description + division list
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Description
                        if (uiState.description.isNotBlank()) {
                            item {
                                Text(
                                    text = uiState.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Division list
                        items(uiState.divisions) { division ->
                            TagDivisionItem(
                                division = division,
                                onClick = {
                                    onDismiss()
                                    onNavigateToDivision(division.id, division.house)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagDivisionItem(
    division: Division,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = division.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDivisionDate(division.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (division.house == 2) "Lords" else "Commons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${division.ayeCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AyeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text("·", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${division.noCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NoColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            DivisionResultBar(ayeCount = division.ayeCount, noCount = division.noCount)
        }
    }
}

private fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
