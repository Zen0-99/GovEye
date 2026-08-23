package com.goveye.app.ui.screens

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * Light/dark/system mode toggle and AMOLED switch. The color scheme is
 * fixed to Sky (grayscale + blue/white background) and not user-selectable.
 * Party colors are applied per-profile via a CompositionLocal override.
 *
 * Notification preferences are per-MP (accessed via the bell icon on each
 * MP's profile header), not global — so no notification toggles here.
 *
 * Download settings include a WiFi-only toggle that controls whether
 * database updates happen exclusively over unmetered connections.
 */
@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel, modifier: Modifier = Modifier, onTestOnboarding: () -> Unit = {}) {
    val downloadSettingsViewModel: DownloadSettingsViewModel = hiltViewModel()

    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            placeholder = "Search settings…"
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
                .padding(MaterialTheme.padding.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // --- Theme mode picker ---
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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

        // --- AMOLED toggle (only visible when app is effectively dark) ---
        if (isEffectivelyDark) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AMOLED dark mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = isAmoled,
                    onCheckedChange = { themeViewModel.setAmoled(it) }
                )
            }
        }

        // --- Info cards toggle ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show info cards",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = showInfoCards,
                onCheckedChange = { themeViewModel.setShowInfoCards(it) }
            )
        }

        // --- Data / Download settings ---
        Text(
            text = "Data",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.padding.large)
        )

        // WiFi-only toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Download over WiFi only",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = wifiOnly,
                onCheckedChange = { enabled ->
                    downloadSettingsViewModel.setWifiOnly(enabled)
                    // Re-schedule the periodic update worker with the new
                    // network constraint so it takes effect immediately.
                    WorkScheduler.scheduleDatabaseUpdateCheck(context, wifiOnly = enabled)
                }
            )
        }

        // --- Debug / Testing ---
        Text(
            text = "Testing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.padding.large)
        )

        // Test onboarding button — shows the onboarding flow without
        // triggering a download. Useful for testing UI changes.
        OutlinedButton(
            onClick = onTestOnboarding,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test onboarding")
        }
    }
}

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
            ThemeMode.SYSTEM -> "System"
        }
