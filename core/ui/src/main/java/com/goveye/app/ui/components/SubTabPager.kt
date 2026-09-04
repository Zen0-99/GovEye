package com.goveye.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A single sub-tab definition. [label] is the text shown on the tab.
 * [badgeCount] is an optional count pill shown next to the label.
 */
data class SubTab(val label: String, val badgeCount: Int? = null)

/**
 * Global sub-tab pager — the single, shared implementation for any screen
 * that has a row of tabs above swipeable content.
 *
 * Encapsulates the pattern used by MpProfileScreen, DirectoryScreen,
 * PartyScreen, and CouncilScreen:
 * - [ScrollableTabRow] (or [TabRow] when [scrollable] = false) with tab items
 * - [HorizontalPager] that only composes the current page (no offscreen work)
 * - Instant jump on tab click ([scrollToPage], not [animateScrollToPage])
 *
 * Callers provide the list of [SubTab] definitions and a [content] lambda
 * that renders the page at a given index. The pager state is created
 * internally and hoisted via [onPageChange] if the caller needs to react
 * to tab switches (e.g., to update the search bar config).
 *
 * @param tabs the tab definitions — labels and optional badge counts
 * @param scrollable whether the tab row should scroll (true for 4+ tabs)
 * @param edgePadding horizontal padding for the tab row edges
 * @param onPageChange called when the current page changes (tab switch)
 * @param content renders the page at the given index. Only the current
 *   page is composed — offscreen pages are skipped.
 */
@Composable
fun SubTabPager(
    tabs: List<SubTab>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    scrollable: Boolean = true,
    edgePadding: androidx.compose.ui.unit.Dp = 16.dp,
    onPageChange: (Int) -> Unit = {},
    content: @Composable (pageIndex: Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Notify caller when the page changes
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row
        val tabRowModifier = Modifier.fillMaxWidth()
        if (scrollable) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                // Transparent so the shell-level gradient (on profile/party
                // screens) shows through. On screens without a gradient,
                // the app background shows through instead.
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = edgePadding,
                modifier = tabRowModifier
            ) {
                tabs.forEachIndexed { index, tab ->
                    SubTabItem(
                        tab = tab,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.scrollToPage(index) }
                        }
                    )
                }
            }
        } else {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = tabRowModifier
            ) {
                tabs.forEachIndexed { index, tab ->
                    SubTabItem(
                        tab = tab,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.scrollToPage(index) }
                        }
                    )
                }
            }
        }

        // Pager — compose current + adjacent pages so mid-scroll shows both
        // pages without pop-in. beyondViewportPageCount=1 keeps the next/prev
        // pages composed, enabling smooth transitions. Pages further away are
        // skipped to avoid loading all tabs at once.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            beyondViewportPageCount = 1
        ) { page ->
            content(page)
        }
    }
}

@Composable
private fun SubTabItem(tab: SubTab, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            if (tab.badgeCount != null) {
                TabTextWithBadge(text = tab.label, badgeCount = tab.badgeCount)
            } else {
                Text(
                    text = tab.label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    )
}
