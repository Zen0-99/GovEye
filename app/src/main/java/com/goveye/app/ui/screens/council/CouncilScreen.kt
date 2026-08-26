package com.goveye.app.ui.screens.council

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.DetailTopBarConfig
import com.goveye.app.ui.screens.directory.MpListRow

@Composable
fun CouncilScreen(
    councilId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: CouncilViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(councilId) {
        viewModel.loadCouncil(councilId)
    }

    val council = uiState.council

    ConfigureDetailTopBar(
        config = DetailTopBarConfig(
            title = council?.name ?: "Council",
            onBack = onBack
        )
    )

    if (uiState.isLoading && council == null) {
        DelayedSpinner(modifier = modifier.padding(top = contentTopPadding))
        return
    }

    if (council == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(top = contentTopPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Council not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val uriHandler = LocalUriHandler.current
    val website = council.website
    val contactEmail = council.contactEmail
    val contactPhone = council.contactPhone
    val twitter = council.twitter
    val mps = uiState.matchingMps

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(top = contentTopPadding),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header
        item {
            CouncilHeader(
                name = council.name,
                type = council.localAuthorityType,
                region = council.region
            )
        }

        // Contact info section
        if (!website.isNullOrBlank()) {
            item {
                InfoRow(
                    icon = { Icon(Icons.Outlined.Language, contentDescription = "Website") },
                    label = "Website",
                    value = website,
                    onClick = { uriHandler.openUri(website) }
                )
            }
        }

        if (!contactEmail.isNullOrBlank()) {
            item {
                InfoRow(
                    icon = { Icon(Icons.Outlined.Mail, contentDescription = "Email") },
                    label = "Email",
                    value = contactEmail,
                    onClick = { uriHandler.openUri("mailto:$contactEmail") }
                )
            }
        }

        if (!contactPhone.isNullOrBlank()) {
            item {
                InfoRow(
                    icon = { Icon(Icons.Outlined.Phone, contentDescription = "Phone") },
                    label = "Phone",
                    value = contactPhone,
                    onClick = { uriHandler.openUri("tel:$contactPhone") }
                )
            }
        }

        if (!twitter.isNullOrBlank()) {
            item {
                InfoRow(
                    icon = { Icon(Icons.Outlined.Language, contentDescription = "Twitter") },
                    label = "Twitter",
                    value = "@$twitter",
                    onClick = { uriHandler.openUri("https://twitter.com/$twitter") }
                )
            }
        }

        // MPs section header
        item {
            Text(
                text = "MPs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // MPs list
        if (mps.isEmpty()) {
            item {
                Text(
                    text = "No MPs found for this council.\n" +
                        "Constituency names may not overlap with the council name.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        } else {
            items(mps, key = { it.id }, contentType = { "mp_row" }) { mp ->
                MpListRow(
                    mp = mp.toDomain(),
                    onClick = { onNavigateToProfile(mp.id) }
                )
            }
        }
    }
}

@Composable
private fun CouncilHeader(name: String, type: String?, region: String?) {
    val typeLabel = when (type) {
        "MD" -> "Metropolitan District"
        "NMD" -> "Non-Metropolitan District"
        "UA" -> "Unitary Authority"
        "LBO" -> "London Borough"
        "CC" -> "County Council"
        "MBC" -> "Metropolitan Borough"
        else -> type ?: "Local Authority"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (region != null) {
                    Text(
                        text = region,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun com.goveye.app.data.local.entity.MpEntity.toDomain(): com.goveye.app.domain.model.Mp =
    com.goveye.app.domain.model.Mp(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = nameFullTitle,
        gender = gender,
        party = com.goveye.app.domain.model.Party(
            partyId,
            partyName,
            partyAbbreviation,
            partyBackgroundColour,
            partyForegroundColour
        ),
        constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
        house = house,
        membershipStartDate = membershipStartDate,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl
    )
