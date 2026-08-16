package com.goveye.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.AppTheme
import com.goveye.app.domain.ThemeMode
import com.goveye.app.ui.theme.ThemeViewModel
import com.goveye.app.ui.theme.padding

/**
 * Settings tab — functional theme picker (D-18).
 *
 * 5 scheme options (Forest/Sky/Ember/Coral/Monet) as filter chips,
 * light/dark/system mode toggle, and AMOLED switch. All changes write
 * to [ThemeViewModel] → [com.goveye.app.data.preference.ThemePreferences]
 * and the theme updates live via animated transitions.
 */
@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel, modifier: Modifier = Modifier) {
    val currentTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isAmoled by themeViewModel.isAmoled.collectAsStateWithLifecycle()

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

        // --- Color scheme picker ---
        Text(
            text = "Color scheme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            AppTheme.entries.forEach { theme ->
                FilterChip(
                    selected = currentTheme == theme,
                    onClick = { themeViewModel.setAppTheme(theme) },
                    label = { Text(theme.displayName) },
                    leadingIcon = {
                        if (theme != AppTheme.MONET) {
                            Surface(
                                modifier = Modifier.size(16.dp).clip(CircleShape),
                                color = theme.previewColor
                            ) {}
                        }
                    }
                )
            }
        }

        // --- Theme mode picker ---
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.padding.medium)
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

        // --- AMOLED toggle ---
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
                onCheckedChange = { themeViewModel.setAmoled(it) },
                enabled = themeMode != ThemeMode.LIGHT
            )
        }
    }
}

private val AppTheme.displayName: String
    get() =
        when (this) {
            AppTheme.FOREST -> "Forest"
            AppTheme.SKY -> "Sky"
            AppTheme.EMBER -> "Ember"
            AppTheme.CORAL -> "Coral"
            AppTheme.MONET -> "Monet"
        }

private val AppTheme.previewColor: Color
    get() =
        when (this) {
            AppTheme.FOREST -> Color(0xFF386B46)
            AppTheme.SKY -> Color(0xFF446B8C)
            AppTheme.EMBER -> Color(0xFF8C4B3A)
            AppTheme.CORAL -> Color(0xFF6B5A80)
            AppTheme.MONET -> Color(0xFF8B6FA3)
        }

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
            ThemeMode.SYSTEM -> "System"
        }
