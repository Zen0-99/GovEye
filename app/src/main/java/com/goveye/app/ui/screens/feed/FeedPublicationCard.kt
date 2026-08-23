package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.goveye.app.domain.model.GovernmentPublication

/**
 * Feed publication card — GOV.UK publication card with optional image on top.
 *
 * With image (D-10): Image on top (16:9 aspect, RoundedCornerShape top corners
 * only), title + org + type badge + tag pills below.
 *
 * Without image: Title + org + type badge + tag pills only (same layout as
 * FeedDivisionCard without vote counts).
 *
 * Icon badge (D-11): [Icons.Outlined.Description] in a 32dp circle at TopEnd.
 * Type badge: "Publication" in a labelSmall chip.
 *
 * Image loading: Coil [SubcomposeAsyncImage] shows a surfaceContainerHighest
 * placeholder rectangle (16:9) while loading. On error, the image area
 * collapses gracefully — card shows title + org + type badge + tag pills only.
 */
@Composable
fun FeedPublicationCard(
    publication: GovernmentPublication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList(),
    onTagClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Icon badge at TopEnd (D-11)
            CardIconBadge(
                icon = Icons.Outlined.Description,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Image on top if available (16:9 aspect)
                if (!publication.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = publication.imageUrl,
                        contentDescription = publication.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            ),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        loading = {
                            // Placeholder rectangle while loading
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        },
                        error = {
                            // On error, collapse — render nothing (zero height).
                            // The content below fills the card naturally.
                        }
                    )
                }

                // Content below image (or full content if no image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = publication.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = publication.organisation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Type badge (D-11)
                        TypeBadge(text = "Publication")
                    }

                    // Tag pills
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        TagPillRow(
                            tags = tags,
                            onTagClick = onTagClick
                        )
                    }
                }
            }
        }
    }
}
