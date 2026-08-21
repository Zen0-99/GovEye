package com.goveye.app.ui.screens.party

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.flow.flowOf

@Composable
fun PartyMembersTab(pagedMps: PagingData<MpEntity>, onNavigateToProfile: (Int) -> Unit, modifier: Modifier = Modifier) {
    val lazyPagingItems: LazyPagingItems<MpEntity> =
        remember { flowOf(pagedMps) }.collectAsLazyPagingItems()

    if (lazyPagingItems.itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No MPs found for this party",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        )
    ) {
        items(lazyPagingItems.itemCount) { index ->
            val mp = lazyPagingItems[index]
            if (mp != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToProfile(mp.id) }
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.padding.medium)
                    ) {
                        Text(
                            text = mp.nameDisplayAs ?: mp.nameListAs,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        mp.constituencyName?.let {
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
    }
}
