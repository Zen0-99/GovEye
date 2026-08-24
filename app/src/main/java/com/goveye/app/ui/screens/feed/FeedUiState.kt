package com.goveye.app.ui.screens.feed

import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.GovernmentPublication
import com.goveye.app.domain.model.Legislation
import com.goveye.app.domain.model.WrittenStatement

/**
 * Card type enum for type filtering in the feed and government tab.
 */
enum class CardType {
    DIVISION,
    PUBLICATION,
    STATEMENT,
    LEGISLATION,
    FINANCIAL,
    SPEECH
}

/**
 * Sealed interface for mixed feed items — supports divisions, publications,
 * statements, and legislation in a single chronological feed.
 *
 * Each subtype exposes a [date] (ISO format string) for chronological grouping
 * and an [id] for LazyColumn keying.
 */
sealed interface FeedItem {
    val date: String
    val id: Int
    val typePrefix: String
    val cardType: CardType

    data class DivisionItem(val division: Division, val tags: List<String> = emptyList()) : FeedItem {
        override val date: String get() = division.date
        override val id: Int get() = division.id
        override val typePrefix: String = "division"
        override val cardType: CardType = CardType.DIVISION
    }

    data class PublicationItem(val publication: GovernmentPublication, val tags: List<String> = emptyList()) :
        FeedItem {
        override val date: String get() = publication.firstPublishedAt
        override val id: Int get() = publication.id
        override val typePrefix: String = "publication"
        override val cardType: CardType = CardType.PUBLICATION
    }

    data class StatementItem(val statement: WrittenStatement, val tags: List<String> = emptyList()) : FeedItem {
        override val date: String get() = statement.dateMade
        override val id: Int get() = statement.id
        override val typePrefix: String = "statement"
        override val cardType: CardType = CardType.STATEMENT
    }

    data class LegislationItem(val legislation: Legislation, val tags: List<String> = emptyList()) : FeedItem {
        override val date: String get() = legislation.date
        override val id: Int get() = legislation.id
        override val typePrefix: String = "legislation"
        override val cardType: CardType = CardType.LEGISLATION
    }

    /**
     * A followed MP's income or expense entry rendered as a [UnifiedFinancialCard]
     * in the feed (with a profile icon). [isIncome] distinguishes the two.
     */
    data class FinancialItem(
        val memberId: Int,
        val memberName: String,
        val memberPartyColorHex: String?,
        val memberPhotoUrl: String?,
        val amount: String,
        val whoOrWhere: String,
        val description: String,
        val category: String,
        val isIncome: Boolean,
        override val date: String,
        override val id: Int = listOf(memberId, amount, date).hashCode(),
        val tags: List<String> = emptyList()
    ) : FeedItem {
        override val typePrefix: String = "financial"
        override val cardType: CardType = CardType.FINANCIAL
    }

    /**
     * A followed MP's speech from a debate, rendered as a [FeedSpeechCard]
     * in the feed (profile icon + 3 lines of speech text + tags inherited
     * from the parent division).
     */
    data class SpeechItem(
        val memberId: Int,
        val memberName: String,
        val memberPartyColorHex: String?,
        val memberPhotoUrl: String?,
        val speechText: String,
        val divisionId: Int,
        val divisionTitle: String,
        override val date: String,
        override val id: Int = listOf(memberId, divisionId).hashCode(),
        val tags: List<String> = emptyList()
    ) : FeedItem {
        override val typePrefix: String = "speech"
        override val cardType: CardType = CardType.SPEECH
    }
}

/**
 * A group of feed items sharing the same date, with a relative date header.
 */
data class FeedDateGroup(
    val dateHeader: String, // "Today", "Yesterday", "20 August 2026"
    val dateKey: String, // ISO date "2026-08-20" for grouping
    val items: List<FeedItem>
)

/**
 * Feed UI state — consumed by FeedScreen.
 */
data class FeedUiState(
    val dateGroups: List<FeedDateGroup> = emptyList(),
    val followedMemberIds: Set<Int> = emptySet(),
    val divisionsWithFollowedVotes: Set<Int> = emptySet(),
    val divisionTags: Map<Int, List<String>> = emptyMap(),
    val announcementTags: Map<String, List<String>> = emptyMap(),
    val followingOnly: Boolean = false,
    val searchQuery: String = "",
    val houseFilter: Int = 0,
    val tagFilter: Set<String> = emptySet(),
    val sourceFilter: Set<String> = emptySet(),
    val departmentFilter: Set<String> = emptySet(),
    val typeFilter: Set<CardType> = emptySet(),
    val currentRecess: RecessDateEntity? = null,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val isRecessEmpty: Boolean = false,
    val recentDivisionsForRecess: List<Division> = emptyList(),
    val hasMore: Boolean = false,
    val totalDivisions: Int = 0
)
