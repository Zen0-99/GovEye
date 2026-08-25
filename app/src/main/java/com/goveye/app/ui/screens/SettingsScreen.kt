package com.goveye.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.settings.DownloadSettingsViewModel
import com.goveye.app.ui.theme.ThemeViewModel
import com.goveye.app.ui.theme.padding
import com.goveye.app.work.WorkScheduler

/**
 * Settings tab — appearance controls (D-18) and download settings.
 *
 * Layout follows the grouped-section pattern (Wakely-inspired): each
 * category is a rounded [Surface] container with hairline dividers
 * between rows. Section headers are uppercase, small, secondary-coloured.
 *
 * The color scheme is fixed to Sky (grayscale + blue/white background)
 * and not user-selectable. Party colors are applied per-profile via a
 * CompositionLocal override.
 *
 * Notification preferences are per-MP (accessed via the bell icon on each
 * MP's profile header), not global — so no notification toggles here.
 */
@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel, modifier: Modifier = Modifier, onTestOnboarding: () -> Unit = {}) {
    val downloadSettingsViewModel: DownloadSettingsViewModel = hiltViewModel()

    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = false
        )
    )
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()
    val showInfoCards by themeViewModel.showInfoCards.collectAsStateWithLifecycle()
    val wifiOnly by downloadSettingsViewModel.wifiOnly.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // AMOLED toggle only makes sense when the app is actually rendering dark.
    // In SYSTEM mode, that depends on the system dark setting.
    val isEffectivelyDark =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.padding.large)
                .padding(bottom = MaterialTheme.padding.extraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        // --- Appearance ---
        SettingsSection(title = "Appearance") {
            // Theme mode picker
            SettingsRow(label = "Theme") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { themeViewModel.setThemeMode(mode) },
                            label = { Text(mode.displayName) }
                        )
                    }
                }
            }

            if (isEffectivelyDark) {
                SettingsDivider()
                SettingsToggleRow(
                    label = "AMOLED dark mode",
                    checked = isAmoled,
                    onCheckedChange = { themeViewModel.setAmoled(it) }
                )
            }

            SettingsDivider()
            SettingsToggleRow(
                label = "Show info cards",
                checked = showInfoCards,
                onCheckedChange = { themeViewModel.setShowInfoCards(it) }
            )
        }

        // --- Data ---
        SettingsSection(title = "Data") {
            SettingsToggleRow(
                label = "Download over WiFi only",
                checked = wifiOnly,
                onCheckedChange = { enabled ->
                    downloadSettingsViewModel.setWifiOnly(enabled)
                    // Re-schedule the periodic update worker with the new
                    // network constraint so it takes effect immediately.
                    WorkScheduler.scheduleDatabaseUpdateCheck(context, wifiOnly = enabled)
                }
            )
        }

        // --- Testing ---
        SettingsSection(title = "Testing") {
            SettingsRow(
                label = "Test onboarding",
                onClick = onTestOnboarding
            )
        }
    }
}

// --- Wakely-inspired grouped section components ---

/**
 * A grouped settings section: uppercase header + rounded [Surface] container.
 *
 * Insert [SettingsDivider] between rows manually (mirrors Wakely's hairline
 * dividers — only between items, not before the first or after the last).
 */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.merge(
                androidx.compose.ui.text.TextStyle(letterSpacing = 1.5.sp)
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = MaterialTheme.padding.mediumSmall,
                bottom = MaterialTheme.padding.small
            )
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * Hairline divider inset 16dp from each side, placed between rows in a section.
 */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

/**
 * A settings row with a label on the left and custom [trailing] content on the right.
 *
 * For navigation-style rows, pass [onClick] to make the whole row tappable.
 */
@Composable
private fun SettingsRow(label: String, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

/**
 * A settings row with a label on the left and a [Switch] on the right.
 */
@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
            ThemeMode.SYSTEM -> "System"
        }
