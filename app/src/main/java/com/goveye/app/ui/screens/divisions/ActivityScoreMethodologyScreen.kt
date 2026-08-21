package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScoreMethodologyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Score Methodology") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Parliamentary Activity Score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = """
This is a mechanical score computed from publicly available parliamentary data.

Weights:
• Votes: 40% — based on the MP's vote participation rate (divisions voted in / total divisions)
• Questions: 20% — based on written and oral questions asked
• Speeches: 20% — based on speeches made in debates
• Committees: 20% — based on committee memberships

Normalization:
Each component is normalized relative to the house average. A count equal to the average earns 50% of the component's weight. A count of 2× the average earns 100%. The score is capped at 100.

This is a transparency tool, not an editorial judgment. A higher score means more recorded activity, not better performance. An MP with a low score may be doing important work that isn't captured by these metrics.

Trait bars show the MP's percentile rank compared to other MPs in the same house (Commons or Lords). The 50th percentile means the MP is exactly average for that trait.

Data sources: UK Parliament Commons/Lords Votes API, Hansard API, Committees API.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
