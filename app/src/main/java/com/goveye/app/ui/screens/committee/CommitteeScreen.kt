package com.goveye.app.ui.screens.committee

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.DetailTopBarConfig
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding

@Composable
fun CommitteeScreen(
    committeeId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: CommitteeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(committeeId) {
        viewModel.loadCommittee(committeeId)
    }

    val committee = uiState.committee

    ConfigureDetailTopBar(
        config = DetailTopBarConfig(
            title = "",
            onBack = onBack,
            accentColor = null,
            iconTint = null,
            actions = emptyList()
        )
    )

    if (uiState.isLoading && committee == null) {
        DelayedSpinner(modifier = modifier)
    } else if (committee == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Committee not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                CommitteeHeader(
                    committee = committee,
                    memberCount = uiState.members.size,
                    contentTopPadding = contentTopPadding
                )
            }

            // Purpose
            committee.purpose?.let { purpose ->
                item {
                    CommitteeInfoCard(
                        title = "Purpose",
                        body = purpose
                    )
                }
            }

            // Contact
            if (committee.contactEmail != null || committee.contactPhone != null || committee.contactAddress != null) {
                item {
                    CommitteeContactCard(
                        email = committee.contactEmail,
                        phone = committee.contactPhone,
                        address = committee.contactAddress
                    )
                }
            }

            // Members
            item {
                Text(
                    text = "Members (${uiState.members.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.padding.large,
                            vertical = MaterialTheme.padding.medium
                        )
                )
            }

            items(uiState.members.size) { index ->
                val mp = uiState.members[index]
                CommitteeMemberRow(
                    mp = mp,
                    onClick = { onNavigateToProfile(mp.id) }
                )
            }
        }
    }
}

@Composable
private fun CommitteeHeader(
    committee: Committee,
    memberCount: Int,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.7f),
                        accentColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = contentTopPadding + MaterialTheme.padding.small, bottom = MaterialTheme.padding.medium)
        ) {
            Text(
                text = committee.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val subtitleParts = buildList {
                committee.house?.let { add(it) }
                committee.categoryName?.let { add(it) }
                if (committee.isActive) add("Active") else add("Inactive")
                add("$memberCount member${if (memberCount != 1) "s" else ""}")
            }
            Text(
                text = subtitleParts.joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun CommitteeInfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.padding.small)
            )
        }
    }
}

@Composable
private fun CommitteeContactCard(email: String?, phone: String?, address: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            Text(
                text = "Contact",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Clickable email
            email?.takeIf { it.isNotBlank() }?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it")))
                            }
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Clickable phone
            phone?.takeIf { it.isNotBlank() }?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")))
                            }
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Address (not clickable)
            address?.takeIf { it.isNotBlank() }?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitteeMemberRow(mp: Mp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            MpAvatar(
                thumbnailUrl = mp.thumbnailUrl,
                displayName = mp.nameDisplayAs ?: mp.nameListAs,
                partyColorHex = mp.party?.backgroundColour,
                size = 40.dp,
                borderWidth = 2.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mp.nameDisplayAs ?: mp.nameListAs,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                mp.constituency?.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
