package com.goveye.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Shared skeleton loading components for content-heavy screens.
//
// Each skeleton mimics the structure of its corresponding card:
// - FeedCardSkeleton — 16:9 image area + title + subtitle + source/date row
// - MpRowSkeleton — 48dp avatar + name line + party/constituency line
//
// All skeletons use a simple alpha pulse (0.3 → 0.6, 800ms) — no shimmer
// gradient mask, keeps it lightweight for short load times.
//
// Skeletons are wrapped in SkeletonScreen which delays display by 500ms
// (matching DelayedSpinner) so fast loads don't flash a skeleton.

/** Alpha pulse animation value shared by all skeleton placeholders. */
@Composable
private fun skeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )
    return alpha
}

/**
 * A single skeleton placeholder block — a rounded rectangle with the given
 * dimensions and animated alpha.
 */
@Composable
private fun SkeletonBlock(width: Dp, height: Dp, alpha: Float, modifier: Modifier = Modifier, cornerRadius: Dp = 4.dp) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor.copy(alpha = alpha))
    )
}

/**
 * Skeleton card that mimics [UnifiedFeedCard] structure:
 * 1. 16:9 image placeholder (full width, aspect ratio matches real cards)
 * 2. Title line (full width, 18dp)
 * 3. Subtitle line (60% width, 14dp)
 * 4. Source (80dp) + Date (60dp) row
 *
 * The 16:9 aspect ratio is critical — most feed cards are tall because of
 * the image/icon placeholder area. Using a fixed height (e.g. 140dp) makes
 * the skeleton shorter than the real card, causing a visual jump when
 * content loads.
 */
@Composable
fun FeedCardSkeleton(alpha: Float, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(16.dp)
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // 1. Image placeholder — 16:9 aspect ratio (matches UnifiedFeedCard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(baseColor.copy(alpha = alpha * 0.5f))
        )
        // 2-4. Content area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title line
            SkeletonBlock(width = 240.dp, height = 18.dp, alpha = alpha)
            // Subtitle line (shorter)
            SkeletonBlock(width = 160.dp, height = 14.dp, alpha = alpha)
            // Source + date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBlock(width = 80.dp, height = 12.dp, alpha = alpha)
                SkeletonBlock(width = 60.dp, height = 12.dp, alpha = alpha)
            }
        }
    }
}

/**
 * Skeleton row that mimics the followed-MP card structure:
 * 48dp avatar circle + name line (titleSmall) + party/constituency line (bodySmall).
 *
 * Used by FollowingScreen and DirectoryScreen Officials tab.
 */
@Composable
fun MpRowSkeleton(alpha: Float, modifier: Modifier = Modifier) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle (48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(baseColor.copy(alpha = alpha))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Name line
                SkeletonBlock(width = 160.dp, height = 16.dp, alpha = alpha)
                // Party · Constituency line
                SkeletonBlock(width = 200.dp, height = 12.dp, alpha = alpha)
            }
        }
    }
}

/**
 * Full-screen skeleton list — shows [itemCount] skeleton cards in a LazyColumn.
 *
 * Delays display by [delayMs] (default 500ms, matching [DelayedSpinner]) so
 * fast loads don't flash a skeleton. If loading finishes before the delay,
 * the skeleton never appears.
 *
 * @param cardType The skeleton card style — [SkeletonCardType.FEED] for
 *   image-on-top cards (feed, government tab), [SkeletonCardType.MP_ROW]
 *   for horizontal avatar+text rows (following, officials tab).
 * @param itemCount Number of skeleton cards to show (default 6).
 * @param contentPadding Padding for the LazyColumn (defaults to feed-style
 *   12dp horizontal + 8dp vertical).
 * @param spacing Spacing between skeleton cards (default 8dp).
 */
@Composable
fun SkeletonScreen(
    cardType: SkeletonCardType,
    itemCount: Int = 6,
    delayMs: Long = 500L,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    spacing: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }
    if (!visible) return

    val alpha = skeletonAlpha()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(count = itemCount, key = { "skeleton-$it" }) { index ->
            when (cardType) {
                SkeletonCardType.FEED -> FeedCardSkeleton(alpha = alpha)
                SkeletonCardType.MP_ROW -> MpRowSkeleton(alpha = alpha)
            }
        }
    }
}

/** Type of skeleton card to show in [SkeletonScreen]. */
enum class SkeletonCardType {
    /** Image-on-top card — matches [UnifiedFeedCard] (feed, government tab). */
    FEED,

    /** Horizontal avatar + text row — matches FollowedMpCard (following, officials). */
    MP_ROW
}
