package com.goveye.app.ui.screens.party

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.flow.Flow

@Composable
fun PartyMembersTab(
    pagedMps: Flow<PagingData<MpEntity>>,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyPagingItems: LazyPagingItems<MpEntity> = pagedMps.collectAsLazyPagingItems()

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
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        items(lazyPagingItems.itemCount, key = lazyPagingItems.itemKey { it.id }) { index ->
            val mp = lazyPagingItems[index]
            if (mp != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfile(mp.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.padding.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
                    ) {
                        MpAvatar(
                            thumbnailUrl = mp.thumbnailUrl,
                            displayName = mp.nameDisplayAs ?: mp.nameListAs,
                            partyColorHex = mp.partyBackgroundColour,
                            size = 40.dp,
                            borderWidth = 2.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mp.nameDisplayAs ?: mp.nameListAs,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
}
