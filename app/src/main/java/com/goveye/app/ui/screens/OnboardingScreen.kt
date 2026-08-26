package com.goveye.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.goveye.app.ui.screens.onboarding.MPsStep
import com.goveye.app.ui.screens.onboarding.OnboardingViewModel
import com.goveye.app.ui.screens.onboarding.PartiesStep
import com.goveye.app.ui.screens.onboarding.TagsStep
import kotlinx.coroutines.delay

/**
 * Onboarding flow - Wakely-style fade transitions between steps.
 *
 * Step 0: Welcome - fades in "Welcome to GovEye" + slogan, holds,
 *          then auto-advances to government selection.
 * Step 1: Government selection - card with UK flag, tap to select,
 *          checkmark + outline on selection, Continue button.
 *
 * After Continue, a fade-out animation plays before [onComplete]
 * is called, so the transition to the download/main screen is smooth.
 *
 * @param onComplete Called when the user finishes onboarding (taps Continue
 *   on the government selection step with a selection made). Receives the
 *   selected government code (e.g. "UK").
 * @param testMode When true, the Welcome step auto-advances faster and
 *   [onComplete] does not trigger a download (used by Settings "Test onboarding").
 */
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    testMode: Boolean = false,
    onGovernmentSelected: (() -> Unit)? = null
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    var currentStep by remember { mutableStateOf(0) }
    var fadingOut by remember { mutableStateOf(false) }
    var selectedGov by remember { mutableStateOf<String?>(null) }
    val fadeAnim = remember { Animatable(1f) }

    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val selectedParties by viewModel.selectedParties.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val partyLeaders by viewModel.partyLeaderInfos.collectAsStateWithLifecycle()
    val recommendedMps by viewModel.recommendedMpDetails.collectAsStateWithLifecycle()
    val tagGroupedMps by viewModel.tagGroupedMpDetails.collectAsStateWithLifecycle()
    val followedMpIds by viewModel.followedMpIds.collectAsStateWithLifecycle()
    val pagedMps = viewModel.pagedMps.collectAsLazyPagingItems()

    // When fadingOut is triggered, persist selections, animate alpha to 0,
    // then call onComplete
    LaunchedEffect(fadingOut) {
        if (fadingOut) {
            if (!testMode) {
                viewModel.persistSelections()
            }
            fadeAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
            onComplete(selectedGov ?: "UK")
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            fadeIn(animationSpec = tween(600, easing = LinearEasing)) togetherWith
                fadeOut(animationSpec = tween(400, easing = LinearEasing))
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .graphicsLayer { alpha = fadeAnim.value }
    ) { step ->
        when (step) {
            0 -> WelcomeStep(
                testMode = testMode,
                onAdvance = { currentStep = 1 }
            )

            1 -> GovernmentSelectionStep(
                selectedGovernment = selectedGov,
                onSelectedChange = { selectedGov = it },
                onContinue = {
                    // Start the download + request notification permission
                    // immediately when the user picks their country, so the
                    // seed DB downloads in the background while the user
                    // completes the remaining onboarding steps.
                    if (!testMode) {
                        onGovernmentSelected?.invoke()
                    }
                    currentStep = 2
                },
                onBack = { currentStep = 0 }
            )

            2 -> TagsStep(
                selectedTags = selectedTags,
                availableTags = availableTags,
                onTagToggle = viewModel::toggleTag,
                onContinue = { currentStep = 3 },
                onBack = { currentStep = 1 },
                onSkip = { currentStep = 3 }
            )

            3 -> PartiesStep(
                selectedParties = selectedParties,
                parties = parties,
                onPartyToggle = viewModel::toggleParty,
                onContinue = { currentStep = 4 },
                onBack = { currentStep = 2 },
                onSkip = { currentStep = 4 }
            )

            4 -> MPsStep(
                partyLeaders = partyLeaders,
                recommendedMps = recommendedMps,
                tagGroupedMps = tagGroupedMps,
                selectedTags = selectedTags,
                followedMpIds = followedMpIds,
                pagedMps = pagedMps,
                onFollowToggle = viewModel::toggleFollowMp,
                onFinish = { fadingOut = true },
                onBack = { currentStep = 3 },
                onSkip = { fadingOut = true }
            )
        }
    }
}

