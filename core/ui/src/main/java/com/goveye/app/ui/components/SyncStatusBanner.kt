package com.goveye.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.ui.theme.padding

@Composable
fun SyncStatusBanner(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val message = when (status) {
        SyncStatus.FRESH -> null
        SyncStatus.STALE -> "Updating..."
        SyncStatus.OFFLINE -> "Offline - showing cached data"
        SyncStatus.ERROR -> "Couldn't reach Parliament. Showing cached data."
        SyncStatus.EMPTY -> null
    }

    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.extraSmall),
        )
    }
}
