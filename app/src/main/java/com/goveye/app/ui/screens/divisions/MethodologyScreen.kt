package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
fun MethodologyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Methodology") },
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
                text = "Rebellion Rate Methodology",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = """
A rebellion is counted when an MP votes against the majority of their own party in a division.

Step-by-step computation:

1. For each division where the MP voted (Aye or No), find all votes cast by members of the MP's party.

2. Count the Ayes and Noes among the MP's party members.

3. The party majority position is whichever side has more votes (Ayes > Noes → party line is Aye; Noes > Ayes → party line is No).

4. If the MP voted opposite to the party majority, this is counted as a rebellion.

5. If the party is evenly split (equal Ayes and Noes), the division is excluded — no rebellion is possible without a clear party line.

6. The rebellion rate is the number of rebellions divided by the total number of divisions where the MP voted.

This is a mechanical calculation based on publicly available parliamentary data. No editorial judgment is made about whether a rebellion is justified. A higher rebellion rate means the MP frequently votes against their party's majority position, not that they are "good" or "bad."

Data source: UK Parliament Commons Votes API and Lords Votes API.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
