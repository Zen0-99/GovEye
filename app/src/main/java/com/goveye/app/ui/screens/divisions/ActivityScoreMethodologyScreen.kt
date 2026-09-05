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
• Votes: 25% — based on the MP's vote participation rate (divisions voted in / total divisions)
• Questions: 20% — based on written and oral questions asked, annualized relative to house average
• Speeches: 20% — based on speeches made in debates, annualized relative to house average
• Committees: 15% — based on committee memberships (10+ committees = full marks)
• Finance: 20% — based on financial declarations (interests + expenses), annualized relative to house average

Normalization:
Votes use the raw participation rate (0-100%). Questions, Speeches, and Finance are annualized (count / years served) and compared to the house average rate — a rate equal to the average earns 50% of the component's weight, 2× the average earns 100%. Committees use an absolute ceiling (10+ = full marks). All components are capped at 100%.

The Performance Breakdown radar chart shows the same rate-vs-average percentages used in the Activity Score, so the two views are always consistent.

This is a transparency tool, not an editorial judgment. A higher score means more recorded activity, not better performance. An MP with a low score may be doing important work that isn't captured by these metrics.

Data sources: UK Parliament Commons/Lords Votes API, Hansard API, Committees API, Register of Financial Interests, IPSA Expense Claims.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
