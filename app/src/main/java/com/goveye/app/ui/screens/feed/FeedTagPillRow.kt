package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.TagWithCount

/**
 * Horizontally scrollable row of tag pills, for feed cards.
 *
 * Styled to match the MP profile [TopicsSection] TagChip — pill shape,
 * primary at 0.12 alpha background, tag name in primary/Medium.
 * Shows up to [maxTags] tags (top by hitCount, since the data is
 * already sorted descending).
 *
 * Includes edge-resistance nested scroll: when the horizontal scroll
 * reaches its edge, the remaining drag is consumed (up to a threshold)
 * before propagating to the parent [HorizontalPager]. This prevents
 * accidental tab swipes when the user is scrolling tags and reaches
 * the end — they have to drag further before the pager takes over.
 */
@Composable
fun FeedTagPillRow(
    tags: List<TagWithCount>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxTags: Int = 3
) {
    if (tags.isEmpty()) return
    val shape = RoundedCornerShape(20.dp)
    val scrollState = rememberScrollState()

    // Edge-resistance: consume drag at the scroll boundary so the parent
    // HorizontalPager doesn't immediately take over. The user must drag
    // past the threshold (in pixels) before the pager receives any delta.
    val resistanceThreshold = 120f
    val edgeResistance = remember {
        object : NestedScrollConnection {
            private var accumulatedDrag = 0f

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // The inner scroll consumed what it could. If there's
                // remaining horizontal delta, it means we're at the edge.
                if (available.x != 0f) {
                    // Accumulate the unconsumed drag. Only release to the
                    // parent after the threshold is exceeded.
                    accumulatedDrag += available.x
                    if (kotlin.math.abs(accumulatedDrag) > resistanceThreshold) {
                        // Threshold exceeded — let the parent have the overflow
                        val sign = if (accumulatedDrag > 0) 1f else -1f
                        val overflow = accumulatedDrag - sign * resistanceThreshold
                        accumulatedDrag = 0f
                        return androidx.compose.ui.geometry.Offset(overflow, 0f)
                    }
                    // Consume everything — the pager doesn't see it yet
                    return available
                }
                accumulatedDrag = 0f
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // Reset accumulator when a new gesture starts
                if (source == NestedScrollSource.Drag) {
                    // If the inner scroll can consume this, let it
                    val canScrollRight = scrollState.canScrollForward
                    val canScrollLeft = scrollState.canScrollBackward
                    if (available.x > 0 && canScrollRight) {
                        accumulatedDrag = 0f
                    } else if (available.x < 0 && canScrollLeft) {
                        accumulatedDrag = 0f
                    }
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Row(
        modifier = modifier
            .nestedScroll(edgeResistance)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.take(maxTags).forEach { tagWithCount ->
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { onTagClick(tagWithCount.tag) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tagWithCount.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
