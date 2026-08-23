package com.goveye.app.ui.screens.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.screens.directory.MpListRow

/**
 * Step 5 — MPs: "Follow MPs" (final step).
 *
 * LazyColumn with 3 sections:
 * - "Party leaders" — horizontal Row(horizontalScroll) of leader cards (D-07).
 * - "Recommended for you" — tag-matched MPs ranked by recency-weighted hits (D-08).
 * - "All MPs" — paged list (collectAsLazyPagingItems) reusing MpListRow pattern.
 *
 * Follow toggle: icon-only IconButton with PersonAdd (unfollowed) / Check (followed).
 * Primary CTA: "Finish setup" (not "Continue") — triggers final fade-out + onComplete.
 *
 * Per UI-SPEC Section 3, Step 5.
 */
@Composable
fun MPsStep(
    partyLeaders: List<PartyLeaderInfo>,
    recommendedMps: List<Mp>,
    selectedTags: Set<String>,
    followedMpIds: Set<Int>,
    pagedMps: LazyPagingItems<Mp>,
    onFollowToggle: (Int) -> Unit,
    onFinish: () -> Unit,
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
            text = "Follow MPs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Party leaders and MPs who match your topics are recommended first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Content — LazyColumn with 3 sections
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Party leaders section — horizontal scroll
            if (partyLeaders.isNotEmpty()) {
                item {
                    Text(
                        text = "Party leaders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        partyLeaders.forEach { leader ->
                            PartyLeaderCard(
                                leader = leader,
                                isFollowed = leader.memberId in followedMpIds,
                                onFollowToggle = { onFollowToggle(leader.memberId) }
                            )
                        }
                    }
                }
            }

            // Recommended for you section
            if (selectedTags.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Recommended for you",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (recommendedMps.isEmpty()) {
                    item {
                        Text(
                            text = "Select topics to see MP recommendations",
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
                        items = recommendedMps,
                        key = { "rec-${it.id}" }
                    ) { mp ->
                        RecommendedMpRow(
                            mp = mp,
                            isFollowed = mp.id in followedMpIds,
                            onFollowToggle = { onFollowToggle(mp.id) }
                        )
                    }
                }
            } else if (partyLeaders.isNotEmpty()) {
                // No tags selected — show hint for recommended section
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Recommended for you",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Text(
                        text = "Select topics to see MP recommendations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            // All MPs section — paged list
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "All MPs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Paged list items
            items(
                count = pagedMps.itemCount,
                key = pagedMps.itemKey { "all-${it.id}" },
                contentType = { "mp_row" }
            ) { index ->
                val mp = pagedMps[index]
                if (mp != null) {
                    MpListRowWithFollow(
                        mp = mp,
                        isFollowed = mp.id in followedMpIds,
                        onFollowToggle = { onFollowToggle(mp.id) }
                    )
                }
            }

            // Paging load state — append (bottom of list)
            val appendState = pagedMps.loadState.append
            if (appendState is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (appendState is LoadState.Error) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Couldn't load more MPs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { pagedMps.retry() }) {
                            Text("Retry")
                        }
                    }
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

        // Bottom buttons — Back (weight 1) + Finish setup (weight 2)
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
                onClick = onFinish,
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Finish setup")
            }
        }
    }
}

@Composable
private fun PartyLeaderCard(leader: PartyLeaderInfo, isFollowed: Boolean, onFollowToggle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp)
    ) {
        MpAvatar(
            thumbnailUrl = null,
            displayName = leader.name,
            partyColorHex = leader.partyBackgroundColour,
            size = 48.dp,
            borderWidth = 2.dp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = leader.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = leader.partyAbbreviation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FollowToggleButton(
            name = leader.name,
            isFollowed = isFollowed,
            onToggle = onFollowToggle
        )
    }
}

@Composable
private fun RecommendedMpRow(mp: Mp, isFollowed: Boolean, onFollowToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MpAvatar(
            thumbnailUrl = mp.thumbnailUrl,
            displayName = mp.nameDisplayAs,
            partyColorHex = mp.party?.backgroundColour,
            size = 48.dp,
            borderWidth = 2.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mp.nameDisplayAs,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mp.party?.abbreviation ?: ""} · ${mp.constituency?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FollowToggleButton(
            name = mp.nameDisplayAs,
            isFollowed = isFollowed,
            onToggle = onFollowToggle
        )
    }
}

@Composable
private fun MpListRowWithFollow(mp: Mp, isFollowed: Boolean, onFollowToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MpAvatar(
            thumbnailUrl = mp.thumbnailUrl,
            displayName = mp.nameDisplayAs,
            partyColorHex = mp.party?.backgroundColour,
            size = 48.dp,
            borderWidth = 2.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mp.nameDisplayAs,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mp.party?.abbreviation ?: ""} · ${mp.constituency?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FollowToggleButton(
            name = mp.nameDisplayAs,
            isFollowed = isFollowed,
            onToggle = onFollowToggle
        )
    }
}

@Composable
private fun FollowToggleButton(name: String, isFollowed: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = if (isFollowed) Icons.Outlined.Check else Icons.Outlined.PersonAdd,
            contentDescription = if (isFollowed) "Following $name" else "Follow $name",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