/**
 * Welcome step - fades in "Welcome to" + "GovEye" + slogan,
 * holds for a moment, then auto-advances.
 *
 * Adapted from Wakely's welcome.tsx:
 * - Fade in (800ms) -> hold (2000ms) -> auto-advance
 * - In testMode, hold is shorter (800ms) for quick testing
 */
@Composable
private fun WelcomeStep(testMode: Boolean, onAdvance: () -> Unit) {
    val fadeAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in
        fadeAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
        // Hold
        delay(if (testMode) 800L else 2000L)
        // Fade out
        fadeAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
        onAdvance()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 40.sp,
                modifier = Modifier.graphicsLayer { alpha = fadeAnim.value }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "GovEye",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 48.sp,
                modifier = Modifier.graphicsLayer { alpha = fadeAnim.value }
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Public servants,\nwatched by the private citizen",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.graphicsLayer { alpha = fadeAnim.value }
            )
        }
    }
}

/**
 * Government selection step - list of government cards.
 *
 * Currently only UK is available. Tapping a card selects it
 * (checkmark appears, outline animates in). Continue button
 * at the bottom proceeds to the download flow.
 *
 * Layout adapted from Wakely's interactive onboarding steps:
 * - Title + subtitle at top
 * - Content in the middle (scrollable list)
 * - Back + Continue buttons at the bottom
 */
@Composable
private fun GovernmentSelectionStep(
    selectedGovernment: String?,
    onSelectedChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
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
            text = "Choose your government",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Select the government you want to keep an eye on. " +
                "More countries coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Government cards
        GovernmentCard(
            name = "United Kingdom",
            downloadSize = "~580 MB",
            selected = selectedGovernment == "UK",
            onClick = { onSelectedChange("UK") },
            modifier = Modifier.fillMaxWidth()
        )

        // Coming soon placeholder
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83C\uDF0D",
                        fontSize = 24.sp
                    )
                }
                Column {
                    Text(
                        text = "More countries",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "US, Germany, France and more - coming soon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom buttons - Wakely style: Back (flex 1) + Continue (flex 2)
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
                enabled = selectedGovernment != null,
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * A selectable government card with a circular flag icon on the left,
 * the country name, download size, and a checkmark on the top-right
 * when selected. The card gets an outline when selected.
 *
 * The flag is drawn with Compose Canvas (Union Jack pattern) instead
 * of an emoji, so it fills the circle cleanly.
 */
@Composable
private fun GovernmentCard(
    name: String,
    downloadSize: String,
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
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular flag - Union Jack drawn with Canvas
                UkFlagCircle(
                    modifier = Modifier.size(48.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = downloadSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Checkmark - top right corner
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
 * Draws the Union Jack (UK flag) inside a circle using Compose Canvas.
 *
 * The flag is clipped to a circle and drawn with the classic Union Jack
 * pattern: blue background, white diagonals, red cross, white saltire
 * borders, and red diagonals.
 */
@Composable
private fun UkFlagCircle(modifier: Modifier = Modifier) {
    val unionJackBlue = Color(0xFF012169)
    val unionJackRed = Color(0xFFC8102E)
    val white = Color.White

    Box(
        modifier = modifier
            .clip(CircleShape)
            .drawWithContent {
                val w = size.width
                val h = size.height

                // Blue background
                drawRect(unionJackBlue)

                // White diagonals (St Andrew's saltire)
                val diagWidth = w * 0.2f
                drawLine(
                    color = white,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = diagWidth
                )
                drawLine(
                    color = white,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = diagWidth
                )

                // Red diagonals (offset from center - St Patrick's cross)
                val redDiagWidth = w * 0.067f
                // Top-left to bottom-right (red offset toward bottom-right)
                drawLine(
                    color = unionJackRed,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = redDiagWidth
                )
                drawLine(
                    color = unionJackRed,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = redDiagWidth
                )

                // White cross (St George's cross - vertical + horizontal)
                val crossWidth = w * 0.2f
                drawLine(
                    color = white,
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = crossWidth
                )
                drawLine(
                    color = white,
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = crossWidth
                )

                // Red cross (thinner, on top of white)
                val redCrossWidth = w * 0.067f
                drawLine(
                    color = unionJackRed,
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = redCrossWidth
                )
                drawLine(
                    color = unionJackRed,
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = redCrossWidth
                )
            }
    )
}
