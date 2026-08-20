package com.goveye.app.ui.screens.feed

import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.domain.model.Division

/**
 * A group of divisions sharing the same date, with a relative date header.
 */
data class FeedDateGroup(
    val dateHeader: String,       // "Today", "Yesterday", "20 August 2026"
    val dateKey: String,          // ISO date "2026-08-20" for grouping
    val divisions: List<Division>,
)

/**
 * Feed UI state — consumed by FeedScreen.
 */
data class FeedUiState(
    val dateGroups: List<FeedDateGroup> = emptyList(),
    val followedMemberIds: Set<Int> = emptySet(),
    val divisionsWithFollowedVotes: Set<Int> = emptySet(),
    val followingOnly: Boolean = false,
    val searchQuery: String = "",
    val houseFilter: Int = 0,
    val currentRecess: RecessDateEntity? = null,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val isRecessEmpty: Boolean = false,
    val recentDivisionsForRecess: List<Division> = emptyList(),
)
