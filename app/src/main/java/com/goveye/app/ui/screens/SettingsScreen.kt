package com.goveye.app.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.theme.ThemeViewModel
import com.goveye.app.ui.theme.padding

/**
 * Settings tab — appearance controls (D-18).
 *
 * Light/dark/system mode toggle and AMOLED switch. The color scheme is
 * fixed to Sky (grayscale + blue/white background) and not user-selectable.
 * Party colors are applied per-profile via a CompositionLocal override.
 */
@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel, modifier: Modifier = Modifier) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()

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
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
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
    }
}

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
            ThemeMode.SYSTEM -> "System"
        }
