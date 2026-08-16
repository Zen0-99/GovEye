package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.ui.theme.padding

@Composable
fun DirectoryScreen(
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.pagedMps.collectAsLazyPagingItems()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search MPs by name…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            IconButton(onClick = {
                viewModel.setViewMode(
                    if (viewMode == DirectoryViewMode.LIST) DirectoryViewMode.GRID
                    else DirectoryViewMode.LIST
                )
            }) {
                Icon(
                    imageVector = if (viewMode == DirectoryViewMode.LIST) Icons.Outlined.GridView
                    else Icons.Outlined.ViewAgenda,
                    contentDescription = if (viewMode == DirectoryViewMode.LIST) "Grid view"
                    else "List view",
                )
            }
        }

        if (searchQuery.isNotBlank()) {
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No MPs found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(searchResults, key = { it.id }) { mp ->
                        MpListRow(
                            mp = mp,
                            onClick = { onNavigateToProfile(mp.id) },
                        )
                    }
                }
            }
        } else {
            val refreshState = lazyPagingItems.loadState.refresh
            when {
                refreshState is androidx.paging.LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                refreshState is androidx.paging.LoadState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Couldn't load MPs",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Check your connection and try again",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                refreshState is androidx.paging.LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No MPs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    when (viewMode) {
                        DirectoryViewMode.LIST -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = lazyPagingItems.itemKey { it.id },
                                ) { index ->
                                    val mp = lazyPagingItems[index]
                                    if (mp != null) {
                                        MpListRow(
                                            mp = mp,
                                            onClick = { onNavigateToProfile(mp.id) },
                                        )
                                    }
                                }
                            }
                        }
                        DirectoryViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = MaterialTheme.padding.medium,
                                    vertical = MaterialTheme.padding.small,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                            ) {
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = lazyPagingItems.itemKey { it.id },
                                ) { index ->
                                    val mp = lazyPagingItems[index]
                                    if (mp != null) {
                                        MpGridCard(
                                            mp = mp,
                                            onClick = { onNavigateToProfile(mp.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
